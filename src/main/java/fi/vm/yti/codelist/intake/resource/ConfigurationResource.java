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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;

@Component
@Path("/configuration")
@Api(value = "configuration")
@Produces("text/plain")
public class ConfigurationResource extends AbstractBaseResource {

    private final ApiUtils apiUtils;

    @Inject
    public ConfigurationResource(final ApiUtils apiUtils) {
        this.apiUtils = apiUtils;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Get configuration values as JSON")
    @ApiResponse(code = 200, message = "Returns the configuration JSON element to the frontend related to this service.")
    public Response getConfig() {
        final String groupManagementPublicUrl = apiUtils.getGroupmanagementPublicUrl();
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode configJson = mapper.createObjectNode();
        final ObjectNode groupManagementConfig = mapper.createObjectNode();
        groupManagementConfig.put("url", groupManagementPublicUrl);
        configJson.set("groupManagementConfig", groupManagementConfig);
        configJson.put("dev", apiUtils.isDev());
        return Response.ok(configJson).build();
    }
}
