package fi.vm.yti.codelist.intake.indexing.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.Transactional;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.codelist.common.dto.AbstractIdentifyableCodeDTO;
import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.common.dto.Views;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.indexing.IndexingTools;
import fi.vm.yti.codelist.intake.jpa.IndexStatusRepository;
import fi.vm.yti.codelist.intake.model.IndexStatus;
import fi.vm.yti.codelist.intake.service.CodeRegistryService;
import fi.vm.yti.codelist.intake.service.CodeSchemeService;
import fi.vm.yti.codelist.intake.service.CodeService;
import fi.vm.yti.codelist.intake.service.ExtensionSchemeService;
import fi.vm.yti.codelist.intake.service.ExtensionService;
import fi.vm.yti.codelist.intake.service.ExternalReferenceService;
import fi.vm.yti.codelist.intake.service.PropertyTypeService;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.update.UpdateManager.UPDATE_FAILED;

@Singleton
@Component
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
    private static final String NAME_EXTENSIONSCHEMES = "ExtensionSchemes";
    private static final String NAME_EXTENSIONS = "Extensions";
    private static final String BULK = "ElasticSearch bulk: ";
    private static int MAX_PAGE_COUNT = 2000;

    private final IndexStatusRepository indexStatusRepository;
    private final CodeSchemeService codeSchemeService;
    private final CodeRegistryService codeRegistryService;
    private final CodeService codeService;
    private final ExternalReferenceService externalReferenceService;
    private final PropertyTypeService propertyTypeService;
    private final ExtensionSchemeService extensionSchemeService;
    private final ExtensionService extensionService;
    private final Client client;
    private IndexingTools indexingTools;
    private boolean hasError;
    private boolean fullIndexInProgress;

    @Inject
    public IndexingImpl(final IndexingTools indexingTools,
                        final Client client,
                        final IndexStatusRepository indexStatusRepository,
                        final CodeRegistryService codeRegistryService,
                        final CodeSchemeService codeSchemeService,
                        final CodeService codeService,
                        final ExternalReferenceService externalReferenceService,
                        final PropertyTypeService propertyTypeService,
                        final ExtensionSchemeService extensionSchemeService,
                        final ExtensionService extensionService) {
        this.indexingTools = indexingTools;
        this.client = client;
        this.indexStatusRepository = indexStatusRepository;
        this.codeRegistryService = codeRegistryService;
        this.codeSchemeService = codeSchemeService;
        this.codeService = codeService;
        this.externalReferenceService = externalReferenceService;
        this.propertyTypeService = propertyTypeService;
        this.extensionSchemeService = extensionSchemeService;
        this.extensionService = extensionService;
    }

    private boolean indexCodeRegistries(final String indexName) {
        final Set<CodeRegistryDTO> codeRegistries = codeRegistryService.findAll();
        return indexData(codeRegistries, indexName, ELASTIC_TYPE_CODEREGISTRY, NAME_CODEREGISTRIES, Views.ExtendedCodeRegistry.class);
    }

    private boolean indexCodeSchemes(final String indexName) {
        final Set<CodeSchemeDTO> codeSchemes = codeSchemeService.findAll();
        return indexData(codeSchemes, indexName, ELASTIC_TYPE_CODESCHEME, NAME_CODESCHEMES, Views.ExtendedCodeScheme.class);
    }

    private boolean indexCodes(final String indexName) {
        final int codeCount = codeService.getCodeCount();
        int page = 1;
        boolean success = true;
        while (page * MAX_PAGE_COUNT <= codeCount) {
            final PageRequest pageRequest = new PageRequest(page, MAX_PAGE_COUNT, new Sort(new Sort.Order(Sort.Direction.ASC, "codeValue")));
            final Set<CodeDTO> codes = codeService.findAll(pageRequest);
            final boolean partIndexSuccess = indexData(codes, indexName, ELASTIC_TYPE_CODE, NAME_CODES, Views.ExtendedCode.class);
            if (!partIndexSuccess && success) {
                success = false;
            }
            page++;
        }
        return success;
    }

    private boolean indexPropertyTypes(final String indexName) {
        final Set<PropertyTypeDTO> propertyTypes = propertyTypeService.findAll();
        return indexData(propertyTypes, indexName, ELASTIC_TYPE_PROPERTYTYPE, NAME_PROPERTYTYPES, Views.ExtendedPropertyType.class);
    }

    private boolean indexExternalReferences(final String indexName) {
        final Set<ExternalReferenceDTO> externalReferences = externalReferenceService.findAll();
        return indexData(externalReferences, indexName, ELASTIC_TYPE_EXTERNALREFERENCE, NAME_EXTERNALREFERENCES, Views.ExtendedExternalReference.class);
    }

    private boolean indexExtensionSchemes(final String indexName) {
        final Set<ExtensionSchemeDTO> extensionSchemes = extensionSchemeService.findAll();
        return indexData(extensionSchemes, indexName, ELASTIC_TYPE_EXTENSIONSCHEME, NAME_EXTENSIONSCHEMES, Views.ExtendedExtensionScheme.class);
    }

    private boolean indexExtensions(final String indexName) {
        final Set<ExtensionDTO> extensions = extensionService.findAll();
        return indexData(extensions, indexName, ELASTIC_TYPE_EXTENSION, NAME_EXTENSIONS, Views.ExtendedExtension.class);
    }

    private <T> boolean deleteData(final Set<T> set,
                                   final String elasticIndex,
                                   final String elasticType,
                                   final String name) {
        boolean success;
        if (!set.isEmpty()) {
            final BulkRequestBuilder bulkRequest = client.prepareBulk();
            for (final T item : set) {
                final AbstractIdentifyableCodeDTO identifyableCode = (AbstractIdentifyableCodeDTO) item;
                bulkRequest.add(client.prepareDelete(elasticIndex, elasticType, identifyableCode.getId().toString()));
                bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
            }
            final BulkResponse response = bulkRequest.get();
            success = handleBulkResponse(name, response);
        } else {
            noContent(name);
            success = true;
        }
        return success;
    }

    private <T> boolean indexData(final Set<T> set,
                                  final String elasticIndex,
                                  final String elasticType,
                                  final String name,
                                  final Class<?> jsonViewClass) {
        boolean success;
        if (!set.isEmpty()) {
            final ObjectMapper mapper = indexingTools.createObjectMapper();
            final BulkRequestBuilder bulkRequest = client.prepareBulk();
            for (final T item : set) {
                try {
                    final AbstractIdentifyableCodeDTO identifyableCode = (AbstractIdentifyableCodeDTO) item;
                    bulkRequest.add(client.prepareIndex(elasticIndex, elasticType, identifyableCode.getId().toString()).setSource(mapper.writerWithView(jsonViewClass).writeValueAsString(item).replace("\\\\n", "\\n"), XContentType.JSON));
                    bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
                } catch (final JsonProcessingException e) {
                    handleBulkErrorWithException(name, e);
                }
            }
            final BulkResponse response = bulkRequest.get();
            success = handleBulkResponse(name, response);
        } else {
            noContent(name);
            success = true;
        }
        return success;
    }

    private void handleBulkErrorWithException(final String name,
                                              final JsonProcessingException e) {
        hasError = true;
        LOG.error("Indexing " + name + " failed.", e);
    }

    private boolean handleBulkResponse(final String type,
                                       final BulkResponse response) {
        if (response.hasFailures()) {
            hasError = true;
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

    public boolean deleteCode(final CodeDTO code) {
        final Set<CodeDTO> codes = new HashSet<>();
        codes.add(code);
        return deleteCodes(codes);
    }

    public boolean deleteCodes(final Set<CodeDTO> codes) {
        if (!codes.isEmpty()) {
            return deleteData(codes, ELASTIC_INDEX_CODE, ELASTIC_TYPE_CODE, NAME_CODES);
        }
        return true;
    }

    public boolean deleteCodeScheme(final CodeSchemeDTO codeScheme) {
        final Set<CodeSchemeDTO> codeSchemes = new HashSet<>();
        codeSchemes.add(codeScheme);
        return deleteCodeSchemes(codeSchemes);
    }

    public boolean deleteCodeSchemes(final Set<CodeSchemeDTO> codeSchemes) {
        if (!codeSchemes.isEmpty()) {
            return deleteData(codeSchemes, ELASTIC_INDEX_CODESCHEME, ELASTIC_TYPE_CODESCHEME, NAME_CODESCHEMES);
        }
        return true;
    }

    public boolean deleteExternalReferences(final Set<ExternalReferenceDTO> externalReferences) {
        if (!externalReferences.isEmpty()) {
            return deleteData(externalReferences, ELASTIC_INDEX_EXTERNALREFERENCE, ELASTIC_TYPE_EXTERNALREFERENCE, NAME_EXTERNALREFERENCES);
        }
        return true;
    }

    public boolean deleteExtensionScheme(final ExtensionSchemeDTO extensionScheme) {
        final Set<ExtensionSchemeDTO> extensionSchemes = new HashSet<>();
        extensionSchemes.add(extensionScheme);
        return deleteExtensionSchemes(extensionSchemes);
    }

    public boolean deleteExtensionSchemes(final Set<ExtensionSchemeDTO> extensionSchemes) {
        if (!extensionSchemes.isEmpty()) {
            return deleteData(extensionSchemes, ELASTIC_INDEX_EXTENSIONSCHEME, ELASTIC_TYPE_EXTENSIONSCHEME, NAME_EXTENSIONSCHEMES);
        }
        return true;
    }

    public boolean deleteExtension(final ExtensionDTO extension) {
        final Set<ExtensionDTO> extensions = new HashSet<>();
        extensions.add(extension);
        return deleteExtensions(extensions);
    }

    public boolean deleteExtensions(final Set<ExtensionDTO> extensions) {
        if (!extensions.isEmpty()) {
            return deleteData(extensions, ELASTIC_INDEX_EXTENSION, ELASTIC_TYPE_EXTENSION, NAME_EXTENSIONS);
        }
        return true;
    }

    public boolean updateCode(final CodeDTO code) {
        final Set<CodeDTO> codes = new HashSet<>();
        codes.add(code);
        return updateCodes(codes);
    }

    public boolean updateCodes(final Set<CodeDTO> codes) {
        if (!codes.isEmpty()) {
            return indexData(codes, ELASTIC_INDEX_CODE, ELASTIC_TYPE_CODE, NAME_CODES, Views.ExtendedCode.class);
        }
        return true;
    }

    public boolean updateCodeScheme(final CodeSchemeDTO codeScheme) {
        final Set<CodeSchemeDTO> codeSchemes = new HashSet<>();
        codeSchemes.add(codeScheme);
        return updateCodeSchemes(codeSchemes);
    }

    public boolean updateCodeSchemes(final Set<CodeSchemeDTO> codeSchemes) {
        if (!codeSchemes.isEmpty()) {
            return indexData(codeSchemes, ELASTIC_INDEX_CODESCHEME, ELASTIC_TYPE_CODESCHEME, NAME_CODESCHEMES, Views.ExtendedCodeScheme.class);
        }
        return true;
    }

    public boolean updateCodeRegistry(final CodeRegistryDTO codeRegistry) {
        final Set<CodeRegistryDTO> codeRegistries = new HashSet<>();
        codeRegistries.add(codeRegistry);
        return updateCodeRegistries(codeRegistries);
    }

    public boolean updateCodeRegistries(final Set<CodeRegistryDTO> codeRegistries) {
        if (!codeRegistries.isEmpty()) {
            return indexData(codeRegistries, ELASTIC_INDEX_CODEREGISTRY, ELASTIC_TYPE_CODEREGISTRY, NAME_CODEREGISTRIES, Views.Normal.class);
        }
        return true;
    }

    public boolean updatePropertyType(final PropertyTypeDTO propertyType) {
        final Set<PropertyTypeDTO> propertyTypes = new HashSet<>();
        propertyTypes.add(propertyType);
        return updatePropertyTypes(propertyTypes);
    }

    public boolean updatePropertyTypes(final Set<PropertyTypeDTO> propertyTypes) {
        if (!propertyTypes.isEmpty()) {
            return indexData(propertyTypes, ELASTIC_INDEX_PROPERTYTYPE, ELASTIC_TYPE_PROPERTYTYPE, NAME_PROPERTYTYPES, Views.Normal.class);
        }
        return true;
    }

    public boolean updateExternalReference(final ExternalReferenceDTO externalReference) {
        final Set<ExternalReferenceDTO> externalReferences = new HashSet<>();
        externalReferences.add(externalReference);
        return updateExternalReferences(externalReferences);
    }

    public boolean updateExternalReferences(final Set<ExternalReferenceDTO> externalReferences) {
        if (!externalReferences.isEmpty()) {
            return indexData(externalReferences, ELASTIC_INDEX_EXTERNALREFERENCE, ELASTIC_TYPE_EXTERNALREFERENCE, NAME_EXTERNALREFERENCES, Views.ExtendedExternalReference.class);
        }
        return true;
    }

    public boolean updateExtensionScheme(final ExtensionSchemeDTO extensionScheme) {
        final Set<ExtensionSchemeDTO> extensionSchemes = new HashSet<>();
        extensionSchemes.add(extensionScheme);
        return updateExtensionSchemes(extensionSchemes);
    }

    public boolean updateExtensionSchemes(final Set<ExtensionSchemeDTO> extensionSchemes) {
        if (!extensionSchemes.isEmpty()) {
            return indexData(extensionSchemes, ELASTIC_INDEX_EXTENSIONSCHEME, ELASTIC_TYPE_EXTENSIONSCHEME, NAME_EXTENSIONSCHEMES, Views.ExtendedExtensionScheme.class);
        }
        return true;
    }

    public boolean updateExtension(final ExtensionDTO extension) {
        final Set<ExtensionDTO> extensions = new HashSet<>();
        extensions.add(extension);
        return updateExtensions(extensions);
    }

    public boolean updateExtensions(final Set<ExtensionDTO> extensions) {
        if (!extensions.isEmpty()) {
            return indexData(extensions, ELASTIC_INDEX_EXTENSION, ELASTIC_TYPE_EXTENSION, NAME_EXTENSIONS, Views.ExtendedExtension.class);
        }
        return true;
    }

    public void reIndexEverythingIfNecessary() {
        if (hasError && !fullIndexInProgress) {
            LOG.info("Doing full ElasticSearch reindexing due to errors!");
            fullIndexInProgress = true;
            hasError = !reIndexEverything();
            fullIndexInProgress = false;
        }
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
        if (reIndex(ELASTIC_INDEX_EXTENSIONSCHEME, ELASTIC_INDEX_EXTENSIONSCHEME)) {
            success = false;
        }
        if (reIndex(ELASTIC_INDEX_EXTENSION, ELASTIC_INDEX_EXTENSION)) {
            success = false;
        }
        return success;
    }

    @Transactional
    public void cleanRunningIndexingBookkeeping() {
        final Set<IndexStatus> indexStatuses = indexStatusRepository.getRunningIndexStatuses();
        indexStatuses.forEach(indexStatus -> indexStatus.setStatus(UPDATE_FAILED));
        indexStatusRepository.save(indexStatuses);
    }

    public boolean reIndex(final String indexName,
                           final String type) {
        final Set<IndexStatus> list = indexStatusRepository.getLatestRunningIndexStatusForIndexAlias(indexName);
        if (list.isEmpty()) {
            reIndexData(indexName, type);
            return true;
        } else {
            LOG.info("Indexing is already running for index: " + indexName);
            return false;
        }
    }

    private void reIndexData(final String indexAlias,
                             final String type) {
        final String indexName = createIndexName(indexAlias);

        final IndexStatus status = new IndexStatus();
        final Date timeStamp = new Date(System.currentTimeMillis());
        status.setId(UUID.randomUUID());
        status.setCreated(timeStamp);
        status.setModified(timeStamp);
        status.setStatus(INDEX_STATUS_RUNNING);
        status.setIndexAlias(indexAlias);
        status.setIndexName(indexName);
        indexStatusRepository.save(status);

        indexingTools.createIndexWithNestedPrefLabel(indexName, type);

        boolean success;
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
            case ELASTIC_INDEX_EXTENSIONSCHEME:
                success = indexExtensionSchemes(indexName);
                break;
            case ELASTIC_INDEX_EXTENSION:
                success = indexExtensions(indexName);
                break;
            default:
                LOG.error("Index type: " + indexAlias + " not supported.");
                success = false;
                break;
        }
        if (success) {
            indexingTools.aliasIndex(indexName, indexAlias);
            final Set<IndexStatus> earlierStatuses = indexStatusRepository.getLatestSuccessfulIndexStatusForIndexAlias(indexAlias);
            earlierStatuses.forEach(earlierIndex -> {
                indexingTools.deleteIndex(earlierIndex.getIndexName());
                earlierIndex.setModified(timeStamp);
                earlierIndex.setStatus(INDEX_STATUS_DELETED);
                indexStatusRepository.save(earlierIndex);
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

    public boolean isHasError() {
        return hasError;
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }
}
