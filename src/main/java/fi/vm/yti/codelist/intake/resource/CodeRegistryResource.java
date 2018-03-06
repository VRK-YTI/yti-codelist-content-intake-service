package fi.vm.yti.codelist.intake.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fi.vm.yti.codelist.intake.exception.*;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.ErrorModel;
import fi.vm.yti.codelist.common.model.Meta;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.common.model.Views;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.domain.Domain;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.jpa.ExternalReferenceRepository;
import fi.vm.yti.codelist.intake.parser.CodeParser;
import fi.vm.yti.codelist.intake.parser.CodeRegistryParser;
import fi.vm.yti.codelist.intake.parser.CodeSchemeParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_PATH_CODEREGISTRIES;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_PATH_CODES;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_PATH_CODESCHEMES;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_PATH_VERSION_V1;
import static fi.vm.yti.codelist.common.constants.ApiConstants.EXCEL_SHEET_CODESCHEMES;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_CODE;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_CODEREGISTRY;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_CODESCHEME;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_CSV;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_EXCEL;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_JSON;
import static fi.vm.yti.codelist.common.constants.ApiConstants.METHOD_DELETE;
import static fi.vm.yti.codelist.common.constants.ApiConstants.METHOD_POST;

@Component
@Path("/v1/coderegistries")
@Api(value = "coderegistries")
@Produces("text/plain")
public class CodeRegistryResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(CodeRegistryResource.class);
    private final Domain domain;
    private final Indexing indexing;
    private final CodeRegistryParser codeRegistryParser;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeSchemeParser codeSchemeParser;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeParser codeParser;
    private final CodeRepository codeRepository;
    private final ExternalReferenceRepository externalReferenceRepository;
    private final AuthorizationManager authorizationManager;

    @Inject
    public CodeRegistryResource(final Domain domain,
                                final Indexing indexing,
                                final CodeRegistryParser codeRegistryParser,
                                final CodeRegistryRepository codeRegistryRepository,
                                final CodeSchemeParser codeSchemeParser,
                                final CodeSchemeRepository codeSchemeRepository,
                                final CodeParser codeParser,
                                final CodeRepository codeRepository,
                                final ExternalReferenceRepository externalReferenceRepository,
                                final AuthorizationManager authorizationManager) {
        this.domain = domain;
        this.indexing = indexing;
        this.codeRegistryParser = codeRegistryParser;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeParser = codeSchemeParser;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeParser = codeParser;
        this.codeRepository = codeRepository;
        this.externalReferenceRepository = externalReferenceRepository;
        this.authorizationManager = authorizationManager;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses CodeRegistries from JSON input.")
    @ApiResponse(code = 200, message = "Returns success.")
    @Transactional
    public Response addOrUpdateCodeRegistriesFromJson(@ApiParam(value = "JSON playload for CodeRegistry data.", required = true) final String jsonPayload) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES);
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODEREGISTRY, null)));
        final Meta meta = new Meta();
        final ResponseWrapper<CodeRegistry> wrapper = new ResponseWrapper<>(meta);
        if (!authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
        }
        try {
            Set<CodeRegistry> codeRegistries = new HashSet<>();
            if (jsonPayload != null && !jsonPayload.isEmpty()) {
                codeRegistries = codeRegistryParser.parseCodeRegistriesFromJson(jsonPayload);
            }
            for (final CodeRegistry register : codeRegistries) {
                LOG.debug("CodeRegistry parsed from input: " + register.getCodeValue());
            }
            if (!codeRegistries.isEmpty()) {
                domain.persistCodeRegistries(codeRegistries);
                // TODO only reindex relevant data.
                indexing.reIndexEverything();
            }
            meta.setMessage("CodeRegistries added or modified: " + codeRegistries.size());
            meta.setCode(200);
            wrapper.setResults(codeRegistries);
            return Response.ok(wrapper).build();
        } catch (final Exception e) {
            return handleInternalServerError(meta, wrapper, "Error parsing CodeRegistries.", e, ErrorConstants.ERR_MSG_USER_500);
        }
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses CodeRegistries from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", required = false, dataType = "file", paramType = "formData")
    })
    @Transactional
    public Response addOrUpdateCodeRegistriesFromFile(@ApiParam(value = "Format for input.", required = true) @QueryParam("format") @DefaultValue("json") final String format,
                                                      @ApiParam(value = "Input-file for CSV or Excel import.", required = false, hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES);
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODEREGISTRY, null)));
        final Meta meta = new Meta();
        final ResponseWrapper<CodeRegistry> wrapper = new ResponseWrapper<>(meta);
        if (!authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
        }
        try {
            Set<CodeRegistry> codeRegistries = new HashSet<>();
            if (FORMAT_CSV.equalsIgnoreCase(format)) {
                codeRegistries = codeRegistryParser.parseCodeRegistriesFromCsvInputStream(inputStream);
            } else if (FORMAT_EXCEL.equalsIgnoreCase(format)) {
                codeRegistries = codeRegistryParser.parseCodeRegistriesFromExcelInputStream(inputStream);
            }
            for (final CodeRegistry register : codeRegistries) {
                LOG.debug("CodeRegistry parsed from input: " + register.getCodeValue());
            }
            if (!codeRegistries.isEmpty()) {
                domain.persistCodeRegistries(codeRegistries);
                // TODO only reindex relevant data.
                indexing.reIndexEverything();
            }
            meta.setMessage("CodeRegistries added or modified: " + codeRegistries.size());
            meta.setCode(200);
            wrapper.setResults(codeRegistries);
            return Response.ok(wrapper).build();
        } catch (final Exception e) {
            return handleInternalServerError(meta, wrapper, "Error parsing CodeRegistries.", e, ErrorConstants.ERR_MSG_USER_500);
        }
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses CodeSchemes from JSON input.")
    @ApiResponse(code = 200, message = "Returns success.")
    @Transactional
    public Response addOrUpdateCodeSchemesFromJson(@ApiParam(value = "Format for input.", required = false) @QueryParam("format") @DefaultValue("json") final String format,
                                                   @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                   @ApiParam(value = "JSON playload for CodeScheme data.", required = true) final String jsonPayload) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/");
        final Meta meta = new Meta();
        final ResponseWrapper<CodeScheme> responseWrapper = new ResponseWrapper<>(meta);
        ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODESCHEME, "codeRegistry,code")));
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
            }
            Set<CodeScheme> codeSchemes = new HashSet<>();
            try {
                if (FORMAT_JSON.equalsIgnoreCase(format) && jsonPayload != null && !jsonPayload.isEmpty()) {
                    codeSchemes = codeSchemeParser.parseCodeSchemesFromJsonInput(codeRegistry, jsonPayload);
                }
            } catch (final Exception e) {
                return handleInternalServerError(meta, responseWrapper, "Internal server error during call to addOrUpdateCodeSchemesFromJson.", e, ErrorConstants.ERR_MSG_USER_500);
            }
            for (final CodeScheme codeScheme : codeSchemes) {
                LOG.debug("CodeScheme parsed from input: " + codeScheme.getCodeValue());
            }
            if (!codeSchemes.isEmpty()) {
                domain.persistCodeSchemes(codeSchemes);
                indexing.updateCodeSchemes(codeSchemes);
                for (final CodeScheme codeScheme : codeSchemes) {
                    LOG.debug("CodeScheme parsed from input: " + codeScheme.getCodeValue());
                }
                if (!codeSchemes.isEmpty()) {
                    domain.persistCodeSchemes(codeSchemes);
                }
            }
            indexCodeSchemes(codeSchemes);
            meta.setMessage("CodeSchemes added or modified: " + codeSchemes.size());
            meta.setCode(200);
            responseWrapper.setResults(codeSchemes);
            return Response.ok(responseWrapper).build();
        }
        meta.setMessage("CodeScheme with code: " + codeRegistryCodeValue + " does not exist yet, please creater register first.");
        meta.setCode(HttpStatus.NOT_ACCEPTABLE.value());
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseWrapper).build();
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses CodeSchemes from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", required = false, dataType = "file", paramType = "formData")
    })
    @Transactional
    public Response addOrUpdateCodeSchemesFromFile(@ApiParam(value = "Format for input.", required = false) @QueryParam("format") @DefaultValue("csv") final String format,
                                                   @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                   @ApiParam(value = "Input-file for CSV or Excel import.", required = false, hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/");
        final Meta meta = new Meta();
        final ResponseWrapper<CodeScheme> responseWrapper = new ResponseWrapper<>(meta);
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODESCHEME, "codeRegistry")));
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
            }
            Set<CodeScheme> codeSchemes = new HashSet<>();
            Set<Code> codes = new HashSet<>();
            if (FORMAT_CSV.equalsIgnoreCase(format)) {
                codeSchemes = codeSchemeParser.parseCodeSchemesFromCsvInputStream(codeRegistry, inputStream);
            } else if (FORMAT_EXCEL.equalsIgnoreCase(format)) {
                try (final Workbook workbook = WorkbookFactory.create(inputStream)) {
                    codeSchemes = codeSchemeParser.parseCodeSchemesFromExcel(codeRegistry, workbook);
                    if (!codeSchemes.isEmpty() && codeSchemes.size() == 1 && workbook.getSheet(EXCEL_SHEET_CODESCHEMES) != null) {
                        codes = codeParser.parseCodesFromExcel(codeSchemes.iterator().next(), workbook);
                    }
                } catch (final YtiCodeListException e) {
                    throw e;
                } catch (final IOException | InvalidFormatException e) {
                    throw new CodeParsingException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                        ErrorConstants.ERR_MSG_USER_MISSING_HEADER_CLASSIFICATION));
                }
            }
            if (!codeSchemes.isEmpty()) {
                domain.persistCodeSchemes(codeSchemes);
                indexing.updateCodeSchemes(codeSchemes);
            }
            if (!codes.isEmpty()) {
                domain.persistCodes(codes);
            }
            indexCodeSchemes(codeSchemes);
            indexCodes(codes);
            meta.setMessage("CodeSchemes added or modified: " + codeSchemes.size());
            meta.setCode(200);
            responseWrapper.setResults(codeSchemes);
            return Response.ok(responseWrapper).build();
        }
        meta.setMessage("CodeScheme with code: " + codeRegistryCodeValue + " does not exist yet, please creater register first.");
        meta.setCode(HttpStatus.NOT_ACCEPTABLE.value());
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseWrapper).build();
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeId}/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Modifies single existing CodeScheme.")
    @ApiResponse(code = 200, message = "Returns success.")
    @Transactional
    public Response updateCodeScheme(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                     @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeId") final String codeSchemeId,
                                     @ApiParam(value = "JSON playload for Code data.", required = false) final String jsonPayload) {

        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeId + "/");
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
            }
            final CodeScheme existingCodeScheme = codeSchemeRepository.findByCodeRegistryAndId(codeRegistry, UUID.fromString(codeSchemeId));
            if (existingCodeScheme != null) {
                try {
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        final CodeScheme codeScheme = codeSchemeParser.parseCodeSchemeFromJsonInput(codeRegistry, jsonPayload);
                        codeSchemeRepository.save(codeScheme);
                        if (indexing.updateCodeScheme(codeScheme) &&
                            indexing.updateCodes(codeRepository.findByCodeScheme(codeScheme)) &&
                            indexing.updateExternalReferences(externalReferenceRepository.findByParentCodeScheme(codeScheme))) {
                            meta.setMessage("CodeScheme " + codeSchemeId + " modified.");
                            meta.setCode(200);
                            return Response.ok(responseWrapper).build();
                        } else {
                            return handleInternalServerError(meta, responseWrapper,
                                "CodeScheme " + codeSchemeId + " modifification failed.", new WebApplicationException(), ErrorConstants.ERR_MSG_USER_500);
                        }
                    } else {
                        meta.setMessage("No JSON payload found.");
                        meta.setCode(HttpStatus.NOT_ACCEPTABLE.value());
                    }
                } catch (final IOException e) {
                    return handleInternalServerError(meta, responseWrapper, "Internal server error during call to addOrUpdateCodeScheme.", e, ErrorConstants.ERR_MSG_USER_500);
                }
            } else {
                meta.setMessage("CodeScheme: " + codeSchemeId + " does not exist yet, please create codeScheme first.");
                meta.setCode(HttpStatus.NOT_ACCEPTABLE.value());
            }
        } else {
            meta.setMessage("CodeRegistry with code: " + codeRegistryCodeValue + " does not exist yet, please create registry first.");
            meta.setCode(HttpStatus.NOT_ACCEPTABLE.value());
        }
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseWrapper).build();
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeId}/codes/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses Codes from JSON input.")
    @ApiResponse(code = 200, message = "Returns success.")
    @Transactional
    @JsonView({Views.ExtendedCode.class, Views.Normal.class})
    public Response addOrUpdateCodesFromJson(@ApiParam(value = "Format for input.", required = true) @QueryParam("format") @DefaultValue("json") final String format,
                                             @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                             @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeId") final String codeSchemeId,
                                             @ApiParam(value = "JSON playload for Code data.", required = true) final String jsonPayload) {

        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeId + API_PATH_CODES + "/");
        final Meta meta = new Meta();
        final ResponseWrapper<Code> responseWrapper = new ResponseWrapper<>(meta);
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODE, "codeRegistry,codeScheme")));
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
            }
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndId(codeRegistry, UUID.fromString(codeSchemeId));
            if (codeScheme != null) {
                Set<Code> codes = new HashSet<>();
                //try {
                    if (FORMAT_JSON.equalsIgnoreCase(format) && jsonPayload != null && !jsonPayload.isEmpty()) {
                        codes = codeParser.parseCodesFromJsonData(codeScheme, jsonPayload);
                    }
                /*
                } catch (final Exception e) {
                    return handleInternalServerError(meta, responseWrapper, "Internal server error during call to addOrUpdateCodesFromJson.", e, ErrorConstants.ERR_MSG_USER_500);
                }
                */
                if (!codes.isEmpty()) {
                    domain.persistCodes(codes);
                    indexing.updateCodes(codes);
                }
                indexing.updateCodes(codes);
                meta.setMessage("Codes added or modified: " + codes.size());
                meta.setCode(200);
                responseWrapper.setResults(codes);
                return Response.ok(responseWrapper).build();
            } else {
                meta.setMessage("CodeScheme with id: " + codeSchemeId + " does not exist yet, please create codeScheme first.");
                meta.setCode(HttpStatus.NOT_ACCEPTABLE.value());
            }
        } else {
            meta.setMessage("CodeRegistry with id: " + codeRegistryCodeValue + " does not exist yet, please create registry first.");
            meta.setCode(HttpStatus.NOT_ACCEPTABLE.value());
        }
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseWrapper).build();
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeId}/codes/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses Codes from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", required = false, dataType = "file", paramType = "formData")
    })
    @JsonView({Views.ExtendedCode.class, Views.Normal.class})
    @Transactional
    public Response addOrUpdateCodesFromFile(@ApiParam(value = "Format for input.", required = true) @QueryParam("format") @DefaultValue("csv") final String format,
                                             @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                             @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeId") final String codeSchemeId,
                                             @ApiParam(value = "Input-file for CSV or Excel import.", required = false, hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream) {

        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeId + API_PATH_CODES + "/");
        final Meta meta = new Meta();
        final ResponseWrapper<Code> responseWrapper = new ResponseWrapper<>(meta);
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODE, "codeRegistry,codeScheme")));
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
            }
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndId(codeRegistry, UUID.fromString(codeSchemeId));
            if (codeScheme != null) {
                Set<Code> codes = new HashSet<>();
                try {
                    if (FORMAT_CSV.equalsIgnoreCase(format)) {
                        codes = codeParser.parseCodesFromCsvInputStream(codeScheme, inputStream);
                    } else if (FORMAT_EXCEL.equalsIgnoreCase(format)) {
                        codes = codeParser.parseCodesFromExcelInputStream(codeScheme, inputStream);
                    }
                }
                catch (final IOException | InvalidFormatException e) {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorConstants.ERR_MSG_USER_500));
                }

                if (!codes.isEmpty()) {
                    domain.persistCodes(codes);
                    indexCodes(codes);
                }
                meta.setMessage("Codes added or modified: " + codes.size());
                meta.setCode(200);
                responseWrapper.setResults(codes);
                return Response.ok(responseWrapper).build();
            } else {
                meta.setMessage("CodeScheme with id: " + codeSchemeId + " does not exist yet, please create CodeScheme first.");
                meta.setCode(HttpStatus.NOT_ACCEPTABLE.value());
            }
        } else {
            meta.setMessage("CodeRegistry with id: " + codeRegistryCodeValue + " does not exist yet, please create CodeRegistry first.");
            meta.setCode(HttpStatus.NOT_ACCEPTABLE.value());
        }
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseWrapper).build();
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeId}/codes/{codeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Modifies single existing Code.")
    @ApiResponse(code = 200, message = "Returns success.")
    @Transactional
    public Response updateCode(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                               @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeId") final String codeSchemeId,
                               @ApiParam(value = "Code codeValue.", required = true) @PathParam("codeId") final String codeId,
                               @ApiParam(value = "JSON playload for Code data.", required = false) final String jsonPayload) {

        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeId + API_PATH_CODES + "/" + codeId + "/");
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
            }
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndId(codeRegistry, UUID.fromString(codeSchemeId));
            if (codeScheme != null) {
                //try {
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        final Code code = codeParser.parseCodeFromJsonData(codeScheme, jsonPayload);
                        codeRepository.save(code);
                        codeSchemeRepository.save(codeScheme);
                        if (indexing.updateCode(code) &&
                            indexing.updateCodeScheme(codeScheme) &&
                            indexing.updateExternalReferences(externalReferenceRepository.findByParentCodeScheme(codeScheme))) {
                            meta.setMessage("Code " + codeId + " modified.");
                            meta.setCode(200);
                            return Response.ok(responseWrapper).build();
                        } else {
                            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(),ErrorConstants.ERR_MSG_USER_500 ));
                        }
                    } else {
                        meta.setMessage("No JSON payload found.");
                        meta.setCode(HttpStatus.NOT_ACCEPTABLE.value());
                    }
                //}
            } else {
                meta.setMessage("CodeScheme with id: " + codeSchemeId + " does not exist yet, please create codeScheme first.");
                meta.setCode(HttpStatus.NOT_ACCEPTABLE.value());
            }
        } else {
            meta.setMessage("CodeRegistry with CodeValue: " + codeRegistryCodeValue + " does not exist yet, please create registry first.");
            meta.setCode(HttpStatus.NOT_ACCEPTABLE.value());
        }
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseWrapper).build();
    }

    @DELETE
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeId}/codes/{codeId}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single code. This means that the item status is set to Status.RETIRED.")
    @ApiResponse(code = 200, message = "Returns success.")
    @Transactional
    public Response retireCode(@ApiParam(value = "CodeRegistry codeValue.", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                               @ApiParam(value = "CodeScheme codeValue.", required = true) @PathParam("codeSchemeId") final String codeSchemeId,
                               @ApiParam(value = "Code codeValue.", required = true) @PathParam("codeId") final String codeId) {
        logApiRequest(LOG, METHOD_DELETE, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeId + API_PATH_CODES + "/" + codeId + "/");
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
            }
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValue(codeRegistry, codeSchemeId);
            if (codeScheme != null) {
                final Code code = codeRepository.findByCodeSchemeAndCodeValue(codeScheme, codeId);
                if (code != null) {
                    code.setStatus(Status.RETIRED.toString());
                    codeRepository.save(code);
                    indexing.reIndexEverything();
                    meta.setMessage("Code marked as RETIRED!");
                    meta.setCode(200);
                    return Response.ok(responseWrapper).build();
                } else {
                    meta.setMessage("Code " + codeId + " not found!");
                    meta.setCode(HttpStatus.NOT_ACCEPTABLE.value());
                }
            } else {
                meta.setMessage("CodeScheme with id: " + codeSchemeId + " not found!");
                meta.setCode(HttpStatus.NOT_ACCEPTABLE.value());
            }
        } else {
            meta.setMessage("CodeRegistry with codeValue: " + codeRegistryCodeValue + " not found!");
            meta.setCode(HttpStatus.NOT_FOUND.value());
        }
        return Response.status(Response.Status.NOT_FOUND).entity(responseWrapper).build();
    }

    private void indexCodeSchemes(final Set<CodeScheme> codeSchemes) {
        if (!codeSchemes.isEmpty()) {
            indexing.updateCodeSchemes(codeSchemes);
            for (final CodeScheme codeScheme : codeSchemes) {
                indexing.updateCodes(codeRepository.findByCodeScheme(codeScheme));
                indexing.updateExternalReferences(externalReferenceRepository.findByParentCodeScheme(codeScheme));
            }
        }
    }

    private void indexCodes(final Set<Code> codes) {
        if (!codes.isEmpty()) {
            indexing.updateCodes(codes);
        }
    }
}
