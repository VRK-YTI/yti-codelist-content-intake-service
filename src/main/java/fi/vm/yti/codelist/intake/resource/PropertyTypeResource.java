package fi.vm.yti.codelist.intake.resource;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import fi.vm.yti.codelist.common.model.Meta;
import fi.vm.yti.codelist.common.model.PropertyType;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.jpa.PropertyTypeRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

/**
 * Content Intake Service: REST resources for PropertyType entity management.
 */
@Component
@Path("/v1/propertytypes")
@Api(value = "propertytypes", description = "Operations for creating, deleting and updating PropertyTypes.")
@Produces(MediaType.APPLICATION_JSON)
public class PropertyTypeResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyTypeResource.class);

    private final Indexing indexing;
    private final PropertyTypeRepository propertyTypeRepository;

    @Inject
    public PropertyTypeResource(final Indexing indexing,
                                final PropertyTypeRepository propertyTypeRepository) {
        this.indexing = indexing;
        this.propertyTypeRepository = propertyTypeRepository;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses PropertyType from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    @Transactional
    public Response addOrUpdatePropertyTypes(@ApiParam(value = "JSON playload for PropertyType data.", required = false) final String jsonPayload) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_PROPERTYTYPES);
        final Meta meta = new Meta();
        final MetaResponseWrapper wrapper = new MetaResponseWrapper(meta);
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        try {
            final List<PropertyType> propertyTypes = mapper.readValue(jsonPayload, new TypeReference<List<PropertyType>>() {
            });
            if (!propertyTypes.isEmpty()) {
                propertyTypeRepository.save(propertyTypes);
                indexing.reIndex(ELASTIC_INDEX_PROPERTYTYPE, ELASTIC_TYPE_PROPERTYTYPE);
            }
            meta.setMessage("PropertyTypes added or modified: " + propertyTypes.size());
            meta.setCode(200);
            return Response.ok(wrapper).build();
        } catch (final IOException e) {
            LOG.error("Error parsing PropertyTypes from JSON.", e.getMessage());
            meta.setCode(400);
            return Response.status(Response.Status.BAD_REQUEST).entity(wrapper).build();
        }
    }

    @POST
    @Path("{propertyTypeId}/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses PropertyType from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    @Transactional
    public Response updatePropertyType(@ApiParam(value = "PropertyTypeId ID", required = true) @PathParam("propertyTypeId") final String propertyTypeId,
                                       @ApiParam(value = "JSON playload for PropertyType data.", required = false) final String jsonPayload) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_PROPERTYTYPES + "/" + propertyTypeId);
        final Meta meta = new Meta();
        final MetaResponseWrapper wrapper = new MetaResponseWrapper(meta);
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        final PropertyType existingPropertyType = propertyTypeRepository.findById(propertyTypeId);
        if (existingPropertyType != null) {
            try {
                final PropertyType propertyType = mapper.readValue(jsonPayload, PropertyType.class);
                propertyTypeRepository.save(propertyType);
                indexing.reIndex(ELASTIC_INDEX_PROPERTYTYPE, ELASTIC_TYPE_PROPERTYTYPE);
                meta.setCode(200);
                return Response.ok(wrapper).build();
            } catch (final IOException e) {
                LOG.error("Error parsing PropertyType from JSON.", e.getMessage());
                meta.setCode(400);
                return Response.status(Response.Status.BAD_REQUEST).entity(wrapper).build();
            }
        } else {
            LOG.error("PropertyType with id " + propertyTypeId + " not found.");
            meta.setMessage("PropertyType with id " + propertyTypeId + " not found");
            meta.setCode(404);
            return Response.status(Response.Status.NOT_FOUND).entity(wrapper).build();
        }
    }
}
