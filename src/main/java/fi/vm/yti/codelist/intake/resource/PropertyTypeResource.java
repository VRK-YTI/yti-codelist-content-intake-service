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
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.Meta;
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.service.PropertyTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_CODEREGISTRY;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_JSON;

@Component
@Path("/v1/propertytypes")
@Produces(MediaType.APPLICATION_JSON)
public class PropertyTypeResource implements AbstractBaseResource {

    private final PropertyTypeService propertyTypeService;
    private final Indexing indexing;

    @Inject
    public PropertyTypeResource(final PropertyTypeService propertyTypeService,
                                final Indexing indexing) {
        this.propertyTypeService = propertyTypeService;
        this.indexing = indexing;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses PropertyTypes from input data.")
    @ApiResponse(responseCode = "200", description = "PropertyTypes added or modified successfully.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PropertyTypeDTO.class))))
    public Response addOrUpdatePropertyTypesFromJson(@Parameter(description = "Format for input.", required = true, in = ParameterIn.QUERY) @QueryParam("format") @DefaultValue("json") final String format,
                                                     @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty,
                                                     @RequestBody(description = "JSON payload for PropertyType data.", required = true) final String jsonPayload) {
        return parseAndPersistPropertyTypesFromSource(FORMAT_JSON, null, jsonPayload, pretty);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses PropertyTypes from input data.")
    @ApiResponse(responseCode = "200", description = "PropertyTypes added or modified successfully.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PropertyTypeDTO.class))))
    public Response addOrUpdatePropertyTypesFromFile(@Parameter(description = "Format for input.", required = true, in = ParameterIn.QUERY) @QueryParam("format") @DefaultValue("json") final String format,
                                                     @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty,
                                                     @Parameter(description = "Input-file for CSV or Excel import.", required = true, in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "binary", description = "Incoming file.")) @FormDataParam("file") final InputStream inputStream) {
        return parseAndPersistPropertyTypesFromSource(format, inputStream, null, pretty);
    }

    @POST
    @Path("{PropertyTypeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses PropertyType from input data.")
    @ApiResponse(responseCode = "200", description = "Returns success.")
    public Response updatePropertyType(@Parameter(description = "PropertyType ID", required = true, in = ParameterIn.PATH) @PathParam("PropertyTypeId") final String propertyTypeId,
                                       @RequestBody(description = "JSON payload for PropertyType data.", required = true) final String jsonPayload) {
        final PropertyTypeDTO propertyType = propertyTypeService.parseAndPersistPropertyTypeFromJson(propertyTypeId, jsonPayload);
        indexing.updatePropertyType(propertyType);
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    private Response parseAndPersistPropertyTypesFromSource(final String format,
                                                            final InputStream inputStream,
                                                            final String jsonPayload,
                                                            final String pretty) {
        final Set<PropertyTypeDTO> propertyTypes = propertyTypeService.parseAndPersistPropertyTypesFromSourceData(format, inputStream, jsonPayload);
        indexing.updatePropertyTypes(propertyTypes);
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODEREGISTRY, null), pretty));
        final ResponseWrapper<PropertyTypeDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("PropertyTypes added or modified: " + propertyTypes.size());
        meta.setCode(200);
        responseWrapper.setResults(propertyTypes);
        return Response.ok(responseWrapper).build();
    }
}
