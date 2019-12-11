package fi.vm.yti.codelist.intake.resource;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg.ObjectWriterInjector;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.Meta;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.service.ExtensionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_EXTENSION;

@Component
@Path("/v1/extensions")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Extension")
public class ExtensionResource implements AbstractBaseResource {

    private final Indexing indexing;
    private final ExtensionService extensionService;

    @Inject
    public ExtensionResource(final Indexing indexing,
                             final ExtensionService extensionService) {
        this.indexing = indexing;
        this.extensionService = extensionService;
    }

    @POST
    @Path("{extensionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses and creates or updates Extensions from JSON input.")
    @ApiResponse(responseCode = "200", description = "Extensions added or modified successfully.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ExtensionDTO.class))))
    public Response addOrUpdateExtensionsFromJson(@Parameter(description = "Extension UUID", required = true, in = ParameterIn.PATH) @PathParam("extensionId") final UUID extensionId,
                                                  @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty,
                                                  @RequestBody(description = "JSON payload for Extension data.", required = true) final String jsonPayload) {
        return parseAndPersistExtensionFromSource(extensionId, jsonPayload, pretty);
    }

    private Response parseAndPersistExtensionFromSource(final UUID extensionId,
                                                        final String jsonPayload,
                                                        final String pretty) {
        final ExtensionDTO extension = extensionService.parseAndPersistExtensionFromJson(extensionId, jsonPayload, false);
        final Set<ExtensionDTO> extensions = new HashSet<>();
        extensions.add(extension);
        indexing.updateExtensions(extensions);
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(FILTER_NAME_EXTENSION, "extension"), pretty));
        final ResponseWrapper<ExtensionDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("Extensions added or modified.");
        meta.setCode(200);
        responseWrapper.setResults(extensions);
        return Response.ok(responseWrapper).build();
    }
}
