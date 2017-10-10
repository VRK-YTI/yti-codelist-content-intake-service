package fi.vm.yti.codelist.intake.api;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.constants.ApiConstants;
import fi.vm.yti.codelist.intake.configuration.ContentIntakeServiceProperties;
import fi.vm.yti.codelist.intake.configuration.PublicApiServiceProperties;

/**
 * Generic utils for serving APIs.
 */
@Component
public class ApiUtils {

    private PublicApiServiceProperties publicApiServiceProperties;
    private ContentIntakeServiceProperties contentIntakeServiceProperties;

    @Inject
    public ApiUtils(final PublicApiServiceProperties publicApiServiceProperties,
                    final ContentIntakeServiceProperties contentIntakeServiceProperties) {
        this.publicApiServiceProperties = publicApiServiceProperties;
        this.contentIntakeServiceProperties = contentIntakeServiceProperties;
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
     * @return Returns the Content Intake Service hostname mapped to the running environment.
     */
    public String getContentIntakeServiceHostname() {
        final StringBuilder builder = new StringBuilder();
        final String port = contentIntakeServiceProperties.getPort();
        builder.append(contentIntakeServiceProperties.getHost());
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