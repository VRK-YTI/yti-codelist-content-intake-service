package fi.vm.yti.codelist.intake.service.impl;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.common.dto.MemberValueDTO;
import fi.vm.yti.codelist.common.dto.OrganizationDTO;
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.common.dto.ValueTypeDTO;
import fi.vm.yti.codelist.common.model.CodeSchemeListItem;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.model.Member;
import fi.vm.yti.codelist.intake.model.MemberValue;
import fi.vm.yti.codelist.intake.model.Organization;
import fi.vm.yti.codelist.intake.model.PropertyType;
import fi.vm.yti.codelist.intake.model.ValueType;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CODE_EXTENSION;

@Component
public class DtoMapperService {

    private final ApiUtils apiUtils;

    @Inject
    public DtoMapperService(final ApiUtils apiUtils) {
        this.apiUtils = apiUtils;
    }

    @Transactional
    public CodeDTO mapDeepCodeDto(final Code code) {
        return mapCodeDto(code, true, true, true);
    }

    @Transactional
    public CodeDTO mapCodeDto(final Code code) {
        return mapCodeDto(code, false, false, true);
    }

    @Transactional
    public CodeDTO mapCodeDto(final Code code,
                              final boolean deep,
                              final boolean includeCodeScheme,
                              final boolean includeBroaderCode) {
        final CodeDTO codeDto = new CodeDTO();
        codeDto.setId(code.getId());
        codeDto.setCodeValue(code.getCodeValue());
        codeDto.setUri(code.getUri());
        if (includeBroaderCode && code.getBroaderCode() != null) {
            codeDto.setBroaderCode(mapCodeDto(code.getBroaderCode(), false, false, false));
        }
        codeDto.setStartDate(code.getStartDate());
        codeDto.setEndDate(code.getEndDate());
        codeDto.setStatus(code.getStatus());
        codeDto.setHierarchyLevel(code.getHierarchyLevel());
        codeDto.setShortName(code.getShortName());
        codeDto.setPrefLabel(code.getPrefLabel());
        codeDto.setDefinition(code.getDefinition());
        if (code.getSubCodeScheme() != null) {
            codeDto.setSubCodeScheme(mapCodeSchemeDto(code.getSubCodeScheme()));
        }
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
            if (code.getMembers() != null) {
                codeDto.setMembers(mapMemberDtos(code.getMembers(), false));
            }
            if (code.getCodeScheme().getExtensions() != null && !code.getCodeScheme().getExtensions().isEmpty()) {
                final Set<Extension> codeExtensions = code.getCodeScheme().getExtensions().stream().filter(extension -> CODE_EXTENSION.equalsIgnoreCase(extension.getPropertyType().getContext())).collect(Collectors.toSet());
                codeDto.setCodeExtensions(mapExtensionDtosWithCodeMembers(codeExtensions, code));
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
            codes.forEach(code -> codeDtos.add(mapCodeDto(code, deep, includeCodeScheme, true)));
        }
        return codeDtos;
    }

    @Transactional
    public CodeSchemeDTO mapDeepCodeSchemeDto(final CodeScheme codeScheme) {
        return mapCodeSchemeDto(codeScheme, true);
    }

    @Transactional
    public CodeSchemeDTO mapCodeSchemeDto(final CodeScheme codeScheme) {
        return mapCodeSchemeDto(codeScheme, false);
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
                codeSchemeDto.setDefaultCode(mapCodeDto(codeScheme.getDefaultCode(), false, true, false));
            }
            if (codeScheme.getInfoDomains() != null) {
                codeSchemeDto.setInfoDomains(mapCodeDtos(codeScheme.getInfoDomains(), false, true));
            }
            if (codeScheme.getExternalReferences() != null) {
                codeSchemeDto.setExternalReferences(mapExternalReferenceDtos(codeScheme.getExternalReferences(), false));
            }
            if (codeScheme.getExtensions() != null) {
                codeSchemeDto.setExtensions(mapExtensionDtos(codeScheme.getExtensions(), false));
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
        codeSchemeDto.setOrganizations(mapOrganizationDtos(codeScheme.getOrganizations(), false));
        codeSchemeDto.setCumulative(codeScheme.isCumulative());
        return codeSchemeDto;
    }

    @Transactional
    public LinkedHashSet<CodeSchemeListItem> getVariantsOfCodeSchemeAsListItems(final CodeScheme codeScheme) {
        final LinkedHashSet<CodeSchemeListItem> result = new LinkedHashSet<>();
        if (!codeScheme.getVariants().isEmpty()) {
            for (CodeScheme variant : codeScheme.getVariants()) {
                final CodeSchemeListItem item = new CodeSchemeListItem(variant.getId(), variant.getPrefLabel(), variant.getCodeValue(),
                    variant.getUri(), variant.getStartDate(), variant.getEndDate(), variant.getStatus());
                result.add(item);
            }
        }
        return result;
    }

    @Transactional
    public LinkedHashSet<CodeSchemeListItem> getVariantMothersOfCodeSchemeAsListItems(final CodeScheme codeScheme) {
        final LinkedHashSet<CodeSchemeListItem> result = new LinkedHashSet<>();
        if (!codeScheme.getVariantMothers().isEmpty()) {
            for (CodeScheme variantMother : codeScheme.getVariantMothers()) {
                final CodeSchemeListItem item = new CodeSchemeListItem(variantMother.getId(), variantMother.getPrefLabel(), variantMother.getCodeValue(),
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
        codeRegistryDto.setDescription(codeRegistry.getDescription());
        codeRegistryDto.setUrl(apiUtils.createCodeRegistryUrl(codeRegistryDto));
        codeRegistryDto.setOrganizations(mapOrganizationDtos(codeRegistry.getOrganizations(), false));
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
    public ExternalReferenceDTO mapExternalReferenceDto(final ExternalReference externalReference) {
        return mapExternalReferenceDto(externalReference, false);
    }

    @Transactional
    public ExternalReferenceDTO mapExternalReferenceDto(final ExternalReference externalReference,
                                                        final boolean deep) {
        if (externalReference == null) {
            return null;
        }
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
        propertyTypeDto.setUri(propertyType.getUri());
        propertyTypeDto.setUrl(apiUtils.createPropertyTypeUrl(propertyTypeDto));
        propertyTypeDto.setCreated(propertyType.getCreated());
        propertyTypeDto.setModified(propertyType.getModified());
        if (propertyType.getValueTypes() != null && !propertyType.getValueTypes().isEmpty()) {
            propertyTypeDto.setValueTypes(mapValueTypeDtos(propertyType.getValueTypes()));
        }
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
    public Set<ValueTypeDTO> mapValueTypeDtos(final Set<ValueType> valueTypes) {
        final Set<ValueTypeDTO> valueTypeDtos = new HashSet<>();
        if (valueTypes != null && !valueTypes.isEmpty()) {
            valueTypes.forEach(valueType -> valueTypeDtos.add(mapValueTypeDto(valueType)));
        }
        return valueTypeDtos;
    }

    @Transactional
    public ValueTypeDTO mapValueTypeDto(final ValueType valueType) {
        final ValueTypeDTO valueTypeDto = new ValueTypeDTO();
        valueTypeDto.setId(valueType.getId());
        valueTypeDto.setTypeUri(valueType.getTypeUri());
        valueTypeDto.setUri(valueType.getUri());
        valueTypeDto.setLocalName(valueType.getLocalName());
        valueTypeDto.setRegexp(valueType.getRegexp());
        valueTypeDto.setPrefLabel(valueType.getPrefLabel());
        valueTypeDto.setRequired(valueType.getRequired());
        valueTypeDto.setUrl(apiUtils.createValueTypeUrl(valueTypeDto));
        return valueTypeDto;
    }

    @Transactional
    public Set<MemberValueDTO> mapMemberValueDtos(final Set<MemberValue> memberValues) {
        final Set<MemberValueDTO> memberValueDtos = new HashSet<>();
        if (memberValues != null && !memberValues.isEmpty()) {
            memberValues.forEach(memberValue -> memberValueDtos.add(mapMemberValueDto(memberValue, false)));
        }
        return memberValueDtos;
    }

    @Transactional
    public MemberValueDTO mapDeepMemberValueDto(final MemberValue memberValue) {
        return mapMemberValueDto(memberValue, true);
    }

    @Transactional
    public MemberValueDTO mapMemberValueDto(final MemberValue memberValue,
                                            final boolean deep) {
        final MemberValueDTO memberValueDto = new MemberValueDTO();
        memberValueDto.setId(memberValue.getId());
        memberValueDto.setCreated(memberValue.getCreated());
        memberValueDto.setModified(memberValue.getModified());
        memberValueDto.setValue(memberValue.getValue());
        memberValueDto.setValueType(mapValueTypeDto(memberValue.getValueType()));
        if (deep) {
            memberValueDto.setMember(mapMemberDto(memberValue.getMember()));
        }
        return memberValueDto;
    }

    @Transactional
    public MemberDTO mapDeepMemberDto(final Member member) {
        return mapMemberDto(member, true);
    }

    @Transactional
    public MemberDTO mapMemberDto(final Member member) {
        return mapMemberDto(member, false);
    }

    @Transactional
    public MemberDTO mapMemberDto(final Member member,
                                  final boolean deep) {
        final MemberDTO memberDto = new MemberDTO();
        memberDto.setId(member.getId());
        memberDto.setOrder(member.getOrder());
        memberDto.setSequenceId(member.getSequenceId());
        if (member.getMemberValues() != null && !member.getMemberValues().isEmpty()) {
            memberDto.setMemberValues(mapMemberValueDtos(member.getMemberValues()));
        }
        memberDto.setCode(mapCodeDto(member.getCode(), false, true, false));
        memberDto.setPrefLabel(member.getPrefLabel());
        if (deep) {
            if (member.getRelatedMember() != null) {
                memberDto.setRelatedMember(mapMemberDto(member.getRelatedMember(), false));
            }
            if (member.getExtension() != null) {
                memberDto.setExtension(mapExtensionDto(member.getExtension(), false, true, true));
            }
        }
        memberDto.setUri(apiUtils.createMemberUri(member));
        memberDto.setUrl(apiUtils.createMemberUrl(member));
        memberDto.setStartDate(member.getStartDate());
        memberDto.setEndDate(member.getEndDate());
        memberDto.setCreated(member.getCreated());
        memberDto.setModified(member.getModified());
        return memberDto;
    }

    @Transactional
    public Set<MemberDTO> mapDeepMemberDtos(final Set<Member> members) {
        return mapMemberDtos(members, true);
    }

    @Transactional
    public Set<MemberDTO> mapMemberDtos(final Set<Member> members,
                                        final boolean deep) {
        final Set<MemberDTO> memberDtos = new HashSet<>();
        if (members != null && !members.isEmpty()) {
            members.forEach(member -> memberDtos.add(mapMemberDto(member, deep)));
        }
        return memberDtos;
    }

    @Transactional
    public ExtensionDTO mapDeepExtensionDto(final Extension extension) {
        return mapExtensionDto(extension, true);
    }

    @Transactional
    public ExtensionDTO mapExtensionDto(final Extension extension) {
        return mapExtensionDto(extension, false);
    }

    @Transactional
    public ExtensionDTO mapExtensionDto(final Extension extension,
                                        final boolean deep) {
        return mapExtensionDto(extension, deep, false, false);
    }

    @Transactional
    public ExtensionDTO mapExtensionDto(final Extension extension,
                                        final boolean deep,
                                        final boolean includeParentCodeScheme,
                                        final boolean includeCodeSchemes) {
        final ExtensionDTO extensionDto = new ExtensionDTO();
        extensionDto.setId(extension.getId());
        extensionDto.setPropertyType(mapPropertyTypeDto(extension.getPropertyType()));
        extensionDto.setPrefLabel(extension.getPrefLabel());
        extensionDto.setStatus(extension.getStatus());
        final String codeValue = extension.getCodeValue();
        extensionDto.setUrl(apiUtils.createExtensionUrl(extension));
        extensionDto.setUri(apiUtils.createExtensionUri(extension));
        extensionDto.setCodeValue(codeValue);
        extensionDto.setStartDate(extension.getStartDate());
        extensionDto.setEndDate(extension.getEndDate());
        if ((deep || includeParentCodeScheme) && extension.getParentCodeScheme() != null) {
            extensionDto.setParentCodeScheme(mapCodeSchemeDto(extension.getParentCodeScheme(), false));
        }
        if ((deep || includeCodeSchemes) && extension.getCodeSchemes() != null) {
            extensionDto.setCodeSchemes(mapCodeSchemeDtos(extension.getCodeSchemes(), false));
        }

        if (deep && extension.getMembers() != null) {
            extensionDto.setMembers(mapMemberDtos(extension.getMembers(), false));
        }
        extensionDto.setUrl(apiUtils.createExtensionUrl(extension.getParentCodeScheme().getCodeRegistry().getCodeValue(), extension.getParentCodeScheme().getCodeValue(), codeValue));
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
    public Set<ExtensionDTO> mapExtensionDtosWithCodeMembers(final Set<Extension> extensions,
                                                             final Code code) {
        final Set<ExtensionDTO> extensionDtos = new HashSet<>();
        if (extensions != null && !extensions.isEmpty()) {
            extensions.forEach(extension -> extensionDtos.add(mapExtensionDtoWithCodeMembers(extension, code)));
        }
        return extensionDtos;
    }

    @Transactional
    public ExtensionDTO mapExtensionDtoWithCodeMembers(final Extension extension,
                                                       final Code code) {
        final ExtensionDTO extensionDto = new ExtensionDTO();
        extensionDto.setId(extension.getId());
        extensionDto.setPropertyType(mapPropertyTypeDto(extension.getPropertyType()));
        extensionDto.setPrefLabel(extension.getPrefLabel());
        extensionDto.setStatus(extension.getStatus());
        final String codeValue = extension.getCodeValue();
        extensionDto.setUrl(apiUtils.createExtensionUrl(extension));
        extensionDto.setUri(apiUtils.createExtensionUri(extension));
        extensionDto.setCodeValue(codeValue);
        extensionDto.setStartDate(extension.getStartDate());
        extensionDto.setEndDate(extension.getEndDate());
        if (extension.getMembers() != null && code != null) {
            final Set<Member> membersForCode = extension.getMembers().stream().filter(member -> member.getCode().getId().equals(code.getId())).collect(Collectors.toSet());
            extensionDto.setMembers(mapMemberDtos(membersForCode, false));
        }
        extensionDto.setUrl(apiUtils.createExtensionUrl(extension.getParentCodeScheme().getCodeRegistry().getCodeValue(), extension.getParentCodeScheme().getCodeValue(), codeValue));
        extensionDto.setCreated(extension.getCreated());
        extensionDto.setModified(extension.getModified());
        return extensionDto;
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
        if (deep && organization.getCodeRegistries() != null) {
            organizationDto.setCodeRegistries(mapCodeRegistryDtos(organization.getCodeRegistries()));
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
