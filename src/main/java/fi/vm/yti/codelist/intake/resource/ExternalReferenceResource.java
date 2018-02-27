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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.model.ExternalReference;
import fi.vm.yti.codelist.common.model.Meta;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.jpa.ExternalReferenceRepository;
import fi.vm.yti.codelist.intake.parser.ExternalReferenceParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_PATH_EXTERNALREFERENCES;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_PATH_VERSION_V1;
import static fi.vm.yti.codelist.common.constants.ApiConstants.ELASTIC_INDEX_EXTERNALREFERENCE;
import static fi.vm.yti.codelist.common.constants.ApiConstants.ELASTIC_TYPE_EXTERNALREFERENCE;
import static fi.vm.yti.codelist.common.constants.ApiConstants.METHOD_POST;

@Component
@Path("/v1/externalreferences")
@Api(value = "externalreferences")
@Produces(MediaType.APPLICATION_JSON)
public class ExternalReferenceResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalReferenceResource.class);

    private final Indexing indexing;
    private final ExternalReferenceRepository externalReferenceRepository;
    private final ExternalReferenceParser externalreferenceParser;
    private final AuthorizationManager authorizationManager;

    @Inject
    public ExternalReferenceResource(final Indexing indexing,
                                     final ExternalReferenceRepository externalReferenceRepository,
                                     final ExternalReferenceParser externalReferenceParser,
                                     final AuthorizationManager authorizationManager) {
        this.indexing = indexing;
        this.externalReferenceRepository = externalReferenceRepository;
        this.externalreferenceParser = externalReferenceParser;
        this.authorizationManager = authorizationManager;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses ExternalReferences from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    @Transactional
    public Response addOrUpdateExternalReferences(@ApiParam(value = "JSON playload for ExternalReference data.", required = false) final String jsonPayload) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_EXTERNALREFERENCES + "/");
        final Meta meta = new Meta();
        final MetaResponseWrapper wrapper = new MetaResponseWrapper(meta);
        if (!authorizationManager.isSuperUser()) {
            return handleUnauthorizedAccess(meta, wrapper,
                "Superuser rights are needed to addOrUpdateExternalReferences.");
        }
        try {
            final Set<ExternalReference> externalReferences = externalreferenceParser.parseExternalReferencesFromJson(jsonPayload);
            if (!externalReferences.isEmpty()) {
                externalReferenceRepository.save(externalReferences);
                indexing.reIndex(ELASTIC_INDEX_EXTERNALREFERENCE, ELASTIC_TYPE_EXTERNALREFERENCE);
            }
            meta.setMessage("ExternalReferences added or modified: " + externalReferences.size());
            meta.setCode(200);
            return Response.ok(wrapper).build();
        } catch (final IOException e) {
            LOG.error("Error parsing ExternalReferences from JSON.", e);
            meta.setCode(400);
            return Response.status(Response.Status.BAD_REQUEST).entity(wrapper).build();
        }
    }

    @POST
    @Path("{externalReferenceId}/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses ExternalReference from input data.")
    @ApiResponse(code = 200, message = "Returns success.")
    @Transactional
    public Response updateExternalReference(@ApiParam(value = "ExternalReference ID", required = true) @PathParam("externalReferenceId") final String externalReferenceId,
                                            @ApiParam(value = "JSON playload for ExternalReference data.", required = false) final String jsonPayload) {
        logApiRequest(LOG, METHOD_POST, API_PATH_VERSION_V1, API_PATH_EXTERNALREFERENCES + "/" + externalReferenceId + "/");
        final Meta meta = new Meta();
        final MetaResponseWrapper wrapper = new MetaResponseWrapper(meta);
        if (!authorizationManager.isSuperUser()) {
            return handleUnauthorizedAccess(meta, wrapper,
                "Superuser rights are needed to updateExternalReference.");
        }
        final UUID uuid = UUID.fromString(externalReferenceId);
        final ExternalReference existingExternalReference = externalReferenceRepository.findById(uuid);
        if (existingExternalReference != null) {
            try {
                final ExternalReference externalReference = externalreferenceParser.parseExternalReferenceFromJson(jsonPayload);
                externalReferenceRepository.save(externalReference);
                indexing.reIndex(ELASTIC_INDEX_EXTERNALREFERENCE, ELASTIC_TYPE_EXTERNALREFERENCE);
                meta.setCode(200);
                return Response.ok(wrapper).build();
            } catch (final IOException e) {
                LOG.error("Error parsing ExternalReferences from JSON.", e);
                meta.setCode(400);
                return Response.status(Response.Status.BAD_REQUEST).entity(wrapper).build();
            }
        } else {
            LOG.error("ExternalReference with id " + externalReferenceId + " not found.");
            meta.setMessage("ExternalReference with id " + externalReferenceId + " not found");
            meta.setCode(404);
            return Response.status(Response.Status.NOT_FOUND).entity(wrapper).build();
        }
    }
}
