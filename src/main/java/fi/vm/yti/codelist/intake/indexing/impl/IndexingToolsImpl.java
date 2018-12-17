package fi.vm.yti.codelist.intake.indexing.impl;

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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.indexing.IndexingTools;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_500;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@Singleton
@Component
public class IndexingToolsImpl implements IndexingTools {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingToolsImpl.class);
    private static final String MAX_RESULT_WINDOW = "max_result_window";
    private static final int MAX_RESULT_WINDOW_SIZE = 50000;

    private static final String NESTED_PREFLABEL_MAPPING_JSON = "{" +
        "\"properties\": {\n" +
        "  \"codeValue\": {\n" +
        "    \"type\": \"text\"," +
        "    \"analyzer\": \"text_analyzer\",\n" +
        "    \"fields\": {\n" +
        "      \"raw\": { \n" +
        "        \"type\": \"keyword\",\n" +
        "        \"normalizer\": \"keyword_normalizer\"\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"id\": {\n" +
        "    \"type\": \"keyword\"" +
        "  },\n" +
        "  \"prefLabel\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  }\n" +
        "}\n}";

    private static final String CODESCHEME_MAPPING = "{" +
        "\"dynamic_templates\": [\n" +
        "  {\n" +
        "    \"prefLabel\": {\n" +
        "      \"path_match\": \"prefLabel.*\",\n" +
        "      \"mapping\": {\n" +
        "        \"type\": \"text\",\n" +
        "        \"analyzer\": \"preflabel_analyzer\",\n" +
        "        \"fields\": {\n" +
        "          \"keyword\": { \n" +
        "            \"type\": \"keyword\",\n" +
        "            \"normalizer\": \"keyword_normalizer\"\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "],\n" +
        "\"properties\": {\n" +
        "  \"codeValue\": {\n" +
        "    \"type\": \"text\"," +
        "    \"analyzer\": \"text_analyzer\",\n" +
        "    \"fields\": {\n" +
        "      \"raw\": { \n" +
        "        \"type\": \"keyword\",\n" +
        "        \"normalizer\": \"keyword_normalizer\"\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"id\": {\n" +
        "    \"type\": \"keyword\"" +
        "  },\n" +
        "  \"prefLabel\": {\n" +
        "    \"type\": \"nested\",\n" +
        "    \"dynamic\": true\n" +
        "  },\n" +
        "  \"infoDomains\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  },\n" +
        "  \"organizations\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  },\n" +
        "  \"codeRegistry\": {\n" +
        "    \"properties\": {\n" +
        "      \"codeValue\": {\n" +
        "        \"type\": \"text\",\n" +
        "        \"analyzer\": \"text_analyzer\"\n" +
        "      },\n" +
        "      \"organizations\": {\n" +
        "        \"type\": \"nested\"\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"extensions\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  },\n" +
        "  \"externalReferences\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  }\n" +
        "}\n}";

    private static final String CODE_MAPPING = "{" +
        "\"properties\": {\n" +
        "  \"codeValue\": {\n" +
        "    \"type\": \"text\"," +
        "    \"analyzer\": \"text_analyzer\",\n" +
        "    \"fields\": {\n" +
        "      \"raw\": { \n" +
        "        \"type\": \"keyword\",\n" +
        "        \"normalizer\": \"keyword_normalizer\"\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"id\": {\n" +
        "    \"type\": \"keyword\"\n" +
        "  },\n" +
        "  \"order\": {\n" +
        "    \"type\": \"integer\"\n" +
        "  },\n" +
        "  \"prefLabel\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  },\n" +
        "  \"codeScheme\": {\n" +
        "    \"properties\": {\n" +
        "      \"codeValue\": {\n" +
        "        \"type\": \"text\",\n" +
        "        \"analyzer\": \"text_analyzer\"\n" +
        "      },\n" +
        "      \"organizations\": {\n" +
        "        \"type\": \"nested\"\n" +
        "      },\n" +
        "      \"codeRegistry\": {\n" +
        "        \"properties\": {\n" +
        "          \"codeValue\": {\n" +
        "            \"type\": \"text\",\n" +
        "            \"analyzer\": \"text_analyzer\"\n" +
        "          },\n" +
        "          \"organizations\": {\n" +
        "            \"type\": \"nested\"\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"members\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  },\n" +
        "  \"externalReferences\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  }\n" +
        "}\n}";

    private static final String EXTENSION_MAPPING = "{" +
        "\"properties\": {\n" +
        "  \"codeValue\": {\n" +
        "    \"type\": \"text\"," +
        "    \"analyzer\": \"text_analyzer\",\n" +
        "    \"fields\": {\n" +
        "      \"raw\": { \n" +
        "        \"type\": \"keyword\",\n" +
        "        \"normalizer\": \"keyword_normalizer\"\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"id\": {\n" +
        "    \"type\": \"keyword\"\n" +
        "  },\n" +
        "  \"prefLabel\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  },\n" +
        "  \"parentCodeScheme\": {\n" +
        "    \"properties\": {\n" +
        "      \"id\": {\n" +
        "        \"type\": \"keyword\"\n" +
        "      },\n" +
        "      \"codeValue\": {\n" +
        "        \"type\": \"text\",\n" +
        "        \"analyzer\": \"text_analyzer\"\n" +
        "      },\n" +
        "      \"codeRegistry\": {\n" +
        "        \"properties\": {\n" +
        "          \"id\": {\n" +
        "            \"type\": \"keyword\"\n" +
        "          },\n" +
        "          \"codeValue\": {\n" +
        "            \"type\": \"text\",\n" +
        "            \"analyzer\": \"text_analyzer\"\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"members\": {\n" +
        "    \"type\": \"nested\",\n" +
        "    \"properties\": {\n" +
        "      \"order\": {\n" +
        "        \"type\": \"integer\"\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}\n}";

    private static final String MEMBER_MAPPING = "{" +
        "\"properties\": {\n" +
        "  \"id\": {\n" +
        "    \"type\": \"keyword\"\n" +
        "  },\n" +
        "  \"prefLabel\": {\n" +
        "    \"type\": \"nested\"\n" +
        "  },\n" +
        "  \"order\": {\n" +
        "    \"type\": \"integer\"\n" +
        "  },\n" +
        "  \"extension\": {\n" +
        "    \"properties\": {\n" +
        "      \"id\": {\n" +
        "        \"type\": \"keyword\"\n" +
        "      },\n" +
        "      \"codeValue\": {\n" +
        "        \"type\": \"text\",\n" +
        "        \"analyzer\": \"text_analyzer\"\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}\n}";

    private final Client client;

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
            } catch (final ExecutionException e) {
                LOG.error("Deleting ElasticSearch index failed for: " + indexName, e);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
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
    public void createIndexWithNestedPrefLabel(final String indexName,
                                               final String type) {
        final boolean exists = client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();
        if (!exists) {
            final CreateIndexRequestBuilder builder = client.admin().indices().prepareCreate(indexName);
            try {
                builder.setSettings(Settings.builder().loadFromSource(jsonBuilder()
                    .startObject()
                    .startObject("index")
                    .field(MAX_RESULT_WINDOW, MAX_RESULT_WINDOW_SIZE)
                    .endObject()
                    .startObject("analysis")
                    .startObject("analyzer")
                    .startObject("text_analyzer")
                    .field("type", "custom")
                    .field("tokenizer", "keyword")
                    .field("filter", new String[]{ "standard", "lowercase", "trim" })
                    .endObject()
                    .startObject("preflabel_analyzer")
                    .field("type", "custom")
                    .field("tokenizer", "ngram")
                    .field("filter", new String[]{ "lowercase", "standard" })
                    .endObject()
                    .endObject()
                    .startObject("normalizer")
                    .startObject("keyword_normalizer")
                    .field("type", "custom")
                    .field("filter", new String[]{ "lowercase" })
                    .endObject()
                    .endObject()
                    .endObject()
                    .endObject().string(), XContentType.JSON));
            } catch (final IOException e) {
                LOG.error("Error parsing index request settings JSON!", e);
            }
            switch (type) {
                case ELASTIC_TYPE_CODESCHEME:
                    builder.addMapping(type, CODESCHEME_MAPPING, XContentType.JSON);
                    break;
                case ELASTIC_TYPE_CODE:
                    builder.addMapping(type, CODE_MAPPING, XContentType.JSON);
                    break;
                case ELASTIC_TYPE_EXTENSION:
                    builder.addMapping(type, EXTENSION_MAPPING, XContentType.JSON);
                    break;
                case ELASTIC_TYPE_MEMBER:
                    builder.addMapping(type, MEMBER_MAPPING, XContentType.JSON);
                    break;
                default:
                    builder.addMapping(type, NESTED_PREFLABEL_MAPPING_JSON, XContentType.JSON);
                    break;
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
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
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