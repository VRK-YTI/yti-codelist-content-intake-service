package fi.vm.yti.codelist.intake.indexing.impl;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.Transactional;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;

import fi.vm.yti.codelist.common.dto.AbstractIdentifyableCodeDTO;
import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.common.dto.ValueTypeDTO;
import fi.vm.yti.codelist.common.dto.Views;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.indexing.IndexingTools;
import fi.vm.yti.codelist.intake.jpa.IndexStatusRepository;
import fi.vm.yti.codelist.intake.model.IndexStatus;
import fi.vm.yti.codelist.intake.service.CodeRegistryService;
import fi.vm.yti.codelist.intake.service.CodeSchemeService;
import fi.vm.yti.codelist.intake.service.CodeService;
import fi.vm.yti.codelist.intake.service.ExtensionService;
import fi.vm.yti.codelist.intake.service.ExternalReferenceService;
import fi.vm.yti.codelist.intake.service.MemberService;
import fi.vm.yti.codelist.intake.service.PropertyTypeService;
import fi.vm.yti.codelist.intake.service.ValueTypeService;
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
    private static final String NAME_VALUETYPES = "ValueTypes";
    private static final String NAME_EXTENSIONS = "Extensions";
    private static final String NAME_MEMBERS = "Members";
    private static final String BULK = "ElasticSearch bulk: ";
    private static final int MAX_PAGE_COUNT = 1000;
    private static final int MAX_MEMBER_PAGE_COUNT = 50;

    private final IndexStatusRepository indexStatusRepository;
    private final CodeSchemeService codeSchemeService;
    private final CodeRegistryService codeRegistryService;
    private final CodeService codeService;
    private final ExternalReferenceService externalReferenceService;
    private final PropertyTypeService propertyTypeService;
    private final ValueTypeService valueTypeService;
    private final ExtensionService extensionService;
    private final MemberService memberService;
    private final RestHighLevelClient client;
    private final IndexingTools indexingTools;
    private boolean hasError;
    private boolean fullIndexInProgress;

    @Inject
    public IndexingImpl(final IndexingTools indexingTools,
                        final RestHighLevelClient client,
                        final IndexStatusRepository indexStatusRepository,
                        final CodeRegistryService codeRegistryService,
                        final CodeSchemeService codeSchemeService,
                        final CodeService codeService,
                        final ExternalReferenceService externalReferenceService,
                        final PropertyTypeService propertyTypeService,
                        final ValueTypeService valueTypeService,
                        final ExtensionService extensionService,
                        final MemberService memberService) {
        this.indexingTools = indexingTools;
        this.client = client;
        this.indexStatusRepository = indexStatusRepository;
        this.codeRegistryService = codeRegistryService;
        this.codeSchemeService = codeSchemeService;
        this.codeService = codeService;
        this.externalReferenceService = externalReferenceService;
        this.propertyTypeService = propertyTypeService;
        this.valueTypeService = valueTypeService;
        this.extensionService = extensionService;
        this.memberService = memberService;
    }

    private boolean indexCodeRegistries(final String indexName) {
        final Set<CodeRegistryDTO> codeRegistries = codeRegistryService.findAll();
        return indexData(codeRegistries, indexName, ELASTIC_TYPE_CODEREGISTRY, NAME_CODEREGISTRIES, Views.ExtendedCodeRegistry.class);
    }

    private boolean indexCodeSchemes(final String indexName) {
        final Set<CodeSchemeDTO> codeSchemes = codeSchemeService.findAll();

        for (CodeSchemeDTO currentCodeScheme : codeSchemes) {
            if (currentCodeScheme.getLastCodeschemeId() != null) {
                codeSchemeService.populateAllVersionsToCodeSchemeDTO(currentCodeScheme);
            }
        }
        return indexData(codeSchemes, indexName, ELASTIC_TYPE_CODESCHEME, NAME_CODESCHEMES, Views.ExtendedCodeScheme.class);
    }

    private int getContentPageCount(final int codeCount,
                                    final int maxCount) {
        return codeCount / maxCount + 1;
    }

    @Transactional
    public boolean indexCodes(final String indexName) {
        final Stopwatch watch = Stopwatch.createStarted();
        final int codeCount = codeService.getCodeCount();
        final int pageCount = getContentPageCount(codeCount, MAX_PAGE_COUNT);
        LOG.info(String.format("ElasticSearch indexing: Starting to index %d pages of codes %d codes.", pageCount, codeCount));
        int page = 0;
        boolean success = true;
        while (page + 1 <= pageCount) {
            final PageRequest pageRequest = new PageRequest(page, MAX_PAGE_COUNT, new Sort(new Sort.Order(Sort.Direction.ASC, "codeValue")));
            final Set<CodeDTO> codes = codeService.findAll(pageRequest);
            final boolean partIndexSuccess = indexData(codes, indexName, ELASTIC_TYPE_CODE, NAME_CODES, Views.ExtendedCode.class);
            if (!partIndexSuccess) {
                success = false;
            }
            page++;
        }
        if (success) {
            LOG.info(String.format("ElasticSearch indexing: Successfully indexed %d codes in %s", codeCount, watch));
        }
        return success;
    }

    private boolean indexPropertyTypes(final String indexName) {
        final Set<PropertyTypeDTO> propertyTypes = propertyTypeService.findAll();
        return indexData(propertyTypes, indexName, ELASTIC_TYPE_PROPERTYTYPE, NAME_PROPERTYTYPES, Views.ExtendedPropertyType.class);
    }

    private boolean indexValueTypes(final String indexName) {
        final Set<ValueTypeDTO> valueTypes = valueTypeService.findAll();
        return indexData(valueTypes, indexName, ELASTIC_TYPE_VALUETYPE, NAME_VALUETYPES, Views.ExtendedValueType.class);
    }

    private boolean indexExternalReferences(final String indexName) {
        final Set<ExternalReferenceDTO> externalReferences = externalReferenceService.findAll();
        return indexData(externalReferences, indexName, ELASTIC_TYPE_EXTERNALREFERENCE, NAME_EXTERNALREFERENCES, Views.ExtendedExternalReference.class);
    }

    private boolean indexExtensions(final String indexName) {
        final Set<ExtensionDTO> extensions = extensionService.findAll();
        return indexData(extensions, indexName, ELASTIC_TYPE_EXTENSION, NAME_EXTENSIONS, Views.ExtendedExtension.class);
    }

    private boolean indexMembers(final String indexName) {

        final Stopwatch watch = Stopwatch.createStarted();
        final int memberCount = memberService.getMemberCount();
        final int pageCount = getContentPageCount(memberCount, MAX_MEMBER_PAGE_COUNT);
        LOG.info(String.format("ElasticSearch indexing: Starting to index %d pages of members %d members.", pageCount, memberCount));
        int page = 0;
        boolean success = true;
        while (page + 1 <= pageCount) {
            final PageRequest pageRequest = new PageRequest(page, MAX_PAGE_COUNT, new Sort(new Sort.Order(Sort.Direction.ASC, "uri")));
            final Set<MemberDTO> members = memberService.findAll(pageRequest);
            final boolean partIndexSuccess = indexData(members, indexName, ELASTIC_TYPE_MEMBER, NAME_MEMBERS, Views.ExtendedMember.class);
            if (!partIndexSuccess) {
                success = false;
            }
            page++;
        }
        if (success) {
            LOG.info(String.format("ElasticSearch indexing: Successfully indexed %d members in %s", memberCount, watch));
        }
        return success;

    }

    private <T> boolean deleteData(final Set<T> set,
                                   final String elasticIndex,
                                   final String elasticType,
                                   final String name) {
        boolean success;
        if (!set.isEmpty()) {
            final BulkRequest bulkRequest = new BulkRequest();
            for (final T item : set) {
                final AbstractIdentifyableCodeDTO identifyableCode = (AbstractIdentifyableCodeDTO) item;
                bulkRequest.add(new DeleteRequest(elasticIndex, elasticType, identifyableCode.getId().toString()));
                bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
            }
            try {
                final BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                success = handleBulkResponse(name, response);
            } catch (final IOException e) {
                LOG.error("Bulk delete request failed!", e);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "ElasticSearch index query error!"));
            }
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
            final BulkRequest bulkRequest = new BulkRequest();
            for (final T item : set) {
                try {
                    final AbstractIdentifyableCodeDTO identifyableCode = (AbstractIdentifyableCodeDTO) item;
                    final String itemPayload = mapper.writerWithView(jsonViewClass).writeValueAsString(item).replace("\\\\n", "\\n");
                    bulkRequest.add(new IndexRequest(elasticIndex, elasticType, identifyableCode.getId().toString()).source(itemPayload, XContentType.JSON));
                    bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
                } catch (final JsonProcessingException e) {
                    handleBulkErrorWithException(name, e);
                }
            }
            try {
                final BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                success = handleBulkResponse(name, response);
            } catch (final IOException e) {
                LOG.error("Bulk index request failed!", e);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "ElasticSearch index query error!"));
            }
        } else {
            noContent(name);
            success = true;
        }
        return success;
    }

    private void handleBulkErrorWithException(final String name,
                                              final JsonProcessingException e) {
        hasError = true;
        LOG.error(String.format("Indexing %s failed.", name), e);
    }

    private boolean handleBulkResponse(final String type,
                                       final BulkResponse response) {
        if (response.hasFailures()) {
            hasError = true;
            LOG.error(String.format("%s%s operation failed with errors: %s", BULK, type, response.buildFailureMessage()));
            return false;
        } else {
            LOG.info(String.format("%s%s operation successfully indexed %d items in %d ms.", BULK, type, response.getItems().length, response.getTook().millis()));
            return true;
        }
    }

    private void noContent(final String type) {
        LOG.info(String.format("%s%s operation ran, but there was no content to be indexed!", BULK, type));
    }

    public boolean deleteCode(final CodeDTO code) {
        final Set<CodeDTO> codes = new HashSet<>();
        codes.add(code);
        return deleteCodes(codes);
    }

    public boolean deleteCodes(final Set<CodeDTO> codes) {
        return codes.isEmpty() || deleteData(codes, ELASTIC_INDEX_CODE, ELASTIC_TYPE_CODE, NAME_CODES);
    }

    public boolean deleteCodeRegistry(final CodeRegistryDTO codeRegistry) {
        final Set<CodeRegistryDTO> codeRegistries = new HashSet<>();
        codeRegistries.add(codeRegistry);
        return deleteCodeRegistries(codeRegistries);
    }

    public boolean deleteCodeRegistries(final Set<CodeRegistryDTO> codeRegistries) {
        return codeRegistries.isEmpty() || deleteData(codeRegistries, ELASTIC_INDEX_CODEREGISTRY, ELASTIC_TYPE_CODEREGISTRY, NAME_CODEREGISTRIES);
    }

    public boolean deleteCodeScheme(final CodeSchemeDTO codeScheme) {
        final Set<CodeSchemeDTO> codeSchemes = new HashSet<>();
        codeSchemes.add(codeScheme);
        return deleteCodeSchemes(codeSchemes);
    }

    public boolean deleteCodeSchemes(final Set<CodeSchemeDTO> codeSchemes) {
        return codeSchemes.isEmpty() || deleteData(codeSchemes, ELASTIC_INDEX_CODESCHEME, ELASTIC_TYPE_CODESCHEME, NAME_CODESCHEMES);
    }

    public boolean deleteExternalReferences(final Set<ExternalReferenceDTO> externalReferences) {
        return externalReferences.isEmpty() || deleteData(externalReferences, ELASTIC_INDEX_EXTERNALREFERENCE, ELASTIC_TYPE_EXTERNALREFERENCE, NAME_EXTERNALREFERENCES);
    }

    public boolean deleteExtension(final ExtensionDTO extension) {
        final Set<ExtensionDTO> extensions = new HashSet<>();
        extensions.add(extension);
        return deleteExtensions(extensions);
    }

    public boolean deleteExtensions(final Set<ExtensionDTO> extensions) {
        return extensions.isEmpty() || deleteData(extensions, ELASTIC_INDEX_EXTENSION, ELASTIC_TYPE_EXTENSION, NAME_EXTENSIONS);
    }

    public boolean deleteMember(final MemberDTO extension) {
        final Set<MemberDTO> members = new HashSet<>();
        members.add(extension);
        return deleteMembers(members);
    }

    public boolean deleteMembers(final Set<MemberDTO> members) {
        return members.isEmpty() || deleteData(members, ELASTIC_INDEX_MEMBER, ELASTIC_TYPE_MEMBER, NAME_MEMBERS);
    }

    public boolean updateCode(final CodeDTO code) {
        final Set<CodeDTO> codes = new HashSet<>();
        codes.add(code);
        return updateCodes(codes);
    }

    public boolean updateCodes(final Set<CodeDTO> codes) {
        return codes.isEmpty() || indexData(codes, ELASTIC_INDEX_CODE, ELASTIC_TYPE_CODE, NAME_CODES, Views.ExtendedCode.class);
    }

    public boolean updateCodeScheme(final CodeSchemeDTO codeScheme) {
        final Set<CodeSchemeDTO> codeSchemes = new HashSet<>();
        codeSchemes.add(codeScheme);
        return updateCodeSchemes(codeSchemes);
    }

    public boolean updateCodeSchemes(final Set<CodeSchemeDTO> codeSchemes) {
        return codeSchemes.isEmpty() || indexData(codeSchemes, ELASTIC_INDEX_CODESCHEME, ELASTIC_TYPE_CODESCHEME, NAME_CODESCHEMES, Views.ExtendedCodeScheme.class);
    }

    public boolean updateCodeRegistry(final CodeRegistryDTO codeRegistry) {
        final Set<CodeRegistryDTO> codeRegistries = new HashSet<>();
        codeRegistries.add(codeRegistry);
        return updateCodeRegistries(codeRegistries);
    }

    public boolean updateCodeRegistries(final Set<CodeRegistryDTO> codeRegistries) {
        return codeRegistries.isEmpty() || indexData(codeRegistries, ELASTIC_INDEX_CODEREGISTRY, ELASTIC_TYPE_CODEREGISTRY, NAME_CODEREGISTRIES, Views.Normal.class);
    }

    public boolean updatePropertyType(final PropertyTypeDTO propertyType) {
        final Set<PropertyTypeDTO> propertyTypes = new HashSet<>();
        propertyTypes.add(propertyType);
        return updatePropertyTypes(propertyTypes);
    }

    public boolean updatePropertyTypes(final Set<PropertyTypeDTO> propertyTypes) {
        return propertyTypes.isEmpty() || indexData(propertyTypes, ELASTIC_INDEX_PROPERTYTYPE, ELASTIC_TYPE_PROPERTYTYPE, NAME_PROPERTYTYPES, Views.Normal.class);
    }

    public boolean updateValueType(final ValueTypeDTO valueType) {
        final Set<ValueTypeDTO> valueTypes = new HashSet<>();
        valueTypes.add(valueType);
        return updateValueTypes(valueTypes);
    }

    public boolean updateValueTypes(final Set<ValueTypeDTO> valueTypes) {
        return valueTypes.isEmpty() || indexData(valueTypes, ELASTIC_INDEX_VALUETYPE, ELASTIC_TYPE_VALUETYPE, NAME_VALUETYPES, Views.Normal.class);
    }

    public boolean updateExternalReference(final ExternalReferenceDTO externalReference) {
        final Set<ExternalReferenceDTO> externalReferences = new HashSet<>();
        externalReferences.add(externalReference);
        return updateExternalReferences(externalReferences);
    }

    public boolean updateExternalReferences(final Set<ExternalReferenceDTO> externalReferences) {
        return externalReferences.isEmpty() || indexData(externalReferences, ELASTIC_INDEX_EXTERNALREFERENCE, ELASTIC_TYPE_EXTERNALREFERENCE, NAME_EXTERNALREFERENCES, Views.ExtendedExternalReference.class);
    }

    public boolean updateExtension(final ExtensionDTO extension) {
        final Set<ExtensionDTO> extensions = new HashSet<>();
        extensions.add(extension);
        return updateExtensions(extensions);
    }

    public boolean updateExtensions(final Set<ExtensionDTO> extensions) {
        return extensions.isEmpty() || indexData(extensions, ELASTIC_INDEX_EXTENSION, ELASTIC_TYPE_EXTENSION, NAME_EXTENSIONS, Views.ExtendedExtension.class);
    }

    public boolean updateMember(final MemberDTO member) {
        final Set<MemberDTO> members = new HashSet<>();
        members.add(member);
        return updateMembers(members);
    }

    public boolean updateMembers(final Set<MemberDTO> members) {
        return members.isEmpty() || indexData(members, ELASTIC_INDEX_MEMBER, ELASTIC_TYPE_MEMBER, NAME_MEMBERS, Views.ExtendedMember.class);
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
        if (reIndex(ELASTIC_INDEX_VALUETYPE, ELASTIC_TYPE_VALUETYPE)) {
            success = false;
        }
        if (reIndex(ELASTIC_INDEX_EXTERNALREFERENCE, ELASTIC_INDEX_EXTERNALREFERENCE)) {
            success = false;
        }
        if (reIndex(ELASTIC_INDEX_EXTENSION, ELASTIC_INDEX_EXTENSION)) {
            success = false;
        }
        if (reIndex(ELASTIC_INDEX_MEMBER, ELASTIC_INDEX_MEMBER)) {
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
            LOG.info(String.format("Indexing is already running for index: %s", indexName));
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
            case ELASTIC_INDEX_EXTENSION:
                success = indexExtensions(indexName);
                break;
            case ELASTIC_INDEX_MEMBER:
                success = indexMembers(indexName);
                break;
            case ELASTIC_INDEX_VALUETYPE:
                success = indexValueTypes(indexName);
                break;
            default:
                LOG.error(String.format("Index type: %s not supported.", indexAlias));
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
}
