package fi.vm.yti.codelist.intake.indexing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import static fi.vm.yti.codelist.common.constants.ApiConstants.ELASTIC_TYPE_CODE;
import static fi.vm.yti.codelist.common.constants.ApiConstants.ELASTIC_TYPE_CODESCHEME;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@Singleton
@Service
public class IndexingToolsImpl implements IndexingTools {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingToolsImpl.class);
    private static final String MAX_RESULT_WINDOW = "max_result_window";
    private static final int MAX_RESULT_WINDOW_SIZE = 500000;

    private static final String NESTED_PREFLABEL_MAPPING_JSON = "{" +
        "\"properties\": {\n" +
        "  \"codeValue\": {\n" +
        "    \"type\": \"text\"," +
        "    \"analyzer\": \"analyzer_keyword\",\n" +
        "    \"fields\": {\n" +
        "      \"raw\": { \n" +
        "        \"type\": \"keyword\"\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"id\": {\n" +
        "    \"type\": \"text\"},\n" +
        "  \"prefLabel\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  }\n" +
        "}\n}";

    private static final String CODESCHEME_MAPPING = "{" +
        "\"properties\": {\n" +
        "  \"codeValue\": {\n" +
        "    \"type\": \"text\"," +
        "    \"analyzer\": \"analyzer_keyword\",\n" +
        "    \"fields\": {\n" +
        "      \"raw\": { \n" +
        "        \"type\": \"keyword\"\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"id\": {\n" +
        "    \"type\": \"text\"},\n" +
        "  \"prefLabel\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  },\n" +
        "  \"dataClassifications\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  },\n" +
        "  \"codeRegistry\": {\n" +
        "    \"properties\": {\n" +
        "      \"organizations\": {\n" +
        "        \"type\": \"nested\"\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"externalReferences\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  }\n" +
        "}\n}";

    private static final String CODE_MAPPING = "{" +
        "\"properties\": {\n" +
        "  \"codeValue\": {\n" +
        "    \"type\": \"text\"," +
        "    \"analyzer\": \"analyzer_keyword\",\n" +
        "    \"fields\": {\n" +
        "      \"raw\": { \n" +
        "        \"type\": \"keyword\"\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"id\": {\n" +
        "    \"type\": \"text\"},\n" +
        "  \"prefLabel\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  },\n" +
        "  \"dataClassifications\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  },\n" +
        "  \"codeScheme\": {\n" +
        "    \"properties\": {\n" +
        "      \"codeValue\": {\n" +
        "        \"type\": \"text\",\n" +
        "        \"analyzer\": \"analyzer_keyword\"\n" +
        "      },\n" +
        "      \"codeRegistry\": {\n" +
        "        \"properties\": {\n" +
        "          \"codeValue\": {\n" +
        "            \"type\": \"text\",\n" +
        "            \"analyzer\": \"analyzer_keyword\"\n" +
        "          },\n" +
        "          \"organizations\": {\n" +
        "            \"type\": \"nested\"\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"externalReferences\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  }\n" +
        "}\n}";

    private Client client;

    @Inject
    public IndexingToolsImpl(final Client client) {
        this.client = client;
    }

    /**
     * Alias index to alias name. Removes possible earlier indexes from this same alias.
     *
     * @param indexName The name of the index to be deleted.
     * @param aliasName The name of the alias.
     */
    public void aliasIndex(final String indexName,
                           final String aliasName) {
        final boolean exists = client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();
        if (exists) {
            final IndicesAliasesRequest request = new IndicesAliasesRequest();
            final ImmutableOpenMap<String, List<AliasMetaData>> aliases = client.admin().indices().prepareGetAliases(aliasName).get().getAliases();
            final List<String> oldIndexNames = new ArrayList<>();
            for (ObjectObjectCursor<String, List<AliasMetaData>> alias : aliases) {
                if (!alias.value.isEmpty()) {
                    request.addAliasAction(IndicesAliasesRequest.AliasActions.remove().alias(aliasName).index(alias.key));
                    oldIndexNames.add(alias.key);
                }
            }
            request.addAliasAction(IndicesAliasesRequest.AliasActions.add().alias(aliasName).index(indexName));
            final IndicesAliasesResponse response = client.admin().indices().aliases(request).actionGet();
            if (!response.isAcknowledged()) {
                logAliasFailed(indexName);
            } else {
                logAliasSuccessful(indexName);
                for (final String oldIndexName : oldIndexNames) {
                    logAliasRemovedSuccessful(oldIndexName);
                }
            }
        } else {
            logIndexDoesNotExist(indexName);
        }
    }

    /**
     * Delete index with name.
     *
     * @param indexName The name of the index to be deleted.
     */
    public void deleteIndex(final String indexName) {
        final DeleteIndexRequest request = new DeleteIndexRequest(indexName);
        final boolean exists = client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();
        if (exists) {
            try {
                final DeleteIndexResponse response = client.admin().indices().delete(request).get();
                if (!response.isAcknowledged()) {
                    logDeleteFailed(indexName);
                } else {
                    logDeleteSuccessful(indexName);
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Deleting ElasticSearch index failed for: " + indexName, e);
            }
        } else {
            logIndexDoesNotExist(indexName);
        }
    }

    /**
     * Creates index with name.
     *
     * @param indexName The name of the index to be created.
     * @param type      Type for this index.
     */
    public void createIndexWithNestedPrefLabel(final String indexName, final String type) {
        final boolean exists = client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();
        if (!exists) {
            final CreateIndexRequestBuilder builder = client.admin().indices().prepareCreate(indexName);
            try {
                builder.setSettings(Settings.builder().loadFromSource(jsonBuilder()
                    .startObject()
                    .startObject("analysis")
                    .startObject("analyzer")
                    .startObject("analyzer_keyword")
                    .field("type", "custom")
                    .field("tokenizer", "keyword")
                    .field("filter", new String[]{"lowercase", "standard"})
                    .endObject()
                    .endObject()
                    .endObject()
                    .endObject().string(), XContentType.JSON)
                    .put(MAX_RESULT_WINDOW, MAX_RESULT_WINDOW_SIZE));
            } catch (final IOException e) {
                LOG.error("Error parsing index request settings JSON!", e);
            }
            if (ELASTIC_TYPE_CODESCHEME.equals(type)) {
                builder.addMapping(type, CODESCHEME_MAPPING, XContentType.JSON);
            } else if (ELASTIC_TYPE_CODE.equals(type)) {
                builder.addMapping(type, CODE_MAPPING, XContentType.JSON);
            } else {
                builder.addMapping(type, NESTED_PREFLABEL_MAPPING_JSON, XContentType.JSON);
            }
            final CreateIndexResponse response = builder.get();
            if (!response.isAcknowledged()) {
                logCreateFailed(indexName);
            } else {
                logCreateSuccessful(indexName);
            }
        } else {
            logIndexExist(indexName);
        }
    }

    public ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        return mapper;
    }

    private void logCreateFailed(final String indexName) {
        logIndex(true, indexName, "create failed.");
    }

    private void logCreateSuccessful(final String indexName) {
        logIndex(false, indexName, "created successfully.");
    }

    private void logAliasFailed(final String indexName) {
        logIndex(true, indexName, "aliasing failed.");
    }

    private void logAliasSuccessful(final String indexName) {
        logIndex(false, indexName, "alias set successfully.");
    }

    private void logAliasRemovedSuccessful(final String indexName) {
        logIndex(false, indexName, "alias removed successfully.");
    }

    private void logDeleteFailed(final String indexName) {
        logIndex(true, indexName, "delete failed.");
    }

    private void logDeleteSuccessful(final String indexName) {
        logIndex(false, indexName, "deleted successfully.");
    }

    private void logIndexExist(final String indexName) {
        logIndex(false, indexName, "already exists, nothing to create.");
    }

    private void logIndexDoesNotExist(final String indexName) {
        logIndex(false, indexName, "does not exist, nothing to remove.");
    }

    private void logIndex(final boolean error,
                          final String indexName,
                          final String text) {
        final String message = "ElasticSearch index: " + indexName + " " + text;
        if (error) {
            LOG.error(message);
        } else {
            LOG.info(message);
        }
    }
}