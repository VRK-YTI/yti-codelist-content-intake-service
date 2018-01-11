package fi.vm.yti.codelist.intake.indexing;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Singleton;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.codelist.common.model.AbstractIdentifyableCode;
import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.ExternalReference;
import fi.vm.yti.codelist.common.model.IndexStatus;
import fi.vm.yti.codelist.common.model.PropertyType;
import fi.vm.yti.codelist.common.model.Views;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.jpa.ExternalReferenceRepository;
import fi.vm.yti.codelist.intake.jpa.IndexStatusRepository;
import fi.vm.yti.codelist.intake.jpa.PropertyTypeRepository;
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
    private static final String NAME_EXTERNALREFERENCES = "ExternalReferences";
    private static final String NAME_PROPERTYTYPES = "PropertyTypes";
    private static final String BULK = "ElasticSearch bulk: ";
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeRepository codeRepository;
    private final IndexStatusRepository indexStatusRepository;
    private final ExternalReferenceRepository externalReferenceRepository;
    private final PropertyTypeRepository propertyTypeRepository;
    private final Client client;
    private IndexingTools indexingTools;

    @Autowired
    public IndexingImpl(final IndexingTools indexingTools,
                        final Client client,
                        final IndexStatusRepository indexStatusRepository,
                        final CodeRegistryRepository codeRegistryRepository,
                        final CodeSchemeRepository codeSchemeRepository,
                        final CodeRepository codeRepository,
                        final ExternalReferenceRepository externalReferenceRepository,
                        final PropertyTypeRepository propertyTypeRepository) {
        this.indexingTools = indexingTools;
        this.client = client;
        this.indexStatusRepository = indexStatusRepository;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
        this.externalReferenceRepository = externalReferenceRepository;
        this.propertyTypeRepository = propertyTypeRepository;
    }

    private boolean indexCodeRegistries(final String indexName) {
        final Set<CodeRegistry> codeRegistries = codeRegistryRepository.findAll();
        return indexData(codeRegistries, indexName, ELASTIC_TYPE_CODEREGISTRY, NAME_CODEREGISTRIES, Views.Normal.class);
    }

    private boolean indexCodeSchemes(final String indexName) {
        final Set<CodeScheme> codeSchemes = codeSchemeRepository.findAll();
        return indexData(codeSchemes, indexName, ELASTIC_TYPE_CODESCHEME, NAME_CODESCHEMES, Views.ExtendedCodeScheme.class);
    }

    private boolean indexCodes(final String indexName) {
        final Set<Code> regions = codeRepository.findAll();
        return indexData(regions, indexName, ELASTIC_TYPE_CODE, NAME_CODES, Views.ExtendedCode.class);
    }

    private boolean indexPropertyTypes(final String indexName) {
        final Set<PropertyType> propertyTypes = propertyTypeRepository.findAll();
        return indexData(propertyTypes, indexName, ELASTIC_TYPE_PROPERTYTYPE, NAME_PROPERTYTYPES, Views.Normal.class);
    }

    private boolean indexExternalReferences(final String indexName) {
        final Set<ExternalReference> externalReferences = externalReferenceRepository.findAll();
        return indexData(externalReferences, indexName, ELASTIC_TYPE_EXTERNALREFERENCE, NAME_EXTERNALREFERENCES, Views.ExtendedExternalReference.class);
    }

    private <T> boolean indexData(final Set<T> set,
                                  final String elasticIndex,
                                  final String elasticType,
                                  final String name,
                                  final Class<?> jsonViewClass) {
        boolean success;
        if (!set.isEmpty()) {
            final ObjectMapper mapper = indexingTools.createObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            final BulkRequestBuilder bulkRequest = client.prepareBulk();
            for (final T item : set) {
                try {
                    final AbstractIdentifyableCode identifyableCode = (AbstractIdentifyableCode) item;
                    bulkRequest.add(client.prepareIndex(elasticIndex, elasticType, identifyableCode.getId().toString()).setSource(mapper.writerWithView(jsonViewClass).writeValueAsString(item), XContentType.JSON));
                    bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
                } catch (JsonProcessingException e) {
                    logBulkErrorWithException(name, e);
                }
            }
            final BulkResponse response = bulkRequest.get();
            success = logBulkResponse(name, response);
        } else {
            noContent(name);
            success = true;
        }
        return success;
    }

    private <T> boolean updateData(final Set<T> set,
                                   final String elasticIndex,
                                   final String elasticType,
                                   final String name,
                                   final Class<?> jsonViewClass) {
        boolean success;
        if (!set.isEmpty()) {
            final ObjectMapper mapper = indexingTools.createObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
            final BulkRequestBuilder bulkRequest = client.prepareBulk();
            for (final T item : set) {
                try {
                    final AbstractIdentifyableCode identifyableCode = (AbstractIdentifyableCode) item;
                    final String doc = mapper.writerWithView(jsonViewClass).writeValueAsString(item);
                    bulkRequest.add(client.prepareUpdate(elasticIndex, elasticType, identifyableCode.getId().toString()).setDoc(doc, XContentType.JSON).setUpsert(doc, XContentType.JSON));
                    bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
                } catch (JsonProcessingException e) {
                    logBulkErrorWithException(name, e);
                }
            }
            final BulkResponse response = bulkRequest.get();
            success = logBulkResponse(name, response);
        } else {
            noContent(name);
            success = true;
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
            LOG.info(BULK + type + " operation successfully indexed " + response.getItems().length + " items in " + response.getTook().millis() + " ms.");
            return true;
        }
    }

    private void noContent(final String type) {
        LOG.info(BULK + type + " operation ran, but there was no content to be indexed!");
    }

    public boolean updateCodes(final Set<Code> codes) {
        boolean success = true;
        if (!codes.isEmpty()) {
            success = updateData(codes, ELASTIC_INDEX_CODE, ELASTIC_TYPE_CODE, NAME_CODES, Views.ExtendedCode.class);
        }
        return success;
    }

    public boolean updateCode(final Code code) {
        final Set<Code> codes = new HashSet<>();
        codes.add(code);
        return updateCodes(codes);
    }

    public boolean updateCodeSchemes(final Set<CodeScheme> codeSchemes) {
        boolean success = true;
        if (!codeSchemes.isEmpty()) {
            return updateData(codeSchemes, ELASTIC_INDEX_CODESCHEME, ELASTIC_TYPE_CODESCHEME, NAME_CODESCHEMES, Views.ExtendedCodeScheme.class);
        }
        return success;
    }

    public boolean updateCodeScheme(final CodeScheme codeScheme) {
        final Set<CodeScheme> codeSchemes = new HashSet<>();
        codeSchemes.add(codeScheme);
        return updateCodeSchemes(codeSchemes);
    }

    public boolean updateExternalReferences(final Set<ExternalReference> externalReferences) {
        boolean success = true;
        if (!externalReferences.isEmpty()) {
            return updateData(externalReferences, ELASTIC_INDEX_EXTERNALREFERENCE, ELASTIC_TYPE_EXTERNALREFERENCE, NAME_EXTERNALREFERENCES, Views.ExtendedExternalReference.class);
        }
        return success;
    }

    public boolean reIndexEverything() {
        boolean success = true;
        if (!reIndex(ELASTIC_INDEX_CODEREGISTRY, ELASTIC_TYPE_CODEREGISTRY)) {
            success = false;
        }
        if (reIndex(ELASTIC_INDEX_CODESCHEME, ELASTIC_TYPE_CODESCHEME)) {
            success = false;
        }
        if (!reIndex(ELASTIC_INDEX_CODE, ELASTIC_TYPE_CODE)) {
            success = false;
        }
        if (reIndex(ELASTIC_INDEX_PROPERTYTYPE, ELASTIC_TYPE_PROPERTYTYPE)) {
            success = false;
        }
        if (reIndex(ELASTIC_INDEX_EXTERNALREFERENCE, ELASTIC_INDEX_EXTERNALREFERENCE)) {
            success = false;
        }
        return success;
    }

    public boolean reIndex(final String indexName, final String type) {
        final List<IndexStatus> list = indexStatusRepository.getLatestRunningIndexStatusForIndexAlias(indexName);
        if (list.isEmpty()) {
            reIndexData(indexName, type);
            return true;
        } else {
            LOG.info("Indexing is already running for index: " + indexName);
            return false;
        }
    }

    private void reIndexData(final String indexAlias, final String type) {
        final String indexName = createIndexName(indexAlias);

        final IndexStatus status = new IndexStatus();
        final Date timeStamp = new Date(System.currentTimeMillis());
        status.setId(UUID.randomUUID());
        status.setCreated(timeStamp);
        status.setModified(timeStamp);
        status.setStatus(INDEX_STATUS_RUNNING);
        status.setIndexAlias(indexName);
        status.setIndexName(indexName);
        indexStatusRepository.save(status);

        final Set<String> types = new HashSet<>();
        types.add(type);

        indexingTools.createIndexWithNestedPrefLabel(indexName, types);

        boolean success = true;
        switch (indexAlias) {
            case ELASTIC_INDEX_CODEREGISTRY:
                success = indexCodeRegistries(indexName);
                break;
            case ELASTIC_INDEX_CODESCHEME:
                success = indexCodeSchemes(indexName);
                break;
            case ELASTIC_INDEX_CODE:
                success = indexCodes(indexName);
                break;
            case ELASTIC_INDEX_PROPERTYTYPE:
                success = indexPropertyTypes(indexName);
                break;
            case ELASTIC_INDEX_EXTERNALREFERENCE:
                success = indexExternalReferences(indexName);
                break;
            default:
                LOG.error("Index type: " + indexAlias + " not supported.");
                success = false;
                break;
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
