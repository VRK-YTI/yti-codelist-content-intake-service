package fi.vm.yti.codelist.intake.service.impl;

import java.util.HashSet;
import java.util.Set;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.OrganizationDTO;
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.model.Organization;
import fi.vm.yti.codelist.intake.model.PropertyType;

abstract class BaseService {

    public CodeDTO mapDeepCodeDto(final Code code) {
        return mapCodeDto(code, true);
    }

    public CodeDTO mapCodeDto(final Code code,
                              final boolean deep) {
        final CodeDTO codeDto = new CodeDTO();
        codeDto.setId(code.getId());
        codeDto.setCodeValue(code.getCodeValue());
        codeDto.setUri(code.getUri());
        codeDto.setUrl(code.getUrl());
        codeDto.setBroaderCodeId(code.getBroaderCodeId());
        codeDto.setStartDate(code.getStartDate());
        codeDto.setEndDate(code.getEndDate());
        codeDto.setStatus(code.getStatus());
        codeDto.setHierarchyLevel(code.getHierarchyLevel());
        codeDto.setShortName(code.getShortName());
        codeDto.setPrefLabel(code.getPrefLabel());
        codeDto.setDefinition(code.getDefinition());
        codeDto.setCodeScheme(mapCodeSchemeDto(code.getCodeScheme(), false));
        codeDto.setConceptUriInVocabularies(code.getConceptUriInVocabularies());
        if (deep) {
            if (code.getExternalReferences() != null) {
                codeDto.setExternalReferences(mapExternalReferenceDtos(code.getExternalReferences(), false));
            }
            if (code.getExtensions() != null) {
                codeDto.setExtensions(mapExtensionDtos(code.getExtensions(), false));
            }
        }
        codeDto.setDescription(code.getDescription());
        codeDto.setOrder(code.getOrder());
        return codeDto;
    }

    public Set<CodeDTO> mapDeepCodeDtos(final Set<Code> codes) {
        return mapCodeDtos(codes, true);
    }

    public Set<CodeDTO> mapCodeDtos(final Set<Code> codes,
                                    final boolean deep) {
        final Set<CodeDTO> codeDtos = new HashSet<>();
        if (codes != null) {
            for (final Code code : codes) {
                codeDtos.add(mapCodeDto(code, deep));
            }
        }
        return codeDtos;
    }

    public CodeSchemeDTO mapDeepCodeSchemeDto(final CodeScheme codeScheme) {
        return mapCodeSchemeDto(codeScheme, true);
    }

    public CodeSchemeDTO mapCodeSchemeDto(final CodeScheme codeScheme,
                                          final boolean deep) {
        final CodeSchemeDTO codeSchemeDto = new CodeSchemeDTO();
        codeSchemeDto.setId(codeScheme.getId());
        codeSchemeDto.setCodeValue(codeScheme.getCodeValue());
        codeSchemeDto.setUri(codeScheme.getUri());
        codeSchemeDto.setUrl(codeScheme.getUrl());
        codeSchemeDto.setChangeNote(codeScheme.getChangeNote());
        codeSchemeDto.setStartDate(codeScheme.getStartDate());
        codeSchemeDto.setEndDate(codeScheme.getEndDate());
        codeSchemeDto.setStatus(codeScheme.getStatus());
        codeSchemeDto.setDefinition(codeScheme.getDefinition());
        codeSchemeDto.setPrefLabel(codeScheme.getPrefLabel());
        codeSchemeDto.setDescription(codeScheme.getDescription());
        codeSchemeDto.setCodeRegistry(mapCodeRegistryDto(codeScheme.getCodeRegistry()));
        codeSchemeDto.setVersion(codeScheme.getVersion());
        codeSchemeDto.setGovernancePolicy(codeScheme.getGovernancePolicy());
        codeSchemeDto.setLegalBase(codeScheme.getLegalBase());
        codeSchemeDto.setConceptUriInVocabularies(codeScheme.getConceptUriInVocabularies());
        if (deep) {
            if (codeScheme.getDataClassifications() != null) {
                codeSchemeDto.setDataClassifications(mapCodeDtos(codeScheme.getDataClassifications(), false));
            }
            if (codeScheme.getExternalReferences() != null) {
                codeSchemeDto.setExternalReferences(mapExternalReferenceDtos(codeScheme.getExternalReferences(), false));
            }
            if (codeScheme.getExtensionSchemes() != null) {
                codeSchemeDto.setExtensionSchemes(mapExtensionSchemeDtos(codeScheme.getExtensionSchemes(), false));
            }
        }
        return codeSchemeDto;
    }

    public Set<CodeSchemeDTO> mapDeepCodeSchemeDtos(final Set<CodeScheme> codeSchemes) {
        return mapCodeSchemeDtos(codeSchemes, true);
    }

    public Set<CodeSchemeDTO> mapCodeSchemeDtos(final Set<CodeScheme> codeSchemes,
                                                final boolean deep) {
        final Set<CodeSchemeDTO> codeSchemeDTOS = new HashSet<>();
        if (codeSchemes != null) {
            for (final CodeScheme codeScheme : codeSchemes) {
                codeSchemeDTOS.add(mapCodeSchemeDto(codeScheme, deep));
            }
        }
        return codeSchemeDTOS;
    }

    public CodeRegistryDTO mapCodeRegistryDto(final CodeRegistry codeRegistry) {
        final CodeRegistryDTO codeRegistryDto = new CodeRegistryDTO();
        codeRegistryDto.setId(codeRegistry.getId());
        codeRegistryDto.setCodeValue(codeRegistry.getCodeValue());
        codeRegistryDto.setUri(codeRegistry.getUri());
        codeRegistryDto.setUrl(codeRegistry.getUrl());
        codeRegistryDto.setPrefLabel(codeRegistry.getPrefLabel());
        codeRegistryDto.setDefinition(codeRegistry.getDefinition());
        codeRegistryDto.setOrganizations(mapOrganizationDtos(codeRegistry.getOrganizations(), false));
        return codeRegistryDto;
    }

    public Set<CodeRegistryDTO> mapCodeRegistryDtos(final Set<CodeRegistry> codeRegistries) {
        final Set<CodeRegistryDTO> codeRegistryDtos = new HashSet<>();
        if (codeRegistries != null) {
            for (final CodeRegistry codeRegistry : codeRegistries) {
                codeRegistryDtos.add(mapCodeRegistryDto(codeRegistry));
            }
        }
        return codeRegistryDtos;
    }

    public ExternalReferenceDTO mapDeepExternalReferenceDto(final ExternalReference externalReference) {
        return mapExternalReferenceDto(externalReference, true);
    }

    public ExternalReferenceDTO mapExternalReferenceDto(final ExternalReference externalReference,
                                                        final boolean deep) {
        final ExternalReferenceDTO externalReferenceDto = new ExternalReferenceDTO();
        externalReferenceDto.setId(externalReference.getId());
        externalReferenceDto.setDescription(externalReference.getDescription());
        externalReferenceDto.setGlobal(externalReference.getGlobal());
        externalReferenceDto.setTitle(externalReference.getTitle());
        externalReferenceDto.setUrl(externalReference.getUrl());
        externalReferenceDto.setUri(externalReference.getUri());
        externalReferenceDto.setPropertyType(mapPropertyTypeDto(externalReference.getPropertyType()));
        if (externalReference.getParentCodeScheme() != null) {
            externalReferenceDto.setParentCodeScheme(mapCodeSchemeDto(externalReference.getParentCodeScheme(), false));
        }
        if (deep) {
            if (externalReference.getCodeSchemes() != null) {
                externalReferenceDto.setCodeSchemes(mapCodeSchemeDtos(externalReference.getCodeSchemes(), false));
            }
            if (externalReference.getCodes() != null) {
                externalReferenceDto.setCodes(mapCodeDtos(externalReference.getCodes(), false));
            }
        }
        return externalReferenceDto;
    }

    public Set<ExternalReferenceDTO> mapDeepExternalReferenceDtos(final Set<ExternalReference> externalReferences) {
        return mapExternalReferenceDtos(externalReferences, true);
    }

    public Set<ExternalReferenceDTO> mapExternalReferenceDtos(final Set<ExternalReference> externalReferences,
                                                              final boolean deep) {
        final Set<ExternalReferenceDTO> externalReferenceDtos = new HashSet<>();
        if (externalReferences != null) {
            for (final ExternalReference externalReference : externalReferences) {
                externalReferenceDtos.add(mapExternalReferenceDto(externalReference, deep));
            }
        }
        return externalReferenceDtos;
    }

    public PropertyTypeDTO mapPropertyTypeDto(final PropertyType propertyType) {
        final PropertyTypeDTO propertyTypeDto = new PropertyTypeDTO();
        propertyTypeDto.setId(propertyType.getId());
        propertyTypeDto.setContext(propertyType.getContext());
        propertyTypeDto.setDefinition(propertyType.getDefinition());
        propertyTypeDto.setLocalName(propertyType.getLocalName());
        propertyTypeDto.setPrefLabel(propertyType.getPrefLabel());
        propertyTypeDto.setType(propertyType.getType());
        propertyTypeDto.setUri(propertyType.getUri());
        propertyTypeDto.setPropertyUri(propertyType.getPropertyUri());
        return propertyTypeDto;
    }

    public Set<PropertyTypeDTO> mapPropertyTypeDtos(final Set<PropertyType> propertyTypes) {
        final Set<PropertyTypeDTO> propertyTypeDtos = new HashSet<>();
        if (propertyTypes != null) {
            for (final PropertyType propertyType : propertyTypes) {
                propertyTypeDtos.add(mapPropertyTypeDto(propertyType));
            }
        }
        return propertyTypeDtos;
    }

    public ExtensionDTO mapDeepExtensionDto(final Extension extension) {
        return mapExtensionDto(extension, true);
    }

    public ExtensionDTO mapExtensionDto(final Extension extension,
                                        final boolean deep) {
        final ExtensionDTO extensionDto = new ExtensionDTO();
        extensionDto.setId(extension.getId());
        extensionDto.setExtensionOrder(extension.getExtensionOrder());
        extensionDto.setExtensionValue(extension.getExtensionValue());
        if (deep) {
            if (extension.getExtensionScheme() != null) {
                extensionDto.setExtensionScheme(mapExtensionSchemeDto(extension.getExtensionScheme(), false));
            }
            if (extension.getCode() != null) {
                extensionDto.setCode(mapCodeDto(extension.getCode(), false));
            }
        }
        return extensionDto;
    }

    public Set<ExtensionDTO> mapDeepExtensionDtos(final Set<Extension> extensions) {
        return mapExtensionDtos(extensions, true);
    }

    public Set<ExtensionDTO> mapExtensionDtos(final Set<Extension> extensions,
                                              final boolean deep) {
        final Set<ExtensionDTO> extensionDtos = new HashSet<>();
        if (extensions != null) {
            for (final Extension extension : extensions) {
                extensionDtos.add(mapExtensionDto(extension, deep));
            }
        }
        return extensionDtos;
    }

    public ExtensionSchemeDTO mapDeepExtensionSchemeDto(final ExtensionScheme extensionScheme) {
        return mapExtensionSchemeDto(extensionScheme, true);
    }

    public ExtensionSchemeDTO mapExtensionSchemeDto(final ExtensionScheme extensionScheme,
                                                    final boolean deep) {
        final ExtensionSchemeDTO extensionSchemeDto = new ExtensionSchemeDTO();
        extensionSchemeDto.setId(extensionScheme.getId());
        extensionSchemeDto.setPropertyType(mapPropertyTypeDto(extensionScheme.getPropertyType()));
        extensionSchemeDto.setPrefLabel(extensionScheme.getPrefLabel());
        extensionSchemeDto.setStatus(extensionScheme.getStatus());
        extensionScheme.setCodeValue(extensionScheme.getCodeValue());
        if (deep) {
            if (extensionScheme.getCodeSchemes() != null) {
                extensionSchemeDto.setCodeSchemes(mapCodeSchemeDtos(extensionScheme.getCodeSchemes(), false));
            }
            if (extensionScheme.getExtensions() != null) {
                extensionSchemeDto.setExtensions(mapExtensionDtos(extensionScheme.getExtensions(), false));
            }
        }
        return extensionSchemeDto;
    }

    public Set<ExtensionSchemeDTO> mapDeepExtensionSchemeDtos(final Set<ExtensionScheme> extensionSchemes) {
        return mapExtensionSchemeDtos(extensionSchemes, true);
    }

    public Set<ExtensionSchemeDTO> mapExtensionSchemeDtos(final Set<ExtensionScheme> extensionSchemes,
                                                          final boolean deep) {
        final Set<ExtensionSchemeDTO> extensionSchemeDtos = new HashSet<>();
        if (extensionSchemes != null) {
            for (final ExtensionScheme extensionScheme : extensionSchemes) {
                extensionSchemeDtos.add(mapExtensionSchemeDto(extensionScheme, deep));
            }
        }
        return extensionSchemeDtos;
    }

    public OrganizationDTO mapOrganizationDto(final Organization organization,
                                              final boolean deep) {
        final OrganizationDTO organizationDto = new OrganizationDTO();
        organizationDto.setId(organization.getId());
        organizationDto.setRemoved(organization.getRemoved());
        organizationDto.setUrl(organization.getUrl());
        organizationDto.setDescription(organization.getDescription());
        organizationDto.setPrefLabel(organization.getPrefLabel());
        if (deep) {
            if (organization.getCodeRegistries() != null) {
                organizationDto.setCodeRegistries(mapCodeRegistryDtos(organization.getCodeRegistries()));
            }
        }
        return organizationDto;
    }

    public Set<OrganizationDTO> mapOrganizationDtos(final Set<Organization> organizations,
                                                    final boolean deep) {
        final Set<OrganizationDTO> organizationDtos = new HashSet<>();
        if (organizations != null) {
            for (final Organization organization : organizations) {
                organizationDtos.add(mapOrganizationDto(organization, deep));
            }
        }
        return organizationDtos;
    }
}
