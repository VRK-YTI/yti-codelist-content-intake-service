package fi.vm.yti.codelist.intake.groupmanagement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

import fi.vm.yti.codelist.common.model.Organization;
import fi.vm.yti.codelist.intake.configuration.GroupManagementProperties;
import fi.vm.yti.codelist.intake.jpa.OrganizationRepository;

@Component
public class OrganizationUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(OrganizationUpdater.class);
    private static final String GROUPMANAGEMENT_PUBLIC_API = "/public-api";
    private static final String GROUPMANAGEMENT_API_ORGANIZATIONS = "/organizations";
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
        final String response = restTemplate.getForObject(getOrganizationApiUrl(), String.class, vars);
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        List<GroupManagementOrganization> groupManagementOrganizations = new ArrayList<>();
        try {
            groupManagementOrganizations = mapper.readValue(response, new TypeReference<List<GroupManagementOrganization>>() {
            });
            LOG.info("Organization data loaded: " + groupManagementOrganizations.size() + " Organizations in " + watch);
            watch.reset().start();
        } catch (IOException e) {
            LOG.error("Organization fetching and prcessing failed!", e.getMessage());
        }
        final Set<Organization> organizations = new HashSet<>();
        for (final GroupManagementOrganization groupManagementOrganization : groupManagementOrganizations) {
            final Organization organization = new Organization();
            organization.setId(groupManagementOrganization.getUuid());
            organization.setUrl(groupManagementOrganization.getUrl());
            organization.setPrefLabels(groupManagementOrganization.getPrefLabel());
            organization.setDescriptions(groupManagementOrganization.getDescription());
            organizations.add(organization);
        }
        if (!organizations.isEmpty()) {
            organizationRepository.save(organizations);
        }
    }

    private String getOrganizationApiUrl() {
        return groupManagementProperties.getUrl() + GROUPMANAGEMENT_PUBLIC_API + GROUPMANAGEMENT_API_ORGANIZATIONS + "/";
    }
}
