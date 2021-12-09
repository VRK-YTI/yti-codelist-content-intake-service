package fi.vm.yti.codelist.intake.integration;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.yti.codelist.intake.ContentIntakeServiceApplication;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { ContentIntakeServiceApplication.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "automatedtest" })
@TestPropertySource(locations = "classpath:test-port.properties")
public class InvalidIdUploadTestT9 extends AbstractIntegrationTestBase {

    private static final String TEST_INVALID_ID_FILENAME_1 = "v1_invalid_id_test.xlsx";
    private static final String INVALID_ID_TEST_CODESCHEME_1 = "invalididcodescheme1";

    @Test
    @Transactional
    public void postCodesWithDuplicateHeaderValuesToCodeSchemeTestExcel() {
        final ResponseEntity<String> response = uploadCodesToCodeSchemeFromExcel(TEST_CODEREGISTRY_CODEVALUE, INVALID_ID_TEST_CODESCHEME_1, TEST_INVALID_ID_FILENAME_1);
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
    }
}
