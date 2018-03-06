package fi.vm.yti.codelist.intake.resource;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fi.vm.yti.codelist.common.model.ErrorModel;
import fi.vm.yti.codelist.intake.exception.ErrorConstants;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.model.Meta;
import fi.vm.yti.codelist.common.model.PropertyType;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.jpa.PropertyTypeRepository;
import fi.vm.yti.codelist.intake.parser.PropertyTypeParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_PATH_PROPERTYTYPES;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_PATH_VERSION_V1;
import static fi.vm.yti.codelist.common.constants.ApiConstants.ELASTIC_INDEX_PROPERTYTYPE;
import static fi.vm.yti.codelist.common.constants.ApiConstants.ELASTIC_TYPE_PROPERTYTYPE;
import static fi.vm.yti.codelist.common.constants.ApiConstants.METHOD_POST;

@Component
@Path("/v1/propertytypes")
@Api(value = "propertytypes")
@Produces(MediaType.APPLICATION_JSON)
public class PropertyTypeResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyTypeResource.class);

    private final Indexing indexing;
    private final PropertyTypeRepository propertyTypeRepository;
    private final PropertyTypeParser propertyTypeParser;
    private final AuthorizationManager authorizationManager;

    @Inject
    public PropertyTypeResource(final Indexing indexing,
                                final PropertyTypeRepository propertyTypeRepository,
                                final PropertyTypeParser propertyTypeParser,
                                final AuthorizationManager authorizationManager) {
        this.indexing = indexing;
        this.propertyTypeRepository = propertyTypeRepository;
        this.propertyTypeParser = propertyTypeParser;
        this.authorizationManager = authorizationManager;
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
        if (!authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
        }
        try {
            final Set<PropertyType> propertyTypes = propertyTypeParser.parsePropertyTypesFromJson(jsonPayload);
            if (!propertyTypes.isEmpty()) {
                propertyTypeRepository.save(propertyTypes);
                indexing.reIndex(ELASTIC_INDEX_PROPERTYTYPE, ELASTIC_TYPE_PROPERTYTYPE);
            }
            meta.setMessage("PropertyTypes added or modified: " + propertyTypes.size());
            meta.setCode(200);
            return Response.ok(wrapper).build();
        } catch (final IOException e) {
            LOG.error("Error parsing PropertyTypes from JSON.", e);
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
        if (!authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
        }
        final UUID uuid = UUID.fromString(propertyTypeId);
        final PropertyType existingPropertyType = propertyTypeRepository.findById(uuid);
        if (existingPropertyType != null) {
            try {
                final PropertyType propertyType = propertyTypeParser.parsePropertyTypeFromJson(jsonPayload);
                propertyTypeRepository.save(propertyType);
                indexing.reIndex(ELASTIC_INDEX_PROPERTYTYPE, ELASTIC_TYPE_PROPERTYTYPE);
                meta.setCode(200);
                return Response.ok(wrapper).build();
            } catch (final IOException e) {
                LOG.error("Error parsing PropertyType from JSON.", e);
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
