package fi.vm.yti.codelist.intake.indexing;

import java.util.Set;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;

public interface Indexing {

    boolean updateCode(final CodeDTO code);

    boolean updateCodes(final Set<CodeDTO> code);

    boolean updateCodeScheme(final CodeSchemeDTO codeScheme);

    boolean updateCodeSchemes(final Set<CodeSchemeDTO> codeSchemes);

    boolean updateCodeRegistry(final CodeRegistryDTO codeRegistry);

    boolean updateCodeRegistries(final Set<CodeRegistryDTO> codeRegistries);

    boolean updateExternalReference(final ExternalReferenceDTO externalReference);

    boolean updateExternalReferences(final Set<ExternalReferenceDTO> externalReferences);

    boolean updatePropertyType(final PropertyTypeDTO propertyType);

    boolean updatePropertyTypes(final Set<PropertyTypeDTO> propertyTypes);

    boolean reIndexEverything();

    void reIndexEverythingIfNecessary();

    boolean reIndex(final String indexName, final String type);
}
