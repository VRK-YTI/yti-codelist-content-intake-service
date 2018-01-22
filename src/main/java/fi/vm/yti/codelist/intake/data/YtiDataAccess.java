package fi.vm.yti.codelist.intake.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.ExternalReference;
import fi.vm.yti.codelist.common.model.PropertyType;
import fi.vm.yti.codelist.common.model.UpdateStatus;
import fi.vm.yti.codelist.intake.configuration.ContentIntakeServiceProperties;
import fi.vm.yti.codelist.intake.domain.Domain;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.jpa.ExternalReferenceRepository;
import fi.vm.yti.codelist.intake.jpa.PropertyTypeRepository;
import fi.vm.yti.codelist.intake.parser.CodeParser;
import fi.vm.yti.codelist.intake.parser.CodeRegistryParser;
import fi.vm.yti.codelist.intake.parser.CodeSchemeParser;
import fi.vm.yti.codelist.intake.parser.ExternalReferenceParser;
import fi.vm.yti.codelist.intake.parser.PropertyTypeParser;
import fi.vm.yti.codelist.intake.update.UpdateManager;
import fi.vm.yti.codelist.intake.util.FileUtils;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.parser.AbstractBaseParser.YTI_DATACLASSIFICATION_CODESCHEME;
import static fi.vm.yti.codelist.intake.parser.AbstractBaseParser.EU_REGISTRY;

@Service
public class YtiDataAccess {

    private static final String DEFAULT_YTIREGISTRY_FILENAME = "ytiregistries.csv";
    private static final String DEFAULT_CODEREGISTRY_FILENAME = "coderegistries.csv";
    private static final String DEFAULT_TESTREGISTRY_FILENAME = "testcoderegistries.csv";
    private static final String DEFAULT_PROPERTYTYPE_FILENAME = "propertytypes.csv";
    private static final String DEFAULT_EXTERNALREFERENCE_FILENAME = "externalreferences.csv";

    private static final Logger LOG = LoggerFactory.getLogger(YtiDataAccess.class);

    private final Domain domain;
    private final ContentIntakeServiceProperties contentIntakeServiceProperties;
    private final UpdateManager updateManager;
    private final CodeRegistryParser codeRegistryParser;
    private final CodeSchemeParser codeSchemeParser;
    private final CodeParser codeParser;
    private final PropertyTypeParser propertyTypeParser;
    private final PropertyTypeRepository propertyTypeRepository;
    private final ExternalReferenceRepository externalReferenceRepository;
    private final ExternalReferenceParser externalReferenceParser;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRepository codeRepository;

    @Inject
    public YtiDataAccess(final Domain domain,
                         final ContentIntakeServiceProperties contentIntakeServiceProperties,
                         final UpdateManager updateManager,
                         final CodeSchemeParser codeSchemeParser,
                         final CodeRegistryParser codeRegistryParser,
                         final CodeParser codeParser,
                         final PropertyTypeParser propertyTypeParser,
                         final PropertyTypeRepository propertyTypeRepository,
                         final ExternalReferenceParser externalReferenceParser,
                         final ExternalReferenceRepository externalReferenceRepository,
                         final CodeRegistryRepository codeRegistryRepository,
                         final CodeSchemeRepository codeSchemeRepository,
                         final CodeRepository codeRepository) {
        this.domain = domain;
        this.contentIntakeServiceProperties = contentIntakeServiceProperties;
        this.updateManager = updateManager;
        this.codeSchemeParser = codeSchemeParser;
        this.codeRegistryParser = codeRegistryParser;
        this.codeParser = codeParser;
        this.propertyTypeRepository = propertyTypeRepository;
        this.propertyTypeParser = propertyTypeParser;
        this.externalReferenceRepository = externalReferenceRepository;
        this.externalReferenceParser = externalReferenceParser;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
    }

    @Transactional
    public void initializeOrRefresh() {
        LOG.info("Initializing data...");
        if (contentIntakeServiceProperties.getInitializeContent()) {
            loadDefaultPropertyTypes();
            loadDefaultExternalReferences();
            loadRegistryContent(DEFAULT_YTIREGISTRY_FILENAME, "V1_YTI");
            classifyDcat();
            loadRegistryContent(DEFAULT_CODEREGISTRY_FILENAME, "V1_DEFAULT");
            if (contentIntakeServiceProperties.getInitializeTestContent()) {
                loadRegistryContent(DEFAULT_TESTREGISTRY_FILENAME, "V1_TEST");
            }
        }
    }

    private void loadRegistryContent(final String filename,
                                     final String identifier) {
        final Set<CodeRegistry> codeRegistries = loadDefaultCodeRegistries(filename, identifier);
        if (!codeRegistries.isEmpty()) {
            final Set<CodeScheme> codeSchemes = loadDefaultCodeSchemes(codeRegistries);
            if (!codeSchemes.isEmpty()) {
                loadDefaultCodes(codeSchemes);
            }
        }
    }

    private Set<CodeRegistry> loadDefaultCodeRegistries(final String filename,
                                                        final String identifier) {
        LOG.info("Loading default CodeRegistries from file: " + filename);
        final Set<CodeRegistry> codeRegistries = new HashSet<>();
        final Stopwatch watch = Stopwatch.createStarted();
        if (updateManager.shouldUpdateData(DATA_CODEREGISTRIES, identifier, filename)) {
            final UpdateStatus updateStatus = updateManager.createStatus(DATA_CODEREGISTRIES, identifier, SOURCE_INTERNAL, filename, UpdateManager.UPDATE_RUNNING);
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_CODEREGISTRIES + "/" + filename)) {
                codeRegistries.addAll(codeRegistryParser.parseCodeRegistriesFromCsvInputStream(inputStream));
                LOG.info("CodeRegistry data loaded: " + codeRegistries.size() + " CodeRegistries in " + watch);
                watch.reset().start();
                domain.persistCodeRegistries(codeRegistries);
                LOG.info("CodeRegistry data persisted in: " + watch);
                if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
                    updateManager.updateSuccessStatus(updateStatus);
                }
            } catch (IOException e) {
                LOG.error("Issue with parsing CodeRegistry file. Message: ", e);
                updateManager.updateFailedStatus(updateStatus);
            }
        } else {
            LOG.info("CodeRegistries already up to date, skipping...");
        }
        return codeRegistries;
    }

    private Set<CodeScheme> loadDefaultCodeSchemes(final Set<CodeRegistry> codeRegistries) {
        LOG.info("Loading default CodeSchemes...");
        final Set<CodeScheme> codeSchemes = new HashSet<>();
        final Stopwatch watch = Stopwatch.createStarted();
        codeRegistries.forEach(codeRegistry -> {
            final String identifier = codeRegistry.getCodeValue();
            if (updateManager.shouldUpdateData(DATA_CODESCHEMES, identifier, identifier + ".csv")) {
                LOG.info("Loading CodeSchemes from CodeRegistry: " + identifier);
                final UpdateStatus updateStatus = updateManager.createStatus(DATA_CODESCHEMES, identifier, SOURCE_INTERNAL, identifier, UpdateManager.UPDATE_RUNNING);
                try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_CODESCHEMES + "/" + identifier + ".csv")) {
                    codeSchemes.addAll(codeSchemeParser.parseCodeSchemesFromCsvInputStream(codeRegistry, inputStream));
                } catch (IOException e) {
                    LOG.error("Issue with parsing CodeScheme file. ", e);
                    updateManager.updateFailedStatus(updateStatus);
                } catch (Exception e) {
                    LOG.error("Issue with existing CodeScheme found. ", e);
                    updateManager.updateFailedStatus(updateStatus);
                }
                if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
                    LOG.info("CodeScheme data loaded: " + codeSchemes.size() + " CodeSchemes in " + watch);
                    watch.reset().start();
                    domain.persistCodeSchemes(codeSchemes);
                    LOG.info("CodeScheme data persisted in: " + watch);
                    updateManager.updateSuccessStatus(updateStatus);
                }
            } else {
                LOG.info("CodeSchemes already up to date, skipping...");
            }
        });
        return codeSchemes;
    }

    private void loadDefaultCodes(final Set<CodeScheme> codeSchemes) {
        LOG.info("Loading default Codes...");
        final Stopwatch watch = Stopwatch.createStarted();
        codeSchemes.forEach(codeScheme -> {
            final String identifier = codeScheme.getCodeRegistry().getCodeValue() + "_" + codeScheme.getCodeValue();
            if (updateManager.shouldUpdateData(DATA_CODES, identifier, identifier + ".csv")) {
                LOG.info("Loading Codes from CodeScheme: " + identifier);
                final Set<Code> codes = new HashSet<>();
                final UpdateStatus updateStatus = updateManager.createStatus(DATA_CODES, identifier, SOURCE_INTERNAL, identifier, UpdateManager.UPDATE_RUNNING);
                try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_CODES + "/" + identifier + ".csv")) {
                    codes.addAll(codeParser.parseCodesFromCsvInputStream(codeScheme, inputStream));
                } catch (IOException e) {
                    LOG.error("Issue with parsing Code file. ", e);
                    updateManager.updateFailedStatus(updateStatus);
                } catch (Exception e) {
                    LOG.error("Issue with existing Code found. ", e);
                    updateManager.updateFailedStatus(updateStatus);
                }
                if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
                    LOG.info("Code data loaded: " + codes.size() + " Codes in " + watch);
                    watch.reset().start();
                    domain.persistCodes(codes);
                    LOG.info("Code data persisted in: " + watch);
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
        if (updateManager.shouldUpdateData(DATA_PROPERTYTYPES, "default", DEFAULT_PROPERTYTYPE_FILENAME)) {
            final UpdateStatus updateStatus = updateManager.createStatus(DATA_PROPERTYTYPES, "default", SOURCE_INTERNAL, DEFAULT_PROPERTYTYPE_FILENAME, UpdateManager.UPDATE_RUNNING);
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_PROPERTYTYPES + "/" + DEFAULT_PROPERTYTYPE_FILENAME)) {
                final List<PropertyType> propertyTypes = propertyTypeParser.parsePropertyTypesFromCsvInputStream(inputStream);
                LOG.info("PropertyType data loaded: " + propertyTypes.size() + " PropertyTypes in " + watch);
                watch.reset().start();
                propertyTypeRepository.save(propertyTypes);
                LOG.info("PropertyType data persisted in: " + watch);
                if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
                    updateManager.updateSuccessStatus(updateStatus);
                }
            } catch (IOException e) {
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
        if (updateManager.shouldUpdateData(DATA_EXTERNALREFERENCES, "default", DEFAULT_EXTERNALREFERENCE_FILENAME)) {
            final UpdateStatus updateStatus = updateManager.createStatus(DATA_EXTERNALREFERENCES, "default", SOURCE_INTERNAL, DEFAULT_EXTERNALREFERENCE_FILENAME, UpdateManager.UPDATE_RUNNING);
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_EXTERNALREFERENCES + "/" + DEFAULT_EXTERNALREFERENCE_FILENAME)) {
                final List<ExternalReference> propertyTypes = externalReferenceParser.parseExternalReferencesFromCsvInputStream(inputStream);
                LOG.info("ExternalReference data loaded: " + propertyTypes.size() + " ExternalReferences in " + watch);
                watch.reset().start();
                externalReferenceRepository.save(propertyTypes);
                LOG.info("ExternalReference data persisted in: " + watch);
                if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
                    updateManager.updateSuccessStatus(updateStatus);
                }
            } catch (IOException e) {
                LOG.error("Issue with parsing ExternalReference file. ", e);
                updateManager.updateFailedStatus(updateStatus);
            }
        } else {
            LOG.info("ExternalReferences already up to date, skipping...");
        }
    }

    private void classifyDcat() {
        LOG.info("Ensuring DCAT classification belongs to GOVE classification.");
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(EU_REGISTRY);
        classifyCodeSchemeWithCodeValue(codeRegistry, YTI_DATACLASSIFICATION_CODESCHEME, "GOVE");
    }

    private void classifyCodeSchemeWithCodeValue(final CodeRegistry codeRegistry, final String codeSchemeCodeValue, final String dataClassificationCodeValue) {
        final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValue(codeRegistry, codeSchemeCodeValue);
        final Code classification = getDataClassification(dataClassificationCodeValue);
        final Set<Code> classifications = new HashSet<>();
        classifications.add(classification);
        codeScheme.setDataClassifications(classifications);
        codeScheme.setModified(new Date(System.currentTimeMillis()));
        codeSchemeRepository.save(codeScheme);
    }

    private Code getDataClassification(final String codeValue) {
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(EU_REGISTRY);
        final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValue(codeRegistry, YTI_DATACLASSIFICATION_CODESCHEME);
        return codeRepository.findByCodeSchemeAndCodeValue(codeScheme, codeValue);
    }
}
