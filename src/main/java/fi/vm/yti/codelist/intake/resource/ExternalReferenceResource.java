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

import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;

import fi.vm.yti.codelist.common.model.ExternalReference;
import fi.vm.yti.codelist.common.model.Meta;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.service.ExternalReferenceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

@Component
@Path("/v1/externalreferences")
@Api(value = "externalreferences")
@Produces(MediaType.APPLICATION_JSON)
public class ExternalReferenceResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalReferenceResource.class);

    private final ExternalReferenceService externalReferenceService;

    @Inject
    public ExternalReferenceResource(final ExternalReferenceService externalReferenceService) {
        this.externalReferenceService = externalReferenceService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses ExternalReferences from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdateExternalReferencesFromJson(@ApiParam(value = "JSON playload for ExternalReference data.", required = false) final String jsonPayload) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_EXTERNALREFERENCES + "/");
        return parseAndPersistExistingReferencesFromSource(FORMAT_JSON, null, jsonPayload);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses ExternalReferences from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", required = false, dataType = "file", paramType = "formData")
    })
    public Response addOrUpdateExternalReferencesFromFile(@ApiParam(value = "Format for input.", required = true) @QueryParam("format") @DefaultValue("json") final String format,
                                                          @ApiParam(value = "Input-file for CSV or Excel import.", required = false, hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_EXTERNALREFERENCES + "/");
        return parseAndPersistExistingReferencesFromSource(FORMAT_JSON, inputStream, null);
    }

    @POST
    @Path("{externalReferenceId}/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses ExternalReference from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response updateExternalReference(@ApiParam(value = "ExternalReference ID", required = true) @PathParam("externalReferenceId") final String externalReferenceId,
                                            @ApiParam(value = "JSON playload for ExternalReference data.", required = false) final String jsonPayload) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_EXTERNALREFERENCES + "/" + externalReferenceId + "/");
        final ExternalReference externalReference = externalReferenceService.parseAndPersistExternalReferenceFromJson(externalReferenceId, jsonPayload, null);
        externalReferenceService.indexExternalReference(externalReference);
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    private Response parseAndPersistExistingReferencesFromSource(final String format,
                                                                 final InputStream inputStream,
                                                                 final String jsonPayload) {
        final Set<ExternalReference> externalReferences = externalReferenceService.parseAndPersistExternalReferencesFromSourceData(format, inputStream, jsonPayload, null);
        externalReferenceService.indexExternalReferences(externalReferences);
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODEREGISTRY, null)));
        final ResponseWrapper<ExternalReference> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("ExternalReferences added or modified: " + externalReferences.size());
        meta.setCode(200);
        responseWrapper.setResults(externalReferences);
        return Response.ok(responseWrapper).build();
    }

}
