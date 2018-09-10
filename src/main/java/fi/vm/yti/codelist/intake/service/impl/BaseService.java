package fi.vm.yti.codelist.intake.service.impl;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.transaction.Transactional;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.OrganizationDTO;
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.common.model.CodeSchemeListItem;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.model.Organization;
import fi.vm.yti.codelist.intake.model.PropertyType;

public abstract class BaseService {

    protected final ApiUtils apiUtils;

    public BaseService(final ApiUtils apiUtils) {
        this.apiUtils = apiUtils;
    }

    @Transactional
    public CodeDTO mapDeepCodeDto(final Code code) {
        return mapCodeDto(code, true, true);
    }

    @Transactional
    public CodeDTO mapCodeDto(final Code code,
                              final boolean deep,
                              final boolean includeCodeScheme) {
        final CodeDTO codeDto = new CodeDTO();
        codeDto.setId(code.getId());
        codeDto.setCodeValue(code.getCodeValue());
        codeDto.setUri(code.getUri());
        codeDto.setBroaderCodeId(code.getBroaderCodeId());
        codeDto.setStartDate(code.getStartDate());
        codeDto.setEndDate(code.getEndDate());
        codeDto.setStatus(code.getStatus());
        codeDto.setHierarchyLevel(code.getHierarchyLevel());
        codeDto.setShortName(code.getShortName());
        codeDto.setPrefLabel(code.getPrefLabel());
        codeDto.setDefinition(code.getDefinition());
        if (includeCodeScheme) {
            codeDto.setCodeScheme(mapCodeSchemeDto(code.getCodeScheme(), false));
            codeDto.setUrl(apiUtils.createCodeUrl(codeDto));
        } else {
            codeDto.setUrl(apiUtils.createCodeUrl(code.getCodeScheme().getCodeRegistry().getCodeValue(), code.getCodeScheme().getCodeValue(), codeDto.getCodeValue()));
        }
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
        codeDto.setCreated(code.getCreated());
        codeDto.setModified(code.getModified());
        return codeDto;
    }

    @Transactional
    public Set<CodeDTO> mapDeepCodeDtos(final Set<Code> codes) {
        return mapCodeDtos(codes, true, true);
    }

    @Transactional
    public Set<CodeDTO> mapCodeDtos(final Set<Code> codes,
                                    final boolean deep,
                                    final boolean includeCodeScheme) {
        final Set<CodeDTO> codeDtos = new HashSet<>();
        if (codes != null && !codes.isEmpty()) {
            codes.forEach(code -> codeDtos.add(mapCodeDto(code, deep, includeCodeScheme)));
        }
        return codeDtos;
    }

    @Transactional
    public CodeSchemeDTO mapDeepCodeSchemeDto(final CodeScheme codeScheme) {
        return mapCodeSchemeDto(codeScheme, true);
    }

    @Transactional
    public CodeSchemeDTO mapCodeSchemeDto(final CodeScheme codeScheme,
                                          final boolean deep) {
        final CodeSchemeDTO codeSchemeDto = new CodeSchemeDTO();
        codeSchemeDto.setId(codeScheme.getId());
        codeSchemeDto.setCodeValue(codeScheme.getCodeValue());
        codeSchemeDto.setUri(codeScheme.getUri());
        codeSchemeDto.setChangeNote(codeScheme.getChangeNote());
        codeSchemeDto.setStartDate(codeScheme.getStartDate());
        codeSchemeDto.setEndDate(codeScheme.getEndDate());
        codeSchemeDto.setStatus(codeScheme.getStatus());
        codeSchemeDto.setSource(codeScheme.getSource());
        codeSchemeDto.setDefinition(codeScheme.getDefinition());
        codeSchemeDto.setPrefLabel(codeScheme.getPrefLabel());
        codeSchemeDto.setDescription(codeScheme.getDescription());
        codeSchemeDto.setCodeRegistry(mapCodeRegistryDto(codeScheme.getCodeRegistry()));
        codeSchemeDto.setVersion(codeScheme.getVersion());
        codeSchemeDto.setGovernancePolicy(codeScheme.getGovernancePolicy());
        codeSchemeDto.setLegalBase(codeScheme.getLegalBase());
        codeSchemeDto.setConceptUriInVocabularies(codeScheme.getConceptUriInVocabularies());
        if (codeScheme.getLanguageCodes() != null) {
            codeSchemeDto.setLanguageCodes(mapCodeDtos(codeScheme.getLanguageCodes(), false, false));
        }
        if (deep) {
            if (codeScheme.getDefaultCode() != null) {
                codeSchemeDto.setDefaultCode(mapCodeDto(codeScheme.getDefaultCode(), false, true));
            }
            if (codeScheme.getDataClassifications() != null) {
                codeSchemeDto.setDataClassifications(mapCodeDtos(codeScheme.getDataClassifications(), false, true));
            }
            if (codeScheme.getExternalReferences() != null) {
                codeSchemeDto.setExternalReferences(mapExternalReferenceDtos(codeScheme.getExternalReferences(), false));
            }
            if (codeScheme.getExtensionSchemes() != null) {
                codeSchemeDto.setExtensionSchemes(mapExtensionSchemeDtos(codeScheme.getExtensionSchemes(), false));
            }
        }
        if (!codeScheme.getVariants().isEmpty()) {
            codeSchemeDto.setVariantsOfThisCodeScheme(getVariantsOfCodeSchemeAsListItems(codeScheme));
        }
        if (!codeScheme.getVariantMothers().isEmpty()) {
            codeSchemeDto.setVariantMothersOfThisCodeScheme(getVariantMothersOfCodeSchemeAsListItems(codeScheme));
        }

        codeSchemeDto.setUrl(apiUtils.createCodeSchemeUrl(codeSchemeDto));
        codeSchemeDto.setCreated(codeScheme.getCreated());
        codeSchemeDto.setModified(codeScheme.getModified());
        codeSchemeDto.setPrevCodeschemeId(codeScheme.getPrevCodeschemeId());
        codeSchemeDto.setNextCodeschemeId(codeScheme.getNextCodeschemeId());
        codeSchemeDto.setLastCodeschemeId(codeScheme.getLastCodeschemeId());
        return codeSchemeDto;
    }

    @Transactional
    public LinkedHashSet<CodeSchemeListItem> getVariantsOfCodeSchemeAsListItems(final CodeScheme codeScheme) {
        LinkedHashSet<CodeSchemeListItem> result = new LinkedHashSet<>();
        if (!codeScheme.getVariants().isEmpty()) {
            for (CodeScheme variant : codeScheme.getVariants()) {
                CodeSchemeListItem item = new CodeSchemeListItem(variant.getId(), variant.getPrefLabel(),
                        variant.getUri(), variant.getStartDate(), variant.getEndDate(), variant.getStatus());
                result.add(item);
            }
        }
        return result;
    }

    @Transactional
    public LinkedHashSet<CodeSchemeListItem> getVariantMothersOfCodeSchemeAsListItems(final CodeScheme codeScheme) {
        LinkedHashSet<CodeSchemeListItem> result = new LinkedHashSet<>();
        if (!codeScheme.getVariantMothers().isEmpty()) {
            for (CodeScheme variantMother : codeScheme.getVariantMothers()) {
                CodeSchemeListItem item = new CodeSchemeListItem(variantMother.getId(), variantMother.getPrefLabel(),
                        variantMother.getUri(), variantMother.getStartDate(), variantMother.getEndDate(), variantMother.getStatus());
                result.add(item);
            }
        }
        return result;
    }

    @Transactional
    public Set<CodeSchemeDTO> mapVariantDtos(final Set<CodeScheme> variants) {
        final Set<CodeSchemeDTO> codeSchemeDtos = new HashSet<>();
        if (variants != null && !variants.isEmpty()) {
            variants.forEach(variant -> codeSchemeDtos.add(mapCodeSchemeDto(variant, false)));
        }
        return codeSchemeDtos;
    }

    @Transactional
    public Set<CodeSchemeDTO> mapVariantMotherDtos(final Set<CodeScheme> variantMothers) {
        final Set<CodeSchemeDTO> codeSchemeDtos = new HashSet<>();
        if (variantMothers != null && !variantMothers.isEmpty()) {
            variantMothers.forEach(variantMother -> codeSchemeDtos.add(mapCodeSchemeDto(variantMother, false)));
        }
        return codeSchemeDtos;
    }

    @Transactional
    public Set<CodeSchemeDTO> mapDeepCodeSchemeDtos(final Set<CodeScheme> codeSchemes) {
        return mapCodeSchemeDtos(codeSchemes, true);
    }

    @Transactional
    public Set<CodeSchemeDTO> mapCodeSchemeDtos(final Set<CodeScheme> codeSchemes,
                                                final boolean deep) {
        final Set<CodeSchemeDTO> codeSchemeDtos = new HashSet<>();

        if (codeSchemes != null && !codeSchemes.isEmpty()) {
            codeSchemes.forEach(codeScheme -> codeSchemeDtos.add(mapCodeSchemeDto(codeScheme, deep)));
        }
        return codeSchemeDtos;
    }

    @Transactional
    public CodeRegistryDTO mapCodeRegistryDto(final CodeRegistry codeRegistry) {
        final CodeRegistryDTO codeRegistryDto = new CodeRegistryDTO();
        codeRegistryDto.setId(codeRegistry.getId());
        codeRegistryDto.setCodeValue(codeRegistry.getCodeValue());
        codeRegistryDto.setUri(codeRegistry.getUri());
        codeRegistryDto.setPrefLabel(codeRegistry.getPrefLabel());
        codeRegistryDto.setDefinition(codeRegistry.getDefinition());
        codeRegistryDto.setOrganizations(mapOrganizationDtos(codeRegistry.getOrganizations(), false));
        codeRegistryDto.setUrl(apiUtils.createCodeRegistryUrl(codeRegistryDto));
        codeRegistryDto.setCreated(codeRegistry.getCreated());
        codeRegistryDto.setModified(codeRegistry.getModified());
        return codeRegistryDto;
    }

    @Transactional
    public Set<CodeRegistryDTO> mapCodeRegistryDtos(final Set<CodeRegistry> codeRegistries) {
        final Set<CodeRegistryDTO> codeRegistryDtos = new HashSet<>();
        if (codeRegistries != null && !codeRegistries.isEmpty()) {
            codeRegistries.forEach(codeRegistry -> codeRegistryDtos.add(mapCodeRegistryDto(codeRegistry)));
        }
        return codeRegistryDtos;
    }

    @Transactional
    public ExternalReferenceDTO mapDeepExternalReferenceDto(final ExternalReference externalReference) {
        return mapExternalReferenceDto(externalReference, true);
    }

    @Transactional
    public ExternalReferenceDTO mapExternalReferenceDto(final ExternalReference externalReference,
                                                        final boolean deep) {
        final ExternalReferenceDTO externalReferenceDto = new ExternalReferenceDTO();
        externalReferenceDto.setId(externalReference.getId());
        externalReferenceDto.setDescription(externalReference.getDescription());
        externalReferenceDto.setGlobal(externalReference.getGlobal());
        externalReferenceDto.setTitle(externalReference.getTitle());
        externalReferenceDto.setHref(externalReference.getHref());
        externalReferenceDto.setPropertyType(mapPropertyTypeDto(externalReference.getPropertyType()));
        if (externalReference.getParentCodeScheme() != null) {
            externalReferenceDto.setParentCodeScheme(mapCodeSchemeDto(externalReference.getParentCodeScheme(), false));
        }
        if (deep) {
            if (externalReference.getCodeSchemes() != null) {
                externalReferenceDto.setCodeSchemes(mapCodeSchemeDtos(externalReference.getCodeSchemes(), false));
            }
            if (externalReference.getCodes() != null) {
                externalReferenceDto.setCodes(mapCodeDtos(externalReference.getCodes(), false, true));
            }
        }
        externalReferenceDto.setUrl(apiUtils.createExternalReferenceUrl(externalReferenceDto));
        externalReferenceDto.setCreated(externalReference.getCreated());
        externalReferenceDto.setModified(externalReference.getModified());
        return externalReferenceDto;
    }

    @Transactional
    public Set<ExternalReferenceDTO> mapDeepExternalReferenceDtos(final Set<ExternalReference> externalReferences) {
        return mapExternalReferenceDtos(externalReferences, true);
    }

    @Transactional
    public Set<ExternalReferenceDTO> mapExternalReferenceDtos(final Set<ExternalReference> externalReferences,
                                                              final boolean deep) {
        final Set<ExternalReferenceDTO> externalReferenceDtos = new HashSet<>();
        if (externalReferences != null && !externalReferences.isEmpty()) {
            externalReferences.forEach(externalReference -> externalReferenceDtos.add(mapExternalReferenceDto(externalReference, deep)));
        }
        return externalReferenceDtos;
    }

    @Transactional
    public PropertyTypeDTO mapPropertyTypeDto(final PropertyType propertyType) {
        final PropertyTypeDTO propertyTypeDto = new PropertyTypeDTO();
        propertyTypeDto.setId(propertyType.getId());
        propertyTypeDto.setContext(propertyType.getContext());
        propertyTypeDto.setDefinition(propertyType.getDefinition());
        propertyTypeDto.setLocalName(propertyType.getLocalName());
        propertyTypeDto.setPrefLabel(propertyType.getPrefLabel());
        propertyTypeDto.setType(propertyType.getType());
        propertyTypeDto.setPropertyUri(propertyType.getPropertyUri());
        propertyTypeDto.setUrl(apiUtils.createPropertyTypeUrl(propertyTypeDto));
        propertyTypeDto.setCreated(propertyType.getCreated());
        propertyTypeDto.setModified(propertyType.getModified());
        return propertyTypeDto;
    }

    @Transactional
    public Set<PropertyTypeDTO> mapPropertyTypeDtos(final Set<PropertyType> propertyTypes) {
        final Set<PropertyTypeDTO> propertyTypeDtos = new HashSet<>();
        if (propertyTypes != null && !propertyTypes.isEmpty()) {
            propertyTypes.forEach(propertyType -> propertyTypeDtos.add(mapPropertyTypeDto(propertyType)));
        }
        return propertyTypeDtos;
    }

    @Transactional
    public ExtensionDTO mapDeepExtensionDto(final Extension extension) {
        return mapExtensionDto(extension, true);
    }

    @Transactional
    public ExtensionDTO mapExtensionDto(final Extension extension,
                                        final boolean deep) {
        final ExtensionDTO extensionDto = new ExtensionDTO();
        extensionDto.setId(extension.getId());
        extensionDto.setOrder(extension.getOrder());
        extensionDto.setExtensionValue(extension.getExtensionValue());
        extensionDto.setCode(mapCodeDto(extension.getCode(), false, true));
        extensionDto.setPrefLabel(extension.getPrefLabel());
        if (deep) {
            if (extension.getExtension() != null) {
                extensionDto.setExtension(mapExtensionDto(extension.getExtension(), false));
            }
            if (extension.getExtensionScheme() != null) {
                extensionDto.setExtensionScheme(mapExtensionSchemeDto(extension.getExtensionScheme(), false, true, true));
            }
        }
        extensionDto.setUrl(apiUtils.createExtensionUrl(extensionDto));
        extensionDto.setStartDate(extension.getStartDate());
        extensionDto.setEndDate(extension.getEndDate());
        extensionDto.setCreated(extension.getCreated());
        extensionDto.setModified(extension.getModified());
        return extensionDto;
    }

    @Transactional
    public Set<ExtensionDTO> mapDeepExtensionDtos(final Set<Extension> extensions) {
        return mapExtensionDtos(extensions, true);
    }

    @Transactional
    public Set<ExtensionDTO> mapExtensionDtos(final Set<Extension> extensions,
                                              final boolean deep) {
        final Set<ExtensionDTO> extensionDtos = new HashSet<>();
        if (extensions != null && !extensions.isEmpty()) {
            extensions.forEach(extension -> extensionDtos.add(mapExtensionDto(extension, deep)));
        }
        return extensionDtos;
    }

    @Transactional
    public ExtensionSchemeDTO mapDeepExtensionSchemeDto(final ExtensionScheme extensionScheme) {
        return mapExtensionSchemeDto(extensionScheme, true);
    }

    @Transactional
    public ExtensionSchemeDTO mapExtensionSchemeDto(final ExtensionScheme extensionScheme,
                                                    final boolean deep) {
        return mapExtensionSchemeDto(extensionScheme, deep, false, false);
    }

    @Transactional
    public ExtensionSchemeDTO mapExtensionSchemeDto(final ExtensionScheme extensionScheme,
                                                    final boolean deep,
                                                    final boolean includeParentCodeScheme,
                                                    final boolean includeCodeSchemes) {
        final ExtensionSchemeDTO extensionSchemeDto = new ExtensionSchemeDTO();
        extensionSchemeDto.setId(extensionScheme.getId());
        extensionSchemeDto.setPropertyType(mapPropertyTypeDto(extensionScheme.getPropertyType()));
        extensionSchemeDto.setPrefLabel(extensionScheme.getPrefLabel());
        extensionSchemeDto.setStatus(extensionScheme.getStatus());
        final String codeValue = extensionScheme.getCodeValue();
        extensionSchemeDto.setCodeValue(codeValue);
        extensionSchemeDto.setStartDate(extensionScheme.getStartDate());
        extensionSchemeDto.setEndDate(extensionScheme.getEndDate());
        if (deep || includeParentCodeScheme) {
            if (extensionScheme.getParentCodeScheme() != null) {
                extensionSchemeDto.setParentCodeScheme(mapCodeSchemeDto(extensionScheme.getParentCodeScheme(), false));
            }
        }
        if (deep || includeCodeSchemes) {
            if (extensionScheme.getCodeSchemes() != null) {
                extensionSchemeDto.setCodeSchemes(mapCodeSchemeDtos(extensionScheme.getCodeSchemes(), false));
            }
        }

        if (deep) {
            if (extensionScheme.getExtensions() != null) {
                extensionSchemeDto.setExtensions(mapExtensionDtos(extensionScheme.getExtensions(), false));
            }
        }
        extensionSchemeDto.setUrl(apiUtils.createExtensionSchemeUrl(extensionScheme.getParentCodeScheme().getCodeRegistry().getCodeValue(), extensionScheme.getParentCodeScheme().getCodeValue(), codeValue));
        extensionSchemeDto.setCreated(extensionScheme.getCreated());
        extensionSchemeDto.setModified(extensionScheme.getModified());
        return extensionSchemeDto;
    }

    @Transactional
    public Set<ExtensionSchemeDTO> mapDeepExtensionSchemeDtos(final Set<ExtensionScheme> extensionSchemes) {
        return mapExtensionSchemeDtos(extensionSchemes, true);
    }

    @Transactional
    public Set<ExtensionSchemeDTO> mapExtensionSchemeDtos(final Set<ExtensionScheme> extensionSchemes,
                                                          final boolean deep) {
        final Set<ExtensionSchemeDTO> extensionSchemeDtos = new HashSet<>();
        if (extensionSchemes != null && !extensionSchemes.isEmpty()) {
            extensionSchemes.forEach(extensionScheme -> extensionSchemeDtos.add(mapExtensionSchemeDto(extensionScheme, deep)));
        }
        return extensionSchemeDtos;
    }

    @Transactional
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

    @Transactional
    public Set<OrganizationDTO> mapOrganizationDtos(final Set<Organization> organizations,
                                                    final boolean deep) {
        final Set<OrganizationDTO> organizationDtos = new HashSet<>();
        if (organizations != null && !organizations.isEmpty()) {
            organizations.forEach(organization -> organizationDtos.add(mapOrganizationDto(organization, deep)));
        }
        return organizationDtos;
    }
}
