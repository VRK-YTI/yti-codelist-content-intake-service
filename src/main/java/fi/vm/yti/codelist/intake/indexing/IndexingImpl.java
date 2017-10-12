package fi.vm.yti.codelist.intake.indexing;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Singleton;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.IndexStatus;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.jpa.IndexStatusRepository;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

@Singleton
@Service
public class IndexingImpl implements Indexing {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingImpl.class);
    private static final String INDEX_STATUS_SUCCESSFUL = "successful";
    private static final String INDEX_STATUS_DELETED = "deleted";
    private static final String INDEX_STATUS_RUNNING = "running";
    private static final String INDEX_STATUS_FAILED = "failed";
    private static final String NAME_CODEREGISTRIES = "CodeRegistries";
    private static final String NAME_CODESCHEMES = "CodeSchemes";
    private static final String NAME_CODES = "Codes";
    private static final String BULK = "ElasticSearch bulk: ";
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeRepository codeRepository;
    private final IndexStatusRepository indexStatusRepository;
    private final Client client;
    private IndexingTools indexingTools;

    @Autowired
    public IndexingImpl(final IndexingTools indexingTools,
                        final Client client,
                        final IndexStatusRepository indexStatusRepository,
                        final CodeRegistryRepository codeRegistryRepository,
                        final CodeSchemeRepository codeSchemeRepository,
                        final CodeRepository codeRepository) {
        this.indexingTools = indexingTools;
        this.client = client;
        this.indexStatusRepository = indexStatusRepository;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
    }

    private boolean indexCodeRegistries(final String indexName) {
        final Set<CodeRegistry> codeRegistries = codeRegistryRepository.findAll();
        return indexType(codeRegistries, indexName, ELASTIC_TYPE_CODEREGISTRY, NAME_CODEREGISTRIES);
    }

    private boolean indexCodeSchemes(final String indexName) {
        final Set<CodeScheme> codeSchemes = codeSchemeRepository.findAll();
        return indexType(codeSchemes, indexName, ELASTIC_TYPE_CODESCHEME, NAME_CODESCHEMES);
    }

    private boolean indexCodes(final String indexName) {
        final Set<Code> regions = codeRepository.findAll();
        return indexType(regions, indexName, ELASTIC_TYPE_CODE, NAME_CODES);
    }

    private <T> boolean indexType(final Set<T> set,
                                  final String elasticIndex,
                                  final String elasticType,
                                  final String name) {
        boolean success;
        if (!set.isEmpty()) {
            final ObjectMapper mapper = indexingTools.createObjectMapper();
            final BulkRequestBuilder bulkRequest = client.prepareBulk();
            for (final T item : set) {
                try {
                    bulkRequest.add(client.prepareIndex(elasticIndex, elasticType).setSource(mapper.writeValueAsString(item)));
                } catch (JsonProcessingException e) {
                    logBulkErrorWithException(name, e);
                }
            }
            final BulkResponse response = bulkRequest.get();
            success = logBulkResponse(name, response);
        } else {
            noContent(name);
            success = false;
        }
        return success;
    }

    private void logBulkErrorWithException(final String name,
                                           final JsonProcessingException e) {
        LOG.error("Indexing " + name + " failed.", e);
    }

    private boolean logBulkResponse(final String type,
                                    final BulkResponse response) {
        if (response.hasFailures()) {
            LOG.error(BULK + type + " operation failed with errors: " + response.buildFailureMessage());
            return false;
        } else {
            LOG.info(BULK + type + " operation successfully persisted " + response.getItems().length + " items in " + response.getTookInMillis() + " ms.");
            return true;
        }
    }

    private void noContent(final String type) {
        LOG.info(BULK + type + " operation failed, no content to be indexed!");
    }

    public void reIndexEverything() {
        reIndex(ELASTIC_INDEX_CODEREGISTRY);
        reIndex(ELASTIC_INDEX_CODESCHEME);
        reIndex(ELASTIC_INDEX_CODE);
    }

    private void reIndex(final String indexName) {
        final List<IndexStatus> list = indexStatusRepository.getLatestRunningIndexStatusForIndexAlias(indexName);
        if (list.isEmpty()) {
            reIndexCodelist(indexName);
        } else {
            LOG.info("Indexing is already running for index: " + indexName);
        }
    }

    private void reIndexCodelist(final String indexAlias) {
        final String indexName = createIndexName(indexAlias);

        final IndexStatus status = new IndexStatus();
        final Date timeStamp = new Date(System.currentTimeMillis());
        status.setId(UUID.randomUUID().toString());
        status.setCreated(timeStamp);
        status.setModified(timeStamp);
        status.setStatus(INDEX_STATUS_RUNNING);
        status.setIndexAlias(indexName);
        status.setIndexName(indexName);
        indexStatusRepository.save(status);
        indexingTools.createIndexWithNestedPrefLabels(indexName);

        boolean success = true;
        if (!indexCodeRegistries(indexName)) {
            success = false;
        }
        if (!indexCodeSchemes(indexName)) {
            success = false;
        }
        if (!indexCodes(indexName)) {
            success = false;
        }
        if (success) {
            indexingTools.aliasIndex(indexName, indexAlias);
            final List<IndexStatus> earlierStatuses = indexStatusRepository.getLatestSuccessfulIndexStatusForIndexAlias(indexAlias);
            earlierStatuses.forEach(index -> {
                indexingTools.deleteIndex(index.getIndexName());
                index.setModified(timeStamp);
                index.setStatus(INDEX_STATUS_DELETED);
                indexStatusRepository.save(index);
            });
            status.setStatus(INDEX_STATUS_SUCCESSFUL);
        } else {
            status.setStatus(INDEX_STATUS_FAILED);
            indexingTools.deleteIndex(indexName);
        }
        indexStatusRepository.save(status);
    }

    private String createIndexName(final String indexName) {
        return indexName + "_" + System.currentTimeMillis();
    }
}
