package fi.vm.yti.codelist.intake.indexing;

import java.util.Set;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.ExternalReference;

public interface Indexing {

    boolean updateCode(final Code code);

    boolean updateCodes(final Set<Code> code);

    boolean updateCodeScheme(final CodeScheme codeScheme);

    boolean updateCodeSchemes(final Set<CodeScheme> codeSchemes);

    boolean updateExternalReferences(final Set<ExternalReference> externalReferences);

    boolean reIndexEverything();

    boolean reIndex(final String indexName, final String type);
}
