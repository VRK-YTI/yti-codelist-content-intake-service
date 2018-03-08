package fi.vm.yti.codelist.intake.resource;

import java.io.InputStream;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
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

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.Meta;
import fi.vm.yti.codelist.common.model.Views;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.service.CodeRegistryService;
import fi.vm.yti.codelist.intake.service.CodeSchemeService;
import fi.vm.yti.codelist.intake.service.CodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
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

    @Inject
    public CodeRegistryResource(final CodeService codeService,
                                final CodeSchemeService codeSchemeService,
                                final CodeRegistryService codeRegistryService) {
        this.codeService = codeService;
        this.codeSchemeService = codeSchemeService;
        this.codeRegistryService = codeRegistryService;
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
        final CodeRegistry codeRegistry = codeRegistryService.parseAndPersistCodeRegistryFromJson(codeRegistryCodeValue, jsonPayload);
        codeRegistryService.indexCodeRegistry(codeRegistry);
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
        final CodeScheme codeScheme = codeSchemeService.parseAndPersistCodeSchemeFromJson(codeRegistryCodeValue, codeSchemeCodeValue, jsonPayload);
        codeSchemeService.indexCodeScheme(codeScheme);
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
    @ApiOperation(value = "Modifies single existing Code.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response updateCode(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                               @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                               @ApiParam(value = "Code codeValue.", required = true) @PathParam("codeCodeValue") final String codeCodeValue,
                               @ApiParam(value = "JSON playload for Code data.", required = false) final String jsonPayload) {

        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeCodeValue + API_PATH_CODES + "/" + codeCodeValue + "/");
        final Code code = codeService.parseAndPersistCodeFromJson(codeRegistryCodeValue, codeSchemeCodeValue, codeCodeValue, jsonPayload);
        codeService.indexCode(code);
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    private Response parseAndPersistCodeRegistriesFromSource(final String format,
                                                             final InputStream inputStream,
                                                             final String jsonPayload) {
        final Set<CodeRegistry> codeRegistries = codeRegistryService.parseAndPersistCodeRegistriesFromSourceData(format, inputStream, jsonPayload);
        codeRegistryService.indexCodeRegistries(codeRegistries);
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODEREGISTRY, null)));
        final ResponseWrapper<CodeRegistry> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("CodeRegistries added or modified: " + codeRegistries.size());
        meta.setCode(200);
        responseWrapper.setResults(codeRegistries);
        return Response.ok(responseWrapper).build();
    }

    private Response parseAndPersistCodeSchemesFromSource(final String codeRegistryCodeValue,
                                                          final String format,
                                                          final InputStream inputStream,
                                                          final String jsonPayload) {
        final Set<CodeScheme> codeSchemes = codeSchemeService.parseAndPersistCodeSchemesFromSourceData(codeRegistryCodeValue, format, inputStream, jsonPayload);
        codeSchemeService.indexCodeSchemes(codeSchemes);
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODESCHEME, "codeRegistry")));
        final ResponseWrapper<CodeScheme> responseWrapper = new ResponseWrapper<>(meta);
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
        final Set<Code> codes = codeService.parseAndPersistCodesFromSourceData(codeRegistryCodeValue, codeSchemeCodeValue, format, inputStream, jsonPayload);
        codeService.indexCodes(codes);
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODE, "codeRegistry,codeScheme")));
        final ResponseWrapper<Code> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("Codes added or modified: " + codes.size());
        meta.setCode(200);
        responseWrapper.setResults(codes);
        return Response.ok(responseWrapper).build();
    }
}
