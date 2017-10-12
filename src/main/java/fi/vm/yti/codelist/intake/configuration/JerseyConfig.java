package fi.vm.yti.codelist.intake.configuration;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.intake.resource.CodeRegistryResource;
import fi.vm.yti.codelist.intake.resource.PingResource;
import fi.vm.yti.codelist.intake.resource.SwaggerResource;
import fi.vm.yti.codelist.intake.resource.VersionResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_BASE_PATH;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_CONTEXT_PATH_INTAKE;

@Component
@SwaggerDefinition(
    info = @Info(
        description = "Code List Service - Content Intake Service - Spring Boot microservice.",
        version = "v1",
        title = "Code List Service - Content Intake Service",
        termsOfService = "https://opensource.org/licenses/EUPL-1.1",
        contact = @Contact(
            name = "Code List Service by the Population Register Center of Finland",
            url = "http://vm.fi/yhteinen-tiedon-hallinta",
            email = "yhteentoimivuus@vrk.fi"
        ),
        license = @License(
            name = "EUPL-1.2",
            url = "https://opensource.org/licenses/EUPL-1.1"
        )
    ),
    host = "localhost:9602",
    basePath = API_CONTEXT_PATH_INTAKE + API_BASE_PATH,
    consumes = {"application/json", "application/xml"},
    produces = {"application/json", "application/xml"},
    schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS}
)
@Api(value = "/api", description = "Code List Service - Content Intake Service")
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        // CORS filtering.
        register(CorsFilter.class);

        // Health.
        register(PingResource.class);

        // Generic resources.
        register(VersionResource.class);
        register(SwaggerResource.class);

        // Multipart support.
        register(MultiPartFeature.class);

        // Admin APIs for YTI model.
        register(CodeRegistryResource.class);
    }
}
