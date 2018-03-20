package fi.vm.yti.codelist.intake.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.base.Stopwatch;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.dto.OrganizationDTO;
import fi.vm.yti.codelist.intake.groupmanagement.GroupManagementOrganizationDTO;
import fi.vm.yti.codelist.intake.jpa.OrganizationRepository;
import fi.vm.yti.codelist.intake.model.Organization;

@Component
public class OrganizationService extends BaseService {

    private static final Logger LOG = LoggerFactory.getLogger(OrganizationService.class);
    private OrganizationRepository organizationRepository;

    public OrganizationService(final OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public Set<OrganizationDTO> findAll() {
        return mapOrganizationDtos(organizationRepository.findAll(), false);
    }

    @Transactional
    public OrganizationDTO findById(final UUID organizationId) {
        return mapOrganizationDto(organizationRepository.findById(organizationId), false);
    }

    @Transactional
    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    public Set<OrganizationDTO> parseAndPersistGroupManagementOrganizationsFromJson(final String jsonPayload) {
        final Stopwatch watch = Stopwatch.createStarted();
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        Set<GroupManagementOrganizationDTO> groupManagementOrganizations = new HashSet<>();
        try {
            groupManagementOrganizations = mapper.readValue(jsonPayload, new TypeReference<Set<GroupManagementOrganizationDTO>>() {
            });
            LOG.info("Organization data loaded: " + groupManagementOrganizations.size() + " Organizations in " + watch);
            watch.reset().start();
        } catch (final IOException e) {
            LOG.error("Organization fetching and processing failed!", e);
        }
        final Set<Organization> organizations = new HashSet<>();
        for (final GroupManagementOrganizationDTO groupManagementOrganization : groupManagementOrganizations) {
            final Organization organization = createOrUpdateOrganizationFromGroupManagementOrganizationDto(groupManagementOrganization);
            final Integer size = organization.getCodeRegistries().size();
            organizations.add(organization);
        }
        if (!organizations.isEmpty()) {
            organizationRepository.save(organizations);
        }
        return mapOrganizationDtos(organizations, true);
    }

    private Organization createOrUpdateOrganizationFromGroupManagementOrganizationDto(final GroupManagementOrganizationDTO groupManagementOrganizationDto) {
        final Organization existingOrganization = organizationRepository.findById(groupManagementOrganizationDto.getUuid());
        final Organization organization;
        if (existingOrganization != null) {
            existingOrganization.setUrl(groupManagementOrganizationDto.getUrl());
            existingOrganization.setPrefLabel(groupManagementOrganizationDto.getPrefLabel());
            existingOrganization.setDescription(groupManagementOrganizationDto.getDescription());
            existingOrganization.setRemoved(groupManagementOrganizationDto.getRemoved());
            organization = existingOrganization;
        } else {
            organization = new Organization();
            organization.setId(groupManagementOrganizationDto.getUuid());
            organization.setUrl(groupManagementOrganizationDto.getUrl());
            organization.setPrefLabel(groupManagementOrganizationDto.getPrefLabel());
            organization.setDescription(groupManagementOrganizationDto.getDescription());
            organization.setRemoved(groupManagementOrganizationDto.getRemoved());
        }
        return organization;
    }
}
