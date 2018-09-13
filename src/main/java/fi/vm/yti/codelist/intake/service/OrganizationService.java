package fi.vm.yti.codelist.intake.service;

import java.util.Set;
import java.util.UUID;

import fi.vm.yti.codelist.common.dto.OrganizationDTO;

public interface OrganizationService {

    Set<OrganizationDTO> findAll();

    Set<OrganizationDTO> findByRemovedIsFalse(boolean onlyOrganizationsWithCodeSchemes);

    OrganizationDTO findById(final UUID organizationId);

    Set<OrganizationDTO> parseAndPersistGroupManagementOrganizationsFromJson(final String jsonPayload);
}
