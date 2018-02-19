package fi.vm.yti.codelist.intake.integration;

import javax.transaction.Transactional;

import fi.vm.yti.codelist.intake.ContentIntakeServiceApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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

import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;

import static fi.vm.yti.codelist.common.constants.ApiConstants.API_PATH_CODEREGISTRIES;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_CSV;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ContentIntakeServiceApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test"})
@TestPropertySource(locations = "classpath:test-port.properties")
public class CodeRegistryResourceT2 extends AbstractIntegrationTestBase {

    private TestRestTemplate restTemplate = new TestRestTemplate();

    @LocalServerPort
    private int randomServerPort;

    @Autowired
    private CodeRegistryRepository codeRegistryRepository;

    @Test
    @Transactional
    public void postRegistriesTest() {
        final String apiUrl = createApiUrl(randomServerPort, API_PATH_CODEREGISTRIES) + "?format=" + FORMAT_CSV;
        final LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        final String registryFilePath = "/" + CODEREGISTRIES_FOLDER_NAME + "/" + TEST_CODEREGISTRY_FILENAME;
        parameters.add("file", new ClassPathResource(registryFilePath));
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        final HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<>(parameters, headers);
        final ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class, "");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(9, codeRegistryRepository.findAll().size());
    }
}
