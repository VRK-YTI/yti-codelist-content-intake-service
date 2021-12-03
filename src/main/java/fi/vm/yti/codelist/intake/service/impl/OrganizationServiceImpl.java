package fi.vm.yti.codelist.intake.service.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.base.Stopwatch;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.dto.OrganizationDTO;
import fi.vm.yti.codelist.intake.dto.GroupManagementOrganizationDTO;
import fi.vm.yti.codelist.intake.jpa.OrganizationRepository;
import fi.vm.yti.codelist.intake.model.Organization;
import fi.vm.yti.codelist.intake.service.OrganizationService;

@Singleton
@Service
public class OrganizationServiceImpl implements OrganizationService {

    private static final Logger LOG = LoggerFactory.getLogger(OrganizationServiceImpl.class);
    private final OrganizationRepository organizationRepository;
    private final DtoMapperService dtoMapperService;

    @Inject
    public OrganizationServiceImpl(final OrganizationRepository organizationRepository,
                                   final DtoMapperService dtoMapperService) {
        this.organizationRepository = organizationRepository;
        this.dtoMapperService = dtoMapperService;
    }

    @Transactional
    public Set<OrganizationDTO> findAll() {
        return dtoMapperService.mapOrganizationDtos(organizationRepository.findAll(), true);
    }

    @Transactional
    public Set<OrganizationDTO> findByRemovedIsFalse(boolean onlyOrganizationsWithCodeSchemes) {
        Set<Organization> organizations;
        if (onlyOrganizationsWithCodeSchemes) {
            organizations = organizationRepository.findByRemovedIsFalseAndCodeSchemesIsNotNullAndParentIsNull();
        } else {
            organizations = organizationRepository.findByRemovedIsFalse();
        }
        return dtoMapperService.mapOrganizationDtos(organizations, true);
    }

    @Transactional
    public OrganizationDTO findById(final UUID organizationId) {
        return dtoMapperService.mapOrganizationDto(organizationRepository.findById(organizationId), true);
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
            if (organization.getCodeRegistries() != null) {
                // Below used to resolve lazy
                final Integer size = organization.getCodeRegistries().size();
            }
            organizations.add(organization);
        }
        if (!organizations.isEmpty()) {
            organizationRepository.saveAll(organizations);
        }
        return dtoMapperService.mapOrganizationDtos(organizations, true);
    }

    private Organization createOrUpdateOrganizationFromGroupManagementOrganizationDto(final GroupManagementOrganizationDTO groupManagementOrganizationDto) {
        final Organization existingOrganization = organizationRepository.findById(groupManagementOrganizationDto.getUuid());
        final Organization organization;
        if (existingOrganization != null) {
            existingOrganization.setUrl(groupManagementOrganizationDto.getUrl());
            existingOrganization.setPrefLabel(groupManagementOrganizationDto.getPrefLabel());
            existingOrganization.setDescription(groupManagementOrganizationDto.getDescription());
            existingOrganization.setRemoved(groupManagementOrganizationDto.getRemoved());
            existingOrganization.setParent(findParentOrganization(groupManagementOrganizationDto));
            organization = existingOrganization;
        } else {
            organization = new Organization();
            organization.setId(groupManagementOrganizationDto.getUuid());
            organization.setUrl(groupManagementOrganizationDto.getUrl());
            organization.setPrefLabel(groupManagementOrganizationDto.getPrefLabel());
            organization.setDescription(groupManagementOrganizationDto.getDescription());
            organization.setRemoved(groupManagementOrganizationDto.getRemoved());
            organization.setParent(findParentOrganization(groupManagementOrganizationDto));
        }
        return organization;
    }

    private Organization findParentOrganization(GroupManagementOrganizationDTO groupManagementOrganizationDTO) {
        if (groupManagementOrganizationDTO.getParentId() != null) {
            Organization parent = organizationRepository.findById(groupManagementOrganizationDTO.getParentId());
            if (parent == null) {
                LOG.warn("Parent organization does not exist {}", groupManagementOrganizationDTO.getParentId());
            } else {
                return parent;
            }
        }
        return null;
    }
}
