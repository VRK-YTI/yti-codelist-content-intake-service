package fi.vm.yti.codelist.intake.configuration;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Priorities;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.springframework.stereotype.Component;

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
import fi.vm.yti.codelist.intake.resource.ExtensionResource;
import fi.vm.yti.codelist.intake.resource.ExternalReferenceResource;
import fi.vm.yti.codelist.intake.resource.ImpersonateUserResource;
import fi.vm.yti.codelist.intake.resource.InfoDomainResource;
import fi.vm.yti.codelist.intake.resource.MemberResource;
import fi.vm.yti.codelist.intake.resource.OrganizationResource;
import fi.vm.yti.codelist.intake.resource.PingResource;
import fi.vm.yti.codelist.intake.resource.PropertyTypeResource;
import fi.vm.yti.codelist.intake.resource.SystemResource;
import fi.vm.yti.codelist.intake.resource.UserResource;
import fi.vm.yti.codelist.intake.resource.ValueTypeResource;
import fi.vm.yti.codelist.intake.resource.VersionResource;
import fi.vm.yti.codelist.intake.resource.externalresources.GroupManagementProxyResource;
import fi.vm.yti.codelist.intake.resource.externalresources.TerminologyProxyResource;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

@Component
@OpenAPIDefinition(
    info = @Info(
        description = "YTI Codelist - Content Intake Service - Spring Boot microservice.",
        version = "v1",
        title = "YTI Codelist Service - Content Intake Service",
        termsOfService = "https://opensource.org/licenses/EUPL-1.1",
        contact = @Contact(
            name = "Code List Service by the Digital and Population Data Services Agency",
            url = "https://yhteentoimiva.suomi.fi/",
            email = "yhteentoimivuus@dvv.fi"
        ),
        license = @License(
            name = "EUPL-1.2",
            url = "https://opensource.org/licenses/EUPL-1.1"
        )
    ),
    servers = {
        @Server(
            description = "Codelist Content Intake Service API",
            url = "/codelist-intake")
    }
)
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        final JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
        provider.setMapper(new CustomObjectMapper());

        // Charset filter
        register(CharsetResponseFilter.class, Priorities.AUTHENTICATION);

        // Cache control headers to no cache.
        register(CacheFilter.class, Priorities.AUTHENTICATION);

        // ExceptionMappers
        register(YtiCodeListExceptionMapper.class, Priorities.AUTHENTICATION);
        register(UncaughtExceptionMapper.class, Priorities.AUTHENTICATION);
        register(EOFExceptionMapper.class, Priorities.AUTHENTICATION);

        // Gzip
        register(EncodingFilter.class);
        register(GZipEncoder.class);
        register(DeflateEncoder.class);

        // Multipart support
        register(MultiPartFeature.class);

        // Logging
        register(RequestLoggingFilter.class);

        // Health
        register(PingResource.class);

        // System
        register(SystemResource.class);

        // GroupManagement
        register(GroupManagementProxyResource.class);

        // Terminology
        register(TerminologyProxyResource.class);

        // Admin resources
        register(AdminResource.class);

        // Generic resources
        register(VersionResource.class);

        // OpenAPI
        register(OpenApiResource.class);

        // User authentication
        register(AuthenticatedUserResource.class);
        register(ImpersonateUserResource.class);

        // Admin APIs for YTI
        register(OrganizationResource.class);
        register(UserResource.class);
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
