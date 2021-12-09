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
public class CodeValueTestT13 extends AbstractIntegrationTestBase {

    private static final String CASE_TEST_CODE_FILENAME = "v1_invalid_code_codevalue.csv";
    private static final String CASE_TEST_CODESCHEME = "invalidacodevaluetest1";

    @Test
    @Transactional
    public void postCodesWithDuplicateValuesToCodeSchemeTest() {
        final ResponseEntity<String> response = uploadCodesToCodeSchemeFromCsv(TEST_CODEREGISTRY_CODEVALUE, CASE_TEST_CODESCHEME, CASE_TEST_CODE_FILENAME);
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
    }
}
