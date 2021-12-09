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
public class CodeDuplicateUploadTestT7 extends AbstractIntegrationTestBase {

    private static final String TEST_CODE_DUPLICATE_FILENAME = "v1_codes_duplicate_test.xlsx";
    private static final String DUPLICATE_TEST_CODESCHEME_1 = "duplicatecodescheme1";

    @Test
    @Transactional
    public void postCodesWithDuplicateValuesToCodeSchemeTest() {
        final ResponseEntity<String> response = uploadCodesToCodeSchemeFromExcel(TEST_CODEREGISTRY_CODEVALUE, DUPLICATE_TEST_CODESCHEME_1, TEST_CODE_DUPLICATE_FILENAME);
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
    }
}
