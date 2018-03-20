package fi.vm.yti.codelist.intake.integration;

import javax.transaction.Transactional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import fi.vm.yti.codelist.intake.ContentIntakeServiceApplication;
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

    @Test
    @Transactional
    public void postCodesWithUnexistingBroaderToCodeSchemeTest() {
        final ResponseEntity<String> response = uploadCodesToCodeSchemeFromExcel(TEST_CODEREGISTRY_CODEVALUE, BROADER_CODESCHEME_1, TEST_CODE_BROADER_UNEXISTING_FILENAME);
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
    }

    @Test
    @Transactional
    public void postCodesWithSelfBroaderToCodeSchemeTest() {
        final ResponseEntity<String> response = uploadCodesToCodeSchemeFromExcel(TEST_CODEREGISTRY_CODEVALUE, BROADER_CODESCHEME_2, TEST_CODE_BROADER_SELF_FILENAME);
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
    }
}