package fi.vm.yti.cls.intake.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.base.Stopwatch;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.cls.common.model.BusinessId;
import fi.vm.yti.cls.common.model.BusinessServiceSubRegion;
import fi.vm.yti.cls.common.model.Code;
import fi.vm.yti.cls.common.model.CodeRegistry;
import fi.vm.yti.cls.common.model.CodeScheme;
import fi.vm.yti.cls.common.model.ElectoralDistrict;
import fi.vm.yti.cls.common.model.HealthCareDistrict;
import fi.vm.yti.cls.common.model.Magistrate;
import fi.vm.yti.cls.common.model.MagistrateServiceUnit;
import fi.vm.yti.cls.common.model.Municipality;
import fi.vm.yti.cls.common.model.PostManagementDistrict;
import fi.vm.yti.cls.common.model.PostalCode;
import fi.vm.yti.cls.common.model.Region;
import fi.vm.yti.cls.common.model.StreetAddress;
import fi.vm.yti.cls.common.model.StreetNumber;
import fi.vm.yti.cls.intake.jpa.BusinessIdRepository;
import fi.vm.yti.cls.intake.jpa.BusinessServiceSubRegionRepository;
import fi.vm.yti.cls.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.cls.intake.jpa.CodeRepository;
import fi.vm.yti.cls.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.cls.intake.jpa.ElectoralDistrictRepository;
import fi.vm.yti.cls.intake.jpa.HealthCareDistrictRepository;
import fi.vm.yti.cls.intake.jpa.MagistrateRepository;
import fi.vm.yti.cls.intake.jpa.MagistrateServiceUnitRepository;
import fi.vm.yti.cls.intake.jpa.MunicipalityRepository;
import fi.vm.yti.cls.intake.jpa.PostManagementDistrictRepository;
import fi.vm.yti.cls.intake.jpa.PostalCodeRepository;
import fi.vm.yti.cls.intake.jpa.RegionRepository;
import fi.vm.yti.cls.intake.jpa.StreetAddressRepository;
import fi.vm.yti.cls.intake.jpa.StreetNumberRepository;
import org.apache.commons.collections4.ListUtils;
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

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Singleton
@Service
public class DomainImpl implements Domain {

    private static final Logger LOG = LoggerFactory.getLogger(DomainImpl.class);
    private static final String MAX_RESULT_WINDOW = "max_result_window";
    private static final int MAX_RESULT_WINDOW_SIZE = 500000;
    private final MunicipalityRepository municipalityRepository;
    private final MagistrateRepository magistrateRepository;
    private final RegionRepository regionRepository;
    private final StreetAddressRepository streetAddressRepository;
    private final StreetNumberRepository streetNumberRepository;
    private final HealthCareDistrictRepository healthCareDistrictRepository;
    private final PostalCodeRepository postalCodeRepository;
    private final ElectoralDistrictRepository electoralDistrictRepository;
    private final MagistrateServiceUnitRepository magistrateServiceUnitRepository;
    private final PostManagementDistrictRepository postManagementDistrictRepository;
    private final BusinessServiceSubRegionRepository businessServiceSubRegionRepository;
    private final BusinessIdRepository businessIdRepository;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeRepository codeRepository;
    private Client client;

    @Inject
    private DomainImpl(final Client client,
                       final MunicipalityRepository municipalityRepository,
                       final MagistrateRepository magistrateRepository,
                       final RegionRepository regionRepository,
                       final StreetAddressRepository streetAddressRepository,
                       final StreetNumberRepository streetNumberRepository,
                       final HealthCareDistrictRepository healthCareDistrictRepository,
                       final PostalCodeRepository postalCodeRepository,
                       final ElectoralDistrictRepository electoralDistrictRepository,
                       final MagistrateServiceUnitRepository magistrateServiceUnitRepository,
                       final PostManagementDistrictRepository postManagementDistrictRepository,
                       final BusinessServiceSubRegionRepository businessServiceSubRegionRepository,
                       final BusinessIdRepository businessIdRepository,
                       final CodeRegistryRepository codeRegistryRepository,
                       final CodeSchemeRepository codeSchemeRepository,
                       final CodeRepository codeRepository) {
        this.client = client;
        this.municipalityRepository = municipalityRepository;
        this.magistrateRepository = magistrateRepository;
        this.regionRepository = regionRepository;
        this.streetAddressRepository = streetAddressRepository;
        this.streetNumberRepository = streetNumberRepository;
        this.healthCareDistrictRepository = healthCareDistrictRepository;
        this.postalCodeRepository = postalCodeRepository;
        this.electoralDistrictRepository = electoralDistrictRepository;
        this.magistrateServiceUnitRepository = magistrateServiceUnitRepository;
        this.postManagementDistrictRepository = postManagementDistrictRepository;
        this.businessServiceSubRegionRepository = businessServiceSubRegionRepository;
        this.businessIdRepository = businessIdRepository;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
    }

    public void persistMagistrates(final List<Magistrate> magistrates) {
        magistrateRepository.save(magistrates);
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
        types.add(DomainConstants.ELASTIC_TYPE_MUNICIPALITY);
        types.add(DomainConstants.ELASTIC_TYPE_MAGISTRATE);
        types.add(DomainConstants.ELASTIC_TYPE_REGION);
        types.add(DomainConstants.ELASTIC_TYPE_POSTALCODE);
        types.add(DomainConstants.ELASTIC_TYPE_BUSINESSSERVICESUBREGION);
        types.add(DomainConstants.ELASTIC_TYPE_BUSINESSID);
        types.add(DomainConstants.ELASTIC_TYPE_POSTMANAGEMENTDISTRICT);
        types.add(DomainConstants.ELASTIC_TYPE_MAGISTRATESERVICEUNIT);
        types.add(DomainConstants.ELASTIC_TYPE_ELECTORALDISTRICT);
        types.add(DomainConstants.ELASTIC_TYPE_HEALTHCAREDISTRICT);
        types.add(DomainConstants.ELASTIC_TYPE_STREETADDRESS);

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

    public void indexMagistrates() {

        final List<Magistrate> magistrates = magistrateRepository.findAll();

        if (!magistrates.isEmpty()) {

            final BulkRequestBuilder bulkRequest = client.prepareBulk();

            magistrates.forEach(magistrate -> {
                final ObjectMapper mapper = createObjectMapper();
                try {
                    bulkRequest.add(client.prepareIndex(DomainConstants.ELASTIC_INDEX_CUSTOMCODES, DomainConstants.ELASTIC_TYPE_MAGISTRATE).setSource(mapper.writeValueAsString(magistrate)));
                } catch (JsonProcessingException e) {
                    LOG.error("Indexing magistrates failed: " + e.getMessage());
                }
            });

            final BulkResponse response = bulkRequest.get();

            if (response.hasFailures()) {
                LOG.error("ElasticSearch bulk magistrates operation failed with errors: " + response.buildFailureMessage());
            } else {
                LOG.info("ElasticSearch bulk magistrates request successfully persisted " + response.getItems().length + " items in " + response.getTookInMillis() + " ms.");
            }

        } else {
            LOG.info("ElasticSearch bulk magistrates request failed: no content to be indexed!");
        }

    }


    public void persistMunicipalities(final List<Municipality> municipalities) {

        municipalityRepository.save(municipalities);

    }

    public void indexMunicipalities() {

        final List<Municipality> municipalities = municipalityRepository.findAll();

        if (!municipalities.isEmpty()) {
            final BulkRequestBuilder bulkRequest = client.prepareBulk();

            final ObjectMapper mapper = createObjectMapper();

            municipalities.stream().forEach(municipality -> {
                try {
                    bulkRequest.add(client.prepareIndex(DomainConstants.ELASTIC_INDEX_CUSTOMCODES, DomainConstants.ELASTIC_TYPE_MUNICIPALITY).setSource(mapper.writeValueAsString(municipality)));
                } catch (JsonProcessingException e) {
                    LOG.error("Indexing municipalities failed: " + e.getMessage());
                }
            });

            final BulkResponse response = bulkRequest.get();

            if (response.hasFailures()) {
                LOG.error("ElasticSearch bulk municipalities operation failed with errors: " + response.buildFailureMessage());
            } else {
                LOG.info("ElasticSearch bulk municipalities request successfully persisted " + response.getItems().length + " items in " + response.getTookInMillis() + " ms.");
            }

        } else {
            LOG.info("ElasticSearch bulk municipalities request failed: no content to be indexed!");
        }

    }


    public void persistRegions(final List<Region> regions) {

        regionRepository.save(regions);

    }

    public void indexRegions() {

        final List<Region> regions = regionRepository.findAll();

        if (!regions.isEmpty()) {
            final BulkRequestBuilder bulkRequest = client.prepareBulk();

            regions.forEach(region -> {
                final ObjectMapper mapper = createObjectMapper();
                try {
                    bulkRequest.add(client.prepareIndex(DomainConstants.ELASTIC_INDEX_CUSTOMCODES, DomainConstants.ELASTIC_TYPE_REGION).setSource(mapper.writeValueAsString(region)));
                } catch (JsonProcessingException e) {
                    LOG.error("Indexing regions failed: " + e.getMessage());
                }
            });

            final BulkResponse response = bulkRequest.get();

            if (response.hasFailures()) {
                LOG.error("ElasticSearch bulk regions operation failed with errors: " + response.buildFailureMessage());
            } else {
                LOG.info("ElasticSearch bulk regions request successfully persisted " + response.getItems().length + " items in " + response.getTookInMillis() + " ms.");
            }

        } else {
            LOG.info("ElasticSearch bulk regions request failed: no content to be indexed!");
        }

    }


    public void persistStreetNumbers(final List<StreetNumber> streetNumbers) {

        final Stopwatch watch = Stopwatch.createStarted();

        streetNumberRepository.save(streetNumbers);

        LOG.info("Persisted " + streetNumbers.size() + " street numbers in " + watch + ".");

    }


    public void persistStreetAddresses(final List<StreetAddress> streetAddresses) {

        final Stopwatch watch = Stopwatch.createStarted();

        streetAddressRepository.save(streetAddresses);

        LOG.info("Persisted " + streetAddresses.size() + " street addresses in " + watch + ".");

    }

    public void indexStreetAddresses() {

        final Stopwatch watch = Stopwatch.createStarted();

        final Set<String> ids = streetAddressRepository.findAllIds();
        final List<String> idList = new ArrayList<>();
        idList.addAll(ids);
        final List<List<String>> idsList = ListUtils.partition(idList, 10000);
        final long streetAddressCount = ids.size();
        final int pageSize = 10000;
        final long pageCount = streetAddressCount / pageSize;

        LOG.info("Indexing " + streetAddressCount + " street addresses...");

        if (!idsList.isEmpty()) {

            for (int i = 0; i <= pageCount; i++) {
                final List<String> subIds = idsList.get(i);
                final Set<StreetAddress> streetAddresses = streetAddressRepository.findByIdIn(subIds);
                LOG.info("ElasticSearch " + streetAddresses.size() + " streetaddresses (page: " + (i + 1) + "/" + (pageCount + 1) + ") loaded from PostgreSQL database in " + watch);
                watch.reset().start();

                final List<StreetAddress> addresses = new ArrayList<>();
                addresses.addAll(streetAddresses);

                final List<List<StreetAddress>> chunks = ListUtils.partition(addresses, 1000);
                chunks.parallelStream().forEach(chunk -> {
                    final BulkRequestBuilder bulkRequest = client.prepareBulk();

                    chunk.forEach(streetAddress -> {
                        final ObjectMapper mapper = createObjectMapper();
                        final SimpleFilterProvider filterProvider = new SimpleFilterProvider();
                        filterProvider.addFilter("postalCode", SimpleBeanPropertyFilter.filterOutAllExcept("url"));
                        filterProvider.addFilter("streetNumber", SimpleBeanPropertyFilter.serializeAllExcept("streetAddress"));
                        filterProvider.setFailOnUnknownId(false);
                        mapper.setFilterProvider(filterProvider);
                        try {
                            bulkRequest.add(client.prepareIndex(DomainConstants.ELASTIC_INDEX_CUSTOMCODES, DomainConstants.ELASTIC_TYPE_STREETADDRESS).setSource(mapper.writeValueAsString(streetAddress)));
                        } catch (JsonProcessingException e) {
                            LOG.error("Indexing streetaddress failed: " + e.getMessage());
                        }
                    });

                    final BulkResponse response = bulkRequest.get();

                    if (response.hasFailures()) {
                        LOG.error("ElasticSearch bulk streetaddress operation failed with errors: " + response.buildFailureMessage());
                    } else {
                        LOG.info("ElasticSearch bulk streetaddress request successfully persisted " + response.getItems().length + " items in " + response.getTookInMillis() + " ms.");
                    }
                });
            }

            LOG.info("ElasticSearch streetaddresses indexed " + streetAddressCount + " items in " + watch);

        } else {

            LOG.info("ElasticSearch no streetaddresses to be indexed!");

        }

    }


    public void persistPostalCodes(final List<PostalCode> postalCodes) {

        postalCodeRepository.save(postalCodes);

    }

    public void indexPostalCodes() {

        final List<PostalCode> postalCodes = postalCodeRepository.findAll();

        if (!postalCodes.isEmpty()) {

            final ObjectMapper mapper = createObjectMapper();

            final BulkRequestBuilder bulkRequest = client.prepareBulk();

            postalCodes.forEach(postalCode -> {
                try {
                    bulkRequest.add(client.prepareIndex(DomainConstants.ELASTIC_INDEX_CUSTOMCODES, DomainConstants.ELASTIC_TYPE_POSTALCODE).setSource(mapper.writeValueAsString(postalCode)));
                } catch (JsonProcessingException e) {
                    LOG.error("Indexing postalcodes failed: " + e.getMessage());
                }
            });

            final BulkResponse response = bulkRequest.get();

            if (response.hasFailures()) {
                LOG.error("ElasticSearch bulk postalCode operation failed with errors: " + response.buildFailureMessage());
            } else {
                LOG.info("ElasticSearch bulk postalCode request successfully persisted " + response.getItems().length + " items in " + response.getTookInMillis() + " ms.");
            }
        } else {
            LOG.info("ElasticSearch bulk postalCode request failed: no content to be indexed!");
        }
    }

    public void persistHealthCareDistricts(final List<HealthCareDistrict> healthCareDistricts) {
        healthCareDistrictRepository.save(healthCareDistricts);
    }

    public void indexHealthCareDistricts() {
        final List<HealthCareDistrict> healthCareDistricts = healthCareDistrictRepository.findAll();
        if (!healthCareDistricts.isEmpty()) {
            final ObjectMapper mapper = createObjectMapper();
            final BulkRequestBuilder bulkRequest = client.prepareBulk();
            healthCareDistricts.forEach(healthCareDistrict -> {
                try {
                    bulkRequest.add(client.prepareIndex(DomainConstants.ELASTIC_INDEX_CUSTOMCODES, DomainConstants.ELASTIC_TYPE_HEALTHCAREDISTRICT).setSource(mapper.writeValueAsString(healthCareDistrict)));
                } catch (JsonProcessingException e) {
                    LOG.error("Indexing healthcaredistricts failed: " + e.getMessage());
                }
            });

            final BulkResponse response = bulkRequest.get();
            if (response.hasFailures()) {
                LOG.error("ElasticSearch bulk healthcaredistrict operation failed with errors: " + response.buildFailureMessage());
            } else {
                LOG.info("ElasticSearch bulk healthcaredistrict request successfully persisted " + response.getItems().length + " items in " + response.getTookInMillis() + " ms.");
            }
        } else {
            LOG.info("ElasticSearch bulk healthcaredistrict request failed: no content to be indexed!");
        }
    }

    public void persistElectoralDistricts(final List<ElectoralDistrict> electoralDistricts) {
        electoralDistrictRepository.save(electoralDistricts);
    }

    public void indexElectoralDistricts() {
        final List<ElectoralDistrict> electoralDistricts = electoralDistrictRepository.findAll();
        if (!electoralDistricts.isEmpty()) {
            final ObjectMapper mapper = createObjectMapper();
            final BulkRequestBuilder bulkRequest = client.prepareBulk();
            electoralDistricts.forEach(electoralDistrict -> {
                try {
                    bulkRequest.add(client.prepareIndex(DomainConstants.ELASTIC_INDEX_CUSTOMCODES, DomainConstants.ELASTIC_TYPE_ELECTORALDISTRICT).setSource(mapper.writeValueAsString(electoralDistrict)));
                } catch (JsonProcessingException e) {
                    LOG.error("Indexing electoraldistricts failed: " + e.getMessage());
                }
            });

            final BulkResponse response = bulkRequest.get();
            if (response.hasFailures()) {
                LOG.error("ElasticSearch bulk electoraldistrict operation failed with errors: " + response.buildFailureMessage());
            } else {
                LOG.info("ElasticSearch bulk electoraldistrict request successfully persisted " + response.getItems().length + " items in " + response.getTookInMillis() + " ms.");
            }
        } else {
            LOG.info("ElasticSearch bulk electoraldistrict request failed: no content to be indexed!");
        }
    }

    public void persistMagistrateServiceUnits(final List<MagistrateServiceUnit> magistrateServiceUnits) {
        magistrateServiceUnitRepository.save(magistrateServiceUnits);
    }

    public void indexMagistrateServiceUnits() {
        final List<MagistrateServiceUnit> magistrateServiceUnits = magistrateServiceUnitRepository.findAll();
        if (!magistrateServiceUnits.isEmpty()) {
            final ObjectMapper mapper = createObjectMapper();
            final BulkRequestBuilder bulkRequest = client.prepareBulk();
            magistrateServiceUnits.forEach(magistrateServiceUnit -> {
                try {
                    bulkRequest.add(client.prepareIndex(DomainConstants.ELASTIC_INDEX_CUSTOMCODES, DomainConstants.ELASTIC_TYPE_MAGISTRATESERVICEUNIT).setSource(mapper.writeValueAsString(magistrateServiceUnit)));
                } catch (JsonProcessingException e) {
                    LOG.error("Indexing magistrateserviceunits failed: " + e.getMessage());
                }
            });

            final BulkResponse response = bulkRequest.get();
            if (response.hasFailures()) {
                LOG.error("ElasticSearch bulk magistrateserviceunit operation failed with errors: " + response.buildFailureMessage());
            } else {
                LOG.info("ElasticSearch bulk magistrateserviceunit request successfully persisted " + response.getItems().length + " items in " + response.getTookInMillis() + " ms.");
            }
        } else {
            LOG.info("ElasticSearch bulk magistrateserviceunit request failed: no content to be indexed!");
        }
    }

    public void persistPostManagementDistricts(final List<PostManagementDistrict> postManagementDistricts) {
        postManagementDistrictRepository.save(postManagementDistricts);
    }

    public void indexPostManagementDistricts() {
        final List<PostManagementDistrict> postManagementDistricts = postManagementDistrictRepository.findAll();
        if (!postManagementDistricts.isEmpty()) {
            final ObjectMapper mapper = createObjectMapper();
            final BulkRequestBuilder bulkRequest = client.prepareBulk();
            postManagementDistricts.forEach(postManagementDistrict -> {
                try {
                    bulkRequest.add(client.prepareIndex(DomainConstants.ELASTIC_INDEX_CUSTOMCODES, DomainConstants.ELASTIC_TYPE_POSTMANAGEMENTDISTRICT).setSource(mapper.writeValueAsString(postManagementDistrict)));
                } catch (JsonProcessingException e) {
                    LOG.error("Indexing postmanagementdistrict failed: " + e.getMessage());
                }
            });

            final BulkResponse response = bulkRequest.get();
            if (response.hasFailures()) {
                LOG.error("ElasticSearch bulk postmanagementdistrict operation failed with errors: " + response.buildFailureMessage());
            } else {
                LOG.info("ElasticSearch bulk postmanagementdistrict request successfully persisted " + response.getItems().length + " items in " + response.getTookInMillis() + " ms.");
            }
        } else {
            LOG.info("ElasticSearch bulk postmanagementdistrict request failed: no content to be indexed!");
        }
    }

    public void persistBusinessServiceSubRegions(final List<BusinessServiceSubRegion> businessServiceSubRegions) {
        businessServiceSubRegionRepository.save(businessServiceSubRegions);
    }

    public void indexBusinessServiceSubRegions() {
        final List<BusinessServiceSubRegion> businessServiceSubRegions = businessServiceSubRegionRepository.findAll();
        if (!businessServiceSubRegions.isEmpty()) {
            final ObjectMapper mapper = createObjectMapper();
            final BulkRequestBuilder bulkRequest = client.prepareBulk();
            businessServiceSubRegions.forEach(businessServiceSubRegion -> {
                try {
                    bulkRequest.add(client.prepareIndex(DomainConstants.ELASTIC_INDEX_CUSTOMCODES, DomainConstants.ELASTIC_TYPE_BUSINESSSERVICESUBREGION).setSource(mapper.writeValueAsString(businessServiceSubRegion)));
                } catch (JsonProcessingException e) {
                    LOG.error("Indexing businessservicesubregions failed: " + e.getMessage());
                }
            });

            final BulkResponse response = bulkRequest.get();
            if (response.hasFailures()) {
                LOG.error("ElasticSearch bulk businessservicesubregion operation failed with errors: " + response.buildFailureMessage());
            } else {
                LOG.info("ElasticSearch bulk businessservicesubregion request successfully persisted " + response.getItems().length + " items in " + response.getTookInMillis() + " ms.");
            }
        } else {
            LOG.info("ElasticSearch bulk businessservicesubregion request failed: no content to be indexed!");
        }
    }

    public void persistBusinessIds(final List<BusinessId> businessIds) {
        businessIdRepository.save(businessIds);
    }

    public void indexBusinessIds() {
        final List<BusinessId> businessIds = businessIdRepository.findAll();
        if (!businessIds.isEmpty()) {
            final ObjectMapper mapper = createObjectMapper();
            final BulkRequestBuilder bulkRequest = client.prepareBulk();
            businessIds.forEach(businessId -> {
                try {
                    bulkRequest.add(client.prepareIndex(DomainConstants.ELASTIC_INDEX_CUSTOMCODES, DomainConstants.ELASTIC_TYPE_BUSINESSID).setSource(mapper.writeValueAsString(businessId)));
                } catch (JsonProcessingException e) {
                    LOG.error("Indexing businessids failed: " + e.getMessage());
                }
            });

            final BulkResponse response = bulkRequest.get();
            if (response.hasFailures()) {
                LOG.error("ElasticSearch bulk businessId operation failed with errors: " + response.buildFailureMessage());
            } else {
                LOG.info("ElasticSearch bulk businessId request successfully persisted " + response.getItems().length + " items in " + response.getTookInMillis() + " ms.");
            }
        } else {
            LOG.info("ElasticSearch bulk businessId request failed: no content to be indexed!");
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
                    bulkRequest.add(client.prepareIndex(DomainConstants.ELASTIC_INDEX_CODEREGISTRIES, DomainConstants.ELASTIC_TYPE_CODEREGISTRY).setSource(mapper.writeValueAsString(codeRegistry)));
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
                    bulkRequest.add(client.prepareIndex(DomainConstants.ELASTIC_INDEX_CODESCHEMES, DomainConstants.ELASTIC_TYPE_CODESCHEME).setSource(mapper.writeValueAsString(codeScheme)));
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

    public void reIndexCodes(final String codeRegistryCodeValue,
                             final String codeSchemeCodeValue) {
        // Clears earlier index for this codeRegistry and codeScheme.
        deleteTypeFromIndex(DomainConstants.ELASTIC_INDEX_CODES, DomainConstants.ELASTIC_TYPE_CODE);
        createIndexWithNestedPrefLabels(DomainConstants.ELASTIC_INDEX_CODES, DomainConstants.ELASTIC_TYPE_CODE);

        // Indexing of data to ElasticSearch.
        indexCodes(codeRegistryCodeValue, codeSchemeCodeValue);
        refreshIndex(DomainConstants.ELASTIC_INDEX_CODES);
    }

    public void indexCodes(final String codeRegistryCodeValue,
                           final String codeSchemeCodeValue) {
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);

        if (codeRegistry != null) {
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValue(codeRegistry, codeSchemeCodeValue);
            if (codeScheme != null) {
                final List<Code> codes = codeRepository.findByCodeScheme(codeScheme);
                if (!codes.isEmpty()) {
                    final ObjectMapper mapper = createObjectMapper();
                    final BulkRequestBuilder bulkRequest = client.prepareBulk();
                    codes.forEach(code -> {
                        try {
                            bulkRequest.add(client.prepareIndex(DomainConstants.ELASTIC_INDEX_CODES, DomainConstants.ELASTIC_TYPE_CODE).setSource(mapper.writeValueAsString(code)));
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
        }
    }

    public void persistCodes(final List<Code> codes) {
        codeRepository.save(codes);
    }

    public void reIndexCodeRegistries() {
        deleteIndex(DomainConstants.ELASTIC_INDEX_CODEREGISTRIES);
        final List<CodeRegistry> codeRegistry = codeRegistryRepository.findAll();
        if (!codeRegistry.isEmpty()) {
            createIndexWithNestedPrefLabels(DomainConstants.ELASTIC_INDEX_CODEREGISTRIES, DomainConstants.ELASTIC_TYPE_CODEREGISTRY);
            indexCodeRegistries();
        }
    }

    public void reIndexCodeSchemes() {
        deleteIndex(DomainConstants.ELASTIC_INDEX_CODESCHEMES);
        final List<CodeScheme> codeSchemes = codeSchemeRepository.findAll();
        if (!codeSchemes.isEmpty()) {
            createIndexWithNestedPrefLabels(DomainConstants.ELASTIC_INDEX_CODESCHEMES, DomainConstants.ELASTIC_TYPE_CODESCHEME);
            indexCodeSchemes();
        }
    }

    public void reIndexCodes() {
        deleteIndex(DomainConstants.ELASTIC_INDEX_CODES);
        final List<Code> codes = codeRepository.findAll();
        codes.forEach(code -> {
            final CodeScheme codeScheme = code.getCodeScheme();
            final String codeSchemeCodeValue = codeScheme.getCodeValue();
            final String codeRegistryCodeValue = codeScheme.getCodeRegistry().getCodeValue();
            createIndexWithNestedPrefLabels(DomainConstants.ELASTIC_INDEX_CODES, codeRegistryCodeValue + codeSchemeCodeValue);
            reIndexCodes(codeRegistryCodeValue, codeSchemeCodeValue);
        });
    }

    public void reIndexYti() {
        // Generic: CodeRegistries, CodeSchemes, Codes
        reIndexCodeRegistries();
        reIndexCodeSchemes();
        reIndexCodes();
    }

    public void reIndexEverything() {
        reIndexYti();

        // Clears earlier custom codes index.
        deleteIndex(DomainConstants.ELASTIC_INDEX_CUSTOMCODES);
        createIndexWithNestedPrefLabels(DomainConstants.ELASTIC_INDEX_CUSTOMCODES);

        // Indexing of custom codelist data to ElasticSearch.
        indexMagistrates();
        indexRegions();
        indexMunicipalities();
        indexHealthCareDistricts();
        indexPostManagementDistricts();
        indexPostalCodes();
        indexElectoralDistricts();
        indexMagistrateServiceUnits();
        indexBusinessServiceSubRegions();
        indexBusinessIds();
        indexStreetAddresses();

        refreshIndex(DomainConstants.ELASTIC_INDEX_CUSTOMCODES);
    }

    private ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        return mapper;
    }

}
