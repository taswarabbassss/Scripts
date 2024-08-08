package com.crn.agency.controller;

import com.crn.agency.constants.TargetNameConstants;
import com.crn.agency.domain.Agency;
import com.crn.agency.domain.PermissionGroup;
import com.crn.agency.domain.ReferralRequirement;
import com.crn.agency.domain.Tenant;
import com.crn.agency.domain.pojo.LastUpdatedAt;
import com.crn.agency.domain.PageableAgencyResponse;
import com.crn.agency.domain.PageableAgencyFavoriteResponse;
import com.crn.agency.domain.PageableAgencyInfoResponse;
import com.crn.agency.helpers.AuditLogging;
import com.crn.agency.model.*;
import com.crn.agency.model.program.ProgramAffiliation;
import com.crn.agency.model.request.AgencyRequest;
import com.crn.agency.model.request.AgencySearchByCategory;
import com.crn.agency.model.request.AgencySearchBySubCategory;
import com.crn.agency.model.request.AllAgenciesRequest;
import com.crn.agency.service.AgencyService;
import com.crn.agency.service.KeywordService;
import com.crn.agency.util.AuthenticationUtil;
import com.crn.common.constants.PermissionConstants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Ahmed Khan on 3/27/2019.
 */
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Api(value = "Agency", description = "Operations related to agency")
@RestController
@RequestMapping("/api")
public class AgencyController {
    private final static Logger LOGGER = LogManager.getLogger(AgencyService.class);

    @Autowired
    private AgencyService agencyService;
    @Autowired
    private KeywordService keywordService;
    @Autowired
    private AuditLogging auditLogging;

    /**
     * Creates the new AgencyRequest
     *
     * @param agency
     * @return the added agency data
     */
    @ApiOperation(value = "create AgencyRequest")
    @RequestMapping(value = "/v1", method = RequestMethod.POST)
    public Agency createAgency(@RequestBody final AgencyRequest agency,
            @RequestHeader(value = "userid") final String userId) {
        try {
            final List<String> keywords = agency.getKeywords();
            keywordService.addNewKeywords(keywords);
            return agencyService.addAgency(agency);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [createAgency()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception, "createAgency",
                    TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [createAgency()]");
            throw exception;
        }
    }

    @ApiOperation(value = "Get Recently created Agencies")
    @RequestMapping(value = "/v1/recentagencies", method = RequestMethod.GET)
    public List<AgencyIdName> getRecentAgencies(@RequestHeader(value = "tenantid") final String tenantId) {
        try {
            return agencyService.getRecentAgencies(tenantId);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getRecentAgencies()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception, "getRecentAgencies",
                    TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getRecentAgencies()]");
            throw exception;
        }
    }

    @ApiOperation(value = "update specific agency")
    @RequestMapping(value = "/v1/{id}", method = RequestMethod.PUT)
    public Agency updateAgencyById(@PathVariable final String id, @RequestBody final AgencyRequest agency,
            @RequestHeader(value = "userid") final String userId) {
        try {
            return agencyService.updateAgency(id, agency);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [updateAgencyById()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception, "updateAgencyById",
                    TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [updateAgencyById()]");
            throw exception;
        }
    }

    @PreAuthorize("@authService.hasPermission(#userId, '" + PermissionConstants.CREATE_DEACTIVATE_ENTITY + "')")
    @ApiOperation(value = "De-Activate specific agency")
    @RequestMapping(value = "/v1/deactivate/{id}", method = RequestMethod.PUT)
    public Agency deactivateAgencyById(@PathVariable final String id,
            @RequestHeader(value = "userid") final String userId) {
        try {
            return agencyService.deActivateAgency(id, userId);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [deactivateAgencyById()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception, "deactivateAgencyById",
                    TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [deactivateAgencyById()]");
            throw exception;
        }
    }

    @ApiOperation(value = "get details of specified Agency")
    @RequestMapping(value = { "/v1/{id}" }, method = RequestMethod.GET)
    @ResponseBody
    public Agency findAgencyById(@PathVariable final String id) {
        try {
            return agencyService.getAgencyByIdWithActiveFavoriteAgencies(id);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [findAgencyById()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception, "findAgencyById",
                    TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [findAgencyById()]");
            throw exception;
        }
    }

    @ApiOperation(value = "get details of specified Agency for Resource directory")
    @RequestMapping(value = { "/v1/resourcedirectory/{id}" }, method = RequestMethod.GET)
    @ResponseBody
    public Agency findAgencyByIdForResourceDirectory(@PathVariable final String id,
            @RequestParam(value = "latitude", required = false) final Double latitude,
            @RequestParam(value = "longitude", required = false) final Double longitude) {
        try {
            return agencyService.findAgencyByIdForResourceDirectory(id, latitude, longitude);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [findAgencyByIdForResourceDirectory()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "findAgencyByIdForResourceDirectory", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [findAgencyByIdForResourceDirectory()]");
            throw exception;
        }
    }

    /* this is simple getAgencyById, but non-authenticated end-point */
    @ApiOperation(value = "get details of specified Agency")
    @RequestMapping(value = { "/v1/agencydetail/{id}" }, method = RequestMethod.GET)
    @ResponseBody
    public Agency findAgencyByIdNonAuthenticated(@PathVariable final String id) {
        try {
            return agencyService.getAgencyById(id);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [findAgencyById()]");
            auditLogging.logExceptionEvent(null, exception, "findAgencyByIdNonAuthenticated",
                    TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [findAgencyById()]");
            throw exception;
        }
    }

    @ApiOperation(value = "get All agencies having services of specified Agency")
    @RequestMapping(value = { "/v1/services/{id}" }, method = RequestMethod.GET)
    @ResponseBody
    public List<AgencyIdName> findAgenciesHavingSameServicesOfSpecificAgency(
            @RequestHeader(value = "tenantid") final String tenantId,
            @PathVariable final String id) {
        try {
            return agencyService.getAllAgenciesHavingSameServiceOfSpecificAgency(id, tenantId);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getAllAgenciesHavingSameServiceOfSpecificAgency()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getAllAgenciesHavingSameServiceOfSpecificAgency", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getAllAgenciesHavingSameServiceOfSpecificAgency()]");
            throw exception;
        }
    }

    @ApiOperation(value = "get payer partner networks")
    @RequestMapping(value = { "/v1/payernetwork" }, method = RequestMethod.GET)
    public List<AgencyIdName> getPayerPartnerNetworks() {
        return agencyService.getPayerNetworkAgencies();
    }

    @ApiOperation(value = "get ids of agencies in specific payer network or agencies which are not part of any network")
    @RequestMapping(value = { "/v1/payernetwork/{idorvalue}" }, method = RequestMethod.GET)
    @ResponseBody
    public List<String> getAgenciesOfSpecificNetworkOrAgenciesWithNoNetwork(
            @PathVariable(value = "idorvalue") final String idOrValue) {
        return agencyService.getAgenciesOfSpecificNetwork(idOrValue);
    }

    @ApiOperation(value = "search Agencies by network, domain or param.")
    @RequestMapping(value = { "/v1/search" }, method = RequestMethod.GET)
    @ResponseBody
    public PageableAgencyInfoResponse getAgencyListWithSearchParams(
            @RequestParam(value = "fieldName", required = false, defaultValue = "NICKNAME") final String fieldName,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "ASC") final String sortDirection,
            @RequestHeader(value = "tenantid") final String tenantId,
            @RequestParam(value = "searchparam", required = false) final String searchParam,
            @RequestParam(value = "domain", required = false) final String domain,
            @RequestParam(value = "payernetworkidorvalue", required = false) final String payerNetworkIdOrValue,
            @Min(0) @RequestParam(value = "pageNumber", defaultValue = "0") final int pageNumber,
            @Min(1) @RequestParam(value = "pageSize", defaultValue = Integer.MAX_VALUE + "") final int pageSize) {
        try {
            return agencyService.getAgencyListWithSearchParams(searchParam, domain, payerNetworkIdOrValue, tenantId,
                    pageNumber, pageSize, fieldName, sortDirection);
        } catch (final Exception exception) {
            LOGGER.error("logging an exception start. [getAgencyListWithSearchParams()].");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getAgencyListWithSearchParams", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getAgencyListWithSearchParams()]");
            throw exception;
        }
    }

    /**
     * Method used to get all the agencies for referral creation drop downs.
     * adding an extra "OTHER" agency for that.
     * 
     * @return all agencies data
     */
    @ApiOperation(value = "get name and ids of all agency")
    @RequestMapping(value = "/v1/agencyidnameforreferral", method = RequestMethod.GET)
    public List<AgencyIdName> getAllAgenciesIdNameForDynamicReferralCreation(
            @RequestHeader(value = "tenantid") final String tenantId) {
        try {
            return agencyService.getAllAgenciesIdNameForDynamicReferralCreation(tenantId);
        } catch (final Exception exception) {
            LOGGER.error("logging an exception start. [getAllAgenciesIdNameForDynamicReferralCreation()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getAllAgenciesIdNameForDynamicReferralCreation", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getAllAgenciesIdNameForDynamicReferralCreation()]");
            throw exception;
        }
    }

    /**
     * Method used to get all the agencies
     *
     * @return all agencies data
     */
    @ApiOperation(value = "get name and ids of all agency")
    @RequestMapping(value = "/v1/agencyidname", method = RequestMethod.GET)
    public List<AgencyIdName> getAllAgenciesIdName(@RequestHeader(value = "tenantid") final String tenantId,
            @RequestParam(value = "networkpartneronly", required = false) final boolean networkPartnerOnly,
            @RequestParam(value = "onlyDisplayInResourceDirSearch", required = false) final boolean onlyDisplayInResourceDirSearch) {
        try {
            return agencyService.getAgenciesIdAndName(tenantId, networkPartnerOnly, onlyDisplayInResourceDirSearch);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getAllAgenciesIdName()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception, "getAllAgenciesIdName",
                    TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getAllAgenciesIdName()]");
            throw exception;
        }
    }

    /**
     * get the data access permission of the specific agency
     *
     * @param id
     * @return data access permission
     */
    @ApiOperation(value = "get data access permission of specified agency")
    @RequestMapping(value = "/v1/datapermission/{id}", method = RequestMethod.GET)
    public List<PermissionGroup> getDataAccessPermission(@PathVariable final String id) {
        try {
            return agencyService.getAccessPermission(id);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getDataAccessPermission()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getDataAccessPermission", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getDataAccessPermission()]");
            throw exception;
        }
    }

    @ApiOperation(value = "get referral required documentation of specified agency")
    @RequestMapping(value = "/v1/referralrequireddocumentation/{id}", method = RequestMethod.GET)
    public ReferralRequirement getReferralRequiredDocumentation(@PathVariable final String id) {
        try {
            return agencyService.getReferralRequiredDocumentation(id);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getReferralRequiredDocumentation()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getReferralRequiredDocumentation", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getReferralRequiredDocumentation()]");
            throw exception;
        }
    }

    @ApiOperation(value = "get paper referral document")
    @RequestMapping(value = "/v1/referral/document/{agencyId}", method = RequestMethod.GET)
    public DownloadedFile downloadAgencyReferralDocument(@PathVariable final String agencyId,
            @RequestParam(value = "filename") final String fileName) throws UnsupportedEncodingException {
        try {
            return agencyService.downloadAgencyReferralDocument(agencyId, fileName);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [downloadClientDocument()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "downloadClientDocument", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [downloadClientDocument()]");
            throw exception;
        }
    }

    @ApiOperation(value = "get one of additional document")
    @RequestMapping(value = "/v1/referral/additionaldocument/{agencyId}", method = RequestMethod.GET)
    public DownloadedFile downloadAdditionalDocument(@PathVariable final String agencyId,
            @RequestParam(value = "filename") final String fileName) {
        try {
            return agencyService.downloadAgencyAdditionalDocument(agencyId, fileName);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [downloadAdditionalDocument()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "downloadAdditionalDocument", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [downloadAdditionalDocument()]");
            throw exception;
        }
    }

    @ApiOperation(value = "get id, name, date and services of all agency. allAgencies flag will by pass the Super Admin check")
    @RequestMapping(value = "/v1", method = RequestMethod.GET)
    public List<AgencyShort> getAllAgencies(@RequestHeader(value = "tenantid") final String tenantId,
            @RequestParam(value = "fieldName", required = false, defaultValue = "DATE") final String fieldName,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "ASC") final String sortDirection,
            @RequestParam(value = "bypassRole", required = false) final boolean bypassRole,
            @RequestParam(value = "allAgencies", required = false) final boolean allAgencies,
            @RequestParam(value = "showRegions", required = false) final boolean showRegions) {
        try {
            return agencyService.getAllAgencies(fieldName, sortDirection, allAgencies, tenantId, bypassRole,
                    showRegions);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getAllAgencies()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception, "getAllAgencies",
                    TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getAllAgencies()]");
            throw exception;
        }
    }

    @ApiOperation(value = "get id, name, date and services of all agency. allAgencies flag will by pass the Super Admin check")
    @RequestMapping(value = "/v1/paginated", method = RequestMethod.GET)
    public PageableAgencyResponse getAllAgenciesPaginated(
            @RequestParam(value = "fieldName", required = false, defaultValue = "DATE") final String fieldName,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "ASC") final String sortDirection,
            @RequestParam(value = "bypassRole", required = false) final boolean bypassRole,
            @RequestParam(value = "searchParam", required = false) final String searchParam,
            @RequestParam(value = "allAgencies", required = false) final boolean allAgencies,
            @RequestParam(value = "isConcise", required = false) final boolean isConcise,
            @Min(0) @RequestParam(value = "pageNumber", defaultValue = "0") final int pageNumber,
            @Min(1) @RequestParam(value = "pageSize", defaultValue = Integer.MAX_VALUE + "") final int pageSize) {
        try {
            return agencyService.getAllAgenciesPaginated(fieldName, sortDirection, allAgencies, searchParam, bypassRole,
                    pageNumber, pageSize, isConcise);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getAllAgenciesPaginated()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getAllAgenciesPaginated", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getAllAgenciesPaginated()]");
            throw exception;
        }
    }

    @ApiOperation(value = "get all agencies along with favorites of the given agency. allAgencies flag will by pass the Super Admin check")
    @RequestMapping(value = "/v1/favorites/agencylist", method = RequestMethod.GET)
    public PageableAgencyFavoriteResponse getAllFavoriteAgencies(
            @RequestParam(value = "fieldName", required = false, defaultValue = "NICKNAME") final String fieldName,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "ASC") final String sortDirection,
            @RequestParam(value = "bypassRole", required = false) final boolean bypassRole,
            @RequestParam(value = "searchParam", required = false) final String searchParam,
            @RequestParam(value = "agencyId", required = false) final String agencyId,
            @RequestParam(value = "onlyFavorites", required = false) final boolean onlyFavorites,
            @Min(0) @RequestParam(value = "pageNumber", defaultValue = "0") final int pageNumber,
            @Min(1) @RequestParam(value = "pageSize", defaultValue = Integer.MAX_VALUE + "") final int pageSize) {
        try {
            return agencyService.getAllAgenciesByFavorite(fieldName, sortDirection, searchParam, bypassRole, pageNumber,
                    pageSize, agencyId, onlyFavorites);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getAllFavoriteAgencies()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getAllFavoriteAgencies", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getAllFavoriteAgencies()]");
            throw exception;
        }
    }

    /**
     * @param categories: string array contains the names of categories
     * @return list of agencies which have given categories
     */
    @ApiOperation(value = "search agencies by specified categories,age group and income of agency")
    @RequestMapping(value = "/v1/categories/search", method = RequestMethod.POST)
    public List<AgencyByService> getAgencyByCategoriesAndRequestParams(
            @RequestHeader(value = "tenantid") final String tenantId,
            @RequestBody final AgencySearchByCategory searchBody) {
        try {
            return agencyService.getAgenciesByCategories(searchBody, tenantId);
        } catch (Exception exception) {
            exception.printStackTrace();
            LOGGER.error("logging an exception start. [getAgencyByCategoriesAndRequestParams()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getAgencyByCategoriesAndRequestParams", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getAgencyByCategoriesAndRequestParams()]");
            throw exception;
        }
    }

    @ApiOperation(value = "search agencies by specified categories,age group and income of agency version 2")
    @RequestMapping(value = "/v2/categories/search", method = RequestMethod.POST)
    public List<AgencyByService> getAgencyByCategoriesAndRequestParamsV2(
            @RequestHeader(value = "tenantid") final String tenantId,
            @RequestBody final AgencySearchByCategory searchBody) {
        try {
            return agencyService.getAgencyByCategoriesAndRequestParamsV2(searchBody, tenantId);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getAgencyByCategoriesAndRequestParamsV2()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getAgencyByCategoriesAndRequestParamsV2", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getAgencyByCategoriesAndRequestParamsV2()]");
            throw exception;
        }
    }

    /**
     * @param category : category name and its subCategories
     * @return the agencies on basis of specific category and its subCategories
     */
    @ApiOperation(value = "search agencies by specifying the sub-categories of specific category")
    @RequestMapping(value = "/v1/subcategories/search", method = RequestMethod.POST)
    public List<AgencyWithNamePictures> getAgencyBySubCategoriesInCategory(
            @RequestHeader(value = "tenantid") final String tenantId,
            @RequestBody final AgencySearchBySubCategory searchBySubCategory) {
        try {
            return agencyService.getAgenciesBySubCategories(searchBySubCategory, tenantId);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getAgenciesBySubCategories()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getAgenciesBySubCategories", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getAgenciesBySubCategories()]");
            throw exception;
        }
    }

    @ApiOperation(value = "delete specific agency")
    @RequestMapping(value = "/v1/{id}", method = RequestMethod.DELETE)
    public String deleteAgencyById(@PathVariable(value = "id") final String id) {
        try {
            return agencyService.deleteAgency(id);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [deleteAgencyById()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception, "deleteAgencyById",
                    TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [deleteAgencyById()]");
            throw exception;
        }
    }

    @ApiOperation(value = "Get list of agency referrals information")
    @RequestMapping(value = "/v1/referral", method = RequestMethod.POST)
    public List<AgencyReferral> getReferralList(@RequestBody final List<AgencyReferral> agencyReferral) {
        try {
            return agencyService.saveAgencyReferral(agencyReferral);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getReferralList()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception, "getReferralList",
                    TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getReferralList()]");
            throw exception;
        }
    }

    @ApiOperation(value = "Get list of agencies referred to Client")
    @RequestMapping(value = "/v1/referral", method = RequestMethod.GET)
    public List<AgencyIdName> getReferredAgencies(
            @RequestParam(value = "agencies", required = false) final String agencies) {
        try {
            return agencyService.getReferredAgencies(agencies);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getReferredAgencies()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception, "getReferredAgencies",
                    TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getReferredAgencies()]");
            throw exception;
        }
    }

    @ApiOperation(value = "Get domains by agency Id")
    @RequestMapping(value = "/v1/domains/{agencyid}", method = RequestMethod.GET)
    public List<String> getDomainsByAgencyId(@PathVariable(value = "agencyid") final String agencyId) {
        try {
            return agencyService.getDomainsByAgencyId(agencyId);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getDomainsByAgencyId()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception, "getDomainsByAgencyId",
                    TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getDomainsByAgencyId()]");
            throw exception;
        }
    }

    @ApiOperation(value = "Get the time stamp for the last time a agency is updated")
    @RequestMapping(value = "/v1/lastupdatedtime", method = RequestMethod.GET)
    public LastUpdatedAt getLastUpdatedDateTimeForAgencies() {
        try {
            return agencyService.getLastUpdatedDateTimeForAgencies();
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getLastUpdatedDateTimeForAgencies()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getLastUpdatedDateTimeForAgencies", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getLastUpdatedDateTimeForAgencies()]");
            throw exception;
        }
    }

    @ApiOperation(value = "Get List of Agencies which can select the referral from specific agencies")
    @RequestMapping(value = "/v1/acceptreferralfrom/{agencyId}/referral/{referralid}", method = RequestMethod.GET)
    public List<AgencyIdName> getAgenciesAcceptReferralFromSpecificAgency(@PathVariable final String agencyId,
            @PathVariable(value = "referralid") final String referralId,
            @RequestParam(value = "onlyAcceptElectronic", required = false, defaultValue = "false") final boolean onlyAcceptElectronic) {
        try {
            return agencyService.getAgenciesAcceptReferralFromSpecificAgency(agencyId, referralId,
                    onlyAcceptElectronic);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getAgenciesAcceptReferralFromSpecificAgency()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getAgenciesAcceptReferralFromSpecificAgency", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getAgenciesAcceptReferralFromSpecificAgency()]");
            throw exception;
        }
    }

    @ApiOperation(value = "Get List of Categories name of specific agency irrespective of domain name")
    @RequestMapping(value = "/v1/categories/{agencyId}", method = RequestMethod.GET)
    public List<String> getCategoriesNameOfSpecificAgency(@PathVariable final String agencyId) {
        try {
            return agencyService.getListOfCategoriesName(agencyId);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getCategoriesNameOfSpecificAgency()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getCategoriesNameOfSpecificAgency", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getCategoriesNameOfSpecificAgency()]");
            throw exception;
        }
    }

    @ApiOperation(value = "get agencies/organizations by organization type")
    @RequestMapping(value = { "/v1/organization/{organizationtype}" }, method = RequestMethod.GET)
    @ResponseBody
    public List<AgencyIdName> getAgenciesByOrganizationType(
            @PathVariable(value = "organizationtype") final String organizationType) {
        try {
            return agencyService.getAgenciesByOrganizationType(organizationType);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getAgenciesByOrganizationType()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getAgenciesByOrganizationType", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getAgenciesByOrganizationType()]");
            throw exception;
        }
    }

    @ApiOperation(value = "Get List agencies that accepts express referral")
    @RequestMapping(value = "/v1/referral/express", method = RequestMethod.GET)
    public List<AgencyIdName> getAgenciesAcceptExpressReferral() {
        try {
            return agencyService.getAgenciesAcceptExpressReferral();
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getAgenciesAcceptExpressReferral()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getAgenciesAcceptExpressReferral", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getAgenciesAcceptExpressReferral()]");
            throw exception;
        }
    }

    @ApiOperation(value = "Get List favorite agencies against agency and user along with referral data by id")
    @RequestMapping(value = "/v1/favoriteagenciesandreferralInfo", method = RequestMethod.GET)
    public FavoriteAgenciesPlusReferralnfo getFavoriteAgenciesAndReferralInfo(
            @RequestHeader(value = "tenantid") final String tenantId,
            @RequestParam(value = "agencyId") final String agencyId,
            @RequestParam(value = "userId") final String userId,
            @RequestParam(value = "isConcise", required = false) final boolean isConcise) {
        try {
            return agencyService.getFavoriteAgenciesAndReferralInfo(agencyId, userId, tenantId, isConcise);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getFavoriteAgenciesAndReferralInfo()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getFavoriteAgenciesAndReferralInfo", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getFavoriteAgenciesAndReferralInfo()]");
            throw exception;
        }
    }

    @ApiOperation(value = "Get organization type and associated agencies of all agencies")
    @RequestMapping(value = "/v1/organizationtype", method = RequestMethod.GET)
    public List<Agency> getOrganizationTypesOfAllAgencies() {
        try {
            return agencyService.getOrganizationTypesOfAllAgencies();
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getOrganizationTypesOfAllAgencies()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getOrganizationTypesOfAllAgencies", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getOrganizationTypesOfAllAgencies()]");
            throw exception;
        }
    }

    @ApiOperation(value = "Check whether given user is referral recipient of given agency ")
    @RequestMapping(value = "/v1/{agencyid}/recipient/{userid}", method = RequestMethod.GET)
    public boolean checkUserIsReferralRecipient(@PathVariable(value = "agencyid") final String agencyId,
            @PathVariable(value = "userid") final String userId) {
        try {
            return agencyService.userIsReferralRecipient(agencyId, userId);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [checkUserIsReferralRecipient()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "checkUserIsReferralRecipient", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [checkUserIsReferralRecipient()]");
            throw exception;
        }
    }

    @ApiOperation(value = "Get tenant information through agency Id")
    @RequestMapping(value = "/v1/tenantinfo/{agencyid}", method = RequestMethod.GET)
    public List<Tenant> getTenantInfoThroughAgency(@PathVariable(value = "agencyid") final String agencyId) {
        try {
            return agencyService.getTenantInfoThroughAgency(agencyId);
        } catch (final Exception exception) {
            LOGGER.error("logging an exception start. [getTenantInfoThroughAgency()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getTenantInfoThroughAgency", TargetNameConstants.TENANT);
            LOGGER.error("logging an exception end. [getTenantInfoThroughAgency()]");
            throw exception;
        }
    }

    @ApiOperation(value = "Get agency level user instructions")
    @RequestMapping(value = "/v1/instructions/{agencyid}", method = RequestMethod.GET)
    public AttestationSetting getAgencyLevelUserInstruction(@PathVariable(value = "agencyid") final String agencyId) {
        try {
            return agencyService.getAgencyLevelUserInstruction(agencyId);
        } catch (final Exception exception) {
            LOGGER.error("logging an exception start. [getAgencyLevelUserInstruction()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getAgencyLevelUserInstruction", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getAgencyLevelUserInstruction()]");
            throw exception;
        }
    }

    @ApiOperation(value = "InterService Communication- get Multiple Agencies ByIds")
    @RequestMapping(value = "/v1/agencies", method = RequestMethod.POST)
    public List<Agency> getAgenciesByIds(@RequestBody final List<String> agencyIds) {
        try {
            LOGGER.info("Controller method getAgenciesByIds()");
            return agencyService.getAgenciesByIds(agencyIds);
        } catch (final Exception exception) {
            LOGGER.error("logging an exception start. [getAgenciesByIds]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception, "getAgenciesByIds",
                    TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getAgenciesByIds]");
            throw exception;
        }
    }

    @ApiOperation(value = "InterService Communication- update Gatekeeper of Specific agency on userUpdation- end-point will remove the given referredUser from agency")
    @RequestMapping(value = "/v1/updategatekeepersofagency/{agencyid}", method = RequestMethod.PUT)
    public HashMap<String, String> updateGateKeepersOfAgency(@PathVariable(value = "agencyid") final String agencyId,
            @RequestBody final CurrentUser currentUser) {
        try {
            LOGGER.info("Controller method updateGateKeepersOfAgency()");
            return agencyService.updateGateKeepersOfAgency(agencyId, currentUser);
        } catch (final Exception exception) {
            LOGGER.error("logging an exception start. [updateGateKeepersOfAgency]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "updateGateKeepersOfAgency", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [updateGateKeepersOfAgency]");
            throw exception;
        }
    }

    @RequestMapping(value = "/v1/agency/activeagencies", method = RequestMethod.POST)
    public ResponseEntity getActiveAgencies(@RequestBody AllAgenciesRequest allAgenciesRequest) {
        return agencyService.getActiveAgencies(allAgenciesRequest);
    }

    @RequestMapping(value = "/v1/agencyIds", method = RequestMethod.POST)
    public List<AgencyIdName> getAgenciesByTenantIds(@RequestBody final List<String> tenantIds) {
        try {
            LOGGER.info("Controller method getAgenciesByIds()");
            return agencyService.getAgencyIdsByTenantIds(tenantIds);
        } catch (final Exception exception) {
            LOGGER.error("logging an exception start. [getAgenciesByIds]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception, "getAgenciesByIds",
                    TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getAgenciesByIds]");
            throw exception;
        }
    }

    @ApiOperation(value = "InterService Communication- to get Default Regions to Update in all users used once only in deployment of 1.19")
    @RequestMapping(value = "/v1/getdefaultregionforusers", method = RequestMethod.GET)
    public HashMap<String, List<TenantRegion>> getAllRegionAndUpdateInUser() {
        try {
            LOGGER.info("getAllRegionAndUpdateInUser()");
            return agencyService.getAllRegionAndUpdateInUser();
        } catch (final Exception exception) {
            LOGGER.error("logging an exception start. [getAllRegionAndUpdateInUser]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getAllRegionAndUpdateInUser", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getAllRegionAndUpdateInUser]");
            throw exception;
        }
    }

    @ApiOperation(value = "To Update ReferralRecipient Object in agencies according to user permission.This end-point will be used once in 1.20")
    @RequestMapping(value = "/v1/updatereferralrecipientaccordingtogatekeeperpermission", method = RequestMethod.GET)
    public HashMap<String, String> updateReferralRecipientAccordingToGatekeeperPermission() {
        try {
            LOGGER.info("updateReferralRecipientAccordingToGatekeeperPermission()");
            return agencyService.updateReferralRecipientAccordingToGatekeeperPermission();
        } catch (final Exception exception) {
            LOGGER.error("logging an exception start. [updateReferralRecipientAccordingToGatekeeperPermission]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "updateReferralRecipientAccordingToGatekeeperPermission", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [updateReferralRecipientAccordingToGatekeeperPermission]");
            throw exception;
        }
    }

    @ApiOperation(value = "InterService Communication- mark Specific agency to partner network")
    @RequestMapping(value = "/v1/markEntityAsPartnerNetwork/{agencyid}", method = RequestMethod.PUT)
    public HashMap<String, String> markEntityAsPartnerNetwork(@PathVariable(value = "agencyid") final String agencyId) {
        try {
            LOGGER.info("Controller method markEntityAsPartnerNetwork()");
            return agencyService.markEntityAsPartnerNetwork(agencyId);
        } catch (final Exception exception) {
            LOGGER.error("logging an exception start. [markEntityAsPartnerNetwork]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "markEntityAsPartnerNetwork", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [markEntityAsPartnerNetwork]");
            throw exception;
        }
    }

    @ApiOperation(value = "Check form uniqueness with name")
    @RequestMapping(value = "/v1/agency/checkcosagencyidunique", method = RequestMethod.GET)
    public boolean checkCosAgencyIdIsUnique(
            @RequestParam(value = "cosagencyid", required = false) final String cosAgencyId,
            @RequestParam(value = "agencyid", required = false) final String agencyId) {
        try {
            return agencyService.checkCosAgencyIdIsUnique(agencyId, cosAgencyId,
                    AuthenticationUtil.getCurrentTenantId());
        } catch (final Exception exception) {
            LOGGER.error("logging an exception start. [checkCosAgencyIdIsUnique()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "checkCosAgencyIdIsUnique", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [checkCosAgencyIdIsUnique()]");
            throw exception;
        }
    }

    @GetMapping(value = "/v1/findbyorgid/{orgid}")
    public List<Agency> getAgencyBasedOnOrgId(@PathVariable(value = "orgid") final String adtOrgId) {
        try {
            LOGGER.info("Controller method getAgencyBasedOnOrgId()");
            return agencyService.getAgencyBasedOnOrgId(adtOrgId);
        } catch (final Exception exception) {
            LOGGER.error("logging an exception start. [getAgencyBasedOnOrgId]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getAgencyBasedOnOrgId", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getAgencyBasedOnOrgId]");
            throw exception;
        }
    }

    // this End-point will be removed after RIE demo-- its only available in DEV and
    // Stage env.
    @ApiOperation(value = "FOR RIE - get details of specific Agency")
    @RequestMapping(value = { "/v1/rie/organization/{id}" }, method = RequestMethod.GET)
    @ResponseBody
    public Agency findAgencyByIdForRie(@PathVariable final String id) {
        try {
            return agencyService.getAgencyById(id);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [findAgencyByIdForRie()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception, "findAgencyByIdForRie",
                    TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [findAgencyByIdForRie()]");
            throw exception;
        }
    }

    //
    // this End-point will be removed after RIE demo-- its only available in DEV and
    // Stage env.
    @ApiOperation(value = "For RIE - Get domains by agency Id")
    @RequestMapping(value = "/v1/rie/domains/{agencyid}", method = RequestMethod.GET)
    public List<String> getDomainsByAgencyIdForRie(@PathVariable(value = "agencyid") final String agencyId) {
        try {
            return agencyService.getDomainsByAgencyId(agencyId);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [getDomainsByAgencyIdForRie()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "getDomainsByAgencyIdForRie", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [getDomainsByAgencyIdForRie()]");
            throw exception;
        }
    }

    @ApiOperation(value = "get all agencies associated with a program")
    @RequestMapping(value = { "/v1/getagencies/{programid}" }, method = RequestMethod.GET)
    @ResponseBody
    public List<AgencyIdName> findAgenciesAssociatedWithAProgram(@PathVariable final String programid) {
        try {
            return agencyService.getAllAgenciesAssociatedWithAProgram(programid);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [findAgenciesAssociatedWithAProgram()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "findAgenciesAssociatedWithAProgram", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [findAgenciesAssociatedWithAProgram()]");
            throw exception;
        }
    }

    @ApiOperation(value = "get all agencies and Programs associated with a service")
    @RequestMapping(value = { "/v1/getprogramsagencies/{serviceId}" }, method = RequestMethod.GET)
    @ResponseBody
    public List<ProgramAndAgencyData> findAgenciesAndProgramsAssociatedWithAService(
            @PathVariable final String serviceId) {
        try {
            return agencyService.findAgenciesAndProgramsAssociatedWithAService(serviceId);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [findAgenciesAndProgramsAssociatedWithAService()]");
            auditLogging.logExceptionEvent(auditLogging.getCurrentLoggedInUserInfo(), exception,
                    "findAgenciesAndProgramsAssociatedWithAService", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [findAgenciesAndProgramsAssociatedWithAService()]");
            throw exception;
        }
    }

    // API for Velatura
    @GetMapping(value = "/v1/velatura/findbyorgids")
    public List<Agency> getAgenciesBasedOnOrgIds(@RequestParam(value = "orgids") final String orgIds) {
        try {
            return agencyService.getAgencyBasedOnOrgIds(orgIds);
        } catch (final Exception exception) {
            throw exception;
        }
    }

    // API for Velatura
    @GetMapping(value = "/v1/velatura/findbyids")
    public List<Agency> getAgenciesBasedOnIds(@RequestParam(value = "ids") final String ids) {
        try {
            return agencyService.getAgencyBasedOnIds(ids);
        } catch (final Exception exception) {
            throw exception;
        }
    }

    @ApiOperation(value = "get affiliated programs for the agency")
    @RequestMapping(value = { "/v1/agency/program/{agencyid}" }, method = RequestMethod.GET)
    @ResponseBody
    public List<ProgramAffiliation> findProgramsByAgencyId(@PathVariable final String agencyid) {
        try {
            return agencyService.findProgramsByAgencyId(agencyid);
        } catch (Exception exception) {
            LOGGER.error("logging an exception start. [findProgramsByAgencyId()]");
            auditLogging.logExceptionEvent(null, exception, "findProgramsByAgencyId", TargetNameConstants.AGENCY);
            LOGGER.error("logging an exception end. [findProgramsByAgencyId()]");
            throw exception;
        }
    }

}
