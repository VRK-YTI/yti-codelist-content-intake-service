package fi.vm.yti.codelist.intake.resource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fi.vm.yti.codelist.intake.api.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Component
@Path("/configuration")
@Produces("text/plain")
@Tag(name = "Configuration")
public class ConfigurationResource implements AbstractBaseResource {

    private final ApiUtils apiUtils;

    @Inject
    public ConfigurationResource(final ApiUtils apiUtils) {
        this.apiUtils = apiUtils;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Get configuration values as JSON")
    @ApiResponse(responseCode = "200", description = "Returns the configuration JSON element to the frontend related to this service.")
    public Response getConfig() {
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode configJson = mapper.createObjectNode();

        final String groupManagementPublicUrl = apiUtils.getGroupmanagementPublicUrl();
        final ObjectNode groupManagementConfig = mapper.createObjectNode();
        groupManagementConfig.put("url", groupManagementPublicUrl);
        configJson.set("groupManagementConfig", groupManagementConfig);

        final String datamodelPublicUrl = apiUtils.getDataModelPublicUrl();
        final ObjectNode dataModelConfig = mapper.createObjectNode();
        dataModelConfig.put("url", datamodelPublicUrl);
        configJson.set("dataModelConfig", dataModelConfig);

        final String terminologyPublicUrl = apiUtils.getTerminologyPublicUrl();
        final ObjectNode terminologyConfig = mapper.createObjectNode();
        terminologyConfig.put("url", terminologyPublicUrl);
        configJson.set("terminologyConfig", terminologyConfig);

        final String commentsPublicUrl = apiUtils.getCommentsPublicUrl();
        final ObjectNode commentsConfig = mapper.createObjectNode();
        commentsConfig.put("url", commentsPublicUrl);
        configJson.set("commentsConfig", commentsConfig);

        final boolean messagingEnabled = apiUtils.getMessagingEnabled();
        final ObjectNode messagingConfig = mapper.createObjectNode();
        messagingConfig.put("enabled", messagingEnabled);
        configJson.set("messagingConfig", messagingConfig);

        configJson.put("env", apiUtils.getEnv());
        configJson.put("defaultStatus", apiUtils.getDefaultStatus());
        configJson.put("codeSchemeSortMode", apiUtils.getCodeSchemeSortMode());
        return Response.ok(configJson).build();
    }
}
