package com.crn.resource.config;

import com.crn.common.exception.CrnCommonException;
import com.crn.resource.filter.TenantContext;
import com.crn.resource.model.Tenant;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.stella.common.service.exception.ErrorType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;

@Component
public class MongoDataSources {
    private static final Logger LOGGER = LogManager.getLogger(MongoDataSources.class);
    @Value("${spring.data.mongodb.host}")
    private String host;
    @Value("${spring.data.mongodb.port}")
    private String port;
    @Value("${spring.data.mongodb.authentication-database}")
    private String authenticationDatabase;
    @Value("${spring.data.mongodb.username}")
    private String username;
    @Value("${spring.data.mongodb.password}")
    private String password;
    @Value("${spring.data.mongodb.database}")
    private String dbName;
    @Value("${spring.data.mongodb.uri}")
    private String dbUri;
    @Value("${prodEnv}")
    private boolean prodEnvironment;
    private static final String ID = "_id";

    /**
     * Key: String tenant tenantId
     * Value: TenantDatasource
     */
    private Map<String, TenantDatasource> tenantClients;
    private HashMap<String, String> allTenantInfo = new LinkedHashMap<>();

    private Map<String, Tenant> allTenantsBasicInformation = new LinkedHashMap<>();
    private final DataSourceProperties dataSourceProperties;

    public MongoDataSources(final DataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }

    /**
     * Initialize all mongo datasource
     */
    @PostConstruct
    @Lazy
    public void initTenant() {
        tenantClients = new HashMap<>();
        tenantClients = loadServiceDatasources();
        loadAllTenantsInfo(allTenantInfo, tenantClients);
    }

    private void loadAllTenantsInfo(final HashMap<String, String> allTenantInfo,
            Map<String, TenantDatasource> tenantClients) {
        tenantClients.keySet().forEach(key -> {
            final MongoClient mongoClient = tenantClients.get(key).getClient();
            final String tenantDatabaseName = tenantClients.get(key).getTenantDatabase();
            final boolean isParentTenant = tenantClients.get(key).isParentTenant();
            final boolean isCommonTenant = tenantClients.get(key).isCommonTenant();
            if (isParentTenant) {
                final MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, tenantDatabaseName);
                final Aggregation aggregation = Aggregation.newAggregation(
                        project("_id", "name", "loginUrl"));
                final AggregationResults<Tenant> results = mongoTemplate.aggregate(aggregation, "tenant", Tenant.class);
                if (org.apache.commons.collections.CollectionUtils.isNotEmpty(results.getMappedResults())) {
                    results.getMappedResults().forEach(tenant -> {
                        allTenantsBasicInformation.put(tenant.getId(), tenant);
                        allTenantInfo.put(tenant.getId(), key);
                    });
                }
            } else if (isCommonTenant) {
                allTenantInfo.put(key, key);
            }

        });
    }

    /**
     * Default Database name for spring initialization. It is used to be injected
     * into the constructor of MultiTenantMongoDBFactory.
     *
     * @return String of default authenticationDatabase.
     */
    // @Bean
    // public String databaseName() {
    //
    // return
    // applicationProperties.getDatasourceDefault().getAuthenticationDatabase();
    // }

    /**
     * Default Mongo Connection for spring initialization.
     * It is used to be injected into the constructor of MultiTenantMongoDBFactory.
     */
    @Bean
    public MongoClient getMongoClient() {
        if (prodEnvironment) {
            return MongoClients.create(dbUri);
        } else {
            MongoCredential credential = MongoCredential.createCredential(username, authenticationDatabase,
                    password.toCharArray());
            return MongoClients.create(MongoClientSettings.builder()
                    .applyToClusterSettings(builder -> builder
                            .hosts(Collections.singletonList(new ServerAddress(host, Integer.parseInt(port)))))
                    .credential(credential)
                    .build());
        }
    }

    /**
     * This will get called for each DB operations
     *
     * @return MongoDatabase
     */
    public MongoDatabase mongoDatabaseCurrentTenantResolver() {
        try {
            final String tenantId = TenantContext.getTenantId();
            if (null == tenantClients.get(tenantId)) {
                if (null != allTenantInfo.get(tenantId)) {
                    final String databaseName = tenantClients.get(allTenantInfo.get(tenantId)).getDatabaseName();
                    return tenantClients.get(allTenantInfo.get(tenantId)).getClient().getDatabase(databaseName);
                } else {
                    loadAllTenantsInfo(allTenantInfo, tenantClients);
                    if (null != allTenantInfo.get(tenantId)) {
                        final String databaseName = tenantClients.get(allTenantInfo.get(tenantId)).getDatabaseName();
                        return tenantClients.get(allTenantInfo.get(tenantId)).getClient().getDatabase(databaseName);
                    } else {
                        throw new CrnCommonException(ErrorType.NOT_FOUND, "Tenant with given Id does not exists");
                    }
                }
            }
            final String databaseName = tenantClients.get(allTenantInfo.get(tenantId)).getDatabaseName();
            return tenantClients.get(tenantId).getClient().getDatabase(databaseName);
        } catch (final NullPointerException exception) {
            throw new CrnCommonException(ErrorType.NOT_FOUND, "Tenant Datasource tenantId not found.");
        }
    }

    public Map<String, TenantDatasource> getAllTenants() {
        return tenantClients;
    }

    /**
     * Get datasource
     *
     * @return map key and datasource infos
     */
    private Map<String, TenantDatasource> loadServiceDatasources() {

        // Save datasource credentials first time
        // In production mode, this part can be skip
        Map<String, TenantDatasource> datasourceMap = new HashMap<>();

        List<DataSourceProperties.Tenant> tenants = dataSourceProperties.getDatasources();
        tenants.forEach(d -> {
            datasourceMap.put(d.getTenantId(),
                    new TenantDatasource(d.getTenantId(), d.getHost(), d.getPort(), d.getAuthenticationDatabase(),
                            d.getUsername(), d.getPassword(), d.getDatabaseName(), d.getUri(), d.getTenantDatabase(),
                            d.getAuth0Configs(), this.prodEnvironment,
                            d.getKnowiConfigs(), d.getTypeOfAuthenticationServer(), d.getKeyCloakConfigs(),
                            d.getConnectionType(), d.isParentTenant(), d.isCommonTenant(), d.getS3BucketName()));
        });

        return datasourceMap;
    }

    /**
     * This will get called for getting Auth0 configs
     *
     * @return MongoDatabase
     */
    public Auth0Configs getAuth0Configurations() {
        try {
            final String tenantId = TenantContext.getTenantId();
            if (!tenantClients.containsKey(tenantId)) {
                if (allTenantInfo.containsKey(tenantId)) {
                    return tenantClients.get(allTenantInfo.get(tenantId)).getAuth0Configs();
                } else {
                    loadAllTenantsInfo(allTenantInfo, tenantClients);
                    if (allTenantInfo.containsKey(tenantId)) {
                        return tenantClients.get(allTenantInfo.get(tenantId)).getAuth0Configs();
                    } else {
                        throw new CrnCommonException(ErrorType.NOT_FOUND, "Tenant with given Id does not exists");
                    }
                }
            } else {
                return tenantClients.get(tenantId).getAuth0Configs();
            }
        } catch (final NullPointerException exception) {
            throw new CrnCommonException(ErrorType.NOT_FOUND, "Tenant Datasource tenantId not found.");
        }
    }

    public Tenant getCurrentTenant() {
        final String tenantId = TenantContext.getTenantId();
        if (allTenantsBasicInformation.containsKey(tenantId)) {
            return allTenantsBasicInformation.get(tenantId);
        } else {
            throw new CrnCommonException(ErrorType.NOT_FOUND, "tenant by Id not found while getting basic details.");
        }
    }

    public List<Tenant> getTenantDetailsBasedOnIds(final List<String> tenantIds) {
        final List<Tenant> tenants = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(tenantIds)) {
            tenantIds.forEach(tenantId -> {
                if (allTenantsBasicInformation.containsKey(tenantId)) {
                    tenants.add(allTenantsBasicInformation.get(tenantId));
                }
            });
        }
        return tenants;
    }

    public String getParentTenantId() {
        final String tenantId = TenantContext.getTenantId();
        if (tenantClients.containsKey(tenantId)) {
            return tenantId;
        }
        if (allTenantInfo.containsKey(tenantId)) {
            return allTenantInfo.get(tenantId);
        }
        // refresh tenant list;
        loadAllTenantsInfo(allTenantInfo, tenantClients);
        if (allTenantInfo.containsKey(tenantId)) {
            return allTenantInfo.get(tenantId);
        }
        throw new CrnCommonException(ErrorType.NOT_FOUND, "Tenant with given Id does not exists");
    }

    // Only get Parent knowi information... not for childs
    public KnowiConfigs getKnowiConfigurations() {
        final String tenantId = getParentTenantId();
        if (tenantClients.containsKey(tenantId)) {
            final KnowiConfigs knowiConfigs = tenantClients.get(tenantId).getKnowiConfigs();
            if (null != knowiConfigs) {
                return knowiConfigs;
            }
        }
        throw new CrnCommonException(ErrorType.NOT_FOUND, "Tenant with given Id does not exists");
    }

    public KeyCloakConfigs getKeyCloakConfigurations() {
        final String tenantId = getParentTenantId();
        if (tenantClients.containsKey(tenantId)) {
            final KeyCloakConfigs keyCloakConfigs = tenantClients.get(tenantId).getKeyCloakConfigs();
            if (null != keyCloakConfigs) {
                return keyCloakConfigs;
            }
        }
        throw new CrnCommonException(ErrorType.NOT_FOUND, "Tenant with given Id does not exists");
    }

    public String getTypeOfAuthenticationServer() {
        final String parentTenantId = getParentTenantId();
        if (tenantClients.containsKey(parentTenantId)) {
            final String typeOfAuthenticationServer = tenantClients.get(parentTenantId).getTypeOfAuthenticationServer();
            return typeOfAuthenticationServer;
        }
        throw new CrnCommonException(ErrorType.NOT_FOUND, "Tenant with given Id does not exists");
    }

    public String getS3BucketNameForCurrentTenantId() {
        try {
            final String parentTenantId = getParentTenantId();
            return tenantClients.get(parentTenantId).getS3BucketName();

        } catch (final Exception exception) {
            throw new CrnCommonException(ErrorType.NOT_FOUND,
                    "Tenant Datasource tenantId not found in getting S3 bucket.");
        }
    }

    /**
     * This will return all tenants information from all databases within the
     * system.
     * 
     * @param tenantIds
     * @return tenant's list
     */
    public List<Tenant> getAllTenantsInTheSystem(final List<String> tenantIds) {
        final List<Tenant> tenants = new ArrayList<>();
        tenantClients.keySet().forEach(key -> {
            final MongoClient mongoClient = tenantClients.get(key).getClient();
            final String tenantDatabaseName = tenantClients.get(key).getTenantDatabase();
            final boolean isParentTenant = tenantClients.get(key).isParentTenant();
            if (isParentTenant) {
                final MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, tenantDatabaseName);
                // adding a null check over here just to avoid in aggregation.
                final Criteria criteria = CollectionUtils.isNotEmpty(tenantIds) ? Criteria.where("_id").in(tenantIds)
                        : Criteria.where("_id").exists(true);
                final Aggregation aggregation = Aggregation.newAggregation(
                        match(criteria),
                        project("_id", "name", "loginUrl", "regionHeaderLogo", "tagLine", "applicationSupport", "about",
                                "nickname"));
                final AggregationResults<Tenant> results = mongoTemplate.aggregate(aggregation, "tenant", Tenant.class);
                if (CollectionUtils.isNotEmpty(results.getMappedResults())) {
                    results.getMappedResults().forEach(tenant -> {
                        tenants.add(tenant);
                    });
                }
            }
        });
        return tenants;
    }

    @Scheduled(cron = "${fiveMinuteCronJobExpression}")
    public void refreshTenantMaps() {
        LOGGER.info("Refreshing tenant maps after 5 minutes");
        loadAllTenantsInfo(allTenantInfo, tenantClients);
    }
}
