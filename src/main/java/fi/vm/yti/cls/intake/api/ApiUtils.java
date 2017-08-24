package fi.vm.yti.cls.intake.api;

import fi.vm.yti.cls.intake.configuration.PublicApiServiceProperties;
import org.springframework.stereotype.Component;

import javax.inject.Inject;


/**
 * Generic utils for serving API population functionality.
 */
@Component
public class ApiUtils {

    private PublicApiServiceProperties m_publicApiServiceProperties;


    @Inject
    public ApiUtils(final PublicApiServiceProperties publicApiServiceProperties) {

        m_publicApiServiceProperties = publicApiServiceProperties;

    }


    /**
     * Creates a resource URL for given resource id with dynamic hostname, port and API context path mapping.
     *
     * @param apiPath API path that serves the resource.
     * @param resourceId ID of the REST resource.
     * @return Fully concatenated resource URL that can be used in API responses as a link to the resource.
     */
    public String createResourceUrl(final String apiPath, final String resourceId) {

        final String port = m_publicApiServiceProperties.getPort();

        final StringBuilder builder = new StringBuilder();

        builder.append(m_publicApiServiceProperties.getScheme());
        builder.append("://");
        builder.append(m_publicApiServiceProperties.getHost());
        if (port != null && port.length() > 0) {
            builder.append(":");
            builder.append(port);
        }
        builder.append(m_publicApiServiceProperties.getContextPath());
        builder.append(ApiConstants.API_BASE_PATH);
        builder.append("/");
        builder.append(ApiConstants.API_VERSION);
        builder.append(apiPath);
        builder.append("/");
        if (resourceId != null) {
            builder.append(resourceId);
            builder.append("/");
        }

        return builder.toString();

    }


    /**
     *
     *
     * @return Returns the Public API Service hostname mapped to the running environment.
     */
    public String getPublicApiServiceHostname() {

        final StringBuilder builder = new StringBuilder();

        final String port = m_publicApiServiceProperties.getPort();

        builder.append(m_publicApiServiceProperties.getHost());
        if (port != null && port.length() > 0) {
            builder.append(":");
            builder.append(port);
        }

        return builder.toString();

    }

}