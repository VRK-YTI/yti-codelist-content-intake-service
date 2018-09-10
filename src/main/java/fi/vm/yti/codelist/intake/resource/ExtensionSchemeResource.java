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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;

import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.model.Meta;
import fi.vm.yti.codelist.intake.service.ExtensionSchemeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_EXTENSIONSCHEME;

@Component
@Path("/v1/extensionschemes")
@Api(value = "extensionschemes")
@Produces(MediaType.APPLICATION_JSON)
public class ExtensionSchemeResource implements AbstractBaseResource {

    private final Indexing indexing;
    private final ExtensionSchemeService extensionSchemeService;

    @Inject
    public ExtensionSchemeResource(final Indexing indexing,
                                   final ExtensionSchemeService extensionService) {
        this.indexing = indexing;
        this.extensionSchemeService = extensionService;
    }

    @POST
    @Path("{extensionSchemeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses and creates or updates Extensions from JSON input.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Returns success.")
    })
    public Response addOrUpdateExtensionsFromJson(@ApiParam(value = "ExtensionScheme UUID", required = true) @PathParam("extensionSchemeId") final UUID extensionSchemeId,
                                                  @ApiParam(value = "JSON playload for Extension data.", required = true) final String jsonPayload) {
        return parseAndPersistExtensionFromSource(extensionSchemeId, jsonPayload);
    }

    private Response parseAndPersistExtensionFromSource(final UUID extensionSchemeId,
                                                        final String jsonPayload) {
        final ExtensionSchemeDTO extension = extensionSchemeService.parseAndPersistExtensionSchemeFromJson(extensionSchemeId, jsonPayload);
        final Set<ExtensionSchemeDTO> extensionSchemes = new HashSet<>();
        extensionSchemes.add(extension);
        indexing.updateExtensionSchemes(extensionSchemes);
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_EXTENSIONSCHEME, "extensionScheme")));
        final ResponseWrapper<ExtensionSchemeDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("ExtensionScheme extensions added or modified.");
        meta.setCode(200);
        responseWrapper.setResults(extensionSchemes);
        return Response.ok(responseWrapper).build();
    }
}
