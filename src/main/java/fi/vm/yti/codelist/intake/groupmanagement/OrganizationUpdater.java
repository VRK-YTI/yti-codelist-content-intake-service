package fi.vm.yti.codelist.intake.groupmanagement;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import fi.vm.yti.codelist.common.dto.OrganizationDTO;
import fi.vm.yti.codelist.intake.configuration.GroupManagementProperties;
import fi.vm.yti.codelist.intake.service.OrganizationService;
import static fi.vm.yti.codelist.common.constants.ApiConstants.GROUPMANAGEMENT_API_CONTEXT_PATH;
import static fi.vm.yti.codelist.common.constants.ApiConstants.GROUPMANAGEMENT_API_ORGANIZATIONS;

@Component
public class OrganizationUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(OrganizationUpdater.class);
    private final OrganizationService organizationService;
    private final RestTemplate restTemplate;
    private GroupManagementProperties groupManagementProperties;

    @Inject
    public OrganizationUpdater(final GroupManagementProperties groupManagementProperties,
                               final OrganizationService organizationService,
                               final RestTemplate restTemplate) {
        this.groupManagementProperties = groupManagementProperties;
        this.organizationService = organizationService;
        this.restTemplate = restTemplate;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void fetchOrganizations() {
        updateOrganizations();
    }

    @Transactional
    public void updateOrganizations() {
        final Map<String, String> vars = new HashMap<>();
        try {
            final String response = restTemplate.getForObject(getGroupManagementOrganizationsApiUrl(), String.class, vars);
            final Set<OrganizationDTO> organizations = organizationService.parseAndPersistGroupManagementOrganizationsFromJson(response);
            LOG.info("Successfully synced " + organizations.size() + " from groupmanagement service!");
        } catch (final Exception e) {
            LOG.error("Organization fetching failed due to exception.", e);
        }
    }

    private String getGroupManagementOrganizationsApiUrl() {
        return groupManagementProperties.getUrl() + GROUPMANAGEMENT_API_CONTEXT_PATH + GROUPMANAGEMENT_API_ORGANIZATIONS + "/";
    }
}
