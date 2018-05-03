package fi.vm.yti.codelist.intake.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.intake.configuration.ContentIntakeServiceProperties;
import fi.vm.yti.codelist.intake.dao.CodeDao;
import fi.vm.yti.codelist.intake.dao.CodeRegistryDao;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.UpdateStatus;
import fi.vm.yti.codelist.intake.service.CodeRegistryService;
import fi.vm.yti.codelist.intake.service.CodeSchemeService;
import fi.vm.yti.codelist.intake.service.CodeService;
import fi.vm.yti.codelist.intake.service.ExternalReferenceService;
import fi.vm.yti.codelist.intake.service.PropertyTypeService;
import fi.vm.yti.codelist.intake.update.UpdateManager;
import fi.vm.yti.codelist.intake.util.FileUtils;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.parser.AbstractBaseParser.JUPO_REGISTRY;
import static fi.vm.yti.codelist.intake.parser.AbstractBaseParser.YTI_DATACLASSIFICATION_CODESCHEME;

@Service
public class YtiDataAccess {

    public static final String DEFAULT_PROPERTYTYPE_FILENAME = "propertytypes.csv";
    public static final String DEFAULT_EXTERNALREFERENCE_FILENAME = "externalreferences.csv";
    private static final String DEFAULT_YTIREGISTRY_FILENAME = "ytiregistries.csv";
    private static final String DEFAULT_CLASSIFICATIONREGISTRY_FILENAME = "classificationregistries.csv";
    private static final String DEFAULT_CODEREGISTRY_FILENAME = "coderegistries.csv";
    private static final String DEFAULT_TESTREGISTRY_FILENAME = "testcoderegistries.csv";
    private static final String SERVICE_CLASSIFICATION_P9 = "P9";
    private static final String DEFAULT_IDENTIFIER = "default";
    private static final Logger LOG = LoggerFactory.getLogger(YtiDataAccess.class);

    private final ContentIntakeServiceProperties contentIntakeServiceProperties;
    private final UpdateManager updateManager;
    private final CodeRegistryDao codeRegistryDao;
    private final CodeSchemeDao codeSchemeDao;
    private final CodeDao codeDao;
    private final CodeRegistryService codeRegistryService;
    private final CodeSchemeService codeSchemeService;
    private final CodeService codeService;
    private final ExternalReferenceService externalReferenceService;
    private final PropertyTypeService propertyTypeService;

    @Inject
    public YtiDataAccess(final ContentIntakeServiceProperties contentIntakeServiceProperties,
                         final UpdateManager updateManager,
                         final CodeRegistryDao codeRegistryDao,
                         final CodeSchemeDao codeSchemeDao,
                         final CodeDao codeDao,
                         final CodeRegistryService codeRegistryService,
                         final CodeSchemeService codeSchemeService,
                         final CodeService codeService,
                         final ExternalReferenceService externalReferenceService,
                         final PropertyTypeService propertyTypeService) {
        this.contentIntakeServiceProperties = contentIntakeServiceProperties;
        this.updateManager = updateManager;
        this.codeRegistryDao = codeRegistryDao;
        this.codeSchemeDao = codeSchemeDao;
        this.codeDao = codeDao;
        this.codeRegistryService = codeRegistryService;
        this.codeSchemeService = codeSchemeService;
        this.codeService = codeService;
        this.externalReferenceService = externalReferenceService;
        this.propertyTypeService = propertyTypeService;
    }

    @Transactional
    public void initializeOrRefresh() {
        LOG.info("Initializing data...");
        if (contentIntakeServiceProperties.getInitializeContent()) {
            initializeDefaultData();
            loadRegistryContent(DEFAULT_YTIREGISTRY_FILENAME, "V2_YTI");
            loadRegistryContent(DEFAULT_CODEREGISTRY_FILENAME, "V1_DEFAULT");
        }
        if (contentIntakeServiceProperties.getInitializeTestContent()) {
            loadRegistryContent(DEFAULT_TESTREGISTRY_FILENAME, "V1_TEST");
        }
    }

    @Transactional
    public void initializeDefaultData() {
        loadDefaultPropertyTypes();
        loadDefaultExternalReferences();
        loadRegistryContent(DEFAULT_CLASSIFICATIONREGISTRY_FILENAME, "V2_CLASSIFICATION");
        classifyServiceClassification();
    }

    private void loadRegistryContent(final String filename,
                                     final String identifier) {
        final Set<CodeRegistryDTO> codeRegistries = loadDefaultCodeRegistries(filename, identifier);
        if (!codeRegistries.isEmpty()) {
            final Set<CodeSchemeDTO> codeSchemes = loadDefaultCodeSchemes(codeRegistries);
            if (!codeSchemes.isEmpty()) {
                loadDefaultCodes(codeSchemes);
            }
        }
    }

    private Set<CodeRegistryDTO> loadDefaultCodeRegistries(final String filename,
                                                           final String identifier) {
        LOG.info("Loading default CodeRegistries from file: " + filename);
        final Set<CodeRegistryDTO> codeRegistries = new HashSet<>();
        final Stopwatch watch = Stopwatch.createStarted();
        if (updateManager.shouldUpdateData(DATA_CODEREGISTRIES, identifier, filename)) {
            final UpdateStatus updateStatus = updateManager.createStatus(DATA_CODEREGISTRIES, identifier, SOURCE_INTERNAL, filename, UpdateManager.UPDATE_RUNNING);
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_CODEREGISTRIES + "/" + filename)) {
                codeRegistries.addAll(codeRegistryService.parseAndPersistCodeRegistriesFromSourceData(true, FORMAT_CSV, inputStream, null));
                LOG.info("CodeRegistry data loaded: " + codeRegistries.size() + " CodeRegistries in " + watch);
                watch.reset().start();
                LOG.info("CodeRegistry data persisted in: " + watch);
                if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
                    updateManager.updateSuccessStatus(updateStatus);
                }
            } catch (final IOException e) {
                LOG.error("Issue with parsing CodeRegistry file. Message: ", e);
                updateManager.updateFailedStatus(updateStatus);
            }
        } else {
            LOG.info("CodeRegistries already up to date, skipping...");
        }
        return codeRegistries;
    }

    private Set<CodeSchemeDTO> loadDefaultCodeSchemes(final Set<CodeRegistryDTO> codeRegistries) {
        LOG.info("Loading default CodeSchemes...");
        final Set<CodeSchemeDTO> codeSchemes = new HashSet<>();
        codeRegistries.forEach(codeRegistry -> {
            final Stopwatch watch = Stopwatch.createStarted();
            final String identifier = codeRegistry.getCodeValue();
            if (updateManager.shouldUpdateData(DATA_CODESCHEMES, identifier, identifier + ".csv")) {
                LOG.info("Loading CodeSchemes from CodeRegistry: " + identifier);
                final UpdateStatus updateStatus = updateManager.createStatus(DATA_CODESCHEMES, identifier, SOURCE_INTERNAL, identifier, UpdateManager.UPDATE_RUNNING);
                try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_CODESCHEMES + "/" + identifier + ".csv")) {
                    watch.reset().start();
                    codeSchemes.addAll(codeSchemeService.parseAndPersistCodeSchemesFromSourceData(true, codeRegistry.getCodeValue(), FORMAT_CSV, inputStream, null));
                    LOG.info("CodeScheme data parsed and persisted in: " + watch);
                } catch (final IOException e) {
                    LOG.error("Issue with parsing CodeScheme file. ", e);
                    updateManager.updateFailedStatus(updateStatus);
                } catch (final Exception e) {
                    LOG.error("Issue with existing CodeScheme found. ", e);
                    updateManager.updateFailedStatus(updateStatus);
                }
                if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
                    LOG.info("Code data update successful!");
                    updateManager.updateSuccessStatus(updateStatus);
                }
            } else {
                LOG.info("CodeSchemes already up to date, skipping...");
            }
        });
        return codeSchemes;
    }

    private void loadDefaultCodes(final Set<CodeSchemeDTO> codeSchemes) {
        LOG.info("Loading default Codes...");
        codeSchemes.forEach(codeScheme -> {
            final Stopwatch watch = Stopwatch.createStarted();
            final String identifier = codeScheme.getCodeRegistry().getCodeValue() + "_" + codeScheme.getCodeValue();
            if (updateManager.shouldUpdateData(DATA_CODES, identifier, identifier + ".csv")) {
                LOG.info("Loading Codes from CodeScheme: " + identifier);
                final UpdateStatus updateStatus = updateManager.createStatus(DATA_CODES, identifier, SOURCE_INTERNAL, identifier, UpdateManager.UPDATE_RUNNING);
                try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_CODES + "/" + identifier + ".csv")) {
                    final Set<CodeDTO> codes = codeService.parseAndPersistCodesFromSourceData(true, codeScheme.getCodeRegistry().getCodeValue(), codeScheme.getCodeValue(), FORMAT_CSV, inputStream, null);
                    LOG.info("Code data loaded: " + codes.size() + " Codes in " + watch);
                } catch (final IOException e) {
                    LOG.error("Issue with parsing Code file. ", e);
                    updateManager.updateFailedStatus(updateStatus);
                } catch (final Exception e) {
                    LOG.error("Issue with existing Code found. ", e);
                    updateManager.updateFailedStatus(updateStatus);
                }
                if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
                    LOG.info("Code data update successful!");
                    updateManager.updateSuccessStatus(updateStatus);
                }
            } else {
                LOG.info("Code already up to date, skipping...");
            }
        });
    }

    private void loadDefaultPropertyTypes() {
        LOG.info("Loading default PropertyTypes...");
        final Stopwatch watch = Stopwatch.createStarted();
        if (updateManager.shouldUpdateData(DATA_PROPERTYTYPES, DEFAULT_IDENTIFIER, DEFAULT_PROPERTYTYPE_FILENAME)) {
            final UpdateStatus updateStatus = updateManager.createStatus(DATA_PROPERTYTYPES, DEFAULT_IDENTIFIER, SOURCE_INTERNAL, DEFAULT_PROPERTYTYPE_FILENAME, UpdateManager.UPDATE_RUNNING);
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_PROPERTYTYPES + "/" + DEFAULT_PROPERTYTYPE_FILENAME)) {
                final Set<PropertyTypeDTO> propertyTypes = propertyTypeService.parseAndPersistPropertyTypesFromSourceData(true, FORMAT_CSV, inputStream, null);
                LOG.info("PropertyType data loaded and persisted " + propertyTypes.size() + " PropertyTypes in " + watch);
                watch.reset().start();
                if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
                    updateManager.updateSuccessStatus(updateStatus);
                }
            } catch (final IOException e) {
                LOG.error("Issue with parsing PropertyType file. ", e);
                updateManager.updateFailedStatus(updateStatus);
            }
        } else {
            LOG.info("PropertyTypes already up to date, skipping...");
        }
    }

    private void loadDefaultExternalReferences() {
        LOG.info("Loading default ExternalReferences...");
        final Stopwatch watch = Stopwatch.createStarted();
        if (updateManager.shouldUpdateData(DATA_EXTERNALREFERENCES, DEFAULT_IDENTIFIER, DEFAULT_EXTERNALREFERENCE_FILENAME)) {
            final UpdateStatus updateStatus = updateManager.createStatus(DATA_EXTERNALREFERENCES, DEFAULT_IDENTIFIER, SOURCE_INTERNAL, DEFAULT_EXTERNALREFERENCE_FILENAME, UpdateManager.UPDATE_RUNNING);
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_EXTERNALREFERENCES + "/" + DEFAULT_EXTERNALREFERENCE_FILENAME)) {
                final Set<ExternalReferenceDTO> externalReferenceDtos = externalReferenceService.parseAndPersistExternalReferencesFromSourceData(true, FORMAT_CSV, inputStream, null, null);
                LOG.info("ExternalReference data loaded and persisted " + externalReferenceDtos.size() + " ExternalReferences in " + watch);
                watch.reset().start();
                if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
                    updateManager.updateSuccessStatus(updateStatus);
                }
            } catch (final IOException e) {
                LOG.error("Issue with parsing ExternalReference file. ", e);
                updateManager.updateFailedStatus(updateStatus);
            }
        } else {
            LOG.info("ExternalReferences already up to date, skipping...");
        }
    }

    private void classifyServiceClassification() {
        LOG.info("Ensuring Service Classification CodeScheme belongs to P9 classification.");
        final CodeRegistry codeRegistry = codeRegistryDao.findByCodeValue(JUPO_REGISTRY);
        classifyCodeSchemeWithCodeValue(codeRegistry, YTI_DATACLASSIFICATION_CODESCHEME, SERVICE_CLASSIFICATION_P9);
    }

    private void classifyCodeSchemeWithCodeValue(final CodeRegistry codeRegistry, final String codeSchemeCodeValue, final String dataClassificationCodeValue) {
        final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryAndCodeValue(codeRegistry, codeSchemeCodeValue);
        final Code classification = getDataClassification(dataClassificationCodeValue);
        final Set<Code> classifications = new HashSet<>();
        classifications.add(classification);
        codeScheme.setDataClassifications(classifications);
        codeScheme.setModified(new Date(System.currentTimeMillis()));
        codeSchemeDao.save(codeScheme);
    }

    private Code getDataClassification(final String codeValue) {
        final CodeRegistry codeRegistry = codeRegistryDao.findByCodeValue(JUPO_REGISTRY);
        final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryAndCodeValue(codeRegistry, YTI_DATACLASSIFICATION_CODESCHEME);
        return codeDao.findByCodeSchemeAndCodeValue(codeScheme, codeValue);
    }
}
