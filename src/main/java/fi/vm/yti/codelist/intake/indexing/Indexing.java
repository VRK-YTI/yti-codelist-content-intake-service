package fi.vm.yti.codelist.intake.indexing;

import java.util.Set;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;

public interface Indexing {

    boolean updateCode(final CodeDTO code);

    boolean updateCodes(final Set<CodeDTO> code);

    boolean deleteCode(final CodeDTO code);

    boolean deleteCodes(final Set<CodeDTO> codes);

    boolean updateCodeScheme(final CodeSchemeDTO codeScheme);

    boolean updateCodeSchemes(final Set<CodeSchemeDTO> codeSchemes);

    boolean deleteCodeScheme(final CodeSchemeDTO codeScheme);

    boolean deleteCodeSchemes(final Set<CodeSchemeDTO> codeSchemes);

    boolean updateCodeRegistry(final CodeRegistryDTO codeRegistry);

    boolean updateCodeRegistries(final Set<CodeRegistryDTO> codeRegistries);

    boolean deleteCodeRegistry(final CodeRegistryDTO codeRegistry);

    boolean deleteCodeRegistries(final Set<CodeRegistryDTO> codeRegistries);

    boolean updateExternalReference(final ExternalReferenceDTO externalReference);

    boolean updateExternalReferences(final Set<ExternalReferenceDTO> externalReferences);

    boolean deleteExternalReferences(final Set<ExternalReferenceDTO> externalReferences);

    boolean updatePropertyType(final PropertyTypeDTO propertyType);

    boolean updatePropertyTypes(final Set<PropertyTypeDTO> propertyTypes);

    boolean updateExtension(final ExtensionDTO extension);

    boolean updateExtensions(final Set<ExtensionDTO> extensions);

    boolean deleteExtension(final ExtensionDTO extension);

    boolean deleteExtensions(final Set<ExtensionDTO> extensions);

    boolean updateMember(final MemberDTO member);

    boolean updateMembers(final Set<MemberDTO> members);

    boolean deleteMember(final MemberDTO member);

    boolean deleteMembers(final Set<MemberDTO> members);

    boolean reIndexEverything();

    void cleanRunningIndexingBookkeeping();

    void reIndexEverythingIfNecessary();

    boolean reIndex(final String indexName, final String type);
}
