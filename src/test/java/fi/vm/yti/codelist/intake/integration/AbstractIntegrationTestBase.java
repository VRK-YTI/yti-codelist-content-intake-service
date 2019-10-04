package fi.vm.yti.codelist.intake.integration;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

abstract public class AbstractIntegrationTestBase {

    public static final String TEST_CODEREGISTRY_CODEVALUE = "testregistry1";
    private static final String TEST_BASE_URL = "http://localhost";
    private static final String CODEREGISTRIES_FOLDER_NAME = "coderegistries";
    private static final String CODESCHEMES_FOLDER_NAME = "codeschemes";
    private static final String CODES_FOLDER_NAME = "codes";
    private static final String PARAMETER_FILE = "file";

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @LocalServerPort
    private int randomServerPort;

    private String createApiUrl(final int serverPort) {
        return TEST_BASE_URL + ":" + serverPort + API_CONTEXT_PATH_INTAKE + API_BASE_PATH + API_PATH_VERSION_V1 + API_PATH_CODEREGISTRIES + "/";
    }

    ResponseEntity<String> uploadCodesToCodeSchemeFromCsv(final String codeRegistryCodeValue,
                                                          final String codeSchemeCodeValue,
                                                          final String codesFilename) {
        return uploadCodesToCodeScheme(codeRegistryCodeValue, codeSchemeCodeValue, codesFilename, FORMAT_CSV);
    }

    @SuppressWarnings("SameParameterValue")
    ResponseEntity<String> uploadCodesToCodeSchemeFromExcel(final String codeRegistryCodeValue,
                                                            final String codeSchemeCodeValue,
                                                            final String codesFilename) {
        return uploadCodesToCodeScheme(codeRegistryCodeValue, codeSchemeCodeValue, codesFilename, FORMAT_EXCEL);
    }

    private ResponseEntity<String> uploadCodesToCodeScheme(final String codeRegistryCodeValue,
                                                           final String codeSchemeCodeValue,
                                                           final String codesFilename,
                                                           final String format) {
        final String apiUrl = createApiUrl(randomServerPort) + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeCodeValue + API_PATH_CODES + "/" + "?format=" + format;
        final String filePath = "/" + CODES_FOLDER_NAME + "/" + codesFilename;
        return uploadFile(apiUrl, filePath);
    }

    ResponseEntity<String> uploadCodeSchemesToCodeRegistryFromCsv(final String codeRegistryCodeValue,
                                                                  final String codeSchemesFilename) {
        return uploadCodeSchemesToCodeRegistry(codeRegistryCodeValue, codeSchemesFilename, FORMAT_CSV);
    }

    ResponseEntity<String> uploadCodeSchemesToCodeRegistryFromExcel(final String codeRegistryCodeValue,
                                                                    final String codeSchemesFilename) {
        return uploadCodeSchemesToCodeRegistry(codeRegistryCodeValue, codeSchemesFilename, FORMAT_EXCEL);
    }

    private ResponseEntity<String> uploadCodeSchemesToCodeRegistry(final String codeRegistryCodeValue,
                                                                   final String codeSchemesFilename,
                                                                   final String format) {
        final String apiUrl = createApiUrl(randomServerPort) + codeRegistryCodeValue + API_PATH_CODESCHEMES + "?format=" + format;
        final String filePath = "/" + CODESCHEMES_FOLDER_NAME + "/" + codeSchemesFilename;
        return uploadFile(apiUrl, filePath);
    }

    @SuppressWarnings("SameParameterValue")
    ResponseEntity<String> uploadCodeRegistriesFromCsv(final String codeRegistriesFilename) {
        return uploadCodeRegistries(codeRegistriesFilename, FORMAT_CSV);
    }

    @SuppressWarnings("SameParameterValue")
    private ResponseEntity<String> uploadCodeRegistries(final String codeRegistriesFilename,
                                                        final String format) {
        final String apiUrl = createApiUrl(randomServerPort) + "/" + "?format=" + format;
        final String filePath = "/" + CODEREGISTRIES_FOLDER_NAME + "/" + codeRegistriesFilename;
        return uploadFile(apiUrl, filePath);
    }

    private ResponseEntity<String> uploadFile(final String apiUrl,
                                              final String registryFilePath) {
        final LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        parameters.add(PARAMETER_FILE, new ClassPathResource(registryFilePath));
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        final HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(parameters, headers);
        return restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class, "");
    }
}
