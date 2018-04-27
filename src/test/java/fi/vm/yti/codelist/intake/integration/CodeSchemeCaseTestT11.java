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
public class CodeSchemeCaseTestT11 extends AbstractIntegrationTestBase {

    private static final String TEST_CODESCHEME_FILENAME = "v1_case_insensitive_schemes.csv";

    @Test
    @Transactional
    public void postCodeSchemesToCodeRegistryTest() {
        final ResponseEntity<String> response = uploadCodeSchemesToCodeRegistryFromCsv(TEST_CODEREGISTRY_CODEVALUE, TEST_CODESCHEME_FILENAME);
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
    }
}
