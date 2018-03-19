package fi.vm.yti.codelist.intake.resource;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.model.Meta;
import fi.vm.yti.codelist.common.model.Views;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.service.CodeRegistryService;
import fi.vm.yti.codelist.intake.service.CodeSchemeService;
import fi.vm.yti.codelist.intake.service.CodeService;
import fi.vm.yti.codelist.intake.service.ExternalReferenceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

@Component
@Path("/v1/coderegistries")
@Api(value = "coderegistries")
@Produces("text/plain")
public class CodeRegistryResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(CodeRegistryResource.class);
    private final CodeService codeService;
    private final CodeSchemeService codeSchemeService;
    private final CodeRegistryService codeRegistryService;
    private final ExternalReferenceService externalReferenceService;
    private final Indexing indexing;

    @Inject
    public CodeRegistryResource(final CodeService codeService,
                                final CodeSchemeService codeSchemeService,
                                final CodeRegistryService codeRegistryService,
                                final ExternalReferenceService externalReferenceService,
                                final Indexing indexing) {
        this.codeService = codeService;
        this.codeSchemeService = codeSchemeService;
        this.codeRegistryService = codeRegistryService;
        this.externalReferenceService = externalReferenceService;
        this.indexing = indexing;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses CodeRegistries from JSON input.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdateCodeRegistriesFromJson(@ApiParam(value = "JSON playload for CodeRegistry data.", required = true) final String jsonPayload) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES);
        return parseAndPersistCodeRegistriesFromSource(FORMAT_JSON, null, jsonPayload);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses CodeRegistries from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", required = false, dataType = "file", paramType = "formData")
    })
    public Response addOrUpdateCodeRegistriesFromFile(@ApiParam(value = "Format for input.", required = true) @QueryParam("format") @DefaultValue("json") final String format,
                                                      @ApiParam(value = "Input-file for CSV or Excel import.", required = false, hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES);
        return parseAndPersistCodeRegistriesFromSource(format, inputStream, null);
    }

    @POST
    @Path("{codeRegistryCodeValue}/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Modifies single existing CodeScheme.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response updateCodeScheme(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                     @ApiParam(value = "JSON playload for Code data.", required = false) final String jsonPayload) {

        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + "/");
        final CodeRegistryDTO codeRegistry = codeRegistryService.parseAndPersistCodeRegistryFromJson(codeRegistryCodeValue, jsonPayload);
        indexing.updateCodeRegistry(codeRegistry);
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses CodeSchemes from JSON input.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdateCodeSchemesFromJson(@ApiParam(value = "Format for input.", required = false) @QueryParam("format") @DefaultValue("json") final String format,
                                                   @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                   @ApiParam(value = "JSON playload for CodeScheme data.", required = true) final String jsonPayload) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/");
        return parseAndPersistCodeSchemesFromSource(codeRegistryCodeValue, FORMAT_JSON, null, jsonPayload);
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
    public Response addOrUpdateCodeSchemesFromFile(@ApiParam(value = "Format for input.", required = false) @QueryParam("format") @DefaultValue("csv") final String format,
                                                   @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                   @ApiParam(value = "Input-file for CSV or Excel import.", required = false, hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/");
        return parseAndPersistCodeSchemesFromSource(codeRegistryCodeValue, format, inputStream, null);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Modifies single existing CodeScheme.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response updateCodeScheme(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                     @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                     @ApiParam(value = "JSON playload for Code data.", required = false) final String jsonPayload) {

        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeCodeValue + "/");
        final CodeSchemeDTO codeScheme = codeSchemeService.parseAndPersistCodeSchemeFromJson(codeRegistryCodeValue, codeSchemeCodeValue, jsonPayload);
        indexing.updateCodeScheme(codeScheme);
        indexing.updateCodes(codeService.findByCodeSchemeId(codeScheme.getId()));
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    @DELETE
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single existing CodeScheme.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "CodeScheme deleted"),
        @ApiResponse(code = 404, message = "CodeScheme not found")
    })

    public Response deleteCodeScheme(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                     @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue) {

        logApiRequest(LOG, METHOD_DELETE, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeCodeValue + "/");
        final CodeSchemeDTO existingCodeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (existingCodeScheme != null) {
            final UUID codeSchemeId = existingCodeScheme.getId();
            final Set<CodeDTO> codes = codeService.findByCodeSchemeId(codeSchemeId);
            final Set<ExternalReferenceDTO> externalReferences = externalReferenceService.findByCodeSchemeId(codeSchemeId);
            final CodeSchemeDTO codeScheme = codeSchemeService.deleteCodeScheme(codeRegistryCodeValue, codeSchemeCodeValue);
            indexing.deleteCodeScheme(codeScheme);
            indexing.deleteCodes(codes);
            indexing.deleteExternalReferences(externalReferences);
        } else {
            return Response.status(404).build();
        }
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/codes/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses Codes from JSON input.")
    @ApiResponse(code = 200, message = "Returns success.")
    @JsonView({Views.ExtendedCode.class, Views.Normal.class})
    public Response addOrUpdateCodesFromJson(@ApiParam(value = "Format for input.", required = true) @QueryParam("format") @DefaultValue("json") final String format,
                                             @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                             @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                             @ApiParam(value = "JSON playload for Code data.", required = true) final String jsonPayload) {

        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeCodeValue + API_PATH_CODES + "/");
        return parseAndPersistCodesFromSource(codeRegistryCodeValue, codeSchemeCodeValue, format, null, jsonPayload);

    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/codes/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses Codes from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", required = false, dataType = "file", paramType = "formData")
    })
    @JsonView({Views.ExtendedCode.class, Views.Normal.class})
    public Response addOrUpdateCodesFromFile(@ApiParam(value = "Format for input.", required = true) @QueryParam("format") @DefaultValue("csv") final String format,
                                             @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                             @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                             @ApiParam(value = "Input-file for CSV or Excel import.", required = false, hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream) {

        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeCodeValue + API_PATH_CODES + "/");
        return parseAndPersistCodesFromSource(codeRegistryCodeValue, codeSchemeCodeValue, format, inputStream, null);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/codes/{codeCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Modifies a single existing Code.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response updateCode(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                               @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                               @ApiParam(value = "Code codeValue.", required = true) @PathParam("codeCodeValue") final String codeCodeValue,
                               @ApiParam(value = "JSON playload for Code data.", required = false) final String jsonPayload) {

        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeCodeValue + API_PATH_CODES + "/" + codeCodeValue + "/");
        final CodeDTO code = codeService.parseAndPersistCodeFromJson(codeRegistryCodeValue, codeSchemeCodeValue, codeCodeValue, jsonPayload);
        indexing.updateCode(code);
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    @DELETE
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/codes/{codeCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single existing Code.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Code deleted"),
        @ApiResponse(code = 404, message = "Code not found")
    })
    public Response deleteCode(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                               @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                               @ApiParam(value = "Code codeValue.", required = true) @PathParam("codeCodeValue") final String codeCodeValue) {

        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeCodeValue + API_PATH_CODES + "/" + codeCodeValue + "/");
        final CodeDTO code = codeService.deleteCode(codeRegistryCodeValue, codeSchemeCodeValue, codeCodeValue);
        if (code != null) {
            indexing.deleteCode(code);
            final Meta meta = new Meta();
            final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
            return Response.ok(responseWrapper).build();
        } else {
            return Response.status(404).build();
        }
    }

    private Response parseAndPersistCodeRegistriesFromSource(final String format,
                                                             final InputStream inputStream,
                                                             final String jsonPayload) {
        final Set<CodeRegistryDTO> codeRegistries = codeRegistryService.parseAndPersistCodeRegistriesFromSourceData(format, inputStream, jsonPayload);
        indexing.updateCodeRegistries(codeRegistries);
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODEREGISTRY, null)));
        final ResponseWrapper<CodeRegistryDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("CodeRegistries added or modified: " + codeRegistries.size());
        meta.setCode(200);
        responseWrapper.setResults(codeRegistries);
        return Response.ok(responseWrapper).build();
    }

    private Response parseAndPersistCodeSchemesFromSource(final String codeRegistryCodeValue,
                                                          final String format,
                                                          final InputStream inputStream,
                                                          final String jsonPayload) {
        final Set<CodeSchemeDTO> codeSchemes = codeSchemeService.parseAndPersistCodeSchemesFromSourceData(codeRegistryCodeValue, format, inputStream, jsonPayload);
        indexing.updateCodeSchemes(codeSchemes);
        indexing.updateCodeRegistry(codeRegistryService.findByCodeValue(codeRegistryCodeValue));
        for (final CodeSchemeDTO codeScheme : codeSchemes) {
            indexing.updateCodes(codeService.findByCodeSchemeId(codeScheme.getId()));
            indexing.updateExternalReferences(externalReferenceService.findByCodeSchemeId(codeScheme.getId()));
        }
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODESCHEME, "codeRegistry")));
        final ResponseWrapper<CodeSchemeDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("CodeSchemes added or modified: " + codeSchemes.size());
        meta.setCode(200);
        responseWrapper.setResults(codeSchemes);
        return Response.ok(responseWrapper).build();
    }

    private Response parseAndPersistCodesFromSource(final String codeRegistryCodeValue,
                                                    final String codeSchemeCodeValue,
                                                    final String format,
                                                    final InputStream inputStream,
                                                    final String jsonPayload) {
        final Set<CodeDTO> codes = codeService.parseAndPersistCodesFromSourceData(codeRegistryCodeValue, codeSchemeCodeValue, format, inputStream, jsonPayload);
        indexing.updateCodes(codes);
        final CodeSchemeDTO codeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        indexing.updateCodeScheme(codeScheme);
        indexing.updateExternalReferences(externalReferenceService.findByCodeSchemeId(codeScheme.getId()));
        indexing.updateCodeRegistry(codeRegistryService.findByCodeValue(codeRegistryCodeValue));
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODE, "codeRegistry,codeScheme")));
        final ResponseWrapper<CodeDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("Codes added or modified: " + codes.size());
        meta.setCode(200);
        responseWrapper.setResults(codes);
        return Response.ok(responseWrapper).build();
    }

    @HEAD
    @Path("{codeRegistryCodeValue}")
    @ApiOperation(value = "Check if a code registry with a given code value exists")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Found"),
        @ApiResponse(code = 404, message = "Not found")
    })
    public Response checkForExistingCodeRegistry(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue) {
        final CodeRegistryDTO registry = this.codeRegistryService.findByCodeValue(codeRegistryCodeValue);
        if (registry == null) {
            return Response.status(404).build();
        }
        return Response.status(200).build();
    }

    @HEAD
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}")
    @ApiOperation(value = "Check if a code scheme with a given code value exists")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Found"),
        @ApiResponse(code = 404, message = "Not found")
    })
    public Response checkForExistingCodeScheme(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                               @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue) {
        final CodeSchemeDTO scheme = this.codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (scheme == null) {
            return Response.status(404).build();
        }
        return Response.status(200).build();
    }

    @HEAD
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/codes/{codeCodeValue}")
    @ApiOperation(value = "Check if a code with a given code value exists")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Found"),
        @ApiResponse(code = 404, message = "Not found")
    })
    public Response checkForExistingCodeValue(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                              @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                              @ApiParam(value = "Code codeValue.", required = true) @PathParam("codeCodeValue") final String codeCodeValue) {
        final CodeDTO code = this.codeService.findByCodeRegistryCodeValueAndCodeSchemeCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue, codeCodeValue);
        if (code == null) {
            return Response.status(404).build();
        }
        return Response.status(200).build();
    }
}
