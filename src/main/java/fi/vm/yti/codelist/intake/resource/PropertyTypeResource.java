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

import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.model.Meta;
import fi.vm.yti.codelist.intake.service.PropertyTypeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_CODEREGISTRY;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_JSON;

@Component
@Path("/v1/propertytypes")
@Api(value = "propertytypes")
@Produces(MediaType.APPLICATION_JSON)
public class PropertyTypeResource extends AbstractBaseResource {

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
    @ApiOperation(value = "Parses PropertyTypes from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdatePropertyTypesFromJson(@ApiParam(value = "JSON playload for PropertyType data.") final String jsonPayload) {
        return parseAndPersistPropertyTypesFromSource(FORMAT_JSON, null, jsonPayload);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses PropertyTypes from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", dataType = "file", paramType = "formData")
    })
    public Response addOrUpdatePropertyTypesFromFile(@ApiParam(value = "Format for input.", required = true) @QueryParam("format") @DefaultValue("json") final String format,
                                                     @ApiParam(value = "Input-file for CSV or Excel import.", hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream) {
        return parseAndPersistPropertyTypesFromSource(format, inputStream, null);
    }

    @POST
    @Path("{PropertyTypeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses PropertyType from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response updatePropertyType(@ApiParam(value = "PropertyType ID", required = true) @PathParam("PropertyTypeId") final String propertyTypeId,
                                       @ApiParam(value = "JSON playload for PropertyType data.") final String jsonPayload) {
        final PropertyTypeDTO propertyType = propertyTypeService.parseAndPersistPropertyTypeFromJson(propertyTypeId, jsonPayload);
        indexing.updatePropertyType(propertyType);
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    private Response parseAndPersistPropertyTypesFromSource(final String format,
                                                            final InputStream inputStream,
                                                            final String jsonPayload) {
        final Set<PropertyTypeDTO> propertyTypes = propertyTypeService.parseAndPersistPropertyTypesFromSourceData(format, inputStream, jsonPayload);
        indexing.updatePropertyTypes(propertyTypes);
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODEREGISTRY, null)));
        final ResponseWrapper<PropertyTypeDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("PropertyTypes added or modified: " + propertyTypes.size());
        meta.setCode(200);
        responseWrapper.setResults(propertyTypes);
        return Response.ok(responseWrapper).build();
    }
}
