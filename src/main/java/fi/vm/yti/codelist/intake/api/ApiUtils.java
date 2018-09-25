package fi.vm.yti.codelist.intake.api;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.common.dto.ValueTypeDTO;
import fi.vm.yti.codelist.intake.configuration.ContentIntakeServiceProperties;
import fi.vm.yti.codelist.intake.configuration.DataModelProperties;
import fi.vm.yti.codelist.intake.configuration.FrontendProperties;
import fi.vm.yti.codelist.intake.configuration.GroupManagementProperties;
import fi.vm.yti.codelist.intake.configuration.PublicApiServiceProperties;
import fi.vm.yti.codelist.intake.configuration.TerminologyProperties;
import fi.vm.yti.codelist.intake.configuration.UriSuomiProperties;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.Member;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_406;

@Component
public class ApiUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ApiUtils.class);

    private final PublicApiServiceProperties publicApiServiceProperties;
    private final UriSuomiProperties uriSuomiProperties;
    private final ContentIntakeServiceProperties contentIntakeServiceProperties;
    private final GroupManagementProperties groupManagementProperties;
    private final TerminologyProperties terminologyProperties;
    private final DataModelProperties dataModelProperties;
    private final FrontendProperties frontendProperties;

    @Inject
    public ApiUtils(final PublicApiServiceProperties publicApiServiceProperties,
                    final ContentIntakeServiceProperties contentIntakeServiceProperties,
                    final GroupManagementProperties groupManagementProperties,
                    final TerminologyProperties terminologyProperties,
                    final DataModelProperties dataModelProperties,
                    final UriSuomiProperties uriSuomiProperties,
                    final FrontendProperties frontendProperties) {
        this.publicApiServiceProperties = publicApiServiceProperties;
        this.uriSuomiProperties = uriSuomiProperties;
        this.contentIntakeServiceProperties = contentIntakeServiceProperties;
        this.groupManagementProperties = groupManagementProperties;
        this.terminologyProperties = terminologyProperties;
        this.dataModelProperties = dataModelProperties;
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
    private String createResourceUrl(final String apiPath,
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

    public String createCodeUri(final CodeRegistry codeRegistry,
                                final CodeScheme codeScheme,
                                final Code code) {
        return createResourceUri(codeRegistry.getCodeValue() + "/" + codeScheme.getCodeValue() + "/code/" + urlEncodeString(code.getCodeValue()));
    }

    public String createExtensionUri(final Extension extension) {
        return createResourceUri(extension.getParentCodeScheme().getCodeRegistry().getCodeValue() + "/" + extension.getParentCodeScheme().getCodeValue() + "/extension/" + urlEncodeString(extension.getCodeValue()));
    }

    public String createMemberUri(final Member member) {
        return createResourceUri(member.getExtension().getParentCodeScheme().getCodeRegistry().getCodeValue() + "/" + member.getExtension().getParentCodeScheme().getCodeValue() + "/extension/" + urlEncodeString(member.getExtension().getCodeValue()) + "/member/" + member.getId());
    }

    public String createCodeUrl(final CodeDTO code) {
        return createCodeUrl(code.getCodeScheme().getCodeRegistry().getCodeValue(), code.getCodeScheme().getCodeValue(), code.getCodeValue());
    }

    public String createCodeUrl(final String codeRegistryCodeValue,
                                final String codeSchemeCodeValue,
                                final String codeValue) {
        return createResourceUrl(API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeCodeValue + API_PATH_CODES, urlEncodeString(codeValue));
    }

    public String createExternalReferenceUrl(final ExternalReferenceDTO externalReference) {
        return createResourceUrl(API_PATH_EXTERNALREFERENCES, externalReference.getId().toString());
    }

    public String createPropertyTypeUrl(final PropertyTypeDTO propertyType) {
        return createResourceUrl(API_PATH_PROPERTYTYPES, propertyType.getId().toString());
    }

    public String createValueTypeUrl(final ValueTypeDTO valueType) {
        return createResourceUrl(API_PATH_VALUETYPES, valueType.getId().toString());
    }

    public String createExtensionUrl(final Extension extension) {
        return createExtensionUrl(extension.getParentCodeScheme().getCodeRegistry().getCodeValue(), extension.getParentCodeScheme().getCodeValue(), extension.getCodeValue());
    }

    public String createExtensionUrl(final String codeRegistryCodeValue,
                                     final String codeSchemeCodeValue,
                                     final String codeValue) {
        return createResourceUrl(API_PATH_CODEREGISTRIES + "/" + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeCodeValue + API_PATH_EXTENSIONS, urlEncodeString(codeValue));
    }

    public String createMemberUrl(final Member member) {
        return createResourceUrl(API_PATH_CODEREGISTRIES + "/" + member.getExtension().getParentCodeScheme().getCodeRegistry().getCodeValue() +
            API_PATH_CODESCHEMES + "/" + member.getExtension().getParentCodeScheme().getCodeValue() +
            API_PATH_EXTENSIONS + urlEncodeString(member.getExtension().getCodeValue()) +
            API_PATH_MEMBERS, member.getId().toString());
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

    public String getTerminologyPublicUrl() {
        return terminologyProperties.getPublicUrl();
    }

    public String getDataModelPublicUrl() {
        return dataModelProperties.getPublicUrl();
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
        } catch (final UnsupportedEncodingException e) {
            LOG.error("Issue with url encoding a string.", e);
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }
}