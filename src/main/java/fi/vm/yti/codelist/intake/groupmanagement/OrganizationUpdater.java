package fi.vm.yti.codelist.intake.groupmanagement;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.base.Stopwatch;

import fi.vm.yti.codelist.intake.model.Organization;
import fi.vm.yti.codelist.intake.configuration.GroupManagementProperties;
import fi.vm.yti.codelist.intake.jpa.OrganizationRepository;
import static fi.vm.yti.codelist.common.constants.ApiConstants.GROUPMANAGEMENT_API_CONTEXT_PATH;
import static fi.vm.yti.codelist.common.constants.ApiConstants.GROUPMANAGEMENT_API_ORGANIZATIONS;

@Component
public class OrganizationUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(OrganizationUpdater.class);
    private GroupManagementProperties groupManagementProperties;
    private OrganizationRepository organizationRepository;

    @Inject
    public OrganizationUpdater(final GroupManagementProperties groupManagementProperties,
                               final OrganizationRepository organizationRepository) {
        this.groupManagementProperties = groupManagementProperties;
        this.organizationRepository = organizationRepository;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void fetchOrganizations() {
        updateOrganizations();
    }

    @Transactional
    public void updateOrganizations() {
        final Stopwatch watch = Stopwatch.createStarted();
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1000);
        requestFactory.setReadTimeout(1000);
        final RestTemplate restTemplate = new RestTemplate(requestFactory);
        final Map<String, String> vars = new HashMap<>();
        try {
            final String response = restTemplate.getForObject(getGroupManagementOrganizationsApiUrl(), String.class, vars);
            final ObjectMapper mapper = new ObjectMapper();
            mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
            Set<GroupManagementOrganization> groupManagementOrganizations = new HashSet<>();
            try {
                groupManagementOrganizations = mapper.readValue(response, new TypeReference<Set<GroupManagementOrganization>>() {
                });
                LOG.info("Organization data loaded: " + groupManagementOrganizations.size() + " Organizations in " + watch);
                watch.reset().start();
            } catch (final IOException e) {
                LOG.error("Organization fetching and processing failed!", e);
            }
            final Set<Organization> organizations = new HashSet<>();
            for (final GroupManagementOrganization groupManagementOrganization : groupManagementOrganizations) {
                final Organization existingOrganization = organizationRepository.findById(groupManagementOrganization.getUuid());
                if (existingOrganization != null) {
                    existingOrganization.setUrl(groupManagementOrganization.getUrl());
                    existingOrganization.setPrefLabel(groupManagementOrganization.getPrefLabel());
                    existingOrganization.setDescription(groupManagementOrganization.getDescription());
                    existingOrganization.setRemoved(groupManagementOrganization.getRemoved());
                    organizations.add(existingOrganization);
                } else {
                    final Organization organization = new Organization();
                    organization.setId(groupManagementOrganization.getUuid());
                    organization.setUrl(groupManagementOrganization.getUrl());
                    organization.setPrefLabel(groupManagementOrganization.getPrefLabel());
                    organization.setDescription(groupManagementOrganization.getDescription());
                    organization.setRemoved(groupManagementOrganization.getRemoved());
                    organizations.add(organization);
                }
            }
            if (!organizations.isEmpty()) {
                organizationRepository.save(organizations);
            }
        } catch (final Exception e) {
            LOG.error("Organization fetching failed due to exception", e);
        }
    }

    private String getGroupManagementOrganizationsApiUrl() {
        return groupManagementProperties.getUrl() + GROUPMANAGEMENT_API_CONTEXT_PATH + GROUPMANAGEMENT_API_ORGANIZATIONS + "/";
    }
}
