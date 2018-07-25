package fi.vm.yti.codelist.intake.indexing;

import java.util.Set;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
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

    boolean deleteExternalReferences(final Set<ExternalReferenceDTO> codeScheme);

    boolean updatePropertyType(final PropertyTypeDTO propertyType);

    boolean updatePropertyTypes(final Set<PropertyTypeDTO> propertyTypes);

    boolean updateExtensionScheme(final ExtensionSchemeDTO extensionScheme);

    boolean updateExtensionSchemes(final Set<ExtensionSchemeDTO> extensionSchemes);

    boolean deleteExtensionScheme(final ExtensionSchemeDTO extension);

    boolean deleteExtensionSchemes(final Set<ExtensionSchemeDTO> extensions);

    boolean updateExtension(final ExtensionDTO extensionScheme);

    boolean updateExtensions(final Set<ExtensionDTO> extensionSchemes);

    boolean deleteExtension(final ExtensionDTO extension);

    boolean deleteExtensions(final Set<ExtensionDTO> extensions);

    boolean reIndexEverything();

    void cleanRunningIndexingBookkeeping();

    void reIndexEverythingIfNecessary();

    boolean reIndex(final String indexName, final String type);

    void populateAllVersionsToCodeSchemeDTO(final CodeSchemeDTO currentCodeScheme);

    void populateVariantInfoToCodeSchemeDTO(final CodeSchemeDTO currentCodeScheme);
}
