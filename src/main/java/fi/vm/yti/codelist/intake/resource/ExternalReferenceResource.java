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
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;

import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.common.dto.Meta;
import fi.vm.yti.codelist.intake.service.ExternalReferenceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_CODEREGISTRY;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_JSON;

@Component
@Path("/v1/externalreferences")
@Api(value = "externalreferences")
@Produces(MediaType.APPLICATION_JSON)
public class ExternalReferenceResource implements AbstractBaseResource {

    private final ExternalReferenceService externalReferenceService;
    private final Indexing indexing;

    @Inject
    public ExternalReferenceResource(final ExternalReferenceService externalReferenceService,
                                     final Indexing indexing) {
        this.externalReferenceService = externalReferenceService;
        this.indexing = indexing;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses ExternalReferences from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdateExternalReferencesFromJson(@ApiParam(value = "JSON playload for ExternalReference data.") final String jsonPayload,
                                                          @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
        return parseAndPersistExistingReferencesFromSource(FORMAT_JSON, null, jsonPayload, pretty);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses ExternalReferences from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", dataType = "file", paramType = "formData")
    })
    public Response addOrUpdateExternalReferencesFromFile(@ApiParam(value = "Format for input.", required = true) @QueryParam("format") @DefaultValue("json") final String format,
                                                          @ApiParam(value = "Input-file for CSV or Excel import.", hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream,
                                                          @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
        return parseAndPersistExistingReferencesFromSource(format, inputStream, null, pretty);
    }

    @POST
    @Path("{externalReferenceId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses ExternalReference from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response updateExternalReference(@ApiParam(value = "ExternalReference ID", required = true) @PathParam("externalReferenceId") final String externalReferenceId,
                                            @ApiParam(value = "JSON playload for ExternalReference data.") final String jsonPayload) {
        final ExternalReferenceDTO externalReference = externalReferenceService.parseAndPersistExternalReferenceFromJson(externalReferenceId, jsonPayload, null);
        indexing.updateExternalReference(externalReference);
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    private Response parseAndPersistExistingReferencesFromSource(final String format,
                                                                 final InputStream inputStream,
                                                                 final String jsonPayload,
                                                                 final String pretty) {
        final Set<ExternalReferenceDTO> externalReferences = externalReferenceService.parseAndPersistExternalReferencesFromSourceData(format, inputStream, jsonPayload, null);
        indexing.updateExternalReferences(externalReferences);
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODEREGISTRY, null), pretty));
        final ResponseWrapper<ExternalReferenceDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("ExternalReferences added or modified: " + externalReferences.size());
        meta.setCode(200);
        responseWrapper.setResults(externalReferences);
        return Response.ok(responseWrapper).build();
    }

}
