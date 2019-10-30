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

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg.ObjectWriterInjector;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.Meta;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.service.ExternalReferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_406;

@Component
@Path("/v1/externalreferences")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "ExternalReference")
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
    @Operation(summary = "Parses ExternalReferences from input data.")
    @ApiResponse(responseCode = "200", description = "Returns success.")
    public Response addOrUpdateExternalReferences(@Parameter(description = "Format for input.", required = true, in = ParameterIn.QUERY) @QueryParam("format") @DefaultValue("json") final String format,
                                                  @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty,
                                                  @Parameter(description = "Input-file for CSV or Excel import.", in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "binary", description = "Incoming file.")) @FormDataParam("file") final InputStream inputStream,
                                                  @RequestBody(description = "JSON payload for ExternalReference data.") final String jsonPayload) {
        if (jsonPayload != null && !jsonPayload.isEmpty() && FORMAT_JSON.equalsIgnoreCase(format)) {
            return parseAndPersistExistingReferencesFromSource(FORMAT_JSON, null, jsonPayload, pretty);
        } else if (inputStream != null && (FORMAT_EXCEL.equalsIgnoreCase(format) || FORMAT_EXCEL_XLS.equalsIgnoreCase(format) || FORMAT_EXCEL_XLSX.equalsIgnoreCase(format) || FORMAT_CSV.equalsIgnoreCase(format))) {
            return parseAndPersistExistingReferencesFromSource(format, inputStream, null, pretty);
        }
        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
    }

    @POST
    @Path("{externalReferenceId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses ExternalReference from input data.")
    @ApiResponse(responseCode = "200", description = "Returns success.")
    public Response updateExternalReference(@Parameter(description = "ExternalReference ID", required = true, in = ParameterIn.PATH) @PathParam("externalReferenceId") final String externalReferenceId,
                                            @RequestBody(description = "JSON payload for ExternalReference data.") final String jsonPayload) {
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
        ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODEREGISTRY, null), pretty));
        final ResponseWrapper<ExternalReferenceDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("ExternalReferences added or modified: " + externalReferences.size());
        meta.setCode(200);
        responseWrapper.setResults(externalReferences);
        return Response.ok(responseWrapper).build();
    }

}
