package fi.vm.yti.codelist.intake.integration;

import javax.inject.Inject;

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
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { ContentIntakeServiceApplication.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "automatedtest" })
@TestPropertySource(locations = "classpath:test-port.properties")
public class CodeSchemeResourceT3 extends AbstractIntegrationTestBase {

    private static final int CODE_SCHEME_COUNT = 17;
    private static final String NOT_FOUND_REGISTRY_CODEVALUE = "notfoundregistry";
    private static final String TEST_CODESCHEME_FILENAME = "v1_testcodeschemes.csv";

    @Inject
    private CodeRegistryRepository codeRegistryRepository;

    @Inject
    private CodeSchemeRepository codeSchemeRepository;

    @Test
    @Transactional
    public void postCodeSchemesToCodeRegistryTest() {
        final ResponseEntity<String> response = uploadCodeSchemesToCodeRegistryFromCsv(TEST_CODEREGISTRY_CODEVALUE, TEST_CODESCHEME_FILENAME);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValueIgnoreCase(TEST_CODEREGISTRY_CODEVALUE);
        assertNotNull(codeRegistry);
        assertEquals(CODE_SCHEME_COUNT, codeSchemeRepository.findByCodeRegistry(codeRegistry).size());
    }

    @Test
    @Transactional
    public void postCodeSchemesToNotExistingCodeRegistryTest() {
        final ResponseEntity<String> response = uploadCodeSchemesToCodeRegistryFromCsv(NOT_FOUND_REGISTRY_CODEVALUE, TEST_CODESCHEME_FILENAME);
        assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
    }
}
