package fi.vm.yti.codelist.intake.integration;

import javax.inject.Inject;
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
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ContentIntakeServiceApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"automatedtest"})
@TestPropertySource(locations = "classpath:test-port.properties")
public class CodeRegistryResourceT2 extends AbstractIntegrationTestBase {

    public static final String TEST_CODEREGISTRY_FILENAME = "v1_testcoderegistries.csv";

    @Inject
    private CodeRegistryRepository codeRegistryRepository;

    @Test
    @Transactional
    public void postRegistriesTest() {
        final ResponseEntity<String> response = uploadCodeRegistriesFromCsv(TEST_CODEREGISTRY_FILENAME);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(10, codeRegistryRepository.findAll().size());
    }
}
