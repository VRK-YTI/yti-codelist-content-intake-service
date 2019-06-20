package fi.vm.yti.codelist.intake.indexing.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

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
import static fi.vm.yti.codelist.intake.util.FileUtils.loadFileFromClassPath;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@Singleton
@Component
public class IndexingToolsImpl implements IndexingTools {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingToolsImpl.class);
    private static final String MAX_RESULT_WINDOW = "max_result_window";
    private static final int MAX_RESULT_WINDOW_SIZE = 50000;
    private static final String MAX_INDEX_FIELDS = "mapping.total_fields.limit";
    private static final int MAX_INDEX_FIELDS_SIZE = 5000;

    private final RestHighLevelClient client;

    @Inject
    public IndexingToolsImpl(final RestHighLevelClient client) {
        this.client = client;
    }

    private boolean checkIfIndexExists(final String indexName) {
        final GetIndexRequest request = new GetIndexRequest();
        request.indices(indexName);
        try {
            return client.indices().exists(request, RequestOptions.DEFAULT);
        } catch (final IOException e) {
            LOG.error("Index checking request failed for index: " + indexName, e);
            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "ElasticSearch index exists query error!"));
        }
    }

    public void aliasIndex(final String indexName,
                           final String aliasName) {
        if (checkIfIndexExists(indexName)) {
            final IndicesAliasesRequest request = new IndicesAliasesRequest();
            final GetAliasesRequest getAliasesRequest = new GetAliasesRequest(aliasName);
            try {
                final GetAliasesResponse aliasGetResponse = client.indices().getAlias(getAliasesRequest, RequestOptions.DEFAULT);
                final List<String> oldIndexNames = new ArrayList<>();
                for (final Map.Entry<String, Set<AliasMetaData>> aliasEntry : aliasGetResponse.getAliases().entrySet()) {
                    final String alias = aliasEntry.getKey();
                    final Set<AliasMetaData> value = aliasEntry.getValue();
                    if (!value.isEmpty()) {
                        request.addAliasAction(IndicesAliasesRequest.AliasActions.remove().alias(aliasName).index(alias));
                        oldIndexNames.add(alias);
                    }
                }
                request.addAliasAction(IndicesAliasesRequest.AliasActions.add().alias(aliasName).index(indexName));
                final AcknowledgedResponse response = client.indices().updateAliases(request, RequestOptions.DEFAULT);
                if (!response.isAcknowledged()) {
                    logAliasFailed(indexName);
                } else {
                    logAliasSuccessful(indexName);
                    for (final String oldIndexName : oldIndexNames) {
                        logAliasRemovedSuccessful(oldIndexName);
                    }
                }
            } catch (final IOException e) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
            }
        } else {
            logIndexDoesNotExist(indexName);
        }
    }

    public void deleteIndex(final String indexName) {
        final DeleteIndexRequest request = new DeleteIndexRequest(indexName);
        if (checkIfIndexExists(indexName)) {
            try {
                final AcknowledgedResponse response = client.indices().delete(request, RequestOptions.DEFAULT);
                if (!response.isAcknowledged()) {
                    logDeleteFailed(indexName);
                } else {
                    logDeleteSuccessful(indexName);
                }
            } catch (final IOException e) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
            }
        } else {
            logIndexDoesNotExist(indexName);
        }
    }

    public void createIndexWithNestedPrefLabel(final String indexName,
                                               final String type) {
        if (!checkIfIndexExists(indexName)) {
            final CreateIndexRequest request = new CreateIndexRequest();
            request.index(indexName);
            try {
                final XContentBuilder contentBuilder = jsonBuilder()
                    .startObject()
                    .startObject("index")
                    .field(MAX_RESULT_WINDOW, MAX_RESULT_WINDOW_SIZE)
                    .field(MAX_INDEX_FIELDS, MAX_INDEX_FIELDS_SIZE)
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
                    .endObject();
                request.source(contentBuilder);
            } catch (final IOException e) {
                LOG.error("Error parsing index request settings JSON!", e);
            }
            switch (type) {
                case ELASTIC_TYPE_CODESCHEME:
                    request.mapping(type, getCodeSchemeMapping(), XContentType.JSON);
                    break;
                case ELASTIC_TYPE_CODE:
                    request.mapping(type, getCodeMapping(), XContentType.JSON);
                    break;
                case ELASTIC_TYPE_EXTENSION:
                    request.mapping(type, getExtensionMapping(), XContentType.JSON);
                    break;
                case ELASTIC_TYPE_MEMBER:
                    request.mapping(type, getMemberMapping(), XContentType.JSON);
                    break;
                default:
                    request.mapping(type, getGenericPrefLabelMapping(), XContentType.JSON);
                    break;
            }
            try {
                final CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
                if (!response.isAcknowledged()) {
                    logCreateFailed(indexName);
                } else {
                    logCreateSuccessful(indexName);
                }
            } catch (final IOException e) {
                LOG.error("Error creating index JSON!", e);
            }
        } else {
            logIndexExist(indexName);
        }
    }

    private String getCodeSchemeMapping() {
        return loadMapping("/esmappings/codescheme_mapping.json");
    }

    private String getCodeMapping() {
        return loadMapping("/esmappings/code_mapping.json");
    }

    private String getExtensionMapping() {
        return loadMapping("/esmappings/extension_mapping.json");
    }

    private String getMemberMapping() {
        return loadMapping("/esmappings/member_mapping.json");
    }

    private String getGenericPrefLabelMapping() {
        return loadMapping("/esmappings/generic_nested_preflabel_mapping.json");
    }

    private String loadMapping(final String fileName) {
        try {
            final InputStream inputStream = loadFileFromClassPath(fileName);
            final ObjectMapper objectMapper = createObjectMapper();
            final Object obj = objectMapper.readTree(inputStream);
            return objectMapper.writeValueAsString(obj);
        } catch (final IOException e) {
            LOG.error("Index mapping loading error for file: " + fileName);
            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "ElasticSearch index mapping loading error for file: " + fileName));
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