package fi.vm.yti.cls.intake.configuration;

import fi.vm.yti.cls.intake.api.ApiConstants;
import fi.vm.yti.cls.intake.resource.CodeRegistryResource;
import fi.vm.yti.cls.intake.resource.ElectoralDistrictResource;
import fi.vm.yti.cls.intake.resource.HealthCareDistrictResource;
import fi.vm.yti.cls.intake.resource.MagistrateResource;
import fi.vm.yti.cls.intake.resource.MagistrateServiceUnitResource;
import fi.vm.yti.cls.intake.resource.PostManagementDistrictResource;
import fi.vm.yti.cls.intake.resource.PostalCodeResource;
import fi.vm.yti.cls.intake.resource.SwaggerResource;
import fi.vm.yti.cls.intake.resource.VersionResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

import javax.ws.rs.ApplicationPath;

@Component
@SwaggerDefinition(
    info = @Info(
        description = "Code List Service - Content Intake Service - Spring Boot microservice.",
        version = "v1",
        title = "Code List Service - Content Intake Service",
        termsOfService = "https://opensource.org/licenses/EUPL-1.1",
        contact = @Contact(
            name = "Code List Service by the Population Register Center of Finland",
            url = "http://vm.fi/yhteinen-tiedon-hallinta"
        ),
        license = @License(
            name = "EUPL-1.2",
            url = "https://opensource.org/licenses/EUPL-1.1"
        )
    ),
    host = "localhost:9601",
    basePath = ApiConstants.API_CONTEXT_PATH + ApiConstants.API_BASE_PATH,
    consumes = {"application/json", "application/xml"},
    produces = {"application/json", "application/xml"},
    schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS}
)
@Api(value = "/api", description = "Code List Service - Content Intake Service")
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        // Generic resources.
        register(VersionResource.class);
        register(SwaggerResource.class);

        // Multipart support.
        register(MultiPartFeature.class);

        // Admin APIs for YTI model.
        register(CodeRegistryResource.class);

        // Admin APIs for Content Intake.
        register(PostalCodeResource.class);
        register(PostManagementDistrictResource.class);
        register(MagistrateServiceUnitResource.class);
        register(MagistrateResource.class);
        register(MagistrateServiceUnitResource.class);
        register(ElectoralDistrictResource.class);
        register(HealthCareDistrictResource.class);
    }

}
