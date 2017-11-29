package fi.vm.yti.codelist.intake.resource;

import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.ExternalReference;
import fi.vm.yti.codelist.common.model.Meta;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

@Component
@Path("/v1/coderegistries")
@Api(value = "coderegistries", description = "Operations for creating, deleting and updating coderegistries, codeschemes and codes.")
@Produces("text/plain")
public class CodeRegistryResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(CodeRegistryResource.class);
    private final Domain domain;
    private final Indexing indexing;
    private final ApiUtils apiUtils;
    private final CodeRegistryParser codeRegistryParser;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeSchemeParser codeSchemeParser;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeParser codeParser;
    private final CodeRepository codeRepository;
    private final ExternalReferenceRepository externalReferenceRepository;

    @Inject
    public CodeRegistryResource(final Domain domain,
                                final Indexing indexing,
                                final ApiUtils apiUtils,
                                final CodeRegistryParser codeRegistryParser,
                                final CodeRegistryRepository codeRegistryRepository,
                                final CodeSchemeParser codeSchemeParser,
                                final CodeSchemeRepository codeSchemeRepository,
                                final CodeParser codeParser,
                                final CodeRepository codeRepository,
                                final ExternalReferenceRepository externalReferenceRepository) {
        this.domain = domain;
        this.indexing = indexing;
        this.apiUtils = apiUtils;
        this.codeRegistryParser = codeRegistryParser;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeParser = codeSchemeParser;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeParser = codeParser;
        this.codeRepository = codeRepository;
        this.externalReferenceRepository = externalReferenceRepository;
    }

    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses CodeRegistries from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", required = false, dataType = "file", paramType = "formData")
    })
    @Transactional
    public Response addOrUpdateCodeRegistries(@ApiParam(value = "Format for input.", required = true) @QueryParam("format") @DefaultValue("JSON") final String format,
                                              @ApiParam(value = "JSON playload for CodeRegistry data.", required = false) final String jsonPayload,
                                              @ApiParam(value = "Input-file for CSV or Excel import.", required = false, hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES);
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODEREGISTRY, null)));
        final Meta meta = new Meta();
        final ResponseWrapper<CodeRegistry> wrapper = new ResponseWrapper<>(meta);
        final ObjectMapper mapper = createObjectMapper();
        try {
            Set<CodeRegistry> codeRegistries = new HashSet<>();
            if (FORMAT_JSON.equals(format) && jsonPayload != null && !jsonPayload.isEmpty()) {
                codeRegistries = mapper.readValue(jsonPayload, new TypeReference<List<CodeRegistry>>() {
                });
            } else if (FORMAT_CSV.equals(format)) {
                codeRegistries = codeRegistryParser.parseCodeRegistriesFromCsvInputStream(inputStream);
            } else if (FORMAT_EXCEL.equals(format)) {
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
        } catch (Exception e) {
            LOG.error("Error parsing CodeRegistries.", e.getMessage());
            meta.setCode(400);
            return Response.status(Response.Status.BAD_REQUEST).entity(wrapper).build();
        }
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/")
    @Consumes({MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses CodeSchemes from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", required = false, dataType = "file", paramType = "formData")
    })
    @Transactional
    public Response addOrUpdateCodeSchemes(@ApiParam(value = "Format for input.", required = true) @QueryParam("format") @DefaultValue("JSON") final String format,
                                           @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                           @ApiParam(value = "JSON playload for CodeScheme data.", required = false) final String jsonPayload,
                                           @ApiParam(value = "Input-file for CSV or Excel import.", required = false, hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/");
        final Meta meta = new Meta();
        final ResponseWrapper<CodeScheme> responseWrapper = new ResponseWrapper<>(meta);
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODESCHEME, "codeRegistry")));
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            Set<CodeScheme> codeSchemes = new HashSet<>();
            try {
                if (FORMAT_JSON.equalsIgnoreCase(format) && jsonPayload != null && !jsonPayload.isEmpty()) {
                    final ObjectMapper mapper = createObjectMapper();
                    codeSchemes = mapper.readValue(jsonPayload, new TypeReference<List<CodeScheme>>() {
                    });
                } else if (FORMAT_CSV.equalsIgnoreCase(format)) {
                    codeSchemes = codeSchemeParser.parseCodeSchemesFromCsvInputStream(codeRegistry, inputStream);
                } else if (FORMAT_EXCEL.equalsIgnoreCase(format)) {
                    codeSchemes = codeSchemeParser.parseCodeSchemesFromExcelInputStream(codeRegistry, inputStream);
                }
            } catch (final Exception e) {
                throw new WebApplicationException(e.getMessage());
            }
            for (final CodeScheme codeScheme : codeSchemes) {
                LOG.debug("CodeScheme parsed from input: " + codeScheme.getCodeValue());
            }
            if (!codeSchemes.isEmpty()) {
                domain.persistCodeSchemes(codeSchemes);
                indexing.updateCodeSchemes(codeSchemes);
                for (final CodeScheme codeScheme : codeSchemes) {
                    indexing.updateCodes(codeRepository.findByCodeScheme(codeScheme));
                    indexing.updateExternalReferences(externalReferenceRepository.findByParentCodeScheme(codeScheme));
                }
            }
            meta.setMessage("CodeSchemes added or modified: " + codeSchemes.size());
            meta.setCode(200);
            responseWrapper.setResults(codeSchemes);
            return Response.ok(responseWrapper).build();
        }
        meta.setMessage("CodeScheme with code: " + codeRegistryCodeValue + " does not exist yet, please creater register first.");
        meta.setCode(404);
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
                                     @ApiParam(value = "JSON playload for Code data.", required = false) final String jsonPayload) throws WebApplicationException {

        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeId + "/");
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            final CodeScheme existingCodeScheme = codeSchemeRepository.findByCodeRegistryAndId(codeRegistry, UUID.fromString(codeSchemeId));
            if (existingCodeScheme != null) {
                try {
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        final ObjectMapper mapper = createObjectMapper();
                        final CodeScheme codeScheme = mapper.readValue(jsonPayload, CodeScheme.class);
                        // TODO Refactor this to use existing value as master when evaluating changes.
                        if (!codeScheme.getCodeValue().equalsIgnoreCase(existingCodeScheme.getCodeValue())) {
                            LOG.error("CodeScheme cannot be updated because codevalue changed: " + codeScheme.getCodeValue());
                            meta.setMessage("CodeScheme cannot be updated because codeValue changed. " + codeScheme.getCodeValue());
                            meta.setCode(406);
                        } else {
                            final Set<ExternalReference> externalReferences = codeScheme.getExternalReferences();
                            externalReferences.forEach(externalReference -> {
                                boolean hasChanges = false;
                                if (externalReference.getId() == null) {
                                    final UUID referenceUuid = UUID.randomUUID();
                                    externalReference.setId(referenceUuid);
                                    externalReference.setUri(apiUtils.createResourceUrl(API_PATH_EXTERNALREFERENCES, referenceUuid.toString()));
                                    hasChanges = true;
                                }
                                if (externalReference.getParentCodeScheme() == null) {
                                    externalReference.setParentCodeScheme(existingCodeScheme);
                                    hasChanges = true;
                                }
                                if (hasChanges) {
                                    externalReference.setModified(new Date(System.currentTimeMillis()));
                                }
                            });
                            if (!externalReferences.isEmpty()) {
                                externalReferenceRepository.save(externalReferences);
                                codeScheme.setExternalReferences(externalReferences);
                            } else {
                                codeScheme.setExternalReferences(null);
                            }
                            codeScheme.setModified(new Date(System.currentTimeMillis()));
                            codeSchemeRepository.save(codeScheme);
                            if (indexing.updateCodeScheme(codeScheme) && indexing.updateCodes(codeRepository.findByCodeScheme(codeScheme)) && indexing.updateExternalReferences(externalReferenceRepository.findByParentCodeScheme(codeScheme))) {
                                meta.setMessage("CodeScheme " + codeSchemeId + " modified.");
                                meta.setCode(200);
                                return Response.ok(responseWrapper).build();
                            } else {
                                meta.setMessage("CodeScheme " + codeSchemeId + " modifification failed.");
                                meta.setCode(500);
                                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseWrapper).build();
                            }
                        }
                    } else {
                        meta.setMessage("No JSON payload found.");
                        meta.setCode(406);
                    }
                } catch (Exception e) {
                    LOG.error("Exception storing CodeScheme: " + e.getMessage());
                    throw new WebApplicationException(e.getMessage());
                }
            } else {
                meta.setMessage("CodeScheme: " + codeSchemeId + " does not exist yet, please create codeScheme first.");
                meta.setCode(406);
            }
        } else {
            meta.setMessage("CodeRegistry with code: " + codeRegistryCodeValue + " does not exist yet, please create registry first.");
            meta.setCode(406);
        }
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseWrapper).build();
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeId}/codes/")
    @Consumes({MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiResponse(code = 200, message = "Returns success.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", required = false, dataType = "file", paramType = "formData")
    })
    @Transactional
    public Response addOrUpdateCodes(@ApiParam(value = "Format for input.", required = true) @QueryParam("format") @DefaultValue("JSON") final String format,
                                     @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                     @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeId") final String codeSchemeId,
                                     @ApiParam(value = "Input-file for CSV or Excel import.", required = false, hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream,
                                     @ApiParam(value = "JSON playload for Code data.", required = false) final String jsonPayload) {

        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeId + API_PATH_CODES + "/");
        final Meta meta = new Meta();
        final ResponseWrapper<Code> responseWrapper = new ResponseWrapper<>(meta);
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODE, "codeRegistry,codeScheme")));
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndId(codeRegistry, UUID.fromString(codeSchemeId));
            if (codeScheme != null) {
                Set<Code> codes = new HashSet<>();
                try {
                    if (FORMAT_JSON.equalsIgnoreCase(format) && jsonPayload != null && !jsonPayload.isEmpty()) {
                        final ObjectMapper mapper = createObjectMapper();
                        codes = mapper.readValue(jsonPayload, new TypeReference<List<Code>>() {
                        });
                    } else if (FORMAT_CSV.equalsIgnoreCase(format)) {
                        codes = codeParser.parseCodesFromCsvInputStream(codeScheme, inputStream);
                    } else if (FORMAT_EXCEL.equalsIgnoreCase(format)) {
                        codes = codeParser.parseCodesFromExcelInputStream(codeScheme, inputStream);
                    }
                } catch (Exception e) {
                    throw new WebApplicationException(e.getMessage());
                }
                for (final Code code : codes) {
                    LOG.debug("Code parsed from input: " + code.getCodeValue());
                }
                if (!codes.isEmpty()) {
                    domain.persistCodes(codes);
                    indexing.updateCodes(codes);
                }
                meta.setMessage("Codes added or modified: " + codes.size());
                meta.setCode(200);
                responseWrapper.setResults(codes);
                return Response.ok(responseWrapper).build();
            } else {
                meta.setMessage("CodeScheme with id: " + codeSchemeId + " does not exist yet, please create codeScheme first.");
                meta.setCode(406);
            }
        } else {
            meta.setMessage("CodeRegistry with id: " + codeRegistryCodeValue + " does not exist yet, please create registry first.");
            meta.setCode(406);
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
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndId(codeRegistry, UUID.fromString(codeSchemeId));
            if (codeScheme != null) {
                final Code existingCode = codeRepository.findByCodeSchemeAndId(codeScheme, UUID.fromString(codeId));
                try {
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        final ObjectMapper mapper = createObjectMapper();
                        final Code code = mapper.readValue(jsonPayload, Code.class);
                        if (!code.getCodeValue().equalsIgnoreCase(existingCode.getCodeValue())) {
                            LOG.error("Code cannot be updated because CodeValue changed: " + code.getCodeValue());
                            meta.setMessage("Code cannot be updated because CodeValue changed. " + code.getCodeValue());
                            meta.setCode(406);
                        } else {
                            code.setModified(new Date(System.currentTimeMillis()));
                            codeRepository.save(code);
                            final boolean success = indexing.updateCode(code);
                            if (success) {
                                meta.setMessage("Code " + codeId + " modified.");
                                meta.setCode(200);
                                return Response.ok(responseWrapper).build();
                            } else {
                                meta.setMessage("Code " + codeId + " modifification failed.");
                                meta.setCode(500);
                                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(responseWrapper).build();
                            }
                        }
                    } else {
                        meta.setMessage("No JSON payload found.");
                        meta.setCode(406);
                    }
                } catch (Exception e) {
                    throw new WebApplicationException(e.getMessage());
                }
            } else {
                meta.setMessage("CodeScheme with id: " + codeSchemeId + " does not exist yet, please create codeScheme first.");
                meta.setCode(406);
            }
        } else {
            meta.setMessage("CodeRegistry with CodeValue: " + codeRegistryCodeValue + " does not exist yet, please create registry first.");
            meta.setCode(406);
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
                    meta.setCode(404);
                }
            } else {
                meta.setMessage("CodeScheme with id: " + codeSchemeId + " not found!");
                meta.setCode(404);
            }
        } else {
            meta.setMessage("CodeRegistry with codeValue: " + codeRegistryCodeValue + " not found!");
            meta.setCode(404);
        }
        return Response.status(Response.Status.NOT_FOUND).entity(responseWrapper).build();
    }
}
