package fi.vm.yti.codelist.intake.api;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.configuration.ContentIntakeServiceProperties;
import fi.vm.yti.codelist.intake.configuration.GroupManagementProperties;
import fi.vm.yti.codelist.intake.configuration.PublicApiServiceProperties;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

/**
 * Generic utils for serving APIs.
 */
@Component
public class ApiUtils {

    private PublicApiServiceProperties publicApiServiceProperties;
    private ContentIntakeServiceProperties contentIntakeServiceProperties;
    private GroupManagementProperties groupManagementProperties;

    @Inject
    public ApiUtils(final PublicApiServiceProperties publicApiServiceProperties,
                    final ContentIntakeServiceProperties contentIntakeServiceProperties,
                    final GroupManagementProperties groupManagementProperties) {
        this.publicApiServiceProperties = publicApiServiceProperties;
        this.contentIntakeServiceProperties = contentIntakeServiceProperties;
        this.groupManagementProperties = groupManagementProperties;
    }

    /**
     * Creates a resource URI for given resource id with dynamic hostname, port and API context path mapping.
     *
     * @param apiPath    API path that serves the resource.
     * @param resourceId ID of the REST resource.
     * @return Fully concatenated resource URL that can be used in API responses as a link to the resource.
     */
    public String createResourceUri(final String apiPath, final String resourceId) {
        final String port = publicApiServiceProperties.getPort();
        final StringBuilder builder = new StringBuilder();
        builder.append(publicApiServiceProperties.getScheme());
        builder.append("://");
        builder.append(publicApiServiceProperties.getHost());
        appendPortToUrlIfNotEmpty(port, builder);
        builder.append(publicApiServiceProperties.getContextPath());
        builder.append(API_BASE_PATH);
        builder.append("/");
        builder.append(API_VERSION);
        builder.append(apiPath);
        builder.append("/");
        if (resourceId != null) {
            builder.append(resourceId);
            builder.append("/");
        }
        return builder.toString();
    }

    public String createCodeRegistryUri(final CodeRegistry codeRegistry) {
        return createResourceUri(API_PATH_CODEREGISTRIES, codeRegistry.getCodeValue());
    }

    public String createCodeSchemeUri(final CodeRegistry codeRegistry, final CodeScheme codeScheme) {
        final String uri;
        if (codeScheme.getStatus().equalsIgnoreCase(Status.VALID.toString())) {
            uri = createResourceUri(API_PATH_CODEREGISTRIES + "/" + codeRegistry.getCodeValue() + API_PATH_CODESCHEMES, codeScheme.getCodeValue());
        } else {
            uri = createResourceUri(API_PATH_CODEREGISTRIES + "/" + codeRegistry.getCodeValue() + API_PATH_CODESCHEMES, codeScheme.getId().toString());
        }
        return uri;
    }

    public String createCodeUri(final CodeRegistry codeRegistry, final CodeScheme codeScheme, final Code code) {
        final String uri;
        if (code.getStatus().equalsIgnoreCase(Status.VALID.toString())) {
            if (codeScheme.getStatus().equalsIgnoreCase(Status.VALID.toString())) {
                uri = createResourceUri(API_PATH_CODEREGISTRIES + "/" + codeRegistry.getCodeValue() + API_PATH_CODESCHEMES + "/" + codeScheme.getCodeValue(), code.getCodeValue());
            } else {
                uri = createResourceUri(API_PATH_CODEREGISTRIES + "/" + codeRegistry.getCodeValue() + API_PATH_CODESCHEMES + "/" + codeScheme.getCodeValue(), code.getId().toString());
            }
        } else {
            if (codeScheme.getStatus().equalsIgnoreCase(Status.VALID.toString())) {
                uri = createResourceUri(API_PATH_CODEREGISTRIES + "/" + codeRegistry.getCodeValue() + API_PATH_CODESCHEMES + "/" + codeScheme.getId(), code.getCodeValue());
            } else {
                uri = createResourceUri(API_PATH_CODEREGISTRIES + "/" + codeRegistry.getCodeValue() + API_PATH_CODESCHEMES + "/" + codeScheme.getId(), code.getId().toString());
            }
        }
        return uri;
    }

    public String getContentIntakeServiceHostname() {
        final StringBuilder builder = new StringBuilder();
        final String port = contentIntakeServiceProperties.getPort();
        builder.append(contentIntakeServiceProperties.getHost());
        appendPortToUrlIfNotEmpty(port, builder);
        return builder.toString();
    }

    public String getGroupmanagementPublicUrl() {
        return groupManagementProperties.getPublicUrl();
    }

    private void appendPortToUrlIfNotEmpty(final String port, final StringBuilder builder) {
        if (port != null && !port.isEmpty()) {
            builder.append(":");
            builder.append(port);
        }
    }
}