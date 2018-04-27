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
@ActiveProfiles({"automatedtest"})
@TestPropertySource(locations = "classpath:test-port.properties")
public class DoubleHeaderUploadTestT8 extends AbstractIntegrationTestBase {

    private static final String TEST_CODE_DUPLICATE_HEADER_FILENAME_EXCEL = "v1_duplicate_header_test.xlsx";
    private static final String TEST_CODE_DUPLICATE_HEADER_FILENAME_CSV = "v1_duplicate_header_test.csv";
    private static final String DUPLICATE_HEADER_TEST_CODESCHEME_1 = "duplicateheadercodescheme1";
    private static final String DUPLICATE_HEADER_TEST_CODESCHEME_2 = "duplicateheadercodescheme2";

    @Test
    @Transactional
    public void postCodesWithDuplicateHeaderValuesToCodeSchemeTestExcel() {
        final ResponseEntity<String> response = uploadCodesToCodeSchemeFromCsv(TEST_CODEREGISTRY_CODEVALUE, DUPLICATE_HEADER_TEST_CODESCHEME_1, TEST_CODE_DUPLICATE_HEADER_FILENAME_EXCEL);
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
    }

    @Test
    @Transactional
    public void postCodesWithDuplicateHeaderValuesToCodeSchemeTestCsv() {
        final ResponseEntity<String> response = uploadCodesToCodeSchemeFromCsv(TEST_CODEREGISTRY_CODEVALUE, DUPLICATE_HEADER_TEST_CODESCHEME_2, TEST_CODE_DUPLICATE_HEADER_FILENAME_CSV);
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
    }
}
