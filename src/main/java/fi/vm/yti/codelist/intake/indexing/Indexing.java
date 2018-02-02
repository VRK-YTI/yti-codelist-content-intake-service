package fi.vm.yti.codelist.intake.indexing;

import java.util.Set;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.ExternalReference;
import fi.vm.yti.codelist.common.model.PropertyType;

public interface Indexing {

    boolean updateCode(final Code code);

    boolean updateCodes(final Set<Code> code);

    boolean updateCodeScheme(final CodeScheme codeScheme);

    boolean updateCodeSchemes(final Set<CodeScheme> codeSchemes);

    boolean updateCodeRegistry(final CodeRegistry codeRegistry);

    boolean updateCodeRegistries(final Set<CodeRegistry> codeRegistries);

    boolean updateExternalReference(final ExternalReference externalReference);

    boolean updateExternalReferences(final Set<ExternalReference> externalReferences);

    boolean updatePropertyType(final PropertyType propertyType);

    boolean updatePropertyTypes(final Set<PropertyType> propertyTypes);

    boolean reIndexEverything();

    boolean reIndex(final String indexName, final String type);
}
