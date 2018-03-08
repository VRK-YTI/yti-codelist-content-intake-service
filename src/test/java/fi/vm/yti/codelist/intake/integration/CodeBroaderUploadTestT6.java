package fi.vm.yti.codelist.intake.integration;

import javax.transaction.Transactional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;

import fi.vm.yti.codelist.intake.ContentIntakeServiceApplication;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ContentIntakeServiceApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test"})
@TestPropertySource(locations = "classpath:test-port.properties")
public class CodeBroaderUploadTestT6 extends AbstractIntegrationTestBase {

    private static final String TEST_CODE_BROADER_SELF_FILENAME = "v1_broader_codes_self_test.xlsx";
    private static final String TEST_CODE_BROADER_UNEXISTING_FILENAME = "v1_broader_codes_unexisting_test.xlsx";
    private static final String BROADER_CODESCHEME_1 = "broadercodescheme1";
    private static final String BROADER_CODESCHEME_2 = "broadercodescheme2";
    private TestRestTemplate restTemplate = new TestRestTemplate();

    @LocalServerPort
    private int randomServerPort;

    @Test
    @Transactional
    public void postCodesWithUnexistingBroaderToCodeSchemeTest() {
        final ResponseEntity<String> response = uploadCodeFile(BROADER_CODESCHEME_1, TEST_CODE_BROADER_UNEXISTING_FILENAME, FORMAT_EXCEL);
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
    }

    @Test
    @Transactional
    public void postCodesWithSelfBroaderToCodeSchemeTest() {
        final ResponseEntity<String> response = uploadCodeFile(BROADER_CODESCHEME_2, TEST_CODE_BROADER_SELF_FILENAME, FORMAT_EXCEL);
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
    }

    public ResponseEntity<String> uploadCodeFile(final String codeSchemeCodeValue,
                                                 final String fileName,
                                                 final String format) {
        final String apiUrl = createApiUrl(randomServerPort, API_PATH_CODEREGISTRIES) + TEST_CODEREGISTRY_CODEVALUE + API_PATH_CODESCHEMES + "/" + codeSchemeCodeValue + API_PATH_CODES + "/" + "?format=" + format;
        final LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        final String registryFilePath = "/" + CODES_FOLDER_NAME + "/" + fileName;
        parameters.add("file", new ClassPathResource(registryFilePath));
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        final HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(parameters, headers);
        return restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class, "");
    }
}
