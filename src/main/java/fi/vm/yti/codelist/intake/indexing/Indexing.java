package fi.vm.yti.codelist.intake.indexing;

import java.util.Set;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeScheme;

public interface Indexing {

    boolean updateCode(final Code code);

    boolean updateCodes(final Set<Code> code);

    boolean updateCodeScheme(final CodeScheme codeScheme);

    boolean updateCodeSchemes(final Set<CodeScheme> codeSchemes);

    boolean reIndexEverything();

    boolean reIndex(final String indexName, final String type);
}
