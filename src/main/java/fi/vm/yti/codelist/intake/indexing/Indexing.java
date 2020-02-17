package fi.vm.yti.codelist.intake.indexing;

import java.util.Set;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.common.dto.ValueTypeDTO;

public interface Indexing {

    void updateCode(final CodeDTO code);

    void updateCodes(final Set<CodeDTO> code);

    void deleteCode(final CodeDTO code);

    void deleteCodes(final Set<CodeDTO> codes);

    void updateCodeScheme(final CodeSchemeDTO codeScheme);

    void updateCodeSchemes(final Set<CodeSchemeDTO> codeSchemes);

    void deleteCodeScheme(final CodeSchemeDTO codeScheme);

    void deleteCodeSchemes(final Set<CodeSchemeDTO> codeSchemes);

    void updateCodeRegistry(final CodeRegistryDTO codeRegistry);

    void updateCodeRegistries(final Set<CodeRegistryDTO> codeRegistries);

    void deleteCodeRegistry(final CodeRegistryDTO codeRegistry);

    void deleteCodeRegistries(final Set<CodeRegistryDTO> codeRegistries);

    void updateExternalReference(final ExternalReferenceDTO externalReference);

    void updateExternalReferences(final Set<ExternalReferenceDTO> externalReferences);

    void deleteExternalReferences(final Set<ExternalReferenceDTO> externalReferences);

    void updatePropertyType(final PropertyTypeDTO propertyType);

    void updatePropertyTypes(final Set<PropertyTypeDTO> propertyTypes);

    void updateValueType(final ValueTypeDTO valueTypeDTO);

    void updateValueTypes(final Set<ValueTypeDTO> valueTypes);

    void updateExtension(final ExtensionDTO extension);

    void updateExtensions(final Set<ExtensionDTO> extensions);

    void deleteExtension(final ExtensionDTO extension);

    void deleteExtensions(final Set<ExtensionDTO> extensions);

    void updateMembers(final Set<MemberDTO> members);

    void deleteMember(final MemberDTO member);

    void deleteMembers(final Set<MemberDTO> members);

    boolean reIndexEverything();

    void cleanRunningIndexingBookkeeping();

    void reIndexEverythingIfNecessary();
}
