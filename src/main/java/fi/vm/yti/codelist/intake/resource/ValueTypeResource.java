package fi.vm.yti.codelist.intake.resource;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

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
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.Meta;
import fi.vm.yti.codelist.common.dto.ValueTypeDTO;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.service.ValueTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_CODEREGISTRY;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_JSON;

@Component
@Path("/v1/valuetypes")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "ValueType")
public class ValueTypeResource implements AbstractBaseResource {

    private final ValueTypeService valueTypeService;
    private final Indexing indexing;

    @Inject
    public ValueTypeResource(final ValueTypeService valueTypeService,
                             final Indexing indexing) {
        this.valueTypeService = valueTypeService;
        this.indexing = indexing;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses ValueTypes from input data.")
    @ApiResponse(responseCode = "200", description = "ValueTypes added or modified successfully.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ValueTypeDTO.class))))
    public Response addOrUpdateValueTypesFromJson(@Parameter(description = "Format for input.", required = true, in = ParameterIn.QUERY) @QueryParam("format") @DefaultValue("json") final String format,
                                                  @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty,
                                                  @RequestBody(description = "JSON payload for ValueType data.", required = true) final String jsonPayload) {
        return parseAndPersistValueTypesFromSource(FORMAT_JSON, null, jsonPayload, pretty);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses ValueTypes from input data.")
    @ApiResponse(responseCode = "200", description = "ValueTypes added or modified successfully.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ValueTypeDTO.class))))
    public Response addOrUpdateValueTypesFromFile(@Parameter(description = "Format for input.", required = true, in = ParameterIn.QUERY) @QueryParam("format") @DefaultValue("json") final String format,
                                                  @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty,
                                                  @Parameter(description = "Input-file for CSV or Excel import.", required = true, in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "binary", description = "Incoming file.")) @FormDataParam("file") final InputStream inputStream) {
        return parseAndPersistValueTypesFromSource(format, inputStream, null, pretty);
    }

    @POST
    @Path("{ValueTypeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses ValueType from input data.")
    @ApiResponse(responseCode = "200", description = "Returns success.")
    public Response updateValueType(@Parameter(description = "ValueType ID", required = true, in = ParameterIn.PATH) @PathParam("ValueTypeId") final String valueTypeId,
                                    @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty,
                                    @RequestBody(description = "JSON payload for ValueType data.", required = true) final String jsonPayload) {
        final UUID uuid = UUID.fromString(valueTypeId);
        final ValueTypeDTO valueType = valueTypeService.parseAndPersistValueTypeFromJson(uuid, jsonPayload);
        indexing.updateValueType(valueType);
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    private Response parseAndPersistValueTypesFromSource(final String format,
                                                         final InputStream inputStream,
                                                         final String jsonPayload,
                                                         final String pretty) {
        final Set<ValueTypeDTO> valueTypes = valueTypeService.parseAndPersistValueTypesFromSourceData(format, inputStream, jsonPayload);
        indexing.updateValueTypes(valueTypes);
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODEREGISTRY, null), pretty));
        final ResponseWrapper<ValueTypeDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("ValueTypes added or modified: " + valueTypes.size());
        meta.setCode(200);
        responseWrapper.setResults(valueTypes);
        return Response.ok(responseWrapper).build();
    }
}
