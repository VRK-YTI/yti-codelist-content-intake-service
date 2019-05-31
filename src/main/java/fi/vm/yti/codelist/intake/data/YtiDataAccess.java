package fi.vm.yti.codelist.intake.data;

import java.io.IOException;
import java.io.InputStream;
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
import fi.vm.yti.codelist.common.dto.ValueTypeDTO;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.configuration.ContentIntakeServiceProperties;
import fi.vm.yti.codelist.intake.dao.CodeDao;
import fi.vm.yti.codelist.intake.dao.CodeRegistryDao;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.dao.ExtensionDao;
import fi.vm.yti.codelist.intake.dao.MemberDao;
import fi.vm.yti.codelist.intake.language.LanguageService;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.Member;
import fi.vm.yti.codelist.intake.model.UpdateStatus;
import fi.vm.yti.codelist.intake.service.CodeRegistryService;
import fi.vm.yti.codelist.intake.service.CodeSchemeService;
import fi.vm.yti.codelist.intake.service.CodeService;
import fi.vm.yti.codelist.intake.service.ExternalReferenceService;
import fi.vm.yti.codelist.intake.service.PropertyTypeService;
import fi.vm.yti.codelist.intake.service.ValueTypeService;
import fi.vm.yti.codelist.intake.update.UpdateManager;
import fi.vm.yti.codelist.intake.util.FileUtils;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.parser.impl.AbstractBaseParser.JUPO_REGISTRY;
import static fi.vm.yti.codelist.intake.parser.impl.AbstractBaseParser.YTI_DATACLASSIFICATION_INFODOMAIN_CODESCHEME;

@Service
public class YtiDataAccess {

    public static final String DEFAULT_PROPERTYTYPE_FILENAME = "propertytypes.csv";
    public static final String DEFAULT_VALUETYPE_FILENAME = "valuetypes.csv";
    public static final String DEFAULT_EXTERNALREFERENCE_FILENAME = "externalreferences.csv";

    private static final Logger LOG = LoggerFactory.getLogger(YtiDataAccess.class);

    private static final String MIGRATION_URIS = "urimigration";
    private static final String MIGRATION_LANGUAGECODES = "languagecodemigration";

    private static final String DEFAULT_CLASSIFICATIONREGISTRY_FILENAME = "classificationregistries.csv"; // classification = information domain
    private static final String DEFAULT_INTEROPERABILITYREGISTRY_FILENAME = "interoperabilityplatformregistries.csv";

    private static final String SERVICE_CLASSIFICATION_P9 = "P9"; // classification = information domain

    private static final String DEFAULT_IDENTIFIER = "default";
    private static final String MIGRATION_URIS_VERSION = "v3";
    private static final String MIGRATION_LANGUAGECODES_VERSION = "v1";
    private static final String PROPERTYTYPE_IDENTIFIER = "v16";
    private static final String VALUETYPE_IDENTIFIER = "v8";

    private boolean isInitializing;

    private final ContentIntakeServiceProperties contentIntakeServiceProperties;
    private final UpdateManager updateManager;
    private final CodeRegistryDao codeRegistryDao;
    private final CodeSchemeDao codeSchemeDao;
    private final CodeDao codeDao;
    private final ExtensionDao extensionDao;
    private final MemberDao memberDao;
    private final CodeRegistryService codeRegistryService;
    private final CodeSchemeService codeSchemeService;
    private final CodeService codeService;
    private final ExternalReferenceService externalReferenceService;
    private final PropertyTypeService propertyTypeService;
    private final ValueTypeService valueTypeService;
    private final ApiUtils apiUtils;
    private final LanguageService languageService;

    @Inject
    public YtiDataAccess(final ContentIntakeServiceProperties contentIntakeServiceProperties,
                         final UpdateManager updateManager,
                         final CodeRegistryDao codeRegistryDao,
                         final CodeSchemeDao codeSchemeDao,
                         final CodeDao codeDao,
                         final ExtensionDao extensionDao,
                         final MemberDao memberDao,
                         final CodeRegistryService codeRegistryService,
                         final CodeSchemeService codeSchemeService,
                         final CodeService codeService,
                         final ExternalReferenceService externalReferenceService,
                         final PropertyTypeService propertyTypeService,
                         final ValueTypeService valueTypeService,
                         final ApiUtils apiUtils,
                         final LanguageService languageService) {
        this.contentIntakeServiceProperties = contentIntakeServiceProperties;
        this.updateManager = updateManager;
        this.codeRegistryDao = codeRegistryDao;
        this.codeSchemeDao = codeSchemeDao;
        this.codeDao = codeDao;
        this.extensionDao = extensionDao;
        this.memberDao = memberDao;
        this.codeRegistryService = codeRegistryService;
        this.codeSchemeService = codeSchemeService;
        this.codeService = codeService;
        this.externalReferenceService = externalReferenceService;
        this.propertyTypeService = propertyTypeService;
        this.valueTypeService = valueTypeService;
        this.apiUtils = apiUtils;
        this.languageService = languageService;
        isInitializing = true;
    }

    public boolean isInitializing() {
        return isInitializing;
    }

    @Transactional
    public void initializeOrRefresh() {
        LOG.info("Initializing data...");
        if (contentIntakeServiceProperties.getInitializeContent()) {
            initializeDefaultData();
            isInitializing = false;
        }
    }

    @Transactional
    public void initializeDefaultData() {
        loadDefaultValueTypes();
        loadDefaultPropertyTypes();
        loadDefaultExternalReferences();
        loadRegistryContent(DEFAULT_CLASSIFICATIONREGISTRY_FILENAME, "V2_CLASSIFICATION"); //classification = information domain
        classifyServiceClassification();
        loadRegistryContent(DEFAULT_INTEROPERABILITYREGISTRY_FILENAME, "V2_INTEROPERABILITY");
        languageService.loadLanguageCodes();
        setLanguageCodesToEarlierCodeSchemes();
        rewriteAllUris();
    }

    @Transactional
    public void setLanguageCodesToEarlierCodeSchemes() {
        LOG.info("Setting language codes to earlier codeschemes...");
        if (updateManager.shouldUpdateData(MIGRATION_LANGUAGECODES, MIGRATION_LANGUAGECODES_VERSION, MIGRATION_LANGUAGECODES_VERSION)) {
            final UpdateStatus updateStatus = updateManager.createStatus(MIGRATION_LANGUAGECODES, MIGRATION_LANGUAGECODES_VERSION, SOURCE_INTERNAL, MIGRATION_LANGUAGECODES_VERSION, UpdateManager.UPDATE_RUNNING);
            final Set<Code> defaultLanguageCodes = new HashSet<>();
            defaultLanguageCodes.add(languageService.getLanguageCode("fi"));
            defaultLanguageCodes.add(languageService.getLanguageCode("sv"));
            defaultLanguageCodes.add(languageService.getLanguageCode("en"));
            final Set<CodeScheme> codeSchemes = codeSchemeDao.findAll();
            if (codeSchemes != null) {
                codeSchemes.forEach(codeScheme -> {
                    if (codeScheme.getLanguageCodes() == null || codeScheme.getLanguageCodes().isEmpty()) {
                        codeScheme.setLanguageCodes(defaultLanguageCodes);
                        codeSchemeDao.save(codeScheme);
                    }
                });
            }
            if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
                updateManager.updateSuccessStatus(updateStatus);
            }
        } else {
            LOG.info("CodeScheme languageCodes already up to date, skipping...");
        }
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
        LOG.info(String.format("Loading default CodeRegistries from file: %s", filename));
        final Set<CodeRegistryDTO> codeRegistries = new HashSet<>();
        final Stopwatch watch = Stopwatch.createStarted();
        if (updateManager.shouldUpdateData(DATA_CODEREGISTRIES, identifier, filename)) {
            final UpdateStatus updateStatus = updateManager.createStatus(DATA_CODEREGISTRIES, identifier, SOURCE_INTERNAL, filename, UpdateManager.UPDATE_RUNNING);
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_CODEREGISTRIES + "/" + filename)) {
                codeRegistries.addAll(codeRegistryService.parseAndPersistCodeRegistriesFromSourceData(true, FORMAT_CSV, inputStream, null));
                LOG.info(String.format("CodeRegistry data loaded: %d CodeRegistries in %s", codeRegistries.size(), watch));
                watch.reset().start();
                LOG.info(String.format("CodeRegistry data persisted in: %s", watch));
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
                LOG.info(String.format("Loading CodeSchemes from CodeRegistry: %s", identifier));
                final UpdateStatus updateStatus = updateManager.createStatus(DATA_CODESCHEMES, identifier, SOURCE_INTERNAL, identifier, UpdateManager.UPDATE_RUNNING);
                try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_CODESCHEMES + "/" + identifier + ".csv")) {
                    watch.reset().start();
                    codeSchemes.addAll(codeSchemeService.parseAndPersistCodeSchemesFromSourceData(true, codeRegistry.getCodeValue(), FORMAT_CSV, inputStream, null, false, "", false));
                    LOG.info(String.format("CodeScheme data parsed and persisted in: %s", watch));
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
                LOG.info(String.format("Loading Codes from CodeScheme: %s", identifier));
                final UpdateStatus updateStatus = updateManager.createStatus(DATA_CODES, identifier, SOURCE_INTERNAL, identifier, UpdateManager.UPDATE_RUNNING);
                try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_CODES + "/" + identifier + ".csv")) {
                    final Set<CodeDTO> codes = codeService.parseAndPersistCodesFromSourceData(true, codeScheme.getCodeRegistry().getCodeValue(), codeScheme.getCodeValue(), FORMAT_CSV, inputStream, null);
                    LOG.info(String.format("Code data loaded: %d Codes in %s", codes.size(), watch));
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
        if (updateManager.shouldUpdateData(DATA_PROPERTYTYPES, PROPERTYTYPE_IDENTIFIER, DEFAULT_PROPERTYTYPE_FILENAME)) {
            final UpdateStatus updateStatus = updateManager.createStatus(DATA_PROPERTYTYPES, PROPERTYTYPE_IDENTIFIER, SOURCE_INTERNAL, DEFAULT_PROPERTYTYPE_FILENAME, UpdateManager.UPDATE_RUNNING);
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_PROPERTYTYPES + "/" + DEFAULT_PROPERTYTYPE_FILENAME)) {
                final Set<PropertyTypeDTO> propertyTypes = propertyTypeService.parseAndPersistPropertyTypesFromSourceData(true, FORMAT_CSV, inputStream, null);
                LOG.info(String.format("PropertyType data loaded and persisted %d PropertyTypes in %s", propertyTypes.size(), watch));
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

    private void loadDefaultValueTypes() {
        LOG.info("Loading default ValueTypes...");
        final Stopwatch watch = Stopwatch.createStarted();
        if (updateManager.shouldUpdateData(DATA_VALUETYPES, VALUETYPE_IDENTIFIER, DEFAULT_VALUETYPE_FILENAME)) {
            final UpdateStatus updateStatus = updateManager.createStatus(DATA_VALUETYPES, VALUETYPE_IDENTIFIER, SOURCE_INTERNAL, DEFAULT_VALUETYPE_FILENAME, UpdateManager.UPDATE_RUNNING);
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_VALUETYPES + "/" + DEFAULT_VALUETYPE_FILENAME)) {
                final Set<ValueTypeDTO> valueTypes = valueTypeService.parseAndPersistValueTypesFromSourceData(true, FORMAT_CSV, inputStream, null);
                LOG.info(String.format("ValueType data loaded and persisted %d ValueType in %s", valueTypes.size(), watch));
                watch.reset().start();
                if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
                    updateManager.updateSuccessStatus(updateStatus);
                }
            } catch (final IOException e) {
                LOG.error("Issue with parsing ValueType file. ", e);
                updateManager.updateFailedStatus(updateStatus);
            }
        } else {
            LOG.info("ValueTypes already up to date, skipping...");
        }
    }

    private void loadDefaultExternalReferences() {
        LOG.info("Loading default ExternalReferences...");
        final Stopwatch watch = Stopwatch.createStarted();
        if (updateManager.shouldUpdateData(DATA_EXTERNALREFERENCES, DEFAULT_IDENTIFIER, DEFAULT_EXTERNALREFERENCE_FILENAME)) {
            final UpdateStatus updateStatus = updateManager.createStatus(DATA_EXTERNALREFERENCES, DEFAULT_IDENTIFIER, SOURCE_INTERNAL, DEFAULT_EXTERNALREFERENCE_FILENAME, UpdateManager.UPDATE_RUNNING);
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_EXTERNALREFERENCES + "/" + DEFAULT_EXTERNALREFERENCE_FILENAME)) {
                final Set<ExternalReferenceDTO> externalReferenceDtos = externalReferenceService.parseAndPersistExternalReferencesFromSourceData(true, FORMAT_CSV, inputStream, null, null);
                LOG.info(String.format("ExternalReference data loaded and persisted %d ExternalReferences in %s", externalReferenceDtos.size(), watch));
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

    private void rewriteAllUris() {
        LOG.info("Rewriting URIs...");
        if (updateManager.shouldUpdateData(MIGRATION_URIS, MIGRATION_URIS_VERSION, MIGRATION_URIS_VERSION)) {
            final UpdateStatus updateStatus = updateManager.createStatus(MIGRATION_URIS, MIGRATION_URIS_VERSION, SOURCE_INTERNAL, MIGRATION_URIS_VERSION, UpdateManager.UPDATE_RUNNING);
            rewriteCodeRegistryUris();
            rewriteCodeSchemeUris();
            rewriteCodeUris();
            rewriteExtensionUris();
            rewriteMemberUris();
            if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
                updateManager.updateSuccessStatus(updateStatus);
            }
        } else {
            LOG.info("URIs already up to date, skipping...");
        }
    }

    private void rewriteCodeRegistryUris() {
        final Set<CodeRegistry> codeRegistries = codeRegistryDao.findAll();
        codeRegistries.forEach(codeRegistry -> codeRegistry.setUri(apiUtils.createCodeRegistryUri(codeRegistry)));
        codeRegistryDao.save(codeRegistries, false);
    }

    private void rewriteCodeSchemeUris() {
        final Set<CodeScheme> codeSchemes = codeSchemeDao.findAll();
        codeSchemes.forEach(codeScheme -> codeScheme.setUri(apiUtils.createCodeSchemeUri(codeScheme)));
        codeSchemeDao.save(codeSchemes, false);
    }

    private void rewriteCodeUris() {
        final Set<Code> codes = codeDao.findAll();
        codes.forEach(code -> code.setUri(apiUtils.createCodeUri(code)));
        codeDao.save(codes, false);
    }

    private void rewriteExtensionUris() {
        final Set<Extension> extensions = extensionDao.findAll();
        extensions.forEach(extension -> extension.setUri(apiUtils.createExtensionUri(extension)));
        extensionDao.save(extensions, false);
    }

    private void rewriteMemberUris() {
        final Set<Member> members = memberDao.findAll();
        members.forEach(member -> member.setUri(apiUtils.createMemberUri(member)));
        memberDao.save(members, false);
    }

    /**
     * classification = information domain
     */
    private void classifyServiceClassification() {
        LOG.info("Ensuring Service Classification CodeScheme belongs to P9 classification.");
        final CodeRegistry codeRegistry = codeRegistryDao.findByCodeValue(JUPO_REGISTRY);
        classifyCodeSchemeWithCodeValue(codeRegistry, YTI_DATACLASSIFICATION_INFODOMAIN_CODESCHEME, SERVICE_CLASSIFICATION_P9);
    }

    /**
     * classification = information domain
     */
    private void classifyCodeSchemeWithCodeValue(final CodeRegistry codeRegistry,
                                                 final String codeSchemeCodeValue,
                                                 final String dataClassificationCodeValue) {
        final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryAndCodeValue(codeRegistry, codeSchemeCodeValue);
        final Code infoDomain = getInfoDomain(dataClassificationCodeValue);
        final Set<Code> infoDomains = new HashSet<>();
        infoDomains.add(infoDomain);
        codeScheme.setInfoDomains(infoDomains);
        codeSchemeDao.save(codeScheme);
    }

    private Code getInfoDomain(final String codeValue) {
        final CodeRegistry codeRegistry = codeRegistryDao.findByCodeValue(JUPO_REGISTRY);
        final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryAndCodeValue(codeRegistry, YTI_DATACLASSIFICATION_INFODOMAIN_CODESCHEME);
        return codeDao.findByCodeSchemeAndCodeValue(codeScheme, codeValue);
    }

}
