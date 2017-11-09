package fi.vm.yti.codelist.intake.indexing;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.inject.Singleton;

import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import static fi.vm.yti.codelist.common.constants.ApiConstants.ELASTIC_TYPE_CODESCHEME;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@Singleton
@Service
public class IndexingToolsImpl implements IndexingTools {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingToolsImpl.class);
    private static final String MAX_RESULT_WINDOW = "max_result_window";
    private static final int MAX_RESULT_WINDOW_SIZE = 500000;

    private static final String NESTED_PREFLABELS_MAPPING_JSON = "{" +
        "\"properties\": {\n" +
        "  \"codeValue\": {\n" +
        "    \"type\": \"text\"," +
        "    \"analyzer\": \"lowercase_analyzer\",\n" +
        "    \"fields\": {\n" +
        "      \"keyword\": { \n" +
        "        \"ignore_above\": 256," +
        "        \"type\": \"keyword\"\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"id\": {\n" +
        "    \"type\": \"text\"},\n" +
        "  \"prefLabels\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  }\n" +
        "}\n}";

    private static final String CODESCHEME_MAPPING = "{" +
        "\"properties\": {\n" +
        "  \"codeValue\": {\n" +
        "    \"type\": \"text\"," +
        "    \"analyzer\": \"lowercase_analyzer\",\n" +
        "    \"fields\": {\n" +
        "      \"keyword\": { \n" +
        "        \"ignore_above\": 256," +
        "        \"type\": \"keyword\"\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"id\": {\n" +
        "    \"type\": \"text\"},\n" +
        "  \"prefLabels\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  },\n" +
        "  \"serviceClassifications\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  }\n" +
        "}\n}";

    private Client client;

    @Autowired
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
            final GetAliasesResponse aliasesResponse = client.admin().indices().getAliases(new GetAliasesRequest(aliasName)).actionGet();
            final ImmutableOpenMap<String, List<AliasMetaData>> aliases = aliasesResponse.getAliases();
            for (final ObjectCursor<?> cursor : aliases.keys()) {
                request.addAliasAction(IndicesAliasesRequest.AliasActions.remove().alias(aliasName).index(cursor.value.toString()));
            }
            request.addAliasAction(IndicesAliasesRequest.AliasActions.add().alias(aliasName).index(indexName));
            final IndicesAliasesResponse response = client.admin().indices().aliases(request).actionGet();
            if (!response.isAcknowledged()) {
                logAliasFailed(indexName);
            } else {
                logAliasSuccessful(indexName);
                for (final ObjectCursor<?> cursor : aliases.keys()) {
                    logAliasRemovedSuccessful(cursor.value.toString());
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
     * @param types     Set of types for this index.
     */
    public void createIndexWithNestedPrefLabels(final String indexName, final Set<String> types) {
        final boolean exists = client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();
        if (!exists) {
            final CreateIndexRequestBuilder builder = client.admin().indices().prepareCreate(indexName);
            try {
                builder.setSettings(Settings.builder().loadFromSource(jsonBuilder()
                    .startObject()
                    .startObject("analysis")
                    .startObject("analyzer")
                    .startObject("lowercase_analyzer")
                    .field("type", "custom")
                    .field("tokenizer", "keyword")
                    .field("filter", new String[]{"lowercase", "standard"})
                    .endObject()
                    .endObject()
                    .endObject()
                    .endObject().string(), XContentType.JSON)
                    .put(MAX_RESULT_WINDOW, MAX_RESULT_WINDOW_SIZE));
            } catch (IOException e) {
                LOG.error("Error parsing index request settings JSON!", e);
            }
            for (final String type : types) {
                if (ELASTIC_TYPE_CODESCHEME.equals(type)) {
                    builder.addMapping(type, CODESCHEME_MAPPING, XContentType.JSON);
                } else {
                    builder.addMapping(type, NESTED_PREFLABELS_MAPPING_JSON, XContentType.JSON);
                }
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