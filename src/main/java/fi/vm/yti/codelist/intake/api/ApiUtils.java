package fi.vm.yti.codelist.intake.api;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.intake.configuration.ContentIntakeServiceProperties;
import fi.vm.yti.codelist.intake.configuration.FrontendProperties;
import fi.vm.yti.codelist.intake.configuration.GroupManagementProperties;
import fi.vm.yti.codelist.intake.configuration.PublicApiServiceProperties;
import fi.vm.yti.codelist.intake.configuration.UriSuomiProperties;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_406;

/**
 * Generic utils for serving APIs.
 */
@Component
public class ApiUtils {

    private PublicApiServiceProperties publicApiServiceProperties;
    private UriSuomiProperties uriSuomiProperties;
    private ContentIntakeServiceProperties contentIntakeServiceProperties;
    private GroupManagementProperties groupManagementProperties;
    private FrontendProperties frontendProperties;

    @Inject
    public ApiUtils(final PublicApiServiceProperties publicApiServiceProperties,
                    final ContentIntakeServiceProperties contentIntakeServiceProperties,
                    final GroupManagementProperties groupManagementProperties,
                    final UriSuomiProperties uriSuomiProperties,
                    final FrontendProperties frontendProperties) {
        this.publicApiServiceProperties = publicApiServiceProperties;
        this.uriSuomiProperties = uriSuomiProperties;
        this.contentIntakeServiceProperties = contentIntakeServiceProperties;
        this.groupManagementProperties = groupManagementProperties;
        this.frontendProperties = frontendProperties;
    }

    public String getEnv() {
        return contentIntakeServiceProperties.getEnv();
    }

    /**
     * Creates a resource URI for given resource id with dynamic hostname, port and API context path mapping.
     *
     * @param apiPath    API path that serves the resource.
     * @param resourceId ID of the REST resource.
     * @return Fully concatenated resource URL that can be used in API responses as a link to the resource.
     */
    public String createResourceUrl(final String apiPath,
                                    final String resourceId) {
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
        }
        return builder.toString();
    }

    /**
     * Creates a resource URI for given resource id with dynamic hostname, port and API context path mapping.
     *
     * @param resourcePath ID of the REST resource.
     * @return Fully concatenated resource URL that can be used in API responses as a link to the resource.
     */
    private String createResourceUri(final String resourcePath) {
        final String port = uriSuomiProperties.getPort();
        final StringBuilder builder = new StringBuilder();
        builder.append(uriSuomiProperties.getScheme());
        builder.append("://");
        builder.append(uriSuomiProperties.getHost());
        appendPortToUrlIfNotEmpty(port, builder);
        builder.append(uriSuomiProperties.getContextPath());
        builder.append("/");
        if (resourcePath != null) {
            builder.append(resourcePath);
        }
        return builder.toString();
    }

    public String createCodeRegistryUri(final CodeRegistry codeRegistry) {
        return createResourceUri(codeRegistry.getCodeValue());
    }

    public String createCodeRegistryUrl(final CodeRegistryDTO codeRegistry) {
        return createResourceUrl(API_PATH_CODEREGISTRIES, codeRegistry.getCodeValue());
    }

    public String createCodeSchemeUri(final CodeScheme codeScheme) {
        return createCodeSchemeUri(codeScheme.getCodeRegistry(), codeScheme);
    }

    public String createCodeSchemeUrl(final CodeSchemeDTO codeScheme) {
        return createCodeSchemeUrl(codeScheme.getCodeRegistry(), codeScheme);
    }

    public String createCodeSchemeUri(final CodeRegistry codeRegistry,
                                      final CodeScheme codeScheme) {
        return createResourceUri(codeRegistry.getCodeValue() + "/" + codeScheme.getCodeValue());
    }

    private String createCodeSchemeUrl(final CodeRegistryDTO codeRegistry,
                                       final CodeSchemeDTO codeScheme) {
        return createResourceUrl(API_PATH_CODEREGISTRIES + "/" + codeRegistry.getCodeValue() + API_PATH_CODESCHEMES, codeScheme.getCodeValue());
    }

    public String createCodeUri(final Code code) {
        return createCodeUri(code.getCodeScheme().getCodeRegistry(), code.getCodeScheme(), code);
    }

    public String createCodeUrl(final CodeDTO code) {
        return createCodeUrl(code.getCodeScheme().getCodeRegistry(), code.getCodeScheme(), code);
    }

    public String createCodeUri(final CodeRegistry codeRegistry,
                                final CodeScheme codeScheme,
                                final Code code) {
        return createResourceUri(codeRegistry.getCodeValue() + "/" + codeScheme.getCodeValue() + "/" + urlEncodeString(code.getCodeValue()));
    }

    private String createCodeUrl(final CodeRegistryDTO codeRegistry,
                                 final CodeSchemeDTO codeScheme,
                                 final CodeDTO code) {
        return createResourceUrl(API_PATH_CODEREGISTRIES + "/" + codeRegistry.getCodeValue() + API_PATH_CODESCHEMES + "/" + codeScheme.getCodeValue() + API_PATH_CODES, urlEncodeString(code.getCodeValue()));
    }

    public String createExternalReferenceUrl(final ExternalReferenceDTO externalReference) {
        return createResourceUrl(API_PATH_EXTERNALREFERENCES, externalReference.getId().toString());
    }

    public String createPropertyTypeUrl(final PropertyTypeDTO propertyType) {
        return createResourceUrl(API_PATH_PROPERTYTYPES, propertyType.getId().toString());
    }

    public String createExtensionSchemeUrl(final ExtensionSchemeDTO extensionScheme) {
        return createResourceUrl(API_PATH_EXTENSIONSCHEMES, extensionScheme.getId().toString());
    }

    public String createExtensionUrl(final ExtensionDTO extension) {
        return createResourceUrl(API_PATH_EXTENSIONS, extension.getId().toString());
    }

    public String getContentIntakeServiceHostname() {
        final StringBuilder builder = new StringBuilder();
        final String port = contentIntakeServiceProperties.getPort();
        builder.append(contentIntakeServiceProperties.getHost());
        appendPortToUrlIfNotEmpty(port, builder);
        return builder.toString();
    }

    public String getDefaultStatus() {
        return frontendProperties.getDefaultStatus();
    }

    public String getCodeSchemeSortMode() {
        return frontendProperties.getCodeSchemeSortMode();
    }

    public String getGroupmanagementPublicUrl() {
        return groupManagementProperties.getPublicUrl();
    }

    private void appendPortToUrlIfNotEmpty(final String port,
                                           final StringBuilder builder) {
        if (port != null && !port.isEmpty()) {
            builder.append(":");
            builder.append(port);
        }
    }

    private String urlEncodeString(final String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }
}