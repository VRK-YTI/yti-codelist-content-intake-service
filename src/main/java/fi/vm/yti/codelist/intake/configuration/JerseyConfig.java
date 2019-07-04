package fi.vm.yti.codelist.intake.configuration;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import fi.vm.yti.codelist.intake.exception.exceptionmapping.EOFExceptionMapper;
import fi.vm.yti.codelist.intake.exception.exceptionmapping.UncaughtExceptionMapper;
import fi.vm.yti.codelist.intake.exception.exceptionmapping.YtiCodeListExceptionMapper;
import fi.vm.yti.codelist.intake.filter.CacheFilter;
import fi.vm.yti.codelist.intake.filter.CharsetResponseFilter;
import fi.vm.yti.codelist.intake.filter.DataInitializationFilter;
import fi.vm.yti.codelist.intake.filter.RequestLoggingFilter;
import fi.vm.yti.codelist.intake.resource.AdminResource;
import fi.vm.yti.codelist.intake.resource.AuthenticatedUserResource;
import fi.vm.yti.codelist.intake.resource.CodeRegistryResource;
import fi.vm.yti.codelist.intake.resource.ConfigurationResource;
import fi.vm.yti.codelist.intake.resource.ExtensionResource;
import fi.vm.yti.codelist.intake.resource.ExternalReferenceResource;
import fi.vm.yti.codelist.intake.resource.ImpersonateUserResource;
import fi.vm.yti.codelist.intake.resource.InfoDomainResource;
import fi.vm.yti.codelist.intake.resource.MemberResource;
import fi.vm.yti.codelist.intake.resource.OrganizationResource;
import fi.vm.yti.codelist.intake.resource.PingResource;
import fi.vm.yti.codelist.intake.resource.PropertyTypeResource;
import fi.vm.yti.codelist.intake.resource.SwaggerResource;
import fi.vm.yti.codelist.intake.resource.ValueTypeResource;
import fi.vm.yti.codelist.intake.resource.VersionResource;
import fi.vm.yti.codelist.intake.resource.externalresources.GroupManagementProxyResource;
import fi.vm.yti.codelist.intake.resource.externalresources.TerminologyProxyResource;
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
        description = "YTI Codelist - Content Intake Service - Spring Boot microservice.",
        version = "v1",
        title = "YTI Codelist Service - Content Intake Service",
        termsOfService = "https://opensource.org/licenses/EUPL-1.1",
        contact = @Contact(
            name = "Code List Service by the Population Register Center of Finland",
            url = "https://yhteentoimiva.suomi.fi/",
            email = "yhteentoimivuus@vrk.fi"
        ),
        license = @License(
            name = "EUPL-1.2",
            url = "https://opensource.org/licenses/EUPL-1.1"
        )
    ),
    host = "localhost:9602",
    basePath = API_CONTEXT_PATH_INTAKE + API_BASE_PATH,
    consumes = { MediaType.APPLICATION_JSON, "application/csv", "application/xls", "application/xlsx" },
    produces = { MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN },
    schemes = { SwaggerDefinition.Scheme.HTTPS }
)
@Api(value = "/api")
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        final JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
        CustomObjectMapper cm = new CustomObjectMapper();
        provider.setMapper(cm);

        // Charset filter
        register(CharsetResponseFilter.class, Priorities.AUTHENTICATION);

        // Cache control headers to no cache.
        register(CacheFilter.class, Priorities.AUTHENTICATION);

        // ExceptionMappers
        register(YtiCodeListExceptionMapper.class, Priorities.AUTHENTICATION);
        register(UncaughtExceptionMapper.class, Priorities.AUTHENTICATION);
        register(EOFExceptionMapper.class, Priorities.AUTHENTICATION);

        // Logging
        register(RequestLoggingFilter.class);

        // Health
        register(PingResource.class);

        // Configuration
        register(ConfigurationResource.class);

        // GroupManagement
        register(GroupManagementProxyResource.class);

        // Terminology
        register(TerminologyProxyResource.class);

        // Admin resources
        register(AdminResource.class);

        // Generic resources
        register(VersionResource.class);
        register(SwaggerResource.class);

        // Multipart support
        register(MultiPartFeature.class);

        // User authentication
        register(AuthenticatedUserResource.class);
        register(ImpersonateUserResource.class);

        // Admin APIs for YTI
        register(OrganizationResource.class);
        register(CodeRegistryResource.class);
        register(ExternalReferenceResource.class);
        register(PropertyTypeResource.class);
        register(InfoDomainResource.class);
        register(ExtensionResource.class);
        register(MemberResource.class);
        register(ValueTypeResource.class);

        // Data initialization filter
        register(DataInitializationFilter.class, Priorities.AUTHORIZATION);
    }
}
