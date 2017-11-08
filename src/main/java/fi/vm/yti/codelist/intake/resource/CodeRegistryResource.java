package fi.vm.yti.codelist.intake.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.Meta;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.domain.Domain;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
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

/**
 * Content Intake Service: REST resources for CodeRegistry, CodeScheme and Code entity management.
 */
@Component
@Path("/v1/coderegistries")
@Api(value = "coderegistries", description = "Operations for creating, deleting and updating coderegistries, codeschemes and codes.")
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

    @Inject
    public CodeRegistryResource(final Domain domain,
                                final Indexing indexing,
                                final CodeRegistryParser codeRegistryParser,
                                final CodeRegistryRepository codeRegistryRepository,
                                final CodeSchemeParser codeSchemeParser,
                                final CodeSchemeRepository codeSchemeRepository,
                                final CodeParser codeParser,
                                final CodeRepository codeRepository) {
        this.domain = domain;
        this.indexing = indexing;
        this.codeRegistryParser = codeRegistryParser;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeParser = codeSchemeParser;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeParser = codeParser;
        this.codeRepository = codeRepository;
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
    public Response addOrUpdateCodeRegistries(@ApiParam(value = "JSON playload for CodeRegistry data.", required = false) final String jsonPayload,
                                              @ApiParam(value = "Input-file for CSV or Excel import.", required = false, hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES);
        final Meta meta = new Meta();
        final MetaResponseWrapper wrapper = new MetaResponseWrapper(meta);
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        try {
            final List<CodeRegistry> codeRegistries;
            if (jsonPayload != null && !jsonPayload.isEmpty()) {
                codeRegistries = mapper.readValue(jsonPayload, new TypeReference<List<CodeRegistry>>() {
                });
            } else {
                codeRegistries = codeRegistryParser.parseCodeRegistriesFromCsvInputStream(inputStream);
            }
            for (final CodeRegistry register : codeRegistries) {
                LOG.debug("CodeRegistry parsed from input: " + register.getCodeValue());
            }
            if (!codeRegistries.isEmpty()) {
                domain.persistCodeRegistries(codeRegistries);
                indexing.reIndexEverything();
            }
            meta.setMessage("CodeRegistries added or modified: " + codeRegistries.size());
            meta.setCode(200);
            return Response.ok(wrapper).build();
        } catch (final IOException e) {
            LOG.error("Error parsing CodeRegistries from JSON.", e.getMessage());
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
    public Response addOrUpdateCodeSchemes(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                           @ApiParam(value = "JSON playload for CodeScheme data.", required = false) final String jsonPayload,
                                           @ApiParam(value = "Input-file for CSV or Excel import.", required = false, hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/");
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            final List<CodeScheme> codeSchemes;
            try {
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    final ObjectMapper mapper = new ObjectMapper();
                    mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
                    codeSchemes = mapper.readValue(jsonPayload, new TypeReference<List<CodeScheme>>() {
                    });
                } else {
                    codeSchemes = codeSchemeParser.parseCodeSchemesFromCsvInputStream(codeRegistry, inputStream);
                }
            } catch (final Exception e) {
                throw new WebApplicationException(e.getMessage());
            }
            for (final CodeScheme codeScheme : codeSchemes) {
                LOG.debug("CodeScheme parsed from input: " + codeScheme.getCodeValue());
            }
            if (!codeSchemes.isEmpty()) {
                domain.persistCodeSchemes(codeSchemes);
                indexing.reIndexEverything();
            }
            meta.setMessage("CodeSchemes added or modified: " + codeSchemes.size());
            meta.setCode(200);
            return Response.ok(responseWrapper).build();
        }
        meta.setMessage("CodeScheme with code: " + codeRegistryCodeValue + " does not exist yet, please creater register first.");
        meta.setCode(404);
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseWrapper).build();
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Modifies single existing CodeScheme.")
    @ApiResponse(code = 200, message = "Returns success.")
    @Transactional
    public Response updateCodeScheme(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                     @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                     @ApiParam(value = "JSON playload for Code data.", required = false) final String jsonPayload) {

        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeCodeValue + "/");
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            final UUID uuid = UUID.fromString(codeSchemeCodeValue);
            final CodeScheme existingCodeScheme = codeSchemeRepository.findByCodeRegistryAndId(codeRegistry, uuid);
            if (existingCodeScheme != null) {
                try {
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        final ObjectMapper mapper = new ObjectMapper();
                        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
                        final CodeScheme codeScheme = mapper.readValue(jsonPayload, CodeScheme.class);
                        if (!codeScheme.getCodeValue().equalsIgnoreCase(existingCodeScheme.getCodeValue())) {
                            LOG.error("CodeScheme cannot be updated because codevalue changed: " + codeScheme.getCodeValue());
                            meta.setMessage("CodeScheme cannot be updated because codeValue changed. " + codeScheme.getCodeValue());
                            meta.setCode(406);
                        } else {
                            codeScheme.setModified(new Date(System.currentTimeMillis()));
                            codeSchemeRepository.save(codeScheme);
                            indexing.reIndexEverything();
                            meta.setMessage("CodeScheme " + codeSchemeCodeValue + " modified.");
                            meta.setCode(200);
                            return Response.ok(responseWrapper).build();
                        }
                    } else {
                        meta.setMessage("No JSON payload found.");
                        meta.setCode(406);
                    }
                } catch (Exception e) {
                    throw new WebApplicationException(e.getMessage());
                }
            } else {
                meta.setMessage("CodeScheme with code: " + codeSchemeCodeValue + " does not exist yet, please create codeScheme first.");
                meta.setCode(406);
            }
        } else {
            meta.setMessage("CodeRegistry with code: " + codeRegistryCodeValue + " does not exist yet, please create registry first.");
            meta.setCode(406);
        }
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseWrapper).build();
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/codes/")
    @Consumes({MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiResponse(code = 200, message = "Returns success.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", required = false, dataType = "file", paramType = "formData")
    })
    @Transactional
    public Response addOrUpdateCodes(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                     @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                     @ApiParam(value = "Input-file for CSV or Excel import.", required = false, hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream,
                                     @ApiParam(value = "JSON playload for Code data.", required = false) final String jsonPayload) {

        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeCodeValue + API_PATH_CODES + "/");
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValue(codeRegistry, codeSchemeCodeValue);
            if (codeScheme != null) {
                final List<Code> codes;
                try {
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        final ObjectMapper mapper = new ObjectMapper();
                        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
                        codes = mapper.readValue(jsonPayload, new TypeReference<List<Code>>() {
                        });
                    } else {
                        codes = codeParser.parseCodesFromCsvInputStream(codeScheme, inputStream);
                    }
                } catch (Exception e) {
                    throw new WebApplicationException(e.getMessage());
                }
                for (final Code code : codes) {
                    LOG.debug("Code parsed from input: " + code.getCodeValue());
                }
                if (!codes.isEmpty()) {
                    domain.persistCodes(codes);
                    indexing.reIndexEverything();
                }
                meta.setMessage("Codes added or modified: " + codes.size());
                meta.setCode(200);
                return Response.ok(responseWrapper).build();
            } else {
                meta.setMessage("CodeScheme with code: " + codeSchemeCodeValue + " does not exist yet, please create codeScheme first.");
                meta.setCode(406);
            }
        } else {
            meta.setMessage("CodeRegistry with code: " + codeRegistryCodeValue + " does not exist yet, please create registry first.");
            meta.setCode(406);
        }
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseWrapper).build();
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/codes/{codeCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Modifies single existing Code.")
    @ApiResponse(code = 200, message = "Returns success.")
    @Transactional
    public Response updateCode(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                               @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                               @ApiParam(value = "Code codeValue.", required = true) @PathParam("codeCodeValue") final String codeCodeValue,
                               @ApiParam(value = "JSON playload for Code data.", required = false) final String jsonPayload) {

        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeCodeValue + API_PATH_CODES + "/" + codeCodeValue + "/");
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValue(codeRegistry, codeSchemeCodeValue);
            if (codeScheme != null) {
                final UUID uuid = UUID.fromString(codeCodeValue);
                final Code existingCode = codeRepository.findByCodeSchemeAndId(codeScheme, uuid);
                try {
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        final ObjectMapper mapper = new ObjectMapper();
                        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
                        final Code code = mapper.readValue(jsonPayload, Code.class);
                        if (!code.getCodeValue().equalsIgnoreCase(existingCode.getCodeValue())) {
                            LOG.error("Code cannot be updated because CodeValue changed: " + code.getCodeValue());
                            meta.setMessage("Code cannot be updated because CodeValue changed. " + code.getCodeValue());
                            meta.setCode(406);
                        } else {
                            code.setModified(new Date(System.currentTimeMillis()));
                            codeRepository.save(code);
                            indexing.reIndexEverything();
                            meta.setMessage("Code " + codeCodeValue + " modified.");
                            meta.setCode(200);
                            return Response.ok(responseWrapper).build();
                        }
                    } else {
                        meta.setMessage("No JSON payload found.");
                        meta.setCode(406);
                    }
                } catch (Exception e) {
                    throw new WebApplicationException(e.getMessage());
                }
            } else {
                meta.setMessage("CodeScheme with CodeValue: " + codeSchemeCodeValue + " does not exist yet, please create codeScheme first.");
                meta.setCode(406);
            }
        } else {
            meta.setMessage("CodeRegistry with CodeValue: " + codeRegistryCodeValue + " does not exist yet, please create registry first.");
            meta.setCode(406);
        }
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseWrapper).build();
    }

    @DELETE
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/codes/{codeCodeValue}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single code. This means that the item status is set to Status.RETIRED.")
    @ApiResponse(code = 200, message = "Returns success.")
    @Transactional
    public Response retireCode(@ApiParam(value = "CodeRegistry codeValue.", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                               @ApiParam(value = "CodeScheme codeValue.", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                               @ApiParam(value = "Code codeValue.", required = true) @PathParam("codeCodeValue") final String codeCodeValue) {
        logApiRequest(LOG, METHOD_DELETE, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeCodeValue + API_PATH_CODES + "/" + codeCodeValue + "/");
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValue(codeRegistry, codeSchemeCodeValue);
            if (codeScheme != null) {
                final Code code = codeRepository.findByCodeSchemeAndCodeValue(codeScheme, codeCodeValue);
                if (code != null) {
                    code.setStatus(Status.RETIRED.toString());
                    codeRepository.save(code);
                    indexing.reIndexEverything();
                    meta.setMessage("Code marked as RETIRED!");
                    meta.setCode(200);
                    return Response.ok(responseWrapper).build();
                } else {
                    meta.setMessage("Code " + codeCodeValue + " not found!");
                    meta.setCode(404);
                }
            } else {
                meta.setMessage("CodeScheme " + codeSchemeCodeValue + " not found!");
                meta.setCode(404);
            }
        } else {
            meta.setMessage("CodeRegistry " + codeRegistryCodeValue + " not found!");
            meta.setCode(404);
        }
        return Response.status(Response.Status.NOT_FOUND).entity(responseWrapper).build();
    }
}
