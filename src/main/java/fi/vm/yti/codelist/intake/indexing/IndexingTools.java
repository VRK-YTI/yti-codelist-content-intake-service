package fi.vm.yti.codelist.intake.indexing;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface IndexingTools {

    void aliasIndex(final String indexName, final String aliasName);

    void deleteIndex(final String indexName);

    void createIndexWithNestedPrefLabels(final String indexName, final Set<String> type);

    ObjectMapper createObjectMapper();
}
