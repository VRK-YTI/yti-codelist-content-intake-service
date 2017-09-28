package fi.vm.yti.codelist.intake.domain;

import java.util.List;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;

public interface Domain {

    /**
     * Methods for persisting data to PostgreSQL.
     */

    void persistCodeRegistries(final List<CodeRegistry> codeRegistries);

    void persistCodeSchemes(final List<CodeScheme> codeSchemes);

    void persistCodes(final List<Code> codes);

    /**
     * Methods for indexing data to ElasticSearch.
     */

    void createIndex(final String indexName);

    void deleteIndex(final String indexName);

    void deleteTypeFromIndex(final String indexName, final String type);

    void ensureNestedPrefLabelsMapping(final String indexName, final String indexType);

    void refreshIndex(final String indexName);

    void indexCodeSchemes();

    void reIndexCodeRegistries();

    void reIndexCodeSchemes();

    void reIndexCodes();

    void reIndexEverything();

}
