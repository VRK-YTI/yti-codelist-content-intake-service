package fi.vm.yti.codelist.intake.integration;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import fi.vm.yti.codelist.intake.ContentIntakeServiceApplication;
import fi.vm.yti.codelist.intake.data.YtiDataAccess;
import fi.vm.yti.codelist.intake.groupmanagement.OrganizationUpdater;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { ContentIntakeServiceApplication.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "automatedtest" })
@TestPropertySource(locations = "classpath:test-port.properties")
public class InitializeDataT1 extends AbstractIntegrationTestBase {

    @Inject
    private YtiDataAccess ytiDataAccess;

    @Inject
    private OrganizationUpdater organizationUpdater;

    @Inject
    private CodeRegistryRepository codeRegistryRepository;

    @Test
    public void initializeDataTest() {
        organizationUpdater.updateOrganizations();
        ytiDataAccess.initializeDefaultData();
        assertEquals(2, codeRegistryRepository.findAll().size());
    }
}
