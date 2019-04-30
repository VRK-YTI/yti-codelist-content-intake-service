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

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;

import fi.vm.yti.codelist.common.dto.Meta;
import fi.vm.yti.codelist.common.dto.ValueTypeDTO;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.service.ValueTypeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_CODEREGISTRY;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_JSON;

@Component
@Path("/v1/valuetypes")
@Api(value = "valuetypes")
@Produces(MediaType.APPLICATION_JSON)
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
    @ApiOperation(value = "Parses ValueTypes from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdateValueTypesFromJson(@ApiParam(value = "JSON playload for ValueType data.") final String jsonPayload,
                                                  @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
        return parseAndPersistValueTypesFromSource(FORMAT_JSON, null, jsonPayload, pretty);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses ValueTypes from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", dataType = "file", paramType = "formData")
    })
    public Response addOrUpdateValueTypesFromFile(@ApiParam(value = "Format for input.", required = true) @QueryParam("format") @DefaultValue("json") final String format,
                                                  @ApiParam(value = "Input-file for CSV or Excel import.", hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream,
                                                  @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
        return parseAndPersistValueTypesFromSource(format, inputStream, null, pretty);
    }

    @POST
    @Path("{ValueTypeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses ValueType from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response updateValueType(@ApiParam(value = "ValueType ID", required = true) @PathParam("ValueTypeId") final String valueTypeId,
                                    @ApiParam(value = "JSON playload for ValueType data.") final String jsonPayload,
                                    @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
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
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODEREGISTRY, null), pretty));
        final ResponseWrapper<ValueTypeDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("ValueTypes added or modified: " + valueTypes.size());
        meta.setCode(200);
        responseWrapper.setResults(valueTypes);
        return Response.ok(responseWrapper).build();
    }
}
