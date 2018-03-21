package fi.vm.yti.codelist.intake.dao;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.OrganizationDTO;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.OrganizationRepository;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.Organization;

@Component
public class CodeRegistryDaoImpl implements CodeRegistryDao {

    private final ApiUtils apiUtils;
    private final CodeRegistryRepository codeRegistryRepository;
    private final OrganizationRepository organizationRepository;

    @Inject
    public CodeRegistryDaoImpl(final ApiUtils apiUtils,
                               final CodeRegistryRepository codeRegistryRepository,
                               final OrganizationRepository organizationRepository) {

        this.apiUtils = apiUtils;
        this.codeRegistryRepository = codeRegistryRepository;
        this.organizationRepository = organizationRepository;
    }

    public Set<CodeRegistry> findAll() {
        return codeRegistryRepository.findAll();
    }

    public CodeRegistry findByCodeValue(final String codeValue) {
        return codeRegistryRepository.findByCodeValue(codeValue);
    }

    @Transactional
    public CodeRegistry updateCodeRegistryFromDto(final CodeRegistryDTO codeRegistryDto) {
        final CodeRegistry codeRegistry = createOrUpdateCodeRegistry(codeRegistryDto);
        if (codeRegistry != null) {
            codeRegistryRepository.save(codeRegistry);
        }
        return codeRegistry;
    }

    @Transactional
    public Set<CodeRegistry> updateCodeRegistriesFromDto(final Set<CodeRegistryDTO> codeRegistryDtos) {
        final Set<CodeRegistry> codeRegistries = new HashSet<>();
        codeRegistryDtos.forEach(codeRegistry -> codeRegistries.add(createOrUpdateCodeRegistry(codeRegistry)));
        if (!codeRegistries.isEmpty()) {
            codeRegistryRepository.save(codeRegistries);
        }
        return codeRegistries;
    }

    private CodeRegistry createOrUpdateCodeRegistry(final CodeRegistryDTO fromCodeRegistry) {
        final CodeRegistry codeRegistry;
        final CodeRegistry existingCodeRegistry = codeRegistryRepository.findByCodeValue(fromCodeRegistry.getCodeValue());
        if (existingCodeRegistry != null) {
            codeRegistry = updateCodeRegistry(existingCodeRegistry, fromCodeRegistry);
        } else {
            codeRegistry = createCodeRegistry(fromCodeRegistry);
        }
        return codeRegistry;
    }

    private CodeRegistry updateCodeRegistry(final CodeRegistry codeRegistry,
                                            final CodeRegistryDTO fromCodeRegistry) {
        final Date timeStamp = new Date(System.currentTimeMillis());
        final String uri = apiUtils.createCodeRegistryUri(codeRegistry);
        final String url = apiUtils.createCodeRegistryUrl(codeRegistry);
        boolean hasChanges = false;
        if (!Objects.equals(codeRegistry.getUri(), uri)) {
            codeRegistry.setUri(uri);
            hasChanges = true;
        }
        if (!Objects.equals(codeRegistry.getUrl(), url)) {
            codeRegistry.setUrl(url);
            hasChanges = true;
        }
        codeRegistry.setOrganizations(resolveOrganizationsFromDtos(fromCodeRegistry.getOrganizations()));
        for (final Map.Entry<String, String> entry : fromCodeRegistry.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(codeRegistry.getPrefLabel(language), value)) {
                codeRegistry.setPrefLabel(language, value);
                hasChanges = true;
            }
        }
        for (final Map.Entry<String, String> entry : fromCodeRegistry.getDefinition().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(codeRegistry.getDefinition(language), value)) {
                codeRegistry.setDefinition(language, value);
                hasChanges = true;
            }
        }
        if (hasChanges) {
            codeRegistry.setModified(timeStamp);
        }
        return codeRegistry;
    }

    private Set<Organization> resolveOrganizationsFromDtos(final Set<OrganizationDTO> organizationDtos) {
        final Set<Organization> organizations = new HashSet<>();
        organizationDtos.forEach(organizationDto -> organizations.add(organizationRepository.findById(organizationDto.getId())));
        return organizations;
    }

    private CodeRegistry createCodeRegistry(final CodeRegistryDTO fromCodeRegistry) {
        final Date timeStamp = new Date(System.currentTimeMillis());
        final CodeRegistry codeRegistry = new CodeRegistry();
        codeRegistry.setId(UUID.randomUUID());
        codeRegistry.setCodeValue(fromCodeRegistry.getCodeValue());
        codeRegistry.setModified(timeStamp);
        codeRegistry.setOrganizations(resolveOrganizationsFromDtos(fromCodeRegistry.getOrganizations()));
        for (Map.Entry<String, String> entry : fromCodeRegistry.getPrefLabel().entrySet()) {
            codeRegistry.setPrefLabel(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : fromCodeRegistry.getDefinition().entrySet()) {
            codeRegistry.setDefinition(entry.getKey(), entry.getValue());
        }
        codeRegistry.setUri(apiUtils.createCodeRegistryUri(codeRegistry));
        codeRegistry.setUrl(apiUtils.createCodeRegistryUrl(codeRegistry));
        return codeRegistry;
    }
}
