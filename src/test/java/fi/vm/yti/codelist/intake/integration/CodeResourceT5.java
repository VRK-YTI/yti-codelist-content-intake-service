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

import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.ContentIntakeServiceApplication;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ContentIntakeServiceApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"automatedtest"})
@TestPropertySource(locations = "classpath:test-port.properties")
public class CodeResourceT5 extends AbstractIntegrationTestBase {

    private static final String TEST_CODE_FILENAME = "v1_testcodes.csv";
    private static final String TEST_CODESCHEME_CODEVALUE = "testscheme1";

    @Inject
    private CodeRegistryRepository codeRegistryRepository;

    @Inject
    private CodeSchemeRepository codeSchemeRepository;

    @Inject
    private CodeRepository codeRepository;

    @Test
    @Transactional
    public void postCodesToCodeSchemeTest() {
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValueIgnoreCase(TEST_CODEREGISTRY_CODEVALUE);
        assertNotNull(codeRegistry);
        final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryCodeValueIgnoreCaseAndCodeValueIgnoreCase(codeRegistry.getCodeValue(), TEST_CODESCHEME_CODEVALUE);
        assertNotNull(codeScheme);
        final ResponseEntity<String> response = uploadCodesToCodeSchemeFromCsv(codeRegistry.getCodeValue(), codeScheme.getCodeValue(), TEST_CODE_FILENAME);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(8, codeRepository.findByCodeSchemeId(codeScheme.getId()).size());
    }
}
