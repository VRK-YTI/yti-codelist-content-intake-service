package fi.vm.yti.codelist.intake.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.constants.ApiConstants;
import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;

@Singleton
@Service
public class DomainImpl implements Domain {

    private static final Logger LOG = LoggerFactory.getLogger(DomainImpl.class);
    private static final String MAX_RESULT_WINDOW = "max_result_window";
    private static final int MAX_RESULT_WINDOW_SIZE = 500000;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeRepository codeRepository;
    private Client client;

    @Inject
    private DomainImpl(final Client client,
                       final CodeRegistryRepository codeRegistryRepository,
                       final CodeSchemeRepository codeSchemeRepository,
                       final CodeRepository codeRepository) {
        this.client = client;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
    }

    /**
     * Indexing
     */

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
                    LOG.error("Deleting ElasticSearch index: " + indexName + " failed.");
                } else {
                    LOG.info("ElasticSearch index: " + indexName + " deleted successfully.");
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Deleting ElasticSearch index: " + indexName + " failed with error: " + e.getMessage());
            }
        } else {
            LOG.info("Index " + indexName + " did not exist in Elastic yet, so nothing to clear.");
        }
    }

    /**
     * Delete type from index.
     *
     * @param indexName The name of the index to be deleted.
     * @param indexType The name of the type of index to be deleted.
     */
    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    public void deleteTypeFromIndex(final String indexName, final String indexType) {
        final boolean exists = client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();

        if (exists) {
            LOG.info("Clearing index " + indexName + " type " + indexType + ".");
            DeleteByQueryAction.INSTANCE.newRequestBuilder(client).filter(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("_type", indexType))).source(indexName).get();
        } else {
            LOG.info("Index " + indexName + " did not exist in Elastic yet, so nothing to clear.");
        }
    }

    /**
     * Delete type from index.
     *
     * @param indexName The name of the index to be deleted.
     */
    public void createIndex(final String indexName) {
        final boolean exists = client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();

        if (!exists) {
            final CreateIndexResponse response = client.admin().indices().prepareCreate(indexName).get();
            if (!response.isAcknowledged()) {
                LOG.error("Creating ElasticSearch index: " + indexName + " failed.");
            } else {
                LOG.info("ElasticSearch index: " + indexName + " created successfully.");
            }
        } else {
            LOG.info("Index " + indexName + " already exists, nothing to create.");
        }
    }

    /**
     * Delete type from index.
     *
     * @param indexName The name of the index to be deleted.
     * @param indexType The name of the type of index to be deleted.
     */
    public void ensureNestedPrefLabelsMapping(final String indexName, final String indexType) {
        final String nestedPrefLabelsMappingJson = "{\"properties\": {\n" +
                "  \"prefLabels\": {\n" +
                "    \"type\": \"nested\"\n" +
                "  }\n" +
                "}\n}";

        final PutMappingRequest mappingRequest = new PutMappingRequest(indexName);
        mappingRequest.type(indexType);
        mappingRequest.source(nestedPrefLabelsMappingJson);
        client.admin().indices().putMapping(mappingRequest).actionGet();
    }

    /**
     * Creates index with name.
     *
     * @param indexName The name of the index to be deleted.
     */
    public void createIndexWithNestedPrefLabels(final String indexName) {
        final List<String> types = new ArrayList<>();

        final boolean exists = client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();
        final String nestedPrefLabelsMappingJson = "{" +
                "\"properties\": {\n" +
                "  \"prefLabels\": {\n" +
                "    \"type\": \"nested\"\n" +
                "  }\n" +
                "}\n}";

        if (!exists) {
            final CreateIndexRequestBuilder builder = client.admin().indices().prepareCreate(indexName);
            builder.setSettings(Settings.builder().put(MAX_RESULT_WINDOW, MAX_RESULT_WINDOW_SIZE));

            for (final String type : types) {
                builder.addMapping(type, nestedPrefLabelsMappingJson);
            }
            final CreateIndexResponse response = builder.get();
            if (!response.isAcknowledged()) {
                LOG.error("Creating ElasticSearch index: " + indexName + " failed.");
            } else {
                LOG.info("ElasticSearch index: " + indexName + " created successfully.");
            }
        } else {
            LOG.info("Index " + indexName + " already exists, nothing to create.");
        }
    }

    /**
     * Creates index with name.
     *
     * @param indexName The name of the index to be deleted.
     */
    public void createIndexWithNestedPrefLabels(final String indexName, final String type) {
        final boolean exists = client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();
        final String nestedNamesMappingJson = "{" +
                "\"properties\": {\n" +
                "  \"prefLabels\": {\n" +
                "    \"type\": \"nested\"\n" +
                "  }\n" +
                "}\n}";

        if (!exists) {
            final CreateIndexRequestBuilder builder = client.admin().indices().prepareCreate(indexName);
            builder.setSettings(Settings.builder().put(MAX_RESULT_WINDOW, MAX_RESULT_WINDOW_SIZE));

            builder.addMapping(type, nestedNamesMappingJson);
            final CreateIndexResponse response = builder.get();
            if (!response.isAcknowledged()) {
                LOG.error("Creating ElasticSearch index: " + indexName + " failed.");
            } else {
                LOG.info("ElasticSearch index: " + indexName + " created successfully.");
            }
        } else {
            LOG.info("Index " + indexName + " already exists, ensuring mapping.");
            client.admin().indices().preparePutMapping(indexName).setType(type).setSource(nestedNamesMappingJson).execute().actionGet();
        }
    }

    /**
     * Refreshes index with name.
     *
     * @param indexName The name of the index to be refreshed.
     */
    public void refreshIndex(final String indexName) {
        final FlushRequest request = new FlushRequest(indexName);
        try {
            client.admin().indices().flush(request).get();
            LOG.info("ElasticSearch index: " + indexName + " flushed.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Flushing ElasticSearch index: " + indexName + " failed with error: " + e.getMessage());
        }
    }

    public void persistCodeRegistries(final List<CodeRegistry> codeRegistries) {
        codeRegistryRepository.save(codeRegistries);
    }

    public void indexCodeRegistries() {
        final List<CodeRegistry> codeRegistries = codeRegistryRepository.findAll();
        if (!codeRegistries.isEmpty()) {
            final BulkRequestBuilder bulkRequest = client.prepareBulk();
            codeRegistries.forEach(codeRegistry -> {
                final ObjectMapper mapper = createObjectMapper();
                try {
                    bulkRequest.add(client.prepareIndex(ApiConstants.ELASTIC_INDEX_CODEREGISTRIES, ApiConstants.ELASTIC_TYPE_CODEREGISTRY).setSource(mapper.writeValueAsString(codeRegistry)));
                } catch (JsonProcessingException e) {
                    LOG.error("Indexing coderegistries failed: " + e.getMessage());
                }
            });

            final BulkResponse response = bulkRequest.get();
            if (response.hasFailures()) {
                LOG.error("ElasticSearch bulk coderegistries operation failed with errors: " + response.buildFailureMessage());
            } else {
                LOG.info("ElasticSearch bulk coderegistries request successfully persisted " + response.getItems().length + " items in " + response.getTookInMillis() + " ms.");
            }
        } else {
            LOG.info("ElasticSearch bulk coderegistries request failed: no content to be indexed!");
        }
    }

    public void persistCodeSchemes(final List<CodeScheme> codeSchemes) {
        codeSchemeRepository.save(codeSchemes);
    }

    public void indexCodeSchemes() {
        final List<CodeScheme> codeSchemes = codeSchemeRepository.findAll();
        if (!codeSchemes.isEmpty()) {
            final BulkRequestBuilder bulkRequest = client.prepareBulk();
            codeSchemes.forEach(codeScheme -> {
                final ObjectMapper mapper = createObjectMapper();
                try {
                    bulkRequest.add(client.prepareIndex(ApiConstants.ELASTIC_INDEX_CODESCHEMES, ApiConstants.ELASTIC_TYPE_CODESCHEME).setSource(mapper.writeValueAsString(codeScheme)));
                } catch (JsonProcessingException e) {
                    LOG.error("Indexing codeSchemes failed: " + e.getMessage());
                }
            });

            final BulkResponse response = bulkRequest.get();
            if (response.hasFailures()) {
                LOG.error("ElasticSearch bulk codeschemes operation failed with errors: " + response.buildFailureMessage());
            } else {
                LOG.info("ElasticSearch bulk codeschemes request successfully persisted " + response.getItems().length + " items in " + response.getTookInMillis() + " ms.");
            }
        } else {
            LOG.info("ElasticSearch bulk codeschemes request failed: no content to be indexed!");
        }
    }

    public void indexCodes() {
        final List<Code> codes = codeRepository.findAll();

        if (!codes.isEmpty()) {
            final ObjectMapper mapper = createObjectMapper();
            final BulkRequestBuilder bulkRequest = client.prepareBulk();
            codes.forEach(code -> {
                try {
                    bulkRequest.add(client.prepareIndex(ApiConstants.ELASTIC_INDEX_CODES, ApiConstants.ELASTIC_TYPE_CODE).setSource(mapper.writeValueAsString(code)));
                } catch (JsonProcessingException e) {
                    LOG.error("Indexing codes failed: " + e.getMessage());
                }
            });

            final BulkResponse response = bulkRequest.get();
            if (response.hasFailures()) {
                LOG.error("ElasticSearch bulk codes operation failed with errors: " + response.buildFailureMessage());
            } else {
                LOG.info("ElasticSearch bulk codes request successfully persisted " + response.getItems().length + " items in " + response.getTookInMillis() + " ms.");
            }
        } else {
            LOG.info("ElasticSearch bulk codes request failed: no content to be indexed!");
        }
    }

    public void persistCodes(final List<Code> codes) {
        codeRepository.save(codes);
    }

    public void reIndexCodeRegistries() {
        deleteIndex(ApiConstants.ELASTIC_INDEX_CODEREGISTRIES);
        final List<CodeRegistry> codeRegistry = codeRegistryRepository.findAll();
        if (!codeRegistry.isEmpty()) {
            createIndexWithNestedPrefLabels(ApiConstants.ELASTIC_INDEX_CODEREGISTRIES, ApiConstants.ELASTIC_TYPE_CODEREGISTRY);
            indexCodeRegistries();
        }
    }

    public void reIndexCodeSchemes() {
        deleteIndex(ApiConstants.ELASTIC_INDEX_CODESCHEMES);
        final List<CodeScheme> codeSchemes = codeSchemeRepository.findAll();
        if (!codeSchemes.isEmpty()) {
            createIndexWithNestedPrefLabels(ApiConstants.ELASTIC_INDEX_CODESCHEMES, ApiConstants.ELASTIC_TYPE_CODESCHEME);
            indexCodeSchemes();
        }
    }

    public void reIndexCodes() {
        deleteIndex(ApiConstants.ELASTIC_INDEX_CODES);
        final List<Code> codes = codeRepository.findAll();
        if (!codes.isEmpty()) {
            createIndexWithNestedPrefLabels(ApiConstants.ELASTIC_INDEX_CODES, ApiConstants.ELASTIC_TYPE_CODE);
            indexCodes();
        }
    }

    public void reIndexYti() {
    }

    public void reIndexEverything() {
        reIndexCodeRegistries();
        reIndexCodeSchemes();
        reIndexCodes();
    }

    private ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        return mapper;
    }

}
