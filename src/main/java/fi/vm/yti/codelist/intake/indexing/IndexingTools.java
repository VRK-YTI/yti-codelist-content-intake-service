package fi.vm.yti.codelist.intake.indexing;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface IndexingTools {

    void aliasIndex(final String indexName,
                    final String aliasName);

    void deleteIndex(final String indexName);

    void createIndexWithNestedPrefLabel(final String indexName,
                                        final String type);

    ObjectMapper createObjectMapper();
}
