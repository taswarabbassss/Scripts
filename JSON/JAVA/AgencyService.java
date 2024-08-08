package com.crn.agency.service;

import com.crn.agency.config.MongoDataSources;
import com.crn.agency.constants.EndPoints;
import com.crn.agency.constants.TargetNameConstants;
import com.crn.agency.domain.Agency;
import com.crn.agency.domain.Language;
import com.crn.agency.domain.Location;
import com.crn.agency.domain.PermissionGroup;
import com.crn.agency.domain.ReferralDoc;
import com.crn.agency.domain.ReferralRequirement;
import com.crn.agency.domain.Region;
import com.crn.agency.domain.SubCategory;
import com.crn.agency.domain.Tenant;
import com.crn.agency.domain.pojo.LastUpdatedAt;
import com.crn.agency.domain.pojo.OffSiteLocation;
import com.crn.agency.domain.pojo.auditlogging.AuditLoggingSearchParamsModel;
import com.crn.agency.domain.PageableAgencyResponse;
import com.crn.agency.domain.PageableAgencyFavoriteResponse;
import com.crn.agency.domain.PageableAgencyInfoResponse;
import com.crn.agency.enums.AttestationAppearEnum;
import com.crn.agency.enums.ContactTypeEnum;
import com.crn.agency.enums.DomainTypeEnum;
import com.crn.agency.enums.OrganizationTypeEnum;
import com.crn.agency.enums.SortAgencyFieldsEnum;
import com.crn.agency.enums.SortDirectionEnum;
import com.crn.agency.enums.UserRolesEnum;
import com.crn.agency.filter.TenantContext;
import com.crn.agency.helpers.AuditLogging;
import com.crn.agency.mapper.AgencyMapper;
import com.crn.agency.model.AgencyByCategory;
import com.crn.agency.model.AgencyByService;
import com.crn.agency.model.AgencyIdAndName;
import com.crn.agency.model.AgencyIdName;
import com.crn.agency.model.AgencyInfo;
import com.crn.agency.model.AgencyReferral;
import com.crn.agency.model.AgencyShort;
import com.crn.agency.model.AgencyWithNamePictures;
import com.crn.agency.model.AttestationSetting;
import com.crn.agency.model.CurrentUser;
import com.crn.agency.model.DownloadedFile;
import com.crn.agency.model.FavoriteAgenciesPlusReferralnfo;
import com.crn.agency.model.FavoriteAgency;
import com.crn.agency.model.FileToUpload;
import com.crn.agency.model.ProgramAndAgencyData;
import com.crn.agency.model.ReferredUsers;
import com.crn.agency.model.SelectedAgencyByCategory;
import com.crn.agency.model.TenantRegion;
import com.crn.agency.model.UserId;
import com.crn.agency.model.EntityMappingIdentification;
import com.crn.agency.model.program.Program;
import com.crn.agency.model.program.ProgramAffiliation;
import com.crn.agency.model.request.AllAgenciesRequest;
import com.crn.agency.model.request.AgencyRequest;
import com.crn.agency.model.request.AgencySearchByCategory;
import com.crn.agency.model.request.AgencySearchBySubCategory;
import com.crn.agency.model.request.PermissionGroupRequest;
import com.crn.agency.repository.AgencyRepository;
import com.crn.agency.repository.RegionRepository;
import com.crn.agency.repository.TenantRepository;
import com.crn.agency.repository.program.ProgramRepository;
import com.crn.agency.util.AuthenticationUtil;
import com.crn.agency.util.DistanceCalculator;
import com.crn.agency.util.GoogleMapHelper;
import com.crn.agency.util.HttpRequestHelper;
import com.crn.agency.util.UserValidationUtil;
import com.crn.agency.validation.ValidationMessageUtils;
import com.crn.agency.validation.ValidationMessages;
import com.crn.common.component.AwsHelper;
import com.crn.common.constants.PermissionConstants;
import com.crn.common.exception.CrnCommonException;
import com.crn.common.util.RegExEscapeUtil;
import com.google.gson.Gson;
import com.stella.common.service.exception.ErrorType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotBlank;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.crn.agency.constants.CommonStringConstants.VALID_FILE_NAME_REGEX;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

/**
 * Created by Ahmed Khan on 4/6/2019.
 */
@Service
public class AgencyService {
    private final static Logger LOGGER = LogManager.getLogger(AgencyService.class);

    @Value("${clientRegion}")
    private String CLIENT_REGION;
    @Value("${s3BucketName}")
    private String BUCKET_NAME;
    @Value("${dataVaultPath}")
    private String AGENCY_DATA_VAULT_PATH;

    @Autowired
    private Validator validator;
    @Autowired
    private EndPoints endPoints;
    @Autowired
    private AuditLogging auditLoggingUtil;
    @Autowired
    private Gson gson;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private TenantService tenantService;
    @Autowired
    private MongoDataSources mongoDataSources;
    @Autowired
    private GoogleMapHelper googleMapHelper;

    private AgencyRepository agencyRepository;

    @Autowired
    private ProgramRepository programRepository;
    @Autowired
    private MongoTemplate mongoTemplate;
    private CountryStateService countryStateService;
    private AwsHelper awsHelper;
    private HttpRequestHelper httpRequestHelper;
    private static double DISTANCE_IN_MILES = 35;
    private static final String IDENTIFICATION_TYPE = "Identification Type";
    private static final String PROGRAM_ID = "programId";

    private static final String SERVICE_ID = "serviceId";
    private static final String AGENCY_ID = "agencyId";
    private static final String PROGRAM = "program";
    private static String AGENCY_211 = "211 - Resource & Referral Specialists";
    // if list is upadate. update at FE common service.
    final List<String> agencyIdsNotToShow = Arrays.asList("5d52caaa149b3f0006e63dcd", "5d5d76bf149b3f0006e63dd5",
            "5d63f664149b3f0006e63ddb",
            "5d6e7a35149b3f0006e63de6", "5d6e82f0149b3f0006e63de7", "5d5d76bf149b3f0006e63dd5",
            "5d63f664149b3f0006e63ddb",
            "5d6e7a35149b3f0006e63de6", "5d6e82f0149b3f0006e63de7", "5d52caaa149b3f0006e63dcd",
            "5dc3500c149b3f0006311c51", "5d9facde149b3f0006dba6ed",
            "5d7188ed149b3f0006e63dec", "5d7189ab149b3f0006e63ded", "5d7187e0149b3f0006e63deb",
            "5d718df1149b3f0006e63def", "5d718732149b3f0006e63dea",
            "5db772c2149b3f00068f660d", "5d9bbd57149b3f00062ae3af", "5d718a13149b3f0006e63dee",
            "5e347c86149b3f0006cd1481", "5e347af3149b3f0006cd1480",
            "5db87349149b3f0006311c50", "5e26c017149b3f00062096c2", "5e7bc9c8149b3f0006ed6923",
            "5dfa7c42149b3f0006371830");

    @Autowired
    public AgencyService setAgencyRepository(final AgencyRepository agencyRepository,
            final HttpRequestHelper httpRequestHelper, final CountryStateService countryStateService) {
        this.agencyRepository = agencyRepository;
        this.httpRequestHelper = httpRequestHelper;
        this.countryStateService = countryStateService;
        this.awsHelper = new AwsHelper(BUCKET_NAME, CLIENT_REGION);
        return this;
    }

    public Agency addAgency(final AgencyRequest agency) {
        LOGGER.info("create AgencyRequest");

        // get user info based on jwt token
        final String userId = AuthenticationUtil.getUserIdFromJwtToken();
        LOGGER.info("getting loggedIn user tenant details...");
        final String tenantId = AuthenticationUtil.getCurrentTenantId();
        // final Tenant tenantInfo = tenantService.getSpecificTenantById(tenantId);

        final CurrentUser currentUserInfo = httpRequestHelper.getCurrentUserInfo(endPoints.getGetCurrentUserInfoPath(),
                userId);
        // navigator and operator can not create the agency
        if (UserRolesEnum.OPERATOR.getValue().equalsIgnoreCase(currentUserInfo.getRole().getTitle())
                || UserRolesEnum.NAVIGATOR.getValue().equalsIgnoreCase(currentUserInfo.getRole().getTitle())
                || (!UserRolesEnum.SUPER_ADMIN_QS_SYSTEMS.getValue()
                        .equalsIgnoreCase(currentUserInfo.getRole().getTitle()) && agency.getIsLocked())) {
            throw new CrnCommonException(ErrorType.UNEXPECTED,
                    currentUserInfo.getRole().getTitle() + " cannot create the agency.");
        }
        // check user have permisson to create agency
        if (!CollectionUtils.isEmpty(currentUserInfo.getUserRolePermissions())) {
            if (agency.getOrganizationType().equals(OrganizationTypeEnum.PAYER_PARTNER_NETWORK.getValue())
                    && !currentUserInfo.getUserRolePermissions()
                            .contains(PermissionConstants.CREATE_DEACTIVATE_NETWORK_PARTNER)) {
                throw new CrnCommonException(ErrorType.UNEXPECTED,
                        currentUserInfo.getRole().getTitle() + " cannot create Payer Network");
            }
            if (!agency.getOrganizationType().equals(OrganizationTypeEnum.PAYER_PARTNER_NETWORK.getValue())
                    && !currentUserInfo.getUserRolePermissions()
                            .contains(PermissionConstants.CREATE_DEACTIVATE_ENTITY)) {
                throw new CrnCommonException(ErrorType.UNEXPECTED,
                        currentUserInfo.getRole().getTitle() + " cannot create the agency.");
            }
        } else {
            throw new CrnCommonException(ErrorType.NOT_FOUND, "User roles and permissions not Found");
        }

        final Set<ConstraintViolation<AgencyRequest>> violations = validator.validate(agency);
        if (agency != null) {
            checkCosAgencyIdIsUnique(null, agency.getCosAgencyId(), AuthenticationUtil.getCurrentTenantId());
        }
        if (CollectionUtils.isEmpty(violations)) {
            validateAgencyInstructionSetting(agency);
            agencyCreationUpdationBasicStepsAndChecks(agency);
            final Agency agencyModel = AgencyMapper.INSTANCE.toDto(agency);
            agencyModel.setIsActive(true);
            agencyModel.setDefaultTenantId(tenantId);
            if (!CollectionUtils.isEmpty(agency.getIdentification())) {
                agency.getIdentification()
                        .forEach(identification -> identification.setValue(identification.getValue().trim()));
            }
            checkIfIdentificationIsADuplicate(agencyModel.getIdentification(), agencyModel.getId());
            final Agency savedAgency = agencyRepository.insert(agencyModel);

            if (!CollectionUtils.isEmpty(agency.getPictures())) {
                final List<String> pictureUrls = agency.getPictures().stream().map(picture -> {
                    final String picBase64 = getBase64AfterRemovingInitalTags(picture);
                    return getFileUrlAfterUpload(picBase64, agency.getName(), savedAgency.getId());
                }).filter(Objects::nonNull).collect(Collectors.toList());
                savedAgency.setPictures(pictureUrls);
            }

            uploadPaperReferrals(savedAgency);
            uploadAgencyAdditionalDocuments(savedAgency);
            sendAgencyInfoToReportModule(savedAgency);
            setTenantInfoOfAgency(savedAgency.getId(), AuthenticationUtil.getJwtToken(), tenantId);
            savedAgency.setLastUpdatedSource("");
            final Agency response = agencyRepository.save(savedAgency);
            auditLoggingUtil.logDbCreateEvent(currentUserInfo, savedAgency, savedAgency.getId(),
                    TargetNameConstants.AGENCY, "Creating the new agency into the authenticationDatabase.");
            return response;
        }
        throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                ValidationMessageUtils.getViolationMessages(violations) + " in create Agency Request");
    }

    public boolean checkCosAgencyIdIsUnique(final String agencyId, final String cosAgencyId,
            final String currentTenantId) {
        if (StringUtils.isBlank(cosAgencyId)) {
            return true;
        }
        final String cosAgencyIdWithEscapedSpecialChars = RegExEscapeUtil.escapeSpecialRegexChars(cosAgencyId);
        final boolean cosAgencyIdAlreadyPresent = agencyRepository.existsByCosAgencyIdWithInTenant(agencyId,
                cosAgencyIdWithEscapedSpecialChars, currentTenantId);
        if (cosAgencyIdAlreadyPresent) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format("CosAgencyId %s :is already assoicated with an entity in this tenant", cosAgencyId));
        }
        return true;
    }

    private void uploadPaperReferrals(final Agency agency) {
        // Check if referral format is of type 'paper' and the document is available
        // then save the document
        if (agency.getReferralInfo() != null && agency.getReferralInfo().getIsPaper()
                && !CollectionUtils.isEmpty(agency.getReferralInfo().getReferralDocs())) {

            final List<ReferralDoc> referralDocs = agency.getReferralInfo().getReferralDocs()
                    .stream()
                    .map(referralDoc -> uploadPaperReferralDocTOS3(referralDoc, agency.getId(), agency.getName()))
                    .collect(Collectors.toList());
            agency.getReferralInfo().setReferralDocs(referralDocs);
        }
    }

    private void agencyCreationUpdationBasicStepsAndChecks(final AgencyRequest agency) {
        final ReferralRequirement referralInfo = agency.getReferralInfo();

        // Check for agency's accepted referral type format
        // perform validation only if agency is part of network.
        if (referralInfo != null && (!referralInfo.getIsElectronic() && !referralInfo.getIsPaper()
                && !referralInfo.getIsLog() && !referralInfo.getIsExpress()) && !agency.getIsOutOfNetwork()) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(), "At least one Referral format "));
        }
        // validate referredAgencies if present
        if (referralInfo != null && !CollectionUtils.isEmpty(referralInfo.getReferralAcceptedFrom())) {
            validateReferralAcceptedAgencies(agency.getReferralInfo().getReferralAcceptedFrom());
        }

        // If no agency logo is attached or an agency abbreviation is provided then set
        // the system generated abbreviation through the agency name initials
        if (CollectionUtils.isEmpty(agency.getPictures()) && StringUtils.isBlank(agency.getAbbreviation())) {
            agency.setAbbreviation(setAgencyAbbreviation(agency.getName()));
        }

        if (!CollectionUtils.isEmpty(agency.getLanguages())
                && agency.getLanguages().stream().anyMatch(lang -> lang.getAbbrevation().equals("OTHER"))
                && StringUtils.isBlank(agency.getOtherLanguage())) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(),
                            "Other language value is required when language list contains OTHER"));
        }

    }

    private void uploadAgencyAdditionalDocuments(final Agency agency) {
        if (agency.getReferralInfo() != null
                && !CollectionUtils.isEmpty(agency.getReferralInfo().getAdditionalDocumentationForms())) {
            final List<FileToUpload> additionalFiles = agency.getReferralInfo().getAdditionalDocumentationForms()
                    .stream()
                    .map(fileToUpload -> uploadAdditionalDocument(fileToUpload, agency.getName(), agency.getId()))
                    .collect(Collectors.toList());
            agency.getReferralInfo().setAdditionalDocumentationForms(additionalFiles);
        }
    }

    private void updateTenantInfoOfAgency(final Agency agency, final Agency oldAgency, final String jwtToken,
            final String tenantId) {
        if ((StringUtils.isNotBlank(agency.getDefaultTenantId())
                && StringUtils.isNotBlank(oldAgency.getDefaultTenantId())
                && !agency.getDefaultTenantId().equals(oldAgency.getDefaultTenantId()))
                || (!CollectionUtils.isEmpty(agency.getTenantIds())
                        && !CollectionUtils.isEmpty(oldAgency.getTenantIds())
                        && agency.getTenantIds().size() != oldAgency.getTenantIds().size())
                || (!CollectionUtils.isEmpty(agency.getTenantIds())
                        && !CollectionUtils.isEmpty(oldAgency.getTenantIds())
                        && !agency.getTenantIds().containsAll(oldAgency.getTenantIds()))) {
            setTenantInfoOfAgency(agency.getId(), jwtToken, tenantId);
        }
    }

    private void setTenantInfoOfAgency(final String agencyId, final String jwtToken, final String tenantId) {
        // CompletableFuture.runAsync(() -> {
        final String requestPath = endPoints.getUpdateTenantInfo();
        httpRequestHelper.updateAgencyByDbScriptMicroService(requestPath, agencyId, jwtToken, tenantId);
        // });
    }

    private void updateAgencyDuplication(final String agencyId, final String jwtToken, final String tenantId) {
        // final String jwtToken = AuthenticationUtil.getJwtToken();
        // CompletableFuture.runAsync(() -> {
        final String requestPath = endPoints.getRemoveAgencyNickname();
        httpRequestHelper.updateAgencyByDbScriptMicroService(requestPath, agencyId, jwtToken, tenantId);
        // });
    }

    private String getBase64AfterRemovingInitalTags(final String base64Str) {
        final int index = base64Str.indexOf("base64,");
        return base64Str.replace(base64Str.substring(0, index + 7), "");
    }

    private String getFileUrlAfterUpload(final String picture, final String agencyName, final String agencyId) {
        try {
            final byte[] decodedFile = Base64.getDecoder()
                    .decode(picture.getBytes(StandardCharsets.UTF_8));
            final Path destinationFile = Paths.get("", agencyName.replaceAll(VALID_FILE_NAME_REGEX, "") + "." + "png");
            final Path write = Files.write(destinationFile, decodedFile);
            final File file = write.toFile();
            final String fileName = ("AgencyPicture" + "_" + agencyId + "_" + LocalDateTime.now(ZoneId.of("UTC")))
                    .replace(":", "");
            final String filePath = AGENCY_DATA_VAULT_PATH + agencyId + "/" + fileName + ".png";
            final String fileUrl = awsHelper.uploadAndGetPublicLinkOfImages(
                    mongoDataSources.getS3BucketNameForCurrentTenantId(), filePath, file);
            file.delete();
            return fileUrl;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendAgencyInfoToReportModule(final Agency savedAgency) {
        final String jwtToken = AuthenticationUtil.getJwtToken();
        final String tenantId = AuthenticationUtil.getCurrentTenantId();
        CompletableFuture.runAsync(() -> {

            final AgencyIdAndName agencyIdName = new AgencyIdAndName();
            agencyIdName.setAgencyId(savedAgency.getId());
            agencyIdName.setName(savedAgency.getName());
            final String requestPath = endPoints.getAddAgencyInReport();
            httpRequestHelper.addAgencyToReportingSystem(requestPath, agencyIdName, jwtToken, tenantId);
        });
    }

    private void validateReferralAcceptedAgencies(final List<AgencyIdName> agencies) {
        final List<String> agencyIds = agencies.stream()
                .map(agencyIdName -> agencyIdName.getId()).collect(Collectors.toList());

        final List<AgencyIdName> foundAgencies = agencyRepository.findByIdIn(agencyIds);

        if (CollectionUtils.isEmpty(foundAgencies)) {
            LOGGER.info("Unable to find referralAcceptedFrom agencies");
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "Unable to find referralAcceptedFrom agencies");
        }
        // Commented code below because deActivated agency doesnot exists in found
        // Agencies
        // else if (foundAgencies.size() != agencies.size()) {
        //
        // String unavailable = agencyIds.stream()
        // .filter(id -> foundAgencies.stream()
        // .anyMatch(agency -> !id.equals(agency.getId())))
        // .collect(Collectors.joining(","));
        // LOGGER.info("Unable to find referralAcceptedFrom agencies");
        // throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "Unable to find
        // referralAcceptedFrom agencies {Ids}: " + unavailable);
        // }

    }

    private void validateAgencyInstructionSetting(final AgencyRequest agency) {
        if (agency.getAgencyUserInstructionSetting() == null) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "Agency level user instruction cannot be null");
        }
        if (agency.getAgencyUserInstructionSetting() != null
                && agency.getAgencyUserInstructionSetting().getAppearAt()
                        .equals(AttestationAppearEnum.EVERY_LOG_IN.getValue())
                && StringUtils.isBlank(agency.getAgencyUserInstructionSetting().getAttestationText())) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    "Instruction cannot be empty when appear At is EVERY_LOG_IN");
        }
    }

    private void checkPermissionToDeActiveAgency(final Agency previousAgency, final AgencyRequest currentAgency,
            final CurrentUser currentUserInfo) {
        if (!currentAgency.getIsActive() && previousAgency.getIsActive()
                && !CollectionUtils.isEmpty(currentUserInfo.getUserRolePermissions())
                && !currentUserInfo.getUserRolePermissions().contains(PermissionConstants.CREATE_DEACTIVATE_ENTITY)) {
            LOGGER.info("User does not have permission to deactivate agency. userId of user is :"
                    + currentUserInfo.getId());
            throw new CrnCommonException(ErrorType.FORBIDDEN, "User does not have permission to deactivate agency");
        }
        if (!currentAgency.getIsActive() && previousAgency.getIsActive()
                && (UserRolesEnum.OPERATOR.getValue().equalsIgnoreCase(currentUserInfo.getRole().getTitle())
                        || UserRolesEnum.NAVIGATOR.getValue().equalsIgnoreCase(currentUserInfo.getRole().getTitle()))) {
            LOGGER.info("agency is not active and user who is updating it is Operator or Navigator");
            throw new CrnCommonException(ErrorType.UNEXPECTED,
                    currentUserInfo.getRole().getTitle() + " cannot de-activate the agency");
        }
    }

    public Agency updateAgency(final String id, final AgencyRequest agency) {

        // check if there is agency present against the id given
        final Optional<Agency> savedAgency = agencyRepository.findById(id);// findBy_id(id);
        if (!savedAgency.isPresent()) {
            throw new CrnCommonException(ErrorType.UNEXPECTED,
                    String.format(ValidationMessages.ENTITY_NOT_FOUND.getMessage(), "agency"));
        }

        final Set<ConstraintViolation<AgencyRequest>> violations = validator.validate(agency);
        if (agency != null) {
            checkCosAgencyIdIsUnique(id, agency.getCosAgencyId(), AuthenticationUtil.getCurrentTenantId());
        }
        if (CollectionUtils.isEmpty(violations)) {
            validateAgencyInstructionSetting(agency);
            // get user info based on jwt token
            final String userId = AuthenticationUtil.getUserIdFromJwtToken();
            final CurrentUser currentUserInfo = httpRequestHelper
                    .getCurrentUserInfo(endPoints.getGetCurrentUserInfoPath(), userId);

            validPermissionAccordingToRole(id, currentUserInfo, savedAgency.get(), agency);
            // check user have permission to update agency
            if (!CollectionUtils.isEmpty(currentUserInfo.getUserRolePermissions())) {
                // check user have permission to edit organizationType = PAYER_PARTNER_NETWORK
                if (agency.getOrganizationType().equals(OrganizationTypeEnum.PAYER_PARTNER_NETWORK.getValue())
                        && !currentUserInfo.getUserRolePermissions()
                                .contains(PermissionConstants.EDIT_NETWORK_PARTNER)) {
                    throw new CrnCommonException(ErrorType.UNEXPECTED,
                            currentUserInfo.getRole().getTitle() + " cannot update Payer Network");
                }
                if (!agency.getOrganizationType().equals(OrganizationTypeEnum.PAYER_PARTNER_NETWORK.getValue())
                        && !currentUserInfo.getUserRolePermissions().contains(PermissionConstants.EDIT_ENTITY)) {
                    throw new CrnCommonException(ErrorType.UNEXPECTED,
                            currentUserInfo.getRole().getTitle() + " cannot update the agency.");
                }
            } else {
                throw new CrnCommonException(ErrorType.NOT_FOUND, "User roles and permissions not Found");
            }

            checkPermissionToDeActiveAgency(savedAgency.get(), agency, currentUserInfo);
            agencyCreationUpdationBasicStepsAndChecks(agency);

            // UPDATE AGENCY USER'S DATA ACCESS PERMISSIONS ---------
            if (!CollectionUtils.isEmpty(agency.getDataAccessPermissions())) {
                List<PermissionGroupRequest> filteredAgencyPermissions = new ArrayList<>();
                for (final PermissionGroupRequest eachPermission : agency.getDataAccessPermissions()) {
                    // If any agency permission is false only then remove it from user otherwise
                    // don't do anything.
                    if (!eachPermission.getIsSpecialized() || !eachPermission.getIsGeneral()
                            || !eachPermission.getIsConfidential()) {
                        filteredAgencyPermissions.add(eachPermission);
                    }
                }

                if (!CollectionUtils.isEmpty(filteredAgencyPermissions)) {
                    LOGGER.info("Updating agency users data access permissions");
                    setUserDataAccessPermissions(id, filteredAgencyPermissions);
                }
            }

            final Agency agencyModel = AgencyMapper.INSTANCE.toDto(agency);
            LOGGER.info("update AgencyRequest");
            agencyModel.setId(id);
            agencyModel.setCreatedAt(savedAgency.get().getCreatedAt());
            agencyModel.setCreatedBy(savedAgency.get().getCreatedBy());
            agencyModel.setCosServiceId(savedAgency.get().getCosServiceId());
            agencyModel.setCosServiceDayHours(savedAgency.get().getCosServiceDayHours());
            agencyModel.setCosLastModifiedAt(savedAgency.get().getCosLastModifiedAt());
            agencyModel.setCosSiteIdForMainAddress(savedAgency.get().getCosSiteIdForMainAddress());
            agencyModel.setCosUploadSource(savedAgency.get().getCosUploadSource());
            agencyModel.setCosUploadMethod(savedAgency.get().getCosUploadMethod());
            if (UserRolesEnum.SUPER_ADMIN_QS_SYSTEMS.getValue()
                    .equalsIgnoreCase(currentUserInfo.getRole().getTitle())) {
                if (agency.isTestAgency()) {
                    agencyModel.setTestAgency(agency.isTestAgency());
                }
                if (agency.getIsLocked()) {
                    agencyModel.setLocked(agency.getIsLocked());
                }
            } else {
                if (savedAgency.get().isTestAgency()) {
                    agencyModel.setTestAgency(savedAgency.get().isTestAgency());
                }
                if (savedAgency.get().getIsLocked()) {
                    agencyModel.setLocked(savedAgency.get().getIsLocked());
                }
            }
            agencyModel.setLastUpdatedSource("");

            final Agency alreadySavedAgency = savedAgency.get();
            if (CollectionUtils.isEmpty(agencyModel.getIdentification())
                    && !CollectionUtils.isEmpty(alreadySavedAgency.getIdentification())) {
                agencyModel.setIdentification(alreadySavedAgency.getIdentification());
            } else if (!CollectionUtils.isEmpty(agencyModel.getIdentification())) {
                agencyModel.getIdentification()
                        .forEach(identification -> identification.setValue(identification.getValue().trim()));
                checkIfIdentificationIsADuplicate(agencyModel.getIdentification(), agencyModel.getId());
            }
            if (CollectionUtils.isEmpty(alreadySavedAgency.getPictures())) {
                alreadySavedAgency.setPictures(new ArrayList<>());
            }

            if (!CollectionUtils.isEmpty(agencyModel.getPictures())) {

                final List<String> pictureUrls = agencyModel.getPictures().stream().map(picture -> {
                    if (!alreadySavedAgency.getPictures().contains(picture)) {
                        final String picBase64 = getBase64AfterRemovingInitalTags(picture);
                        return getFileUrlAfterUpload(picBase64, agencyModel.getName(), alreadySavedAgency.getId());
                    } else {
                        // awsHelper.deleteFilefromS3UsingUrl(picture);
                        return picture;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());
                agencyModel.setPictures(pictureUrls);
            }

            // Remove picture from previous saved agency if current agency to update is not
            // empty and if agency to update contains picture already present in previous
            // saved agency
            alreadySavedAgency.getPictures()
                    .removeIf(savedPicture -> (!CollectionUtils.isEmpty(agencyModel.getPictures())
                            && agencyModel.getPictures().contains(savedPicture)));
            // delete the pictures which are not present in current picture but in
            // previously saved agency
            alreadySavedAgency.getPictures().forEach(picture -> {
                awsHelper.deleteFileFromS3UsingUrl(picture);
            });

            updatePaperReferralForms(agencyModel, savedAgency.get());
            updateAgencyAdditionalDocuments(agencyModel, savedAgency.get());
            updateRegionsInAgencyUsers(agencyModel, savedAgency.get());

            if ((agencyModel.getIsOutOfNetwork() || !agencyModel.getIsActive()) &&
                    (agencyModel.getGeneralReferralHubId() != null)
                    && !"".equals(agencyModel.getGeneralReferralHubId())) {

                Optional<Tenant> tenant = tenantRepository.findById(agencyModel.getGeneralReferralHubId());
                if (tenant.isPresent()) {
                    tenant.get().setGeneralReferralHubId(null);
                    tenant.get().setGeneralReferralHub(false);
                    tenantRepository.save(tenant.get());
                }
                agencyModel.setGeneralReferralHub(false);
                agencyModel.setGeneralReferralHubId(null);
            }

            final Agency updatedAgency = agencyRepository.save(agencyModel);

            // //call below to update organization type of agency present in
            // associatedOrgsByOrgType list
            // updateOrganizationTypeInAssociatedOrgz(agencyModel, savedAgency.get());
            // // also update the agency's nickname in others collections.
            // updateAgencyDuplication(savedAgency.get().getId());
            // updateTenantInfoOfAgency(agencyModel,alreadySavedAgency);
            final Agency response;
            if (savedAgency.get().getIsActive() && !updatedAgency.getIsActive()) {

                LOGGER.info("Agency is not active so calling saveDeActivatePropertiesInAgency() from updateAgency()");
                response = saveDeActivatePropertiesInAgency(updatedAgency, currentUserInfo.getId());
            } else {
                response = updatedAgency;
            }

            updateDbScriptsHandling(agencyModel, alreadySavedAgency);
            auditLoggingUtil.logDbUpdateEvent(currentUserInfo, response, alreadySavedAgency, response.getId(),
                    TargetNameConstants.AGENCY, "Updating the agency (AgencyId: " + response.getId() + " )");
            return response;
        }
        throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                ValidationMessageUtils.getViolationMessages(violations) + " for updating Agency");
    }

    private void updateRegionsInAgencyUsers(final Agency currentAgency, final Agency oldAgency) {
        if (!CollectionUtils.isEmpty(currentAgency.getRegions()) && !CollectionUtils.isEmpty(oldAgency.getRegions())) {
            final List<String> currentRegionIds = currentAgency.getRegions().stream().map(TenantRegion::getId)
                    .collect(Collectors.toList());
            final List<String> oldRegionIds = oldAgency.getRegions().stream().map(TenantRegion::getId)
                    .collect(Collectors.toList());

            if (currentRegionIds.size() != oldRegionIds.size() || !currentRegionIds.containsAll(oldRegionIds)
                    || !oldRegionIds.containsAll(currentRegionIds)) {
                final List<Region> regionsOfCurrentAgency = regionRepository.findRegionByIds(currentRegionIds);
                if (!CollectionUtils.isEmpty(regionsOfCurrentAgency)) {
                    // final List<Region> distinctRegionList = regionsOfCurrentAgency.stream()
                    // .sorted(Comparator.comparing(Region::getName))
                    // .collect(Collectors.collectingAndThen(
                    // Collectors.toCollection(() -> new
                    // TreeSet<>(Comparator.comparing(Region::getTenantId))),
                    // ArrayList::new));
                    final String path = endPoints.getUpdateUserDefaultRegion() + "/" + currentAgency.getId();
                    httpRequestHelper.updateUserRegions(path, regionsOfCurrentAgency);
                }
            }
        }
    }

    private void updateDbScriptsHandling(final Agency agency, final Agency savedAgency) {
        final String jwtToken = AuthenticationUtil.getJwtToken();
        final String tenantId = AuthenticationUtil.getCurrentTenantId();
        // CompletableFuture.runAsync(() -> {
        updateOrganizationTypeInAssociatedOrgz(agency, savedAgency, jwtToken, tenantId);
        // also update the agency's nickname in others collections.
        updateTenantInfoOfAgency(agency, savedAgency, jwtToken, tenantId);
        updateAgencyDuplication(savedAgency.getId(), jwtToken, tenantId);
        // });
    }

    private void updatePaperReferralForms(final Agency agency, final Agency oldAgency) {
        if (oldAgency.getReferralInfo() == null) {
            final ReferralRequirement referralRequirement = new ReferralRequirement();
            referralRequirement.setReferralDocs(new ArrayList<>());
            oldAgency.setReferralInfo(referralRequirement);
        }
        if (CollectionUtils.isEmpty(oldAgency.getReferralInfo().getReferralDocs())) {
            oldAgency.getReferralInfo().setReferralDocs(new ArrayList<>());
        }
        if (agency.getReferralInfo() != null && !CollectionUtils.isEmpty(agency.getReferralInfo().getReferralDocs())) {

            final List<ReferralDoc> referralDocs = agency.getReferralInfo().getReferralDocs()
                    .stream().map(doc -> {
                        // upload only new files
                        if (oldAgency.getReferralInfo().getReferralDocs().stream()
                                .noneMatch(
                                        oldFile -> oldFile.getPaperReferralForm().equals(doc.getPaperReferralForm()))) {
                            return uploadPaperReferralDocTOS3(doc, oldAgency.getId(), agency.getName());
                        }
                        return doc;
                    }).collect(Collectors.toList());
            agency.getReferralInfo().setReferralDocs(referralDocs);
        }
        // to delete files which were present in OldAgency and not in new agency Object.
        // we remove the same file exists in both Agency Objects first
        oldAgency.getReferralInfo().getReferralDocs()
                .removeIf(oldFile -> agency.getReferralInfo() != null
                        && !CollectionUtils.isEmpty(agency.getReferralInfo().getReferralDocs())
                        && agency.getReferralInfo().getReferralDocs().stream()
                                .anyMatch(newFile -> newFile.getPaperReferralForm()
                                        .equals(oldFile.getPaperReferralForm())));

        // then remove the different files present in Old Agency Object
        final String s3BucketName = mongoDataSources.getS3BucketNameForCurrentTenantId();
        oldAgency.getReferralInfo().getReferralDocs().forEach(file -> {
            this.awsHelper.deleteDocumentFromS3(s3BucketName, AGENCY_DATA_VAULT_PATH + oldAgency.getId(),
                    file.getPaperReferralForm());
        });

    }

    private void updateAgencyAdditionalDocuments(final Agency agency, final Agency oldAgency) {
        if (oldAgency.getReferralInfo() == null) {
            final ReferralRequirement referralRequirement = new ReferralRequirement();
            referralRequirement.setAdditionalDocumentationForms(new ArrayList<>());
            oldAgency.setReferralInfo(referralRequirement);
        }
        if (CollectionUtils.isEmpty(oldAgency.getReferralInfo().getAdditionalDocumentationForms())) {
            oldAgency.getReferralInfo().setAdditionalDocumentationForms(new ArrayList<>());
        }
        if (agency.getReferralInfo() != null
                && !CollectionUtils.isEmpty(agency.getReferralInfo().getAdditionalDocumentationForms())) {

            final List<FileToUpload> agencyAdditionalDocuments = agency.getReferralInfo()
                    .getAdditionalDocumentationForms()
                    .stream().map(fileToUpload -> {
                        // upload only new files
                        if (oldAgency.getReferralInfo().getAdditionalDocumentationForms().stream()
                                .noneMatch(oldFile -> oldFile.getFile().equals(fileToUpload.getFile()))) {
                            return uploadAdditionalDocument(fileToUpload, agency.getName(), oldAgency.getId());
                        }
                        return fileToUpload;
                    }).collect(Collectors.toList());
            agency.getReferralInfo().setAdditionalDocumentationForms(agencyAdditionalDocuments);
        }
        // to delete files which were present in OldAgency and not in new agency Object.
        // we remove the same file exists in both Agency Objects first
        oldAgency.getReferralInfo().getAdditionalDocumentationForms()
                .removeIf(oldFile -> agency.getReferralInfo().getAdditionalDocumentationForms().stream()
                        .anyMatch(newFile -> newFile.getFile().equals(oldFile.getFile())));

        final String s3BucketName = mongoDataSources.getS3BucketNameForCurrentTenantId();
        // then remove the different files present in Old Agency Object
        oldAgency.getReferralInfo().getAdditionalDocumentationForms().forEach(file -> {
            this.awsHelper.deleteDocumentFromS3(s3BucketName, AGENCY_DATA_VAULT_PATH + oldAgency.getId(),
                    file.getFile());
        });

    }

    private ReferralDoc uploadPaperReferralDocTOS3(final ReferralDoc doc, final String agencyId,
            final String agencyName) {
        final String fileName = ("REFERRAL_FORM" + "_" + agencyId + "_" + LocalDateTime.now(ZoneId.of("UTC")))
                .replace(":", "");
        doc.setDocumentName(fileName);
        try {
            uploadDocumentToS3(doc.getFileExtension(), doc.getPaperReferralForm(), agencyName, agencyId, fileName);
            // uploadPaperReferralForm(agency, fileName);
            doc.setPaperReferralForm(fileName);
            return doc;
        } catch (final IOException e) {
            LOGGER.info(e.getMessage());
            throw new CrnCommonException(ErrorType.UNEXPECTED, e.getMessage());
        }
    }

    private FileToUpload uploadAdditionalDocument(final FileToUpload fileToUpload, final String agencyName,
            final String agencyId) {
        final String fileName = ("ADDITIONAL_FORM" + "_" + agencyId + "_" + LocalDateTime.now(ZoneId.of("UTC")))
                .replace(":", "");
        try {
            uploadDocumentToS3(fileToUpload.getFileExtension(), fileToUpload.getFile(), agencyName, agencyId, fileName);
            fileToUpload.setFile(fileName); // set path of uploaded file after uploading
            return fileToUpload;
        } catch (final IOException ex) {
            ex.printStackTrace();
            throw new CrnCommonException(ErrorType.UNEXPECTED, "Unable to upload additional Document");
        }
    }

    private void updateOrganizationTypeInAssociatedOrgz(final Agency agency, final Agency oldAgency,
            final String jwtToken, final String tenantId) {
        if (!agency.getOrganizationType().equals(oldAgency.getOrganizationType())) {
            // final String jwtToken = AuthenticationUtil.getJwtToken();
            // CompletableFuture.runAsync(() -> {

            final String requestPath = endPoints.getUpdateAgencyOrgType();
            httpRequestHelper.updateAgencyByDbScriptMicroService(requestPath, agency.getId(), jwtToken, tenantId);
            // });
        }
    }

    // Update the data access permissions for an agency users when the Agency is
    // updated
    private void setUserDataAccessPermissions(final String agencyId,
            final List<PermissionGroupRequest> updatedAgencyPermissions) {
        // call user's microservice to update the data access permissions of specific
        // agency users
        final String path = endPoints.getUpdateUserDataAccessPermissions() + agencyId;
        final boolean userResponse = httpRequestHelper.updateUserDataAccessPermissions(path, updatedAgencyPermissions);

        if (userResponse) {
            LOGGER.info("Successfully updated agency users' data access permissions.");
        }
    }

    private void validPermissionAccordingToRole(final String idOfAgencyForUpdate, final CurrentUser currentUserInfo,
            final Agency savedAgency, final AgencyRequest agencyForUpdate) {

        // in case of locked only super admin can edit
        if (!UserRolesEnum.SUPER_ADMIN_QS_SYSTEMS.getValue().equalsIgnoreCase(currentUserInfo.getRole().getTitle())
                && savedAgency.getIsLocked()) {
            throw new CrnCommonException(ErrorType.UNEXPECTED,
                    "Locked, only " + UserRolesEnum.SUPER_ADMIN_QS_SYSTEMS.getValue() + " can create/update");
        }
        // in case of agency admin,case work, allow to edit their own agency or
        // NAVIGATOR cann't update agency.
        if (!UserRolesEnum.TENANT_ADMIN.getValue().equalsIgnoreCase(currentUserInfo.getRole().getTitle())
                && !UserRolesEnum.SUPER_ADMIN_QS_SYSTEMS.getValue()
                        .equalsIgnoreCase(currentUserInfo.getRole().getTitle())) {

            if (UserRolesEnum.OPERATOR.getValue().equalsIgnoreCase(currentUserInfo.getRole().getTitle())
                    || UserRolesEnum.NAVIGATOR.getValue().equalsIgnoreCase(currentUserInfo.getRole().getTitle())) {
                throw new CrnCommonException(ErrorType.UNEXPECTED,
                        currentUserInfo.getRole().getTitle() + " cannot update the agency information.");
            }

            // check the agency of current logged-in user and agency for edit should be same
            if (!UserValidationUtil.isSameAgency(idOfAgencyForUpdate, currentUserInfo.getAgency().getId())) {
                throw new CrnCommonException(ErrorType.UNEXPECTED, "Not Allowed with this user role.");
            }

            final List<PermissionGroup> updatedPermissionGroups = AgencyMapper.INSTANCE
                    .toPermissionGroups(agencyForUpdate.getDataAccessPermissions());
            // check the permissions are changed or not.
            if (!checkDataAccessPermissions(updatedPermissionGroups, savedAgency.getDataAccessPermissions())) {
                throw new CrnCommonException(ErrorType.UNEXPECTED,
                        "Only super admin can edit agency's Data Access Permissions");
            }

            // check the covered entity changed or not.
            if (savedAgency.getCoveredEntity() != agencyForUpdate.getCoveredEntity()) {
                throw new CrnCommonException(ErrorType.UNEXPECTED,
                        "Only super admin can change the value of covered entity");
            }

        }
    }

    public boolean checkDataAccessPermissions(final List<PermissionGroup> newPermissions,
            final List<PermissionGroup> savedPermissions) {
        if (newPermissions.size() != savedPermissions.size()) {
            throw new CrnCommonException(ErrorType.UNEXPECTED,
                    "Only super admin can edit agency's Data Access Permissions");
        }
        for (int i = 0; i < newPermissions.size(); i++) {
            if (!newPermissions.get(i).getName().equals(savedPermissions.get(i).getName()) &&
                    newPermissions.get(i).getIsConfidential() == savedPermissions.get(i).getIsConfidential() &&
                    newPermissions.get(i).getIsGeneral() == savedPermissions.get(i).getIsGeneral() &&
                    newPermissions.get(i).getIsSpecialized() == savedPermissions.get(i).getIsSpecialized()) {
                return false;
            }
        }
        return true;
    }

    private String setAgencyAbbreviation(final String agencyName) {
        if (StringUtils.isBlank(agencyName)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(), "Agency Name"));
        }

        // split the string using 'space'
        // and print the first character of every word
        final String[] eachWord = agencyName.split(" ");
        final StringBuilder agencyAbbreviation = new StringBuilder();
        int count = 0;
        for (final String eachLetter : eachWord) {
            if (!StringUtils.isBlank(eachLetter) && StringUtils.isAlphanumeric(eachLetter) && count < 4) {
                agencyAbbreviation.append(Character.toUpperCase(eachLetter.charAt(0)));
                count++;
            }
        }

        return agencyAbbreviation.toString();
    }

    public Agency deActivateAgency(final String id, final String userId) {
        LOGGER.info("De Activating Agency with id: " + id);
        // check if there is agency present against the id given
        final Optional<Agency> agencyDocument = agencyRepository.findById(id);// findBy_id(id);
        if (agencyDocument.isEmpty()) {
            LOGGER.info("Unable to find agency with id: " + id);
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.ENTITY_NOT_FOUND.getMessage(), "agency"));
        }
        final Agency previousAgency = gson.fromJson(gson.toJson(agencyDocument.get()), Agency.class);
        final CurrentUser currentUserInfo = httpRequestHelper.getCurrentUserInfo(endPoints.getGetCurrentUserInfoPath(),
                userId);
        if (UserRolesEnum.OPERATOR.getValue().equalsIgnoreCase(currentUserInfo.getRole().getTitle())
                || UserRolesEnum.NAVIGATOR.getValue().equalsIgnoreCase(currentUserInfo.getRole().getTitle())) {
            LOGGER.info("User who is deActivating Agency is operator or navigator");
            throw new CrnCommonException(ErrorType.UNEXPECTED,
                    currentUserInfo.getRole().getTitle() + " cannot de-activate the agency");
        }
        final Agency savedAgency;
        // if this agency is previously active only then execute the deactive flow
        if (agencyDocument.get().getIsActive()) {
            savedAgency = saveDeActivatePropertiesInAgency(agencyDocument.get(), userId);
        } else {
            savedAgency = agencyDocument.get();
        }
        auditLoggingUtil.logDbUpdateEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), savedAgency, previousAgency,
                savedAgency.getId(), TargetNameConstants.AGENCY,
                "Deactivating the agency (AgencyId: " + savedAgency.getId() + " )");
        return savedAgency;
    }

    private Agency saveDeActivatePropertiesInAgency(final Agency agencyToUpdate, final String userId) {
        LOGGER.info("in the method saveDeActivatePropertiesInAgency()");
        agencyToUpdate.setIsActive(false);
        agencyToUpdate.setDisplayInResourceDirSearch(false);
        final LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("UTC"));
        agencyToUpdate.setStatusChangedOn(currentTime);
        agencyToUpdate.setLastModifiedAt(currentTime);
        agencyToUpdate.setStatusChangedBy(userId);

        final String path = endPoints.getChangeStatusOfUsersOfAgency() + "/" + agencyToUpdate.getId()
                + "?status=InActive";
        httpRequestHelper.changeStatusOfUsersOfAgency(path, AuthenticationUtil.getJwtToken());

        return agencyRepository.save(agencyToUpdate);
    }

    public List<AgencyIdName> getAllAgenciesHavingSameServiceOfSpecificAgency(final String id, final String tenantId) {
        if (StringUtils.isBlank(id)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(), "agency id"));
        }
        List<String> services = agencyRepository.getServicesOfSpecificAgency(id);

        final List<AgencyIdName> response = agencyRepository.getAgenciesContainingGivenServices(services, id, tenantId);
        auditLoggingUtil.logDbGetAllEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), TargetNameConstants.AGENCY,
                "Getting all agencies having services of specified Agency (AgencyId: " + id + " )");
        return response;
    }

    public List<AgencyIdName> getPayerNetworkAgencies() {
        return agencyRepository.getPayerNetworkAgencies(AuthenticationUtil.getCurrentTenantId());
    }

    public List<String> getAgenciesOfSpecificNetwork(final String idOrValue) {

        return validatePayerNetworkIdOrValueAndGetAgencyIds(idOrValue);
    }

    private List<String> validatePayerNetworkIdOrValueAndGetAgencyIds(final String idOrValue) {
        if (StringUtils.isBlank(idOrValue)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, String
                    .format(ValidationMessages.REQUIRED.getMessage(), "network Id or value only CRN_PARTNERS_ONLY"));
        }
        if (idOrValue.equals("CRN_PARTNERS_ONLY")) {
            // CRN-2079 change in definition of crn partners
            // return agencyRepository.getAllAgenciesNotPartOfAnyNetwork();
            // Agencies having users will be considered as crn partners.
            return agencyRepository.getListOfAgenciesWithUsersPresent();
        } else {
            final Optional<Agency> optionalAgency = agencyRepository.findAgencyByIdAndOrganizationType(idOrValue,
                    OrganizationTypeEnum.PAYER_PARTNER_NETWORK.getValue());
            if (optionalAgency.isEmpty()) {
                throw new CrnCommonException(ErrorType.NOT_FOUND,
                        String.format(ValidationMessages.ENTITY_NOT_FOUND.getMessage(), "network with given id"));
            }
            return agencyRepository.getAgenciesOfNetwork(idOrValue);
        }
    }

    private List<String> getListOfAgenciesWithUsers() {
        final String path = endPoints.getAgencyIdsListHavingUsers();
        return httpRequestHelper.getListOfAgenciesWithUsers(path);
    }

    public Agency getAgencyById(final String id) {

        // for now requirements not clear, so comment it.
        // final CurrentUser currentUserInfo = getCurrentUserInfo();
        // if
        // (!UserRolesEnum.SUPER_ADMIN.getValue().equalsIgnoreCase(currentUserInfo.getRole().getTitle()))
        // {
        // // check the agency of current logged-in user and agency for edit should be
        // same
        // if (!UserValidationUtil.isSameAgency(id,
        // currentUserInfo.getAgency().getId())) {
        // throw new AgencyException(ErrorType.FORBIDDEN, "User can't view this
        // agency.");
        // }
        // return agency.get();
        // }
        final Agency agency = validateAndGetAgencyById(id);
        auditLoggingUtil.logDbGetSingleEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), agency.getId(),
                TargetNameConstants.AGENCY, "Getting the detail of agency (AgencyId: " + agency.getId() + " )");
        return agency;
    }

    public Agency getAgencyByIdWithActiveFavoriteAgencies(final String id) {

        final Agency agency = agencyRepository.getAgencyWithFavoriteAgenciesInfo(id);
        agency.setAgencyFavorites(agency.getAgencyFavorites().stream()
                .filter(agencyIdName -> agencyIdName.getIsActive()).collect(Collectors.toList()));
        auditLoggingUtil.logDbGetSingleEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), agency.getId(),
                TargetNameConstants.AGENCY, "Getting the detail of agency (AgencyId: " + agency.getId() + " )");
        return agency;
    }

    public Agency findAgencyByIdForResourceDirectory(final String id, final Double latitude, final Double longitude) {
        LOGGER.info("findAgencyByIdForResourceDirectory() called with id: " + id + " latitude: " + latitude
                + " longitude: " + longitude + " for resource directory");
        if (latitude == null || longitude == null) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(), "latitude and longitude"));
        }
        final Agency agency = validateAndGetAgencyById(id);

        final List<OffSiteLocation> offSiteLocations = calculateDistanceAndSortOffSiteLocations(agency, latitude,
                longitude);
        agency.setOffSiteLocations(offSiteLocations);
        auditLoggingUtil.logDbGetSingleEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), agency.getId(),
                TargetNameConstants.AGENCY, "Getting the detail of agency (AgencyId: " + agency.getId() + " )");
        return agency;
    }

    private List<OffSiteLocation> calculateDistanceAndSortOffSiteLocations(
            final AgencyWithNamePictures agencyWithNamePictures, final Location locationForDistance) {
        final List<OffSiteLocation> agencyOffSiteLocations = CollectionUtils
                .isEmpty(agencyWithNamePictures.getOffSiteLocations()) ? new ArrayList<>()
                        : agencyWithNamePictures.getOffSiteLocations();
        if (null == locationForDistance
                || (locationForDistance.getLatitude() == null && locationForDistance.getLongitude() == null)) {
            return agencyOffSiteLocations;
        }
        final Agency agency = AgencyMapper.INSTANCE.toAgency(agencyWithNamePictures);

        return calculateDistanceAndSortOffSiteLocations(agency, locationForDistance.getLatitude(),
                locationForDistance.getLongitude());
    }

    /**
     * This method will calculate distance between agency and offsite locations and
     * sort offsite locations based on distance.
     * Adds primary location to offsite location as well. Primary will be always be
     * present at the start of the list.
     * 
     * @param agency
     * @param latitude
     * @param longitude
     * @return
     */
    private List<OffSiteLocation> calculateDistanceAndSortOffSiteLocations(final Agency agency, final double latitude,
            final double longitude) {
        LOGGER.info("calculating distance based on latitude and longitude");
        final List<OffSiteLocation> sortedOffSiteLocations = new ArrayList<>();
        final List<OffSiteLocation> agencyOffSiteLocations = CollectionUtils.isEmpty(agency.getOffSiteLocations())
                ? new ArrayList<>()
                : agency.getOffSiteLocations();
        LOGGER.info("Adding primary location in the offsite locations list");
        final OffSiteLocation primaryLocation = new OffSiteLocation();

        final Location primaryGeoLocationInfo = agency.getGeoLocationInfo();
        primaryLocation.setGeoLocationInfo(primaryGeoLocationInfo);
        primaryLocation.setAddress(agency.getAddress());
        primaryLocation.setLocationName(
                StringUtils.isNotBlank(agency.getNickname()) ? agency.getNickname() : agency.getName());
        primaryLocation.setLanguages(agency.getLanguages());
        primaryLocation.setPrimaryAddress(true);

        if (!CollectionUtils.isEmpty(agency.getContacts())) {
            agency.getContacts().stream().forEach(contact -> {
                if (org.apache.commons.lang3.StringUtils.isNotBlank(contact.getType())) {
                    if (contact.getType().equalsIgnoreCase(ContactTypeEnum.PHONE.getValue())) {
                        primaryLocation.setExtension(contact.getExtension());
                        primaryLocation.setPhone(contact.getValue());
                    } else if (contact.getType().equalsIgnoreCase(ContactTypeEnum.EMAIL.getValue())) {
                        primaryLocation.setEmail(contact.getValue());
                    } else if (contact.getType().equalsIgnoreCase(ContactTypeEnum.TWITTER.getValue())) {
                        primaryLocation.setSocialMedia(contact.getValue());
                    }
                }
            });
        }
        agencyOffSiteLocations.add(primaryLocation);

        agencyOffSiteLocations.forEach(offSiteLocation -> {
            if (offSiteLocation.getGeoLocationInfo() != null
                    && null != offSiteLocation.getGeoLocationInfo().getLatitude()
                    && null != offSiteLocation.getGeoLocationInfo().getLongitude()) {
                final double distance = DistanceCalculator.distance(latitude, longitude,
                        offSiteLocation.getGeoLocationInfo().getLatitude(),
                        offSiteLocation.getGeoLocationInfo().getLongitude(), "");
                offSiteLocation.setProximity(distance);
            }
            sortedOffSiteLocations.add(offSiteLocation);
        });
        Collections.sort(sortedOffSiteLocations, Comparator.comparing(OffSiteLocation::getProximity));

        // select and set shortest offsitelocations from list
        if (!CollectionUtils.isEmpty(sortedOffSiteLocations) && null != sortedOffSiteLocations.get(0)) {
            sortedOffSiteLocations.get(0).setShortestDistance(true);
        }
        return sortedOffSiteLocations;
    }

    private Agency validateAndGetAgencyById(final String id) {
        if (StringUtils.isBlank(id)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(), "agency id"));
        }
        Optional<Agency> agency = agencyRepository.findById(id);

        if (agency.isEmpty()) {
            LOGGER.error("Unable to get Agency");
            throw new CrnCommonException(ErrorType.NOT_FOUND, "Unable to get Agency");
        }
        return agency.get();
    }

    public DownloadedFile downloadAgencyReferralDocument(final String agencyId, final String fileName)
            throws UnsupportedEncodingException {

        final Agency agency = validateAgencyAndFileName(agencyId, fileName);
        if (agency.getReferralInfo() == null || CollectionUtils.isEmpty(agency.getReferralInfo().getReferralDocs())) {
            throw new CrnCommonException(ErrorType.NOT_FOUND, "Invalid File Name or given file not present");
        }
        final Optional<ReferralDoc> optionalReferralDoc = agency.getReferralInfo().getReferralDocs()
                .stream().filter(document -> document.getPaperReferralForm().equals(fileName))
                .findFirst();
        if (optionalReferralDoc.isEmpty()) {
            throw new CrnCommonException(ErrorType.NOT_FOUND, "Invalid File Name or given file not present");
        }
        final ReferralDoc referralDoc = optionalReferralDoc.get();
        final String fileExtension = referralDoc.getFileExtension();
        final String referralDocName = referralDoc.getPaperReferralForm();
        final DownloadedFile document = getDowloadedFile(fileExtension, referralDocName, agencyId);
        auditLoggingUtil.logDbGetSingleEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), agencyId,
                TargetNameConstants.AGENCY,
                "Getting paper referral document against agency (AgencyId: " + agencyId + " )");
        return document;
    }

    public DownloadedFile downloadAgencyAdditionalDocument(final String agencyId, final String fileName) {

        final Agency agency = validateAgencyAndFileName(agencyId, fileName);

        if (agency.getReferralInfo() == null ||
                CollectionUtils.isEmpty(agency.getReferralInfo().getAdditionalDocumentationForms())) {
            throw new CrnCommonException(ErrorType.NOT_FOUND, "Invalid File Name or given file not present");
        }

        final Optional<FileToUpload> optionalFile = agency.getReferralInfo().getAdditionalDocumentationForms()
                .stream().filter(document -> document.getFile().equals(fileName))
                .findFirst();
        if (optionalFile.isEmpty()) {
            throw new CrnCommonException(ErrorType.NOT_FOUND, "Invalid File Name or given file not present");
        }
        final FileToUpload document = optionalFile.get();

        final String fileExtension = document.getFileExtension();
        final String documentName = document.getFile();
        try {
            final DownloadedFile downloadedFile = getDowloadedFile(fileExtension, documentName, agencyId);
            auditLoggingUtil.logDbGetSingleEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), agencyId,
                    TargetNameConstants.AGENCY,
                    "Getting additional document against agency (AgencyId: " + agencyId + " )");
            return downloadedFile;
        } catch (final Exception ex) {
            ex.printStackTrace();
            throw new CrnCommonException(ErrorType.UNEXPECTED, "Unable to download file");
        }
    }

    private DownloadedFile getDowloadedFile(final String fileExtension, final String fileName, final String agencyId)
            throws UnsupportedEncodingException {
        final DownloadedFile downloadedFile = new DownloadedFile();
        final String s3BucketName = mongoDataSources.getS3BucketNameForCurrentTenantId();
        if (fileExtension.equals("doc") || fileExtension.equals("docx") || fileExtension.equals("xls")
                || fileExtension.equals("xlsx")) {
            final long expTimeMillis = 1000 * 60 * 5; // url would be valid for 5 minutes only
            final Date expirationTimeInMillis = new java.util.Date();
            expirationTimeInMillis.setTime(expTimeMillis + expirationTimeInMillis.getTime());
            final String fileUrl = this.awsHelper.getDocumentLinkFromS3(s3BucketName, AGENCY_DATA_VAULT_PATH + agencyId,
                    fileName + "." + fileExtension, expirationTimeInMillis);
            downloadedFile.setFileUrl(URLEncoder.encode(fileUrl, "UTF-8"));
            return downloadedFile;
        }
        final String documentFromS3 = this.awsHelper.downloadDocumentFromS3(s3BucketName,
                AGENCY_DATA_VAULT_PATH + agencyId, fileName);
        downloadedFile.setFileContent(documentFromS3);
        return downloadedFile;
    }

    private Agency validateAgencyAndFileName(final String agencyId, final String fileName) {
        if (StringUtils.isBlank(agencyId)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(), "agency id"));
        }
        if (StringUtils.isBlank(fileName)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(), "file Name"));
        }

        final Optional<Agency> agency = agencyRepository.findById(agencyId);
        if (agency.isEmpty()) {
            throw new CrnCommonException(ErrorType.NOT_FOUND,
                    String.format(ValidationMessages.ENTITY_NOT_FOUND.getMessage(), "agency"));
        }
        return agency.get();
    }

    public List<PermissionGroup> getAccessPermission(final String id) {
        if (StringUtils.isBlank(id)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(), "agency id"));
        }
        try {
            LOGGER.info("get AgencyRequest's data access permission by Id");
            final List<PermissionGroup> permissionGroups = agencyRepository.getDataAccessPermission(id)
                    .getDataAccessPermissions();
            auditLoggingUtil.logDbGetSingleEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), id,
                    TargetNameConstants.AGENCY, "Getting data access permissions (AgencyId: " + id + " )");
            return permissionGroups;
        } catch (final Exception ex) {
            LOGGER.error("Exception in getAccessPermission: " + ex);
            throw new CrnCommonException(ErrorType.UNEXPECTED, "Unable to get Data Access Permission", ex.getMessage());
        }
    }

    public ReferralRequirement getReferralRequiredDocumentation(final String id) {
        if (StringUtils.isBlank(id)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(), "agency id"));
        }
        if (!agencyRepository.existsById(id)) {
            throw new CrnCommonException(ErrorType.NOT_FOUND,
                    String.format(ValidationMessages.ENTITY_NOT_FOUND.getMessage(), "agency"));
        }
        return agencyRepository.getReferralRequirementOfSpecificAgency(id);
    }

    /**
     * get id and name of the the all agencies
     *
     * @return id and name of the the all agencies
     */
    public List<AgencyIdName> getAgenciesIdAndName(final String tenantId, final boolean networkPartnerOnly,
            final boolean onlyDisplayInResourceDirSearch) {
        try {
            LOGGER.info("get id and name of all agencies");
            final CurrentUser currentUserInfo = getCurrentUserInfo();
            final List<AgencyIdName> agencyIdNames = agencyRepository.getAllAgenciesIdName(tenantId, networkPartnerOnly,
                    onlyDisplayInResourceDirSearch);
            agencyIdNames.forEach(agencyIdName -> {
                if (agencyIdName.getNickname() == null) {
                    agencyIdName.setNickname("");
                }
            });
            auditLoggingUtil.logDbGetAllEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), TargetNameConstants.AGENCY,
                    "Getting only id and name of all agencies.");
            return agencyIdNames;
        } catch (final Exception ex) {
            LOGGER.error("Exception in getAgenciesIdAndName: " + ex);
            throw new CrnCommonException(ErrorType.UNEXPECTED, "Unable to get Agencies", ex.getLocalizedMessage());
        }
    }

    /**
     * @param fieldName
     * @param sortDirection
     *                      //@param isActive
     * @param allAgencies   is true when we are getting agency list for any type of
     *                      user it will by pass the super admin check.
     *                      e.g Add/Update agency when selecting referral accepted
     *                      from or marking some agencies favourite.
     * @return
     */
    public List<AgencyShort> getAllAgencies(final String fieldName, final String sortDirection,
            final boolean allAgencies, final String tenantId, final boolean bypassRole,
            final boolean showRegions) {
        try {
            final CurrentUser currentUserInfo = getCurrentUserInfo();
            LOGGER.info("get all agencies data");
            validateSortingParams(fieldName, sortDirection);
            final String sortBaseProperty = SortAgencyFieldsEnum.valueOf(fieldName).getValue();
            final String direction = SortDirectionEnum.valueOf(sortDirection).getValue();
            // will get only current logged-in user's agencies in case of agency admin
            final List<AgencyShort> agencies;
            if (UserRolesEnum.TENANT_ADMIN.getValue().equalsIgnoreCase(currentUserInfo.getRole().getTitle())
                    || bypassRole == true) {
                agencies = agencyRepository.findAllAgencies(sortBaseProperty, Sort.Direction.fromString(direction),
                        null, tenantId, allAgencies);
            } else if (UserRolesEnum.SUPER_ADMIN_QS_SYSTEMS.getValue()
                    .equalsIgnoreCase(currentUserInfo.getRole().getTitle())) {
                agencies = agencyRepository.findAllAgencies(sortBaseProperty, Sort.Direction.fromString(direction),
                        null, tenantId, allAgencies);
            } else {
                agencies = agencyRepository.findAllAgencies(sortBaseProperty, Sort.Direction.fromString(direction),
                        currentUserInfo.getAgency().getId(), tenantId, allAgencies);
            }
            if (showRegions) {
                final List<Region> allRegionsByTenantId = regionRepository
                        .findAllRegionByTenantId(AuthenticationUtil.getCurrentTenantId());
                final HashMap<String, Region> regionHashMap = new LinkedHashMap<>();
                if (!CollectionUtils.isEmpty(allRegionsByTenantId)) {
                    allRegionsByTenantId.forEach(region -> {
                        regionHashMap.put(region.getId(), region);
                    });
                    agencies.forEach(agency -> {
                        if (!CollectionUtils.isEmpty(agency.getRegions())) {
                            final List<Region> regionDetails = new ArrayList<>();
                            agency.getRegions().forEach(region -> {
                                if (null != regionHashMap.get(region.getId())) {
                                    regionDetails.add(regionHashMap.get(region.getId()));
                                }
                            });
                            agency.setRegionDetails(regionDetails);
                        }
                    });
                }
            }
            auditLoggingUtil.logDbGetAllEvent(currentUserInfo, TargetNameConstants.AGENCY, "Getting all agencies.");
            return agencies;
        } catch (final Exception ex) {
            LOGGER.error("Exception in getAllAgencies: " + ex);
            throw new CrnCommonException(ErrorType.UNEXPECTED, "Unable to get all Agencies", ex.getLocalizedMessage());
        }
    }

    /**
     * @param fieldName
     * @param sortDirection
     * @param allAgencies
     * @param searchParam
     * @param bypassRole
     * @param pageNumber
     * @param pageSize
     * @param isConcise
     * @return
     *         a paginated version of getAllAgencies with searchparam incorporated
     *         as well
     */
    public PageableAgencyResponse getAllAgenciesPaginated(final String fieldName, final String sortDirection,
            final boolean allAgencies, final String searchParam, final boolean bypassRole,
            final int pageNumber, final int pageSize, final boolean isConcise) {
        try {
            final CurrentUser currentUserInfo = getCurrentUserInfo();
            final Pageable pageable = PageRequest.of(pageNumber, pageSize);
            final String tenantId = TenantContext.getTenantId();
            LOGGER.info("get all agencies data");
            validateSortingParams(fieldName, sortDirection);
            final String sortBaseProperty = SortAgencyFieldsEnum.valueOf(fieldName).getValue();
            final String direction = SortDirectionEnum.valueOf(sortDirection).getValue();
            // will get only current logged-in user's agencies in case of agency admin
            final PageableAgencyResponse agenciesResponse;
            if (UserRolesEnum.TENANT_ADMIN.getValue().equalsIgnoreCase(currentUserInfo.getRole().getTitle())
                    || bypassRole == true) {
                agenciesResponse = agencyRepository.findAllAgencies(pageable, searchParam, isConcise, sortBaseProperty,
                        Sort.Direction.fromString(direction), null, tenantId, allAgencies);
            } else if (UserRolesEnum.SUPER_ADMIN_QS_SYSTEMS.getValue()
                    .equalsIgnoreCase(currentUserInfo.getRole().getTitle())) {
                agenciesResponse = agencyRepository.findAllAgencies(pageable, searchParam, isConcise, sortBaseProperty,
                        Sort.Direction.fromString(direction), null, tenantId, allAgencies);
            } else {
                agenciesResponse = agencyRepository.findAllAgencies(pageable, searchParam, isConcise, sortBaseProperty,
                        Sort.Direction.fromString(direction), currentUserInfo.getAgency().getId(), tenantId,
                        allAgencies);
            }
            auditLoggingUtil.logDbGetAllEvent(currentUserInfo, TargetNameConstants.AGENCY, "Getting all agencies.");
            return agenciesResponse;
        } catch (final Exception ex) {
            LOGGER.error("Exception in getAllAgencies: " + ex);
            throw new CrnCommonException(ErrorType.UNEXPECTED, "Unable to get all Agencies", ex.getLocalizedMessage());
        }
    }

    /**
     * @param fieldName
     * @param sortDirection
     * @param searchParam
     * @param bypassRole
     * @param pageNumber
     * @param pageSize
     * @return
     *         a paginated version of getAllAgencies with searchparam incorporated
     *         as well
     */
    public PageableAgencyFavoriteResponse getAllAgenciesByFavorite(final String fieldName, final String sortDirection,
            final String searchParam, final boolean bypassRole,
            final int pageNumber, final int pageSize, final String agencyId, final boolean onlyFavorites) {
        try {
            final CurrentUser currentUserInfo = getCurrentUserInfo();
            final Pageable pageable = PageRequest.of(pageNumber, pageSize);
            final String tenantId = TenantContext.getTenantId();
            validateSortingParams(fieldName, sortDirection);
            final String sortBaseProperty = SortAgencyFieldsEnum.valueOf(fieldName).getValue();
            final String direction = SortDirectionEnum.valueOf(sortDirection).getValue();
            final PageableAgencyFavoriteResponse agenciesResponse;
            if (UserRolesEnum.TENANT_ADMIN.getValue().equalsIgnoreCase(currentUserInfo.getRole().getTitle())
                    || bypassRole == true) {
                agenciesResponse = agencyRepository.findAllAgenciesByFavorite(pageable, searchParam, onlyFavorites,
                        agencyId, sortBaseProperty, Sort.Direction.fromString(direction), tenantId);
            } else if (UserRolesEnum.SUPER_ADMIN_QS_SYSTEMS.getValue()
                    .equalsIgnoreCase(currentUserInfo.getRole().getTitle())) {
                agenciesResponse = agencyRepository.findAllAgenciesByFavorite(pageable, searchParam, onlyFavorites,
                        agencyId, sortBaseProperty, Sort.Direction.fromString(direction), tenantId);
            } else {
                agenciesResponse = agencyRepository.findAllAgenciesByFavorite(pageable, searchParam, onlyFavorites,
                        agencyId, sortBaseProperty, Sort.Direction.fromString(direction), tenantId);
            }
            if (CollectionUtils.isEmpty(agenciesResponse.getData()) || onlyFavorites || StringUtils.isBlank(agencyId)) {
                return agenciesResponse;
            } else {
                final Agency agencyShort = agencyRepository.getFavoriteAgenciesById(agencyId);
                if (agencyShort == null) {
                    return agenciesResponse;
                }
                if (CollectionUtils.isEmpty(agencyShort.getAgencyFavorites())) {
                    return agenciesResponse;
                }
                final List<String> agencyFavorites = agencyShort.getAgencyFavorites().stream()
                        .map(agency -> agency.getId()).collect(Collectors.toList());
                agenciesResponse.getData().forEach(agency -> {
                    if (agencyFavorites.contains(agency.getId())) {
                        agency.setFavorite(true);
                    } else {
                        agency.setFavorite(false);
                    }
                });
            }
            auditLoggingUtil.logDbGetAllEvent(currentUserInfo, TargetNameConstants.AGENCY,
                    "Getting all favorite agencies.");
            return agenciesResponse;
        } catch (final Exception ex) {
            LOGGER.error("Exception in getAllAgenciesByFavorite: " + ex);
            throw new CrnCommonException(ErrorType.UNEXPECTED, "Unable to get all favorite Agencies",
                    ex.getLocalizedMessage());
        }
    }

    private void validateSortingParams(final String fieldName, final String sortDirection) {
        if (!EnumUtils.isValidEnum(SortAgencyFieldsEnum.class, fieldName)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "Enter Valid fieldName");
        }
        if (!EnumUtils.isValidEnum(SortDirectionEnum.class, sortDirection)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "Enter Valid direction");
        }

    }

    public String deleteAgency(final String id) {
        if (StringUtils.isBlank(id)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(), "agency id"));
        }
        final Optional<Agency> agency = agencyRepository.findById(id);
        if (agency.isEmpty()) {
            throw new CrnCommonException(ErrorType.NOT_FOUND, "Agency not found.");
        }
        try {
            LOGGER.info("delete agency by id");
            agencyRepository.deleteById(id);
            auditLoggingUtil.logDeleteEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), agency.get(),
                    agency.get().getId(), TargetNameConstants.AGENCY,
                    "Deleting the agency (AgencyId: " + agency.get().getId() + " )");
            return "Agency Deleted Successfully";
        } catch (final Exception ex) {
            LOGGER.error("Exception in deleteAgency: " + ex);
            throw new CrnCommonException(ErrorType.UNEXPECTED, "Unable to get delete AgencyRequest");
        }
    }

    public List<AgencyByService> getAgenciesByCategoriesForApiInterface(final AgencySearchByCategory searchByCategory,
            final String tenantId) {
        // For API interface get lat/long through Zip Code using Google Map API
        if (null != searchByCategory && StringUtils.isNotBlank(searchByCategory.getZipCode())) {
            if (!isValidZipCode(searchByCategory.getZipCode())) {
                throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "Zip Code is not valid");
            }
            final Location location = googleMapHelper.getLocationUsingZipCode(searchByCategory.getZipCode());
            if (null == location) {
                throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "Address is not valid");
            }
            searchByCategory.setZipCode(null);
            searchByCategory.setLocation(location);
        }
        return getAgenciesByCategories(searchByCategory, tenantId);
    }

    private boolean isValidZipCode(final String zipCode) {
        final String regex = "^[0-9]{5}(?:-[0-9]{4})?$";
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(zipCode);
        return (matcher.find() && matcher.group().equals(zipCode));
    }

    public List<AgencyByService> getAgenciesByCategories(final AgencySearchByCategory searchByCategory,
            final String tenantId) {
        try {
            LOGGER.info("get searched agency by given categories");
            // get user info based on jwt token
            final String userId = AuthenticationUtil.getUserIdFromJwtToken();
            final String jwtToken = AuthenticationUtil.getJwtToken();
            final CurrentUser currentUserInfo = httpRequestHelper
                    .getCurrentUserInfo(endPoints.getGetCurrentUserInfoPath(), userId);
            final List<SelectedAgencyByCategory> agencies = new ArrayList<>(agencyRepository.searchAgenciesByCategories(
                    searchByCategory.getCategories(), searchByCategory.getAgeGroup(), searchByCategory.getIncomeLevel(),
                    searchByCategory.getKeywords(),
                    searchByCategory.getPublicTransitAccess(), searchByCategory.getZipCode(),
                    searchByCategory.getEmploymentStatus(),
                    searchByCategory.getInsuranceType(), searchByCategory.getVeteranStatus(),
                    searchByCategory.getStreetAddress(), searchByCategory.getCounty(), searchByCategory.getTenantIds(),
                    searchByCategory.getCity(), searchByCategory.getRegionIds(), tenantId,
                    searchByCategory.getNetworkPartnerId()));

            final List<SelectedAgencyByCategory> generalReferralHubAgency = agencyRepository
                    .findByGeneralReferralHubIdInAndGeneralReferralHubAndDisplayInResourceDirSearch(
                            searchByCategory.getTenantIds(), true, true);
            generalReferralHubAgency.stream().forEach(referralAgency -> {
                SelectedAgencyByCategory agency211 = agencies.stream()
                        .filter(agency -> referralAgency.getId().equals(agency.getId()))
                        .findFirst()
                        .orElse(null);
                if (agency211 != null) {
                    agencies.remove(agency211);
                }
            });

            // filter out 5 agencies and remove from list
            var filteredOutAgencies = agencies.stream()
                    .filter(agency -> (agency.getId().equals("5d52caaa149b3f0006e63dcd")
                            || agency.getId().equals("5d5d76bf149b3f0006e63dd5")
                            || agency.getId().equals("5d63f664149b3f0006e63ddb") ||
                            agency.getId().equals("5d6e7a35149b3f0006e63de6")
                            || agency.getId().equals("5d6e82f0149b3f0006e63de7")))
                    .collect(Collectors.toList());
            agencies.removeAll(filteredOutAgencies);

            final List<SelectedAgencyByCategory> selectedAgencies;
            // filter agencies based on the location if present
            if (searchByCategory.getLocation() != null) {
                selectedAgencies = filterAgenciesBasedOnLocation(searchByCategory.getLocation(), agencies);
            } else {
                selectedAgencies = agencies;
            }

            // Get users personal favorite agencies plus the favorite agencies of the user
            // associated agency
            final List<FavoriteAgency> agenciesId = getCurrentUserFavoriteAgencies(currentUserInfo);

            final Map<String, List<AgencyByCategory>> map = getOrganizedAgencyData(selectedAgencies,
                    searchByCategory.getCategories(), agenciesId, searchByCategory.getLocationForDistance());

            // This Async Task will add the Resource Directory Search Info in the
            // report-microservice for dashboard
            CompletableFuture.runAsync(() -> {
                final UserId userIdObj = new UserId();
                userIdObj.setUserId(currentUserInfo.getId());
                final String requestPath = endPoints.getAddResourceDirectorySearchInReport();
                httpRequestHelper.addResDirectorySearchInfoInReportSystem(requestPath, userIdObj, jwtToken, tenantId);
            });

            List<AgencyByService> agencyByServices = map.keySet().stream().map(key -> {
                final AgencyByService agencyByService = new AgencyByService();
                final List<AgencyByCategory> agencyByCategoryList = map.get(key);
                if (!CollectionUtils.isEmpty(agencyByCategoryList)) {
                    agencyByService.setService(key);
                    agencyByService.setCategories(agencyByCategoryList);
                    return agencyByService;
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());

            generalReferralHubAgency.stream().forEach(agency -> addAgency211(agency, agencyByServices, agenciesId));

            auditLoggingUtil.logSearchEvent(currentUserInfo, searchByCategory, TargetNameConstants.AGENCY,
                    "Searching agencies by specified categories, age_group, income etc.");
            return agencyByServices;
        } catch (final Exception ex) {
            LOGGER.error("Exception in getAgenciesByCategories: " + ex);
            ex.printStackTrace();
            throw new CrnCommonException(ErrorType.NOT_FOUND, "No Record Matches Your Search Criteria");
        }
    }

    public List<AgencyByService> getAgencyByCategoriesAndRequestParamsV2(final AgencySearchByCategory searchByCategory,
            final String tenantId) {
        try {
            LOGGER.info("get searched agency by given categories");
            // get user info based on jwt token
            final String userId = AuthenticationUtil.getUserIdFromJwtToken();
            final String jwtToken = AuthenticationUtil.getJwtToken();
            final CurrentUser currentUserInfo = httpRequestHelper
                    .getCurrentUserInfo(endPoints.getGetCurrentUserInfoPath(), userId);

            final List<SelectedAgencyByCategory> agencies = new ArrayList<>(agencyRepository.searchAgenciesByCategories(
                    searchByCategory.getCategories(), searchByCategory.getAgeGroup(), searchByCategory.getIncomeLevel(),
                    searchByCategory.getKeywords(),
                    searchByCategory.getPublicTransitAccess(), searchByCategory.getZipCode(),
                    searchByCategory.getEmploymentStatus(),
                    searchByCategory.getInsuranceType(), searchByCategory.getVeteranStatus(),
                    searchByCategory.getStreetAddress(), searchByCategory.getCounty(), searchByCategory.getTenantIds(),
                    searchByCategory.getCity(), searchByCategory.getRegionIds(), tenantId,
                    searchByCategory.getNetworkPartnerId()));

            final List<SelectedAgencyByCategory> generalReferralHubAgency = agencyRepository
                    .findByGeneralReferralHubIdInAndGeneralReferralHubAndDisplayInResourceDirSearch(
                            searchByCategory.getTenantIds(), true, true);

            // to reduce inner loops processing
            final Map<String, SelectedAgencyByCategory> generalReferralHubAgencyMap;
            if (CollectionUtils.isEmpty(generalReferralHubAgency)) {
                generalReferralHubAgencyMap = new LinkedHashMap<>();
            } else {
                generalReferralHubAgencyMap = generalReferralHubAgency.stream()
                        .filter(obj -> null != obj || StringUtils.isNotBlank(obj.getId()))
                        .collect(Collectors.toMap(selectedAgencyByCategory -> selectedAgencyByCategory != null
                                ? selectedAgencyByCategory.getId()
                                : null, obj -> obj));
            }

            final List<SelectedAgencyByCategory> agenciesAfterReferralHubFiltered = agencies
                    .stream()
                    .filter(
                            agency -> !generalReferralHubAgencyMap.containsKey(agency.getId()))
                    .collect(Collectors.toList());

            final List<SelectedAgencyByCategory> selectedAgencies;
            // filter agencies based on the location if present
            if (searchByCategory.getLocation() != null) {
                selectedAgencies = filterAgenciesBasedOnLocation(searchByCategory.getLocation(),
                        agenciesAfterReferralHubFiltered);
            } else {
                selectedAgencies = agenciesAfterReferralHubFiltered;
            }

            // This Async Task will add the Resource Directory Search Info in the
            // report-microservice for dashboard
            CompletableFuture.runAsync(() -> {
                final UserId userIdObj = new UserId();
                userIdObj.setUserId(currentUserInfo.getId());
                final String requestPath = endPoints.getAddResourceDirectorySearchInReport();
                httpRequestHelper.addResDirectorySearchInfoInReportSystem(requestPath, userIdObj, jwtToken, tenantId);
            });

            final List<String> favoriteAgencyIds;
            if (searchByCategory.isOnlyFavourite()) {
                favoriteAgencyIds = getCurrentUserAndAgencyFavoriteAgenciesIds(currentUserInfo);
            } else {
                favoriteAgencyIds = new ArrayList<>();
            }
            final List<AgencyByService> agencyByServices = getOrganizedServiceAndCategoriesDataV2(selectedAgencies,
                    searchByCategory.getCategories(), favoriteAgencyIds);
            generalReferralHubAgency.stream().forEach(agency -> addAgency211(agency, agencyByServices, null));

            auditLoggingUtil.logSearchEvent(currentUserInfo, searchByCategory, TargetNameConstants.AGENCY,
                    "Searching agencies by specified categories, age_group, income etc.");
            // NP-9273 return error message No Record Matches Your Search Criteria
            if (CollectionUtils.isEmpty(agencyByServices)) {
                throw new CrnCommonException(ErrorType.NOT_FOUND, "No Record Matches Your Search Criteria");
            }
            return agencyByServices;
        } catch (final Exception ex) {
            LOGGER.error("Exception in getAgenciesByCategories: " + ex);
            ex.printStackTrace();
            throw new CrnCommonException(ErrorType.NOT_FOUND, "No Record Matches Your Search Criteria");
        }
    }

    private List<AgencyByService> getOrganizedServiceAndCategoriesDataV2(
            final List<SelectedAgencyByCategory> selectedAgencies, final List<String> searchedCategories,
            final List<String> favouriteAgencyIds) {

        LOGGER.info("getting Organized Service And Categories Data V2");
        // create Map for service with categories List for fast processing
        final Map<String, List<String>> serviceCategoryMap = new LinkedHashMap<>();
        selectedAgencies.stream().forEach(agency -> {
            // NP-8819 filter on favourite agencies
            if (!CollectionUtils.isEmpty(favouriteAgencyIds) && !favouriteAgencyIds.contains(agency.getId())) {
                return;
            }
            if (!CollectionUtils.isEmpty(agency.getServices())) {
                agency.getServices().stream().forEach(service -> {
                    if (!CollectionUtils.isEmpty(service.getCategories())) {
                        service.getCategories().stream().forEach(category -> {
                            final String serviceName = service.getName();
                            final String categoryName = category.getName();

                            if (StringUtils.isBlank(serviceName) || StringUtils.isBlank(categoryName)) {
                                return;
                            }
                            // add only those categories which are search else add all categories
                            if (CollectionUtils.isEmpty(searchedCategories)
                                    || searchedCategories.contains(categoryName)) {

                                if (!serviceCategoryMap.containsKey(serviceName)) {
                                    serviceCategoryMap.put(serviceName, new ArrayList<>());
                                }
                                if (!serviceCategoryMap.get(serviceName).contains(categoryName)) {
                                    serviceCategoryMap.get(serviceName).add(categoryName);
                                }
                            }
                        });
                    }
                });
            }
        });

        // Use existing structure to avoid any major ripples on the FE.
        final List<AgencyByService> agencyByServices = serviceCategoryMap.keySet().stream().map(key -> {
            final AgencyByService agencyByService = new AgencyByService();
            final List<String> agencyByCategoryList = serviceCategoryMap.get(key);
            if (!CollectionUtils.isEmpty(agencyByCategoryList)) {
                agencyByService.setService(key);
                final List<AgencyByCategory> categories = new ArrayList<>();

                agencyByCategoryList.stream().forEach(s -> {
                    final AgencyByCategory agencyByCategory = new AgencyByCategory();
                    agencyByCategory.setCategoryName(s);
                    categories.add(agencyByCategory);
                });

                agencyByService.setCategories(categories);
                return agencyByService;
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        return agencyByServices;
    }

    /*
     * This function will return user personal favorite + the associated agency
     * favorite agencies
     */
    private List<FavoriteAgency> getCurrentUserFavoriteAgencies(final CurrentUser currentUserInfo) {
        final List<FavoriteAgency> favoriteAgencies = new ArrayList<>();

        // step 1: get current user favorite agencies
        final List<AgencyIdName> userFavoriteAgencies = currentUserInfo.getUserFavoriteAgencies();
        if (!CollectionUtils.isEmpty(userFavoriteAgencies)) {
            favoriteAgencies.addAll(userFavoriteAgencies.stream().map(userFavoriteAgency -> {
                final FavoriteAgency favoriteAgency = new FavoriteAgency();
                favoriteAgency.setAgencyId(userFavoriteAgency.getId());
                favoriteAgency.setUserFavorite(true);
                return favoriteAgency;
            }).collect(Collectors.toList()));

        }

        // step 2: get favorite agencies of parent agency(associated agency)
        final AgencyIdName userAssociatedAgency = currentUserInfo.getAgency();
        final Agency agency = agencyRepository.getFavoriteAgenciesById(userAssociatedAgency.getId());
        if (agency != null && !CollectionUtils.isEmpty(agency.getAgencyFavorites())) {

            favoriteAgencies.addAll(agency.getAgencyFavorites().stream().map(agencyFavorite -> {

                final FavoriteAgency favoriteAgency = new FavoriteAgency();
                favoriteAgency.setAgencyId(agencyFavorite.getId());
                favoriteAgency.setAgencyFavorite(true);

                // if agency already exist , remove it and update the userFavorite attribute in
                // it
                final OptionalInt indexOpt = IntStream.range(0, favoriteAgencies.size())
                        .filter(i -> favoriteAgencies.get(i).getAgencyId().equals(agencyFavorite.getId())).findFirst();
                if (indexOpt.isPresent()) {
                    favoriteAgencies.remove(indexOpt.getAsInt());
                    favoriteAgency.setUserFavorite(true);
                }
                return favoriteAgency;
            }).collect(Collectors.toList()));
        }
        return favoriteAgencies;
    }

    private List<String> getCurrentUserAndAgencyFavoriteAgenciesIds(final CurrentUser currentUserInfo) {
        final List<String> favoriteAgencies = new ArrayList<>();

        // step 1: get current user favorite agencies
        final List<AgencyIdName> userFavoriteAgencies = currentUserInfo.getUserFavoriteAgencies();
        if (!CollectionUtils.isEmpty(userFavoriteAgencies)) {
            favoriteAgencies
                    .addAll(userFavoriteAgencies.stream().map(AgencyIdName::getId).collect(Collectors.toList()));
        }

        // step 2: get favorite agencies of parent agency(associated agency)
        final AgencyIdName userAssociatedAgency = currentUserInfo.getAgency();
        final Agency agency = agencyRepository.getFavoriteAgenciesById(userAssociatedAgency.getId());
        if (agency != null && !CollectionUtils.isEmpty(agency.getAgencyFavorites())) {
            favoriteAgencies
                    .addAll(agency.getAgencyFavorites().stream().map(AgencyIdName::getId).collect(Collectors.toList()));
        }
        // ids can be duplicated but no need to filter unique ids.
        return favoriteAgencies;
    }

    // its a map of category name and list of agencies that contains that category
    private Map<String, List<AgencyByCategory>> getOrganizedAgencyData(final List<SelectedAgencyByCategory> agencies,
            final List<String> categories,
            final List<FavoriteAgency> favoriteAgencies,
            final Location locationForDistance) {
        Map<String, List<AgencyWithNamePictures>> categoryMap = new LinkedHashMap<>();
        Map<String, List<AgencyByCategory>> agencyServiceMap = new LinkedHashMap<>();
        Map<String, List<String>> serviceCategoryMap = new LinkedHashMap<>();

        // to reduce inner loops processing
        final Map<String, FavoriteAgency> favoriteAgenciesMap;
        if (CollectionUtils.isEmpty(favoriteAgencies)) {
            favoriteAgenciesMap = new LinkedHashMap<>();
        } else {
            favoriteAgenciesMap = favoriteAgencies.stream()
                    .filter(obj -> null != obj || StringUtils.isNotBlank(obj.getAgencyId()))
                    .collect(Collectors.toMap(FavoriteAgency::getAgencyId, obj -> obj));
        }

        if (CollectionUtils.isEmpty(categories)) //
        {
            if (!CollectionUtils.isEmpty(agencies)) {
                agencies.stream().forEach(agnency -> {
                    if (!CollectionUtils.isEmpty(agnency.getServices())) {
                        agnency.getServices().stream().forEach(service -> {
                            if (!CollectionUtils.isEmpty(service.getCategories())) {
                                service.getCategories().stream().forEach(category -> {
                                    if (!categories.contains(category.getName())) {
                                        if (StringUtils.isNotBlank(category.getName()))
                                            categories.add(category.getName());
                                    }
                                });
                            }
                        });
                    }
                });
            }
        }
        categories.add("211 - Resource & Referral Specialists");
        categories.stream().forEach(x -> categoryMap.put(x, new ArrayList<AgencyWithNamePictures>()));
        if (!CollectionUtils.isEmpty(agencies)) {
            agencies.stream().forEach(
                    x -> {
                        if (!CollectionUtils.isEmpty(x.getServices())) {
                            x.getServices().stream().forEach(
                                    service -> {
                                        if (!CollectionUtils.isEmpty(service.getCategories())) {
                                            service.getCategories().stream().forEach(
                                                    category -> {
                                                        if (categoryMap.containsKey(category.getName())) {
                                                            List<String> categoryNames = serviceCategoryMap
                                                                    .get(service.getName());
                                                            if (CollectionUtils.isEmpty(categoryNames)) {
                                                                categoryNames = new ArrayList<>();
                                                            }
                                                            List<AgencyWithNamePictures> agencyList = categoryMap
                                                                    .get(category.getName());
                                                            final AtomicBoolean checkAgencyPresentInAgencyList = new AtomicBoolean(
                                                                    false);
                                                            agencyList.forEach(
                                                                    agencyObj -> {
                                                                        if (agencyObj.getId().equals(x.getId())) {
                                                                            checkAgencyPresentInAgencyList.set(true);
                                                                        }
                                                                    });
                                                            if (!checkAgencyPresentInAgencyList.get()) {
                                                                final AgencyWithNamePictures agencyWithNamePictures = new AgencyWithNamePictures(
                                                                        x.getId(), x.getName(), x.getPictures(),
                                                                        x.getAbbreviation(),
                                                                        x.getGeoLocationInfo(), x.getOffSiteLocations(),
                                                                        x.getReferralAcceptedFrom(),
                                                                        x.isReferralAcceptedFromAll(), x.getNickname(),
                                                                        x.getIsExpress(), x.getIsLog(),
                                                                        x.getIsElectronic(), x.getIsPaper(),
                                                                        x.getOrganizationType(),
                                                                        x.getAssociatedOrgsByOrgType(),
                                                                        x.getAdditionalDocumentationForms(),
                                                                        !x.getIsOutOfNetwork(), x.getTenantIds(),
                                                                        x.getIsOutOfNetwork(),
                                                                        x.getOutOfNetworkReferralInfo(), x.getAddress(),
                                                                        x.getContacts(), x.getLanguages());

                                                                if (favoriteAgenciesMap
                                                                        .containsKey(agencyWithNamePictures.getId())) {
                                                                    final FavoriteAgency matchedAgency = favoriteAgenciesMap
                                                                            .get(agencyWithNamePictures.getId());
                                                                    if (matchedAgency.isAgencyFavorite()) {
                                                                        agencyWithNamePictures.setAgencyFavorite(true);
                                                                    }
                                                                    if (matchedAgency.isUserFavorite()) {
                                                                        agencyWithNamePictures.setUserFavorite(true);
                                                                    }
                                                                }

                                                                // calculating distance and sorting offsite locations
                                                                final List<OffSiteLocation> offSiteLocationsSorted = calculateDistanceAndSortOffSiteLocations(
                                                                        agencyWithNamePictures, locationForDistance);
                                                                agencyWithNamePictures
                                                                        .setOffSiteLocations(offSiteLocationsSorted);
                                                                if (!CollectionUtils.isEmpty(offSiteLocationsSorted)
                                                                        && null != offSiteLocationsSorted.get(0)) {
                                                                    agencyWithNamePictures
                                                                            .setProximity(offSiteLocationsSorted.get(0)
                                                                                    .getProximity());
                                                                }

                                                                agencyList.add(agencyWithNamePictures);
                                                            }
                                                            if (!categoryNames.contains(category.getName())) {
                                                                categoryNames.add(category.getName());
                                                            }
                                                            if (StringUtils.isNotEmpty(category.getName())) {
                                                                categoryMap.put(category.getName(), agencyList);
                                                            }
                                                            serviceCategoryMap.put(service.getName(), categoryNames);
                                                        }
                                                    });
                                        }
                                    });
                        }
                    });
        }
        final List<AgencyByCategory> agencyByCategoryList = categoryMap.keySet().stream().map(key -> {
            final AgencyByCategory agencyByCategory = new AgencyByCategory();
            final List<AgencyWithNamePictures> agencyList = categoryMap.get(key);
            if (!CollectionUtils.isEmpty(agencyList)) {
                agencyByCategory.setCategoryName(key);

                final List<AgencyWithNamePictures> agencyListSorted = agencyListSorting(agencyList);
                agencyByCategory.setAgencies(agencyListSorted);

                return agencyByCategory;
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        serviceCategoryMap.keySet().forEach(key -> {
            final List<String> categoryNames = serviceCategoryMap.get(key);
            categoryNames.forEach(categoryName -> {
                final List<AgencyByCategory> correspondingAgencies = agencyByCategoryList.stream()
                        .filter(dto -> dto.getCategoryName().equals(categoryName)).collect(Collectors.toList());
                final List<AgencyByCategory> mapList = agencyServiceMap.containsKey(key) ? agencyServiceMap.get(key)
                        : new ArrayList<>();

                mapList.addAll(correspondingAgencies);
                agencyServiceMap.put(key, mapList);
            });
        });
        return agencyServiceMap;
    }

    public List<AgencyWithNamePictures> getAgenciesBySubCategoriesForApiInterface(
            final AgencySearchBySubCategory searchBySubCategory, final String tenantId) {
        // For API interface get lat/long through Zip Code using Google Map API
        if (null != searchBySubCategory && StringUtils.isNotBlank(searchBySubCategory.getZipCode())) {
            if (!isValidZipCode(searchBySubCategory.getZipCode())) {
                throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "Zip Code is not valid");
            }
            final Location location = googleMapHelper.getLocationUsingZipCode(searchBySubCategory.getZipCode());
            if (null == location) {
                throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "Address is not valid");
            }
            searchBySubCategory.setZipCode(null);
            searchBySubCategory.setLocation(location);
        }
        return getAgenciesBySubCategories(searchBySubCategory, tenantId);
    }

    /**
     * Search the Agencies on the basis of subCategories within the specific
     * Category
     *
     * @param searchBySubCategory
     * @return
     */
    public List<AgencyWithNamePictures> getAgenciesBySubCategories(final AgencySearchBySubCategory searchBySubCategory,
            final String tenantId) {
        final Set<ConstraintViolation<AgencySearchBySubCategory>> violations = validator.validate(searchBySubCategory);
        if (!CollectionUtils.isEmpty(violations)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    ValidationMessageUtils.getViolationMessages(violations));
        }
        LOGGER.info("get searched agency by given category and its sub-categories");
        // put if condition to be on save side
        if (!StringUtils.isBlank(searchBySubCategory.getCategory().getName())) {
            final com.crn.agency.domain.Category categoryModel = AgencyMapper.INSTANCE
                    .toCategoryDto(searchBySubCategory.getCategory());
            final List<String> subCategories;
            if (CollectionUtils.isEmpty(categoryModel.getSubCategories())) {
                subCategories = new ArrayList<>();
            } else {
                subCategories = categoryModel.getSubCategories().stream().map(SubCategory::getName)
                        .collect(Collectors.toList());
            }

            final List<String> languageValues;
            if (CollectionUtils.isEmpty(searchBySubCategory.getLanguages())) {
                languageValues = null;
            } else {
                languageValues = searchBySubCategory.getLanguages().stream().map(Language::getValue)
                        .collect(Collectors.toList());
            }
            // get user info based on jwt token
            final String userId = AuthenticationUtil.getUserIdFromJwtToken();
            final CurrentUser currentUserInfo = httpRequestHelper
                    .getCurrentUserInfo(endPoints.getGetCurrentUserInfoPath(), userId);
            // final List<String> tenantIds =
            // agencyRepository.getTenantIdsByAgencyId(currentUserInfo.getAgency().getId());
            final List<AgencyWithNamePictures> narrowDownAgencies = agencyRepository.searchAgenciesBySubCategories(
                    searchBySubCategory.getCategory().getName(), subCategories, searchBySubCategory.getKeywords(),
                    searchBySubCategory.getInsuranceType(), searchBySubCategory.getAgeGroup(),
                    searchBySubCategory.getIncomeLevel(), languageValues, searchBySubCategory.getVeteranStatus(),
                    searchBySubCategory.getCounty(), searchBySubCategory.getTenantIds(), searchBySubCategory.getCity(),
                    searchBySubCategory.getRegionIds(), tenantId, searchBySubCategory.getNetworkPartnerId());

            if (CollectionUtils.isEmpty(narrowDownAgencies)) {
                throw new CrnCommonException(ErrorType.NOT_FOUND,
                        "Unable to find any agency based on the search criteria");
            }

            final List<SelectedAgencyByCategory> generalReferralHubAgency = agencyRepository
                    .findByGeneralReferralHubIdInAndGeneralReferralHubAndDisplayInResourceDirSearch(
                            searchBySubCategory.getTenantIds(), true, true);

            // to reduce inner loops processing
            final Map<String, SelectedAgencyByCategory> generalReferralHubAgencyMap;
            if (CollectionUtils.isEmpty(generalReferralHubAgency)) {
                generalReferralHubAgencyMap = new LinkedHashMap<>();
            } else {
                generalReferralHubAgencyMap = generalReferralHubAgency.stream()
                        .filter(obj -> null != obj || StringUtils.isNotBlank(obj.getId()))
                        .collect(Collectors.toMap(selectedAgencyByCategory -> selectedAgencyByCategory != null
                                ? selectedAgencyByCategory.getId()
                                : null, obj -> obj));
            }

            final List<AgencyWithNamePictures> agenciesAfterReferralHubFiltered = narrowDownAgencies
                    .stream()
                    .filter(
                            agency -> !generalReferralHubAgencyMap.containsKey(agency.getId()))
                    .collect(Collectors.toList());

            // Get users personal favorite agencies plus the favorite agencies of the user
            // associated agency
            final List<FavoriteAgency> favoriteAgencies = getCurrentUserFavoriteAgencies(currentUserInfo);

            final List<AgencyWithNamePictures> agenciesList;
            if (searchBySubCategory.getLocation() != null) {
                agenciesList = filterAgenciesBasedOnLocationForSubCateogriesSearch(searchBySubCategory.getLocation(),
                        agenciesAfterReferralHubFiltered);
            } else {
                agenciesList = agenciesAfterReferralHubFiltered;
            }
            final List<AgencyWithNamePictures> agenciesListAfterFavouriteFilter = setCrnPartnerAndFavoriteAgenciesAndOffsiteLocationSortingAndFavoriteAgencyFilter(
                    agenciesList, favoriteAgencies, searchBySubCategory);

            final List<AgencyWithNamePictures> agenciesListSorted = agencyListSorting(agenciesListAfterFavouriteFilter);

            auditLoggingUtil.logSearchEvent(currentUserInfo, searchBySubCategory, TargetNameConstants.AGENCY,
                    "Searching agencies by specifying the sub-categories of specific category.");
            return agenciesListSorted;
        }
        return null;
    }

    private List<AgencyWithNamePictures> setCrnPartnerAndFavoriteAgenciesAndOffsiteLocationSortingAndFavoriteAgencyFilter(
            final List<AgencyWithNamePictures> agenciesList,
            final List<FavoriteAgency> favoriteAgencies,
            final AgencySearchBySubCategory searchBySubCategory) {

        final List<AgencyWithNamePictures> agenciesListAfterFavouriteFilter = new ArrayList<>();
        // to reduce inner loops processing
        final Map<String, FavoriteAgency> favoriteAgenciesMap;
        if (CollectionUtils.isEmpty(favoriteAgencies)) {
            favoriteAgenciesMap = new LinkedHashMap<>();
        } else {
            favoriteAgenciesMap = favoriteAgencies.stream()
                    .filter(obj -> null != obj || StringUtils.isNotBlank(obj.getAgencyId()))
                    .collect(Collectors.toMap(FavoriteAgency::getAgencyId, obj -> obj));
        }

        agenciesList.forEach(agency -> {
            // there is favourite filter and current agency is favourite.
            if (searchBySubCategory != null && searchBySubCategory.isOnlyFavourite()
                    && !favoriteAgenciesMap.containsKey(agency.getId())) {
                return;
            }

            // calculating distance and sorting offsite locations
            final List<OffSiteLocation> offSiteLocationsSorted = calculateDistanceAndSortOffSiteLocations(agency,
                    searchBySubCategory.getLocationForDistance());
            agency.setOffSiteLocations(offSiteLocationsSorted);
            if (!CollectionUtils.isEmpty(offSiteLocationsSorted)
                    && null != offSiteLocationsSorted.get(0)) {
                agency.setProximity(offSiteLocationsSorted.get(0).getProximity());
            }
            agency.setCrnPartner(!agency.getIsOutOfNetwork());
            if (favoriteAgenciesMap.containsKey(agency.getId())) {
                final FavoriteAgency matchedAgency = favoriteAgenciesMap.get(agency.getId());
                if (matchedAgency.isAgencyFavorite()) {
                    agency.setAgencyFavorite(true);
                }
                if (matchedAgency.isUserFavorite()) {
                    agency.setUserFavorite(true);
                }
            }
            agenciesListAfterFavouriteFilter.add(agency);
        });

        return agenciesListAfterFavouriteFilter;
    }

    public List<AgencyReferral> saveAgencyReferral(final List<AgencyReferral> agencyReferrals) {
        List<AgencyReferral> finalAgencyList = new LinkedList<>();
        final List<String> agencyIds = agencyReferrals.stream().map(AgencyReferral::getAgencyId)
                .collect(Collectors.toList());
        final List<Agency> activeAgencies = agencyRepository.getActiveAgencyIds(agencyIds);

        if (activeAgencies.size() > 0) {
            // LOGGER.info("active agency list size is greater than zero. Call the post api
            // to send referrals");
            //
            // finalAgencyList = agencyReferrals.stream().filter(agency ->
            // {
            // return activeAgencies.contains(agency.getAgencyId());
            // }).collect(Collectors.toList());
            // if (finalAgencyList.size() > 0) {
            // LOGGER.info("agency list size is greater than zero. Call the post api to send
            // referrals");
            // post("http://" + SERVER_ADDRESS + AGENCY_REFERRAL,
            // finalAgencyList.toString());
            // }
            final String path = endPoints.getAgencyReferral();
            final List<AgencyReferral> careTasks = httpRequestHelper.createReferralTaskForAgency(path, agencyReferrals);
            if (CollectionUtils.isEmpty(careTasks)) {
                throw new CrnCommonException(ErrorType.UNEXPECTED, "Unable to create a referral care task for agency");
            }
            auditLoggingUtil.logDbGetAllEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), TargetNameConstants.AGENCY,
                    "Getting details of the agency referrals.");
            return careTasks;
        }
        return finalAgencyList;

    }

    public List<AgencyIdName> getReferredAgencies(final String agencies) {
        final List<String> AgencyIds = new ArrayList<>(Arrays.asList(agencies.split(",")));
        if (CollectionUtils.isEmpty(AgencyIds)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(), "Agencies"));
        }
        final List<Agency> activeAgencies = agencyRepository.getActiveAgencyIds(AgencyIds);
        if (CollectionUtils.isEmpty(activeAgencies) || (activeAgencies.size() != AgencyIds.size())) {
            throw new CrnCommonException(ErrorType.NOT_FOUND,
                    String.format(ValidationMessages.ENTITY_NOT_FOUND.getMessage(), "Some of given agencies"));
        }
        final List<AgencyIdName> agencyIdNames = activeAgencies.stream().map(agency -> {
            final AgencyIdName agencyIdName = AgencyMapper.INSTANCE.toAgencyIdName(agency);
            return agencyIdName;
        }).collect(Collectors.toList());
        auditLoggingUtil.logDbGetAllEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), TargetNameConstants.AGENCY,
                "Getting list of agencies referred to Client.");
        return agencyIdNames;

    }

    public void post(final String completeUrl, final String body) {
        final HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(completeUrl);
        httpPost.setHeader("Content-type", "application/json");
        try {
            StringEntity stringEntity = new StringEntity(body);
            httpPost.getRequestLine();
            httpPost.setEntity(stringEntity);

            httpClient.execute(httpPost);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getDomainsByAgencyId(final String agencyId) {
        final List<String> domains = agencyRepository.getDomainsByAgencyId(agencyId);
        auditLoggingUtil.logDbGetSingleEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), agencyId,
                TargetNameConstants.AGENCY, "Getting Domains against agency (AgencyId: " + agencyId + " )");
        return domains;
    }

    public List<AgencyIdName> getRecentAgencies(final String tenantId) {
        /// todo:: for now get all the records, but in future it will be the listed to
        /// some count
        final List<AgencyIdName> recentAgencies = agencyRepository.getRecentAgencies(tenantId);
        if (CollectionUtils.isEmpty(recentAgencies)) {
            LOGGER.error("Unable to find agencies");
            throw new CrnCommonException(ErrorType.NOT_FOUND, "Unable to find agencies");
        }
        auditLoggingUtil.logDbGetAllEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), TargetNameConstants.AGENCY,
                "Getting Recently Created Agencies.");
        return recentAgencies;
    }

    public LastUpdatedAt getLastUpdatedDateTimeForAgencies() {
        return agencyRepository.getLastUpdatedDateTimeForAgencies(AuthenticationUtil.getCurrentTenantId());
    }

    public List<String> getListOfCategoriesName(final String id) {
        final List<String> categories;
        // return all categories when referral type is other
        if (id.equals("OTHER")) {
            categories = httpRequestHelper.getServiceCategoriesNameList(endPoints.getGetServiceCategoriesNameList());
        } else {
            final Agency agency = getAgencyAfterValidate(id);
            categories = agencyRepository.getCategoriesNameOfSpecificAgency(agency.getId());
        }
        auditLoggingUtil.logDbGetSingleEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), id,
                TargetNameConstants.AGENCY, "Getting list of Categories name of specific agency.");
        return categories;
    }

    final Agency getAgencyAfterValidate(final String agencyId) {
        if (StringUtils.isBlank(agencyId)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "Agency Id is required");
        }
        final Optional<Agency> optionalAgency = agencyRepository.findById(agencyId);
        if (optionalAgency.isEmpty()) {
            throw new CrnCommonException(ErrorType.NOT_FOUND,
                    String.format(ValidationMessages.ENTITY_NOT_FOUND.getMessage(), "agency"));
        }
        return optionalAgency.get();
    }

    public List<AgencyIdName> getAgenciesAcceptExpressReferral() {
        final String userId = AuthenticationUtil.getUserIdFromJwtToken();
        final CurrentUser currentUserInfo = httpRequestHelper.getCurrentUserInfo(endPoints.getGetCurrentUserInfoPath(),
                userId);

        final List<AgencyIdName> agencyIdAndNames = agencyRepository.getAgenciesAcceptExpressReferral(
                currentUserInfo.getAgency().getId(), AuthenticationUtil.getCurrentTenantId());
        auditLoggingUtil.logDbGetAllEvent(currentUserInfo, TargetNameConstants.AGENCY,
                "Getting List agencies that accepts express referral.");
        return agencyIdAndNames;
    }

    public List<AgencyIdName> getAgenciesAcceptReferralFromSpecificAgency(final String agencyId,
            final String referralId, final boolean onlyAcceptElectronic) {
        if (StringUtils.isBlank(agencyId)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "Agency Id is required");
        }
        if (StringUtils.isBlank(referralId)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "Referral Id is required");
        }
        final String sortBaseProperty = SortAgencyFieldsEnum.NAME.getValue();
        final String direction = SortDirectionEnum.ASC.getValue();
        List<String> agencyIdsHavingUsers = getListOfAgenciesWithUsers();

        final List<AgencyIdName> response = new ArrayList<>(
                agencyRepository.getAgenciesAcceptReferralFromSpecificAgency(agencyId, agencyIdsHavingUsers,
                        sortBaseProperty, Sort.Direction.fromString(direction), AuthenticationUtil.getCurrentTenantId(),
                        onlyAcceptElectronic));
        final String requestPath = endPoints.getAgencyIdsFromHistoryOfReferral();
        final List<String> agencyIds = httpRequestHelper.getListOfAgenciesWithUsers(requestPath + "/" + referralId);

        response.removeIf(agency -> !CollectionUtils.isEmpty(agencyIds) && agencyIds.contains(agency.getId()));
        return response;
    }

    private List<AgencyWithNamePictures> filterAgenciesBasedOnLocationForSubCateogriesSearch(
            final Location referenceLocation,
            final List<AgencyWithNamePictures> agencies) {
        final List<AgencyWithNamePictures> filteredAgencies = new ArrayList<>();
        agencies.stream().forEach(agency -> {
            // get Agency all locations (geoLocation + offsite Location)
            final List<Location> agencyLocations = new ArrayList<>();
            if (agency.getGeoLocationInfo() != null) {
                agencyLocations.add(agency.getGeoLocationInfo());
            }
            if (!CollectionUtils.isEmpty(agency.getOffSiteLocations())) {
                agencyLocations.addAll(agency.getOffSiteLocations().stream().map(OffSiteLocation::getGeoLocationInfo)
                        .collect(Collectors.toList()));
            }
            // filter agency based on location
            agencyLocations.stream().forEach(location -> {
                // add null checks for lat and lng validation in case of not data present.
                if (location != null
                        && !(referenceLocation.getLatitude() == null || referenceLocation.getLongitude() == null)) {
                    // if agency already exists in the response, then no need to add it again.
                    if (!filteredAgencies.stream()
                            .filter(filteredAgency -> filteredAgency.getId().equals(agency.getId())).findAny()
                            .isPresent()) {
                        final double agencyDistanceFromReferencePoint = DistanceCalculator.distance(
                                referenceLocation.getLatitude(), referenceLocation.getLongitude(),
                                location.getLatitude(), location.getLongitude(), "M");
                        if (agencyDistanceFromReferencePoint <= DISTANCE_IN_MILES) {
                            filteredAgencies.add(agency);
                        }
                    }
                }
            });

        });

        return filteredAgencies;
    }

    private List<SelectedAgencyByCategory> filterAgenciesBasedOnLocation(final Location referenceLocation,
            final List<SelectedAgencyByCategory> agencies) {
        final List<SelectedAgencyByCategory> filteredAgencies = new ArrayList<>();
        agencies.stream().forEach(agency -> {
            // get Agency all locations (geoLocation + offsite Location)
            final List<Location> agencyLocations = new ArrayList<>();
            if (agency.getGeoLocationInfo() != null) {
                agencyLocations.add(agency.getGeoLocationInfo());
            }
            if (!CollectionUtils.isEmpty(agency.getOffSiteLocations())) {
                agencyLocations.addAll(agency.getOffSiteLocations().stream().map(OffSiteLocation::getGeoLocationInfo)
                        .filter(Objects::nonNull).collect(Collectors.toList()));
            }
            // filter agency based on location
            agencyLocations.stream().forEach(location -> {
                // if agency already exists in the response, then no need to add it again.
                if (!filteredAgencies.stream().filter(filteredAgency -> filteredAgency.getId().equals(agency.getId()))
                        .findAny().isPresent()) {
                    if (referenceLocation.getLatitude() != null && referenceLocation.getLongitude() != null
                            && location.getLongitude() != null && location.getLatitude() != null) {
                        final double agencyDistanceFromReferencePoint = DistanceCalculator.distance(
                                referenceLocation.getLatitude(), referenceLocation.getLongitude(),
                                location.getLatitude(), location.getLongitude(), "M");
                        if (agencyDistanceFromReferencePoint <= DISTANCE_IN_MILES) {
                            filteredAgencies.add(agency);
                        }
                    }
                }
            });

        });

        return filteredAgencies;
    }

    private void assignFavoriteAgencyAttributesAndSortOffsiteLocationAndNetworkPartnerToAgencyList(
            final List<FavoriteAgency> favoriteAgencies,
            final List<AgencyWithNamePictures> agencies) {

        // to reduce inner loops processing
        final Map<String, FavoriteAgency> favoriteAgenciesMap;
        if (CollectionUtils.isEmpty(favoriteAgencies)) {
            favoriteAgenciesMap = new LinkedHashMap<>();
        } else {
            favoriteAgenciesMap = favoriteAgencies.stream()
                    .filter(obj -> null != obj || StringUtils.isNotBlank(obj.getAgencyId()))
                    .collect(Collectors.toMap(FavoriteAgency::getAgencyId, obj -> obj));
        }

        agencies.stream().forEach(agency -> {
            if (favoriteAgenciesMap.containsKey(agency.getId())) {
                final FavoriteAgency matchedAgency = favoriteAgenciesMap.get(agency.getId());
                if (matchedAgency.isAgencyFavorite()) {
                    agency.setAgencyFavorite(true);
                }
                if (matchedAgency.isUserFavorite()) {
                    agency.setUserFavorite(true);
                }
            }
        });

    }

    final List<AgencyByService> addAgency211(final SelectedAgencyByCategory agency211,
            final List<AgencyByService> agencyByServices, final List<FavoriteAgency> favoriteAgencies) {

        final AgencyByCategory agencyByCategory211 = new AgencyByCategory();
        agencyByCategory211.setCategoryName("");
        agencyByCategory211.setGeneralReferralHubId(agency211.getGeneralReferralHubId());
        agencyByCategory211.setGeneralReferralHub(agency211.isGeneralReferralHub());
        final AgencyWithNamePictures agencyWithNamePicture = new AgencyWithNamePictures(agency211.getId(),
                agency211.getName(), agency211.getPictures(), agency211.getAbbreviation(),
                agency211.getGeoLocationInfo(), agency211.getOffSiteLocations(), agency211.getReferralAcceptedFrom(),
                agency211.isReferralAcceptedFromAll(), agency211.getNickname(), agency211.getIsExpress(),
                agency211.getIsLog(), agency211.getIsElectronic(), agency211.getIsPaper(),
                agency211.getOrganizationType(), agency211.getAssociatedOrgsByOrgType(),
                agency211.getAdditionalDocumentationForms(),
                !agency211.getIsOutOfNetwork(), agency211.getTenantIds(), agency211.getIsOutOfNetwork(),
                agency211.getOutOfNetworkReferralInfo(),
                agency211.isGeneralReferralHub(), agency211.getGeneralReferralHubId());

        agencyWithNamePicture.setUserFavorite(agency211.isUserFavorite());
        agencyWithNamePicture.setAgencyFavorite(agency211.isAgencyFavorite());
        agencyWithNamePicture.setAbbreviation(agency211.getAbbreviation());
        agencyWithNamePicture.setCrnPartner(!agencyWithNamePicture.getIsOutOfNetwork());
        if (agency211.getReferralInfo() != null) {
            agencyWithNamePicture.setIsElectronic(agency211.getReferralInfo().getIsElectronic());
            agencyWithNamePicture.setIsExpress(agency211.getReferralInfo().getIsExpress());
            agencyWithNamePicture.setIsLog(agency211.getReferralInfo().getIsLog());
            agencyWithNamePicture.setIsPaper(agency211.getReferralInfo().getIsPaper());
            agencyWithNamePicture.setReferralAcceptedFromAll(agency211.getReferralInfo().isReferralAcceptedFromAll());
            agencyWithNamePicture.setReferralAcceptedFrom(agency211.getReferralInfo().getReferralAcceptedFrom());
        }

        if (!CollectionUtils.isEmpty(favoriteAgencies)) {
            final Optional<FavoriteAgency> matchedAgencyOptional = favoriteAgencies.stream()
                    .filter(favoriteAgency -> favoriteAgency.getAgencyId().equals(agencyWithNamePicture.getId()))
                    .findFirst();
            if (matchedAgencyOptional.isPresent()) {
                final FavoriteAgency matchedAgency = matchedAgencyOptional.get();
                if (matchedAgency.isAgencyFavorite()) {
                    agencyWithNamePicture.setAgencyFavorite(true);
                }
                if (matchedAgency.isUserFavorite()) {
                    agencyWithNamePicture.setUserFavorite(true);
                }
            }
        }

        final List<AgencyWithNamePictures> agencyWithNamePictures = new ArrayList<>();
        agencyWithNamePictures.add(agencyWithNamePicture);
        agencyByCategory211.setAgencies(agencyWithNamePictures);

        final AgencyByService agencyByService211 = new AgencyByService();
        agencyByService211.setService("");
        agencyByService211.setGeneralReferralHubId(agency211.getGeneralReferralHubId());
        agencyByService211.setGeneralReferralHub(agency211.isGeneralReferralHub());

        final List<AgencyByCategory> agencyByCategories = new ArrayList<>();
        agencyByCategories.add(agencyByCategory211);
        agencyByService211.setCategories(agencyByCategories);
        agencyByServices.add(agencyByService211);
        return agencyByServices;
    }

    private void uploadDocumentToS3(final String fileExtension, final String document,
            final String agencyName, final String agencyId, final String fileName) throws IOException {
        final String filePath = AGENCY_DATA_VAULT_PATH + agencyId;
        final String s3BucketName = mongoDataSources.getS3BucketNameForCurrentTenantId();
        if (fileExtension.toLowerCase().equals("doc") || fileExtension.toLowerCase().equals("docx")
                || fileExtension.toLowerCase().equals("xls") || fileExtension.toLowerCase().equals("xlsx")) {
            final byte[] decodedFile = Base64.getDecoder().decode(document.getBytes(StandardCharsets.UTF_8));
            final Path destinationFile = Paths.get("", agencyName + "." + fileExtension);
            final Path write = Files.write(destinationFile, decodedFile);
            final File file = write.toFile();
            this.awsHelper.uploadDocumentToS3(s3BucketName, file, filePath, fileName + "." + fileExtension);
            file.delete();
        } else {
            this.awsHelper.uploadDocumentToS3(s3BucketName, document, filePath, fileName);
        }
    }

    public CurrentUser getCurrentUserInfo() {
        // get user info based on jwt token
        final String userId = AuthenticationUtil.getUserIdFromJwtToken();
        final CurrentUser currentUserInfo = httpRequestHelper.getCurrentUserInfo(endPoints.getGetCurrentUserInfoPath(),
                userId);
        return currentUserInfo;
    }

    /*
     * This function will get the agencies based on organization type
     * if organization type == Program -> get ony Agencies
     * if organization type == Service -> get ony Agencies + Program
     * if organization type == Payer/Partner Network -> get ony Agencies + Program +
     * Services
     */
    public List<AgencyIdName> getAgenciesByOrganizationType(final String organizationType) {
        if (StringUtils.isBlank(organizationType)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "Organization type is required");
        }
        final List<String> searchOrganizationType = new ArrayList<>();
        if (OrganizationTypeEnum.PROGRAM.getValue().equals(organizationType)) {
            // get Agencies
            searchOrganizationType.add(OrganizationTypeEnum.AGENCY.getValue());

        } else if (OrganizationTypeEnum.SERVICE.getValue().equals(organizationType)) {
            // get Agencies and Program
            searchOrganizationType.addAll(Arrays.asList(OrganizationTypeEnum.AGENCY.getValue(),
                    OrganizationTypeEnum.PROGRAM.getValue()));

        } else if (OrganizationTypeEnum.PAYER_PARTNER_NETWORK.getValue().equals(organizationType)) {
            // get Agencies, Program and services
            searchOrganizationType.addAll(Arrays.asList(OrganizationTypeEnum.AGENCY.getValue(),
                    OrganizationTypeEnum.PROGRAM.getValue(), OrganizationTypeEnum.SERVICE.getValue()));
        } else {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format("Unable to find Agencies by organization Type %s", organizationType));
        }

        final List<AgencyIdName> agencies = agencyRepository.findByOrganizationTypeIn(searchOrganizationType,
                AuthenticationUtil.getCurrentTenantId());
        auditLoggingUtil.logDbGetAllEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), TargetNameConstants.AGENCY,
                "Getting agencies/organizations by organization type(Type: " + organizationType + " )");
        return agencies;
    }

    public FavoriteAgenciesPlusReferralnfo getFavoriteAgenciesAndReferralInfo(final String agencyId,
            final String userId, final String tenantId, final boolean isConcise) {
        if (StringUtils.isBlank(agencyId)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "Agency Id type is required");
        }
        if (StringUtils.isBlank(userId)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "UserId type is required");
        }

        final List<String> agenciesIdsOfCrnPartner = getListOfAgenciesWithUsers();

        LOGGER.info("getting agency's fav agencies");
        // get favorite agencies against specific agency
        final FavoriteAgenciesPlusReferralnfo response = agencyRepository.getFavoriteAgenciesAndReferralInfo(agencyId,
                tenantId, isConcise);
        if (!CollectionUtils.isEmpty(response.getAgencyFavorites())) {
            final List<String> agencyIds = response.getAgencyFavorites().stream().map(AgencyWithNamePictures::getId)
                    .distinct().collect(Collectors.toList());
            LOGGER.info("getting agency's fav agencies detail");
            final List<AgencyWithNamePictures> agencyFav = agencyRepository.findAgenciesByIdsBasicInfo(agencyIds,
                    tenantId, isConcise);
            agencyFav.stream().forEach(agencyInfo -> agencyInfo.setCrnPartner(!agencyInfo.getIsOutOfNetwork()));
            response.setAgencyFavorites(agencyFav);
        } else {
            LOGGER.info("agency has no fav agencies.");
            response.setAgencyFavorites(new ArrayList<>());
        }

        LOGGER.info("getting user's fav agencies");
        // get favorite agencies against specific user
        final List<AgencyIdName> agencies = httpRequestHelper
                .getUserFavoriteAgencies(endPoints.getUserFavoriteAgencies() + "/" + userId);
        if (!CollectionUtils.isEmpty(agencies)) {
            final List<String> ids = agencies.stream().map(AgencyIdName::getId).distinct().collect(Collectors.toList());
            LOGGER.info("getting user's fav agencies detail");
            final List<AgencyWithNamePictures> userFav = agencyRepository.findAgenciesByIdsBasicInfo(ids, tenantId,
                    isConcise);
            userFav.stream().forEach(agencyInfo -> agencyInfo.setCrnPartner(!agencyInfo.getIsOutOfNetwork()));
            response.setUserFavoriteAgencies(userFav);
        } else {
            LOGGER.info("user has no fav agencies detail");
            response.setUserFavoriteAgencies(new ArrayList<>());
        }
        auditLoggingUtil.logDbGetSingleEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), agencyId,
                TargetNameConstants.AGENCY,
                "Getting List of favorite agencies against user and agency id along with referral data.");
        return response;
    }

    public PageableAgencyInfoResponse getAgencyListWithSearchParams(final String searchParam,
            final String domain,
            final String payerNetworkIdOrValue,
            final String tenantId,
            final int pageNumber, final int pageSize,
            final String fieldName, final String sortDirection) {
        if (StringUtils.isNotBlank(domain) && !EnumUtils.isValidEnum(DomainTypeEnum.class, domain)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "Invalid domain to search.");
        }
        final Pageable pageable = PageRequest.of(pageNumber, pageSize);
        final List<String> agencyIdsOfSpecificNetwork;
        final List<String> agenciesIdsOfCrnPartner;
        validateSortingParams(fieldName, sortDirection);
        final String sortBaseProperty = SortAgencyFieldsEnum.valueOf(fieldName).getValue();
        final String direction = SortDirectionEnum.valueOf(sortDirection).getValue();
        if (StringUtils.isNotBlank(payerNetworkIdOrValue)) {
            // Get agency ids of that network.
            agencyIdsOfSpecificNetwork = getAgenciesOfSpecificNetwork(payerNetworkIdOrValue);

            if (payerNetworkIdOrValue.equals("CRN_PARTNERS_ONLY")) {
                agenciesIdsOfCrnPartner = agencyIdsOfSpecificNetwork;
            } else {
                agenciesIdsOfCrnPartner = getListOfAgenciesWithUsers();
            }
        } else {
            agencyIdsOfSpecificNetwork = new ArrayList<>();
            agenciesIdsOfCrnPartner = getListOfAgenciesWithUsers();
        }
        final PageableAgencyInfoResponse pageableAgencyInfoResponse = agencyRepository
                .findAgenciesBasicInfoBySearchParamDomainOrAgencyIds(pageable, searchParam, domain,
                        agencyIdsOfSpecificNetwork, tenantId, sortBaseProperty, Sort.Direction.fromString(direction));
        final List<AgencyInfo> agencyInfoList = pageableAgencyInfoResponse.getData();
        if (!CollectionUtils.isEmpty(agenciesIdsOfCrnPartner)) {
            agencyInfoList.forEach(
                    agencyInfo -> agencyInfo.setCrnPartner(agenciesIdsOfCrnPartner.contains(agencyInfo.getId())));

        }

        final AuditLoggingSearchParamsModel searchParamsModel = new AuditLoggingSearchParamsModel();
        searchParamsModel.setSearchParam(searchParam);
        searchParamsModel.setDomain(domain);
        searchParamsModel.setPayerNetworkIdOrValue(payerNetworkIdOrValue);
        auditLoggingUtil.logSearchEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), searchParamsModel,
                TargetNameConstants.AGENCY, "Searching agencies by specific params.");

        return pageableAgencyInfoResponse;
    }

    public List<Agency> getOrganizationTypesOfAllAgencies() {
        return agencyRepository.getAgenciesOrganizations(AuthenticationUtil.getCurrentTenantId());
    }

    public boolean userIsReferralRecipient(final String agencyId, final String userId) {
        if (StringUtils.isBlank(agencyId)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(), "agency Id"));
        }
        if (StringUtils.isBlank(userId)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(), "userId Id"));
        }
        final boolean agencyExists = agencyRepository.existsById(agencyId);
        if (!agencyExists) {
            throw new CrnCommonException(ErrorType.NOT_FOUND,
                    String.format(ValidationMessages.ENTITY_NOT_FOUND.getMessage(), "agency"));
        }
        final Optional<Agency> optionalAgency = agencyRepository.findAgencyByIdAndReferralRecipientUser(agencyId,
                userId);
        if (optionalAgency.isEmpty()) {
            return false;
        }
        return true;
    }

    public List<Tenant> getTenantInfoThroughAgency(final String agencyId) {
        LOGGER.info("Get tenant info by agency");
        if (StringUtils.isBlank(agencyId)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(), "agency Id"));
        }
        final Optional<Agency> optionalAgency = agencyRepository.findById(agencyId);
        if (optionalAgency.isEmpty()) {
            throw new CrnCommonException(ErrorType.NOT_FOUND,
                    String.format(ValidationMessages.ENTITY_NOT_FOUND.getMessage(), "agency Id"));
        }
        final Agency agency = optionalAgency.get();

        final List<Tenant> tenants = tenantRepository.findByIdIn(agency.getTenantIds());

        LOGGER.info("Tenant ids extracted successfully");
        return tenants;
    }

    public AttestationSetting getAgencyLevelUserInstruction(final String agencyId) {
        LOGGER.info("Get agency level users instruction by agencyId");
        final Agency agency = getAgencyById(agencyId);
        return agency.getAgencyUserInstructionSetting();
    }

    public List<AgencyIdName> getAllAgenciesIdNameForDynamicReferralCreation(final String tenantId) {
        LOGGER.info("get id and name of all agencies");
        final List<AgencyIdName> agencyIdNames = agencyRepository.getAllAgenciesIdName(tenantId, agencyIdsNotToShow);
        if (!CollectionUtils.isEmpty(agencyIdNames)) {
            agencyIdNames.stream().forEach(agencyIdName -> {
                final String nameOrNickname = StringUtils.isNotBlank(agencyIdName.getNickname())
                        ? agencyIdName.getNickname()
                        : agencyIdName.getName();
                agencyIdName.setNameOrNickname(nameOrNickname);
            });
        }
        final List<AgencyIdName> otherAgenciesList = new ArrayList<>();
        List<AgencyIdName> agencysortedList = agencyIdNames.stream()
                .sorted(Comparator.comparing(AgencyIdName::getNameOrNickname))
                .filter(agency -> {
                    if (agency.getName().startsWith("Other ")) {
                        otherAgenciesList.add(agency);
                        return false;
                    } else {
                        return true;
                    }
                })
                .collect(Collectors.toList());
        agencysortedList.addAll(otherAgenciesList);
        auditLoggingUtil.logDbGetAllEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), TargetNameConstants.AGENCY,
                "Getting only id and name of all agencies.");
        return agencysortedList;
    }

    public List<Agency> getAgenciesByIds(final List<String> agencyIds) {
        LOGGER.info("getting Agencies for ids: " + agencyIds);
        if (!CollectionUtils.isEmpty(agencyIds)) {
            return agencyRepository.findAgenciesByIds(agencyIds);
        }
        return new ArrayList<>();
    }

    public HashMap<String, String> updateGateKeepersOfAgency(final String agencyId, final CurrentUser currentUser) {
        final Agency agency = validateAndGetAgencyById(agencyId);
        if (null != agency.getReferralInfo() && !CollectionUtils.isEmpty(agency.getReferralInfo().getReferredUsers())) {
            final List<ReferredUsers> referredUsers = new ArrayList<>();
            agency.getReferralInfo().getReferredUsers().forEach(user -> {
                if (!user.getId().equals(currentUser.getId())) {
                    referredUsers.add(user);
                }
            });
            agencyRepository.updateReferredUserOfSpecificAgency(agencyId, referredUsers);
        }
        final HashMap<String, String> successMessage = new LinkedHashMap<>();
        successMessage.put("message", "GateKeepers of agency updated successfully");
        return successMessage;
    }

    public List<AgencyIdName> getAgencyIdsByTenantIds(final List<String> tenantIds) {
        return agencyRepository.findByTenantIdsIn(tenantIds);
    }

    public HashMap<String, List<TenantRegion>> getAllRegionAndUpdateInUser() {
        final List<Agency> allAgencies = agencyRepository.findAll();
        final List<Region> allRegions = regionRepository.findAll();
        final HashMap<String, List<TenantRegion>> regionAgencyMap = new LinkedHashMap<>();
        allAgencies.forEach(agency -> {
            if (!CollectionUtils.isEmpty(agency.getRegions())) {
                final List<TenantRegion> defaultRegions = getDistinctTenantRegions(agency.getRegions(), allRegions);
                if (!CollectionUtils.isEmpty(defaultRegions)) {
                    regionAgencyMap.put(agency.getId(), defaultRegions);
                }
            }
        });
        return regionAgencyMap;
    }

    private List<TenantRegion> getDistinctTenantRegions(final List<TenantRegion> agencyTenantRegion,
            final List<Region> regions) {
        final List<String> agencyTenants = agencyTenantRegion.stream().map(TenantRegion::getTenantId).distinct()
                .collect(Collectors.toList());
        final List<TenantRegion> defaultRegions = new ArrayList<>();
        if (!CollectionUtils.isEmpty(agencyTenants)) {
            agencyTenants.forEach(tenantId -> {
                final List<@NotBlank String> regionIds = agencyTenantRegion.stream()
                        .filter(region -> region.getTenantId().equals(tenantId)).map(TenantRegion::getId)
                        .collect(Collectors.toList());
                final Optional<Region> optionalRegion = regions.stream()
                        .filter(region -> regionIds.contains(region.getId()))
                        .min(Comparator.comparing(Region::getName));
                if (optionalRegion.isPresent()) {
                    final Region region = optionalRegion.get();
                    final TenantRegion tenantRegion = new TenantRegion();
                    tenantRegion.setId(region.getId());
                    tenantRegion.setTenantId(region.getTenantId());
                    defaultRegions.add(tenantRegion);
                }
            });
        }

        return defaultRegions;
    }

    public HashMap<String, String> updateReferralRecipientAccordingToGatekeeperPermission() {
        final String requestPath = endPoints.getUsersHavingGatekeeperPermission();
        final List<CurrentUser> usersHavingGateKeeperPermission = httpRequestHelper
                .getUsersHavingGateKeeperPermission(requestPath);
        final HashMap<String, List<String>> gateKeeperPermissionUsersMap = new LinkedHashMap<>();
        usersHavingGateKeeperPermission.forEach(user -> {
            gateKeeperPermissionUsersMap.put(user.getId(), user.getUserRolePermissions());
        });
        final List<Agency> allAgencies = agencyRepository.findAll();
        allAgencies.forEach(agency -> {
            if (null != agency.getReferralInfo()) {
                if (!CollectionUtils.isEmpty(agency.getReferralInfo().getReferredUsers())) {
                    final List<ReferredUsers> referredUsers = new ArrayList<>();
                    agency.getReferralInfo().getReferredUsers().forEach(user -> {
                        if (null != gateKeeperPermissionUsersMap.get(user.getId())) {
                            referredUsers.add(user);
                        }
                    });
                    agency.getReferralInfo().setReferredUsers(referredUsers);
                }
            }
        });
        agencyRepository.saveAll(allAgencies);
        final HashMap<String, String> successMessage = new LinkedHashMap<>();
        successMessage.put("message", "Script executed successfully");
        return successMessage;
    }

    public HashMap<String, String> markEntityAsPartnerNetwork(final String agencyId) {
        if (StringUtils.isBlank(agencyId)) {
            LOGGER.info("agency id is required.");
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(), "agencyId"));
        }
        LOGGER.info("Marking entity as partner network by id: {}", agencyId);
        agencyRepository.markEntityAsPartnerNetwork(agencyId);
        final HashMap<String, String> successMessage = new LinkedHashMap<>();
        successMessage.put("message", "Successfully marked as partner network");
        return successMessage;
    }

    public List<Agency> getAgencyBasedOnOrgId(final String adtOrgId) {
        if (StringUtils.isBlank(adtOrgId)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "adtOrg Id is required");
        }
        return agencyRepository.getAgencyBasedOnOrgId(adtOrgId, AuthenticationUtil.getCurrentTenantId());
    }

    public Predicate<AgencyWithNamePictures> remainingAgencies(List<AgencyWithNamePictures> sortedAgencyListAll) {
        try {
            Set<String> ids = sortedAgencyListAll.stream().map(s -> s.getId()).collect(Collectors.toSet());
            return e -> (ids != null && !ids.contains(e.getId()));
        } catch (final Exception ex) {
            LOGGER.error("Exception in remainingAgencies: " + ex);
            ex.printStackTrace();
            return null;
        }
    }

    public List<AgencyWithNamePictures> agencyListSorting(List<AgencyWithNamePictures> agencyList) {
        try {
            final List<AgencyWithNamePictures> sortedAgencyListAll = new ArrayList<>();
            final List<Predicate<AgencyWithNamePictures>> agencySortingFilters = new ArrayList<>();

            final Predicate<AgencyWithNamePictures> partnerAndFavoriteStep1 = agency -> (agency.isCrnPartner() == true
                    && (agency.isUserFavorite() == true || agency.isAgencyFavorite() == true));
            final Predicate<AgencyWithNamePictures> partnerAndNotFavoriteStep2 = agency -> (agency
                    .isCrnPartner() == true
                    && (agency.isUserFavorite() == false && agency.isAgencyFavorite() == false));

            final Predicate<AgencyWithNamePictures> notPartnerAndFavoriteAndEmailFaxStep3 = agency -> (agency
                    .isCrnPartner() == false && (agency.isUserFavorite() == true || agency.isAgencyFavorite() == true)
                    && (agency.getOutOfNetworkReferralInfo().isByEmail() == true
                            || agency.getOutOfNetworkReferralInfo().isByFax() == true));
            final Predicate<AgencyWithNamePictures> notPartnerAndFavoriteAndLogStep4 = agency -> (agency
                    .isCrnPartner() == false && (agency.isUserFavorite() == true || agency.isAgencyFavorite() == true)
                    && agency.getIsLog() == true);

            final Predicate<AgencyWithNamePictures> notPartnerAndNotFavoriteAndEmailFaxStep5 = agency -> (agency
                    .isCrnPartner() == false && (agency.isUserFavorite() == false && agency.isAgencyFavorite() == false)
                    && (agency.getOutOfNetworkReferralInfo().isByEmail() == true
                            || agency.getOutOfNetworkReferralInfo().isByFax() == true));
            final Predicate<AgencyWithNamePictures> notPartnerAndNotFavoriteAndLogStep6 = agency -> (agency
                    .isCrnPartner() == false && (agency.isUserFavorite() == false && agency.isAgencyFavorite() == false)
                    && agency.getIsLog() == true);

            agencySortingFilters.add(partnerAndFavoriteStep1);
            agencySortingFilters.add(partnerAndNotFavoriteStep2);
            agencySortingFilters.add(notPartnerAndFavoriteAndEmailFaxStep3);
            agencySortingFilters.add(notPartnerAndFavoriteAndLogStep4);
            agencySortingFilters.add(notPartnerAndNotFavoriteAndEmailFaxStep5);
            agencySortingFilters.add(notPartnerAndNotFavoriteAndLogStep6);

            agencySortingFilters.stream().forEach(filter -> {
                sortedAgencyListAll.addAll(agencyList.stream()
                        .filter(remainingAgencies(sortedAgencyListAll))
                        .filter(filter)
                        .sorted(Comparator.comparingDouble(AgencyWithNamePictures::getProximity))
                        .collect(Collectors.toList()));
            });
            sortedAgencyListAll.addAll(agencyList.stream()
                    .filter(remainingAgencies(sortedAgencyListAll))
                    .sorted(Comparator.comparingDouble(AgencyWithNamePictures::getProximity))
                    .collect(Collectors.toList()));
            return sortedAgencyListAll;
        } catch (final Exception ex) {
            LOGGER.error("Exception in agencyListSoarting: " + ex);
            ex.printStackTrace();
            return null;
        }
    }

    private void checkIfIdentificationIsADuplicate(final List<EntityMappingIdentification> identifications,
            final String id) {
        if (!CollectionUtils.isEmpty(identifications)) {
            final Set<String> agencyTypes = identifications.stream().map(identification -> identification.getType())
                    .collect(Collectors.toSet());
            if (agencyTypes.size() != identifications.size()) {
                throw new CrnCommonException(ErrorType.FORBIDDEN, String
                        .format(ValidationMessages.DUPLICATE_VALUES_NOT_ALLOWED.getMessage(), IDENTIFICATION_TYPE));
            }
            final List<EntityMappingIdentification> agenciesWithSameIdentification = agencyRepository
                    .identificationExists(identifications, id);
            if (!CollectionUtils.isEmpty(agenciesWithSameIdentification)) {
                final StringBuilder takenValues = new StringBuilder();
                agenciesWithSameIdentification.forEach(agencyWithSameIdentification -> {
                    takenValues.append("[" + agencyWithSameIdentification.getType() + ", ");
                    takenValues.append(agencyWithSameIdentification.getValue() + "]");
                });
                throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, String
                        .format(ValidationMessages.IDENTIFICATION_VALUES_ALREADY_TAKEN.getMessage(), takenValues));
            }
        }

    }

    public List<AgencyIdName> getAllAgenciesAssociatedWithAProgram(final String programId) {
        if (StringUtils.isBlank(programId)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(), PROGRAM_ID));
        }
        final String tenantId = TenantContext.getTenantId();
        final List<AgencyIdName> agencies = agencyRepository.getAllAgenciesAssociatedWithAProgram(programId, tenantId);
        auditLoggingUtil.logDbGetAllEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), TargetNameConstants.AGENCY,
                "Getting all agencies associated with the program (ProgramId: " + programId + " )");
        return agencies;
    }

    public List<ProgramAndAgencyData> findAgenciesAndProgramsAssociatedWithAService(final String serviceId) {
        if (StringUtils.isBlank(serviceId)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(), SERVICE_ID));
        }
        List<ProgramAndAgencyData> programAndAgencyDataList = new ArrayList<>();
        final String tenantId = TenantContext.getTenantId();
        List<Program> programs = programRepository.findAllProgramByServiceId(serviceId, tenantId);
        programs.forEach(data -> {
            final ProgramAndAgencyData programAndAgencyData = new ProgramAndAgencyData("PROGRAM", data);
            programAndAgencyDataList.add(programAndAgencyData);

        });
        final List<AgencyIdAndName> agencies = agencyRepository.getAgencyByServiceId(serviceId, tenantId);
        agencies.forEach(data -> {
            final ProgramAndAgencyData programAndAgencyData = new ProgramAndAgencyData("AGENCY", data);
            programAndAgencyDataList.add(programAndAgencyData);

        });

        auditLoggingUtil.logDbGetAllEvent(auditLoggingUtil.getCurrentLoggedInUserInfo(), TargetNameConstants.AGENCY,
                "Getting all agencies and Program associated with the program (ServiceId: " + serviceId + " )");
        return programAndAgencyDataList;
    }

    // For Velatura
    public List<Agency> getAgencyBasedOnOrgIds(final String adtOrgIds) {
        if (StringUtils.isBlank(adtOrgIds)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "adtOrg Id is required");
        }
        final List<String> orgIds = new ArrayList<>(Arrays.asList(adtOrgIds.split(",")));

        return agencyRepository.getAgenciesBasedOnOrgIdsList(orgIds, AuthenticationUtil.getCurrentTenantId());
    }

    public List<ProgramAffiliation> findProgramsByAgencyId(final String agencyId) {
        if (StringUtils.isBlank(agencyId)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE,
                    String.format(ValidationMessages.REQUIRED.getMessage(), AGENCY_ID));
        }
        final List<ProgramAffiliation> affiliatedPrograms = agencyRepository.findProgramsByAgencyId(agencyId);

        if (affiliatedPrograms.isEmpty()) {
            LOGGER.error("Unable to get Programs for the given Agency");
        }
        return affiliatedPrograms;
    }

    // For Velatura
    public List<Agency> getAgencyBasedOnIds(final String ids) {
        if (StringUtils.isBlank(ids)) {
            throw new CrnCommonException(ErrorType.VALIDATION_FAILURE, "Id is required");
        }
        final List<String> agencyIds = new ArrayList<>(Arrays.asList(ids.split(",")));

        return agencyRepository.getActiveAgencyIds(agencyIds);
    }

    public ResponseEntity getActiveAgencies(AllAgenciesRequest allAgenciesRequest) {
        List<String> allAgencies = allAgenciesRequest.getAllAgencies();
        final Aggregation activeAgenciesAggregation = newAggregation(
                match(Criteria.where("_id").in(allAgencies).and("isActive").is(true)),
                group().addToSet("_id").as("allAgencies"), project().andExclude("_id"));
        List<AllAgenciesRequest> onlyActiveAgencies = mongoTemplate
                .aggregate(activeAgenciesAggregation, "agency", AllAgenciesRequest.class).getMappedResults();
        return ResponseEntity.ok(onlyActiveAgencies.get(0).getAllAgencies());
    }
}
