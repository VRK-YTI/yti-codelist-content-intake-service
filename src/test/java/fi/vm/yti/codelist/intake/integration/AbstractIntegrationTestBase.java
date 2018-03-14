package fi.vm.yti.codelist.intake.integration;

import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

abstract public class AbstractIntegrationTestBase {

    public static final String TEST_BASE_URL = "http://localhost";
    public static final String CODEREGISTRIES_FOLDER_NAME = "coderegistries";
    public static final String CODESCHEMES_FOLDER_NAME = "codeschemes";
    public static final String CODES_FOLDER_NAME = "codes";
    public static final String TEST_CODEREGISTRY_CODEVALUE = "testregistry1";
    private static final String PARAMETER_FILE = "file";

    private TestRestTemplate restTemplate = new TestRestTemplate();

    @LocalServerPort
    private int randomServerPort;

    public String createApiUrl(final int serverPort,
                               final String apiPath) {
        return TEST_BASE_URL + ":" + serverPort + API_CONTEXT_PATH_INTAKE + API_BASE_PATH + API_PATH_VERSION_V1 + apiPath + "/";
    }

    public ResponseEntity<String> uploadCodesToCodeSchemeFromCsv(final String codeRegistryCodeValue,
                                                                 final String codeSchemeCodeValue,
                                                                 final String codesFilename) {
        return uploadCodesToCodeSchemeFromExcel(codeRegistryCodeValue, codeSchemeCodeValue, codesFilename, FORMAT_CSV);
    }

    public ResponseEntity<String> uploadCodesToCodeSchemeFromExcel(final String codeRegistryCodeValue,
                                                                   final String codeSchemeCodeValue,
                                                                   final String codesFilename) {
        return uploadCodesToCodeSchemeFromExcel(codeRegistryCodeValue, codeSchemeCodeValue, codesFilename, FORMAT_EXCEL);
    }

    private ResponseEntity<String> uploadCodesToCodeSchemeFromExcel(final String codeRegistryCodeValue,
                                                                    final String codeSchemeCodeValue,
                                                                    final String codesFilename,
                                                                    final String format) {
        final String apiUrl = createApiUrl(randomServerPort, API_PATH_CODEREGISTRIES) + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + codeSchemeCodeValue + API_PATH_CODES + "/" + "?format=" + format;
        final LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        final String registryFilePath = "/" + CODES_FOLDER_NAME + "/" + codesFilename;
        parameters.add(PARAMETER_FILE, new ClassPathResource(registryFilePath));
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        final HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(parameters, headers);
        return restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class, "");
    }

    public ResponseEntity<String> uploadCodeSchemesToCodeRegistryFromCsv(final String codeRegistryCodeValue,
                                                                         final String codeSchemesFilename) {
        return uploadCodeSchemesToCodeRegistryFromExcel(codeRegistryCodeValue, codeSchemesFilename, FORMAT_CSV);
    }

    public ResponseEntity<String> uploadCodeSchemesToCodeRegistryFromExcel(final String codeRegistryCodeValue,
                                                                           final String codeSchemesFilename) {
        return uploadCodeSchemesToCodeRegistryFromExcel(codeRegistryCodeValue, codeSchemesFilename, FORMAT_EXCEL);
    }

    private ResponseEntity<String> uploadCodeSchemesToCodeRegistryFromExcel(final String codeRegistryCodeValue,
                                                                            final String codeSchemesFilename,
                                                                            final String format) {
        final String apiUrl = createApiUrl(randomServerPort, API_PATH_CODEREGISTRIES) + codeRegistryCodeValue + API_PATH_CODESCHEMES + "/" + "?format=" + format;
        final LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        final String registryFilePath = "/" + CODESCHEMES_FOLDER_NAME + "/" + codeSchemesFilename;
        parameters.add(PARAMETER_FILE, new ClassPathResource(registryFilePath));
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        final HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(parameters, headers);
        return restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class, "");
    }

    public ResponseEntity<String> uploadCodeRegistriesFromCsv(final String codeRegistriesFilename) {
        return uploadCodeRegistries(codeRegistriesFilename, FORMAT_CSV);
    }

    public ResponseEntity<String> uploadCodeRegistriesFromExcel(final String codeRegistriesFilename) {
        return uploadCodeRegistries(codeRegistriesFilename, FORMAT_EXCEL);
    }

    private ResponseEntity<String> uploadCodeRegistries(final String codeRegistriesFilename,
                                                        final String format) {
        final String apiUrl = createApiUrl(randomServerPort, API_PATH_CODEREGISTRIES) + "/" + "?format=" + format;
        final LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        final String registryFilePath = "/" + CODEREGISTRIES_FOLDER_NAME + "/" + codeRegistriesFilename;
        parameters.add(PARAMETER_FILE, new ClassPathResource(registryFilePath));
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        final HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(parameters, headers);
        return restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class, "");
    }
}
