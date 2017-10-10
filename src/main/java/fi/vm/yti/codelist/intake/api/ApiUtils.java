package fi.vm.yti.codelist.intake.api;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.constants.ApiConstants;
import fi.vm.yti.codelist.intake.configuration.PublicApiServiceProperties;

/**
 * Generic utils for serving API population functionality.
 */
@Component
public class ApiUtils {

    private PublicApiServiceProperties publicApiServiceProperties;

    @Inject
    public ApiUtils(final PublicApiServiceProperties publicApiServiceProperties) {
        this.publicApiServiceProperties = publicApiServiceProperties;
    }

    /**
     * Creates a resource URL for given resource id with dynamic hostname, port and API context path mapping.
     *
     * @param apiPath API path that serves the resource.
     * @param resourceId ID of the REST resource.
     * @return Fully concatenated resource URL that can be used in API responses as a link to the resource.
     */
    public String createResourceUrl(final String apiPath, final String resourceId) {
        final String port = publicApiServiceProperties.getPort();
        final StringBuilder builder = new StringBuilder();
        builder.append(publicApiServiceProperties.getScheme());
        builder.append("://");
        builder.append(publicApiServiceProperties.getHost());
        appendPortToUrlIfNotEmpty(port, builder);
        builder.append(publicApiServiceProperties.getContextPath());
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
        final String port = publicApiServiceProperties.getPort();
        builder.append(publicApiServiceProperties.getHost());
        appendPortToUrlIfNotEmpty(port, builder);
        return builder.toString();
    }
    
    private void appendPortToUrlIfNotEmpty(final String port, final StringBuilder builder) {
        if (port != null && !port.isEmpty()) {
            builder.append(":");
            builder.append(port);
        }
    }
}