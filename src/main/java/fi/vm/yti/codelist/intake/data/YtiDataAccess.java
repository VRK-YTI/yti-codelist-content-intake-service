package fi.vm.yti.codelist.intake.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.Organization;
import fi.vm.yti.codelist.common.model.PropertyType;
import fi.vm.yti.codelist.common.model.UpdateStatus;
import fi.vm.yti.codelist.intake.domain.Domain;
import fi.vm.yti.codelist.intake.jpa.OrganizationRepository;
import fi.vm.yti.codelist.intake.jpa.PropertyTypeRepository;
import fi.vm.yti.codelist.intake.parser.CodeParser;
import fi.vm.yti.codelist.intake.parser.CodeRegistryParser;
import fi.vm.yti.codelist.intake.parser.CodeSchemeParser;
import fi.vm.yti.codelist.intake.parser.PropertyTypeParser;
import fi.vm.yti.codelist.intake.update.UpdateManager;
import fi.vm.yti.codelist.intake.util.FileUtils;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

@Service
public class YtiDataAccess {

    private static final String VRK_ORG_ID = "d9c76d52-03d3-4480-8c2c-b66e6d9c57f2";
    private static final String TEST_ORG_ID = "74a41211-8c99-4835-a519-7a61612b1098";

    private static final String DEFAULT_YTIREGISTRY_FILENAME = "ytiregistries.csv";
    private static final String DEFAULT_CODEREGISTRY_FILENAME = "coderegistries.csv";
    private static final String DEFAULT_PROPERTYTYPE_FILENAME = "propertytypes.csv";

    private static final Logger LOG = LoggerFactory.getLogger(YtiDataAccess.class);

    private final Domain domain;
    private final UpdateManager updateManager;
    private final CodeRegistryParser codeRegistryParser;
    private final CodeSchemeParser codeSchemeParser;
    private final CodeParser codeParser;
    private final PropertyTypeParser propertyTypeParser;
    private final PropertyTypeRepository propertyTypeRepository;
    private final OrganizationRepository organizationRepository;

    @Inject
    public YtiDataAccess(final Domain domain,
                         final UpdateManager updateManager,
                         final CodeSchemeParser codeSchemeParser,
                         final CodeRegistryParser codeRegistryParser,
                         final CodeParser codeParser,
                         final PropertyTypeParser propertyTypeParser,
                         final PropertyTypeRepository propertyTypeRepository,
                         final OrganizationRepository organizationRepository) {
        this.domain = domain;
        this.updateManager = updateManager;
        this.codeSchemeParser = codeSchemeParser;
        this.codeRegistryParser = codeRegistryParser;
        this.codeParser = codeParser;
        this.propertyTypeRepository = propertyTypeRepository;
        this.propertyTypeParser = propertyTypeParser;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public void initializeOrRefresh() {
        LOG.info("Initializing YTI DataAccess with mock/test data...");
        final Organization vrkOrganization = organizationRepository.findById(UUID.fromString(VRK_ORG_ID));
        loadRegistryContent(DEFAULT_YTIREGISTRY_FILENAME, vrkOrganization);
        final Organization testOrganization = organizationRepository.findById(UUID.fromString(TEST_ORG_ID));
        loadRegistryContent(DEFAULT_CODEREGISTRY_FILENAME, testOrganization);
        loadDefaultPropertyTypes();
    }

    private void loadRegistryContent(final String filename, final Organization organization) {
        final List<CodeRegistry> coodeRegistries = loadDefaultCodeRegistries(filename, organization);
        final List<CodeScheme> codeSchemes = loadDefaultCodeSchemes(coodeRegistries);
        loadDefaultCodes(codeSchemes);
    }

    private List<CodeRegistry> loadDefaultCodeRegistries(final String filename,
                                                         final Organization organization) {
        LOG.info("Loading default CodeRegistries fromm file: " + filename);
        final List<CodeRegistry> codeRegistries = new ArrayList<>();
        final Stopwatch watch = Stopwatch.createStarted();
        if (updateManager.shouldUpdateData(DATA_CODEREGISTRIES, filename)) {
            final UpdateStatus updateStatus = updateManager.createStatus(DATA_CODEREGISTRIES, SOURCE_INTERNAL, filename, UpdateManager.UPDATE_RUNNING);
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_CODEREGISTRIES + "/" + filename);) {
                codeRegistries.addAll(codeRegistryParser.parseCodeRegistriesFromCsvInputStream(inputStream));
                for (final CodeRegistry codeRegistry : codeRegistries) {
                    final Set<Organization> organizations = new HashSet<>();
                    organizations.add(organization);
                    codeRegistry.setOrganizations(organizations);
                }
                LOG.info("CodeRegistry data loaded: " + codeRegistries.size() + " CodeRegistries in " + watch);
                watch.reset().start();
                domain.persistCodeRegistries(codeRegistries);
                LOG.info("CodeRegistry data persisted in: " + watch);
                if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
                    updateManager.updateSuccessStatus(updateStatus);
                }
            } catch (IOException e) {
                LOG.error("Issue with parsing CodeRegistry file. Message: " + e.getMessage());
                updateManager.updateFailedStatus(updateStatus);
            }
        } else {
            LOG.info("CodeRegistries already up to date, skipping...");
        }
        return codeRegistries;
    }

    private List<CodeScheme> loadDefaultCodeSchemes(final List<CodeRegistry> codeRegistries) {
        LOG.info("Loading default CodeSchemes...");
        final List<CodeScheme> codeSchemes = new ArrayList<>();
        final Stopwatch watch = Stopwatch.createStarted();
        codeRegistries.forEach(codeRegistry -> {
            final String identifier = codeRegistry.getCodeValue();
            if (updateManager.shouldUpdateData(DATA_CODESCHEMES, identifier)) {
                LOG.info("Loading CodeSchemes from CodeRegistry: " + identifier);
                final UpdateStatus updateStatus = updateManager.createStatus(DATA_CODESCHEMES, SOURCE_INTERNAL, identifier, UpdateManager.UPDATE_RUNNING);
                try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_CODESCHEMES + "/" + identifier + ".csv");) {
                    codeSchemes.addAll(codeSchemeParser.parseCodeSchemesFromCsvInputStream(codeRegistry, inputStream));
                } catch (IOException e) {
                    LOG.error("Issue with parsing CodeScheme file. Message: " + e.getMessage());
                    updateManager.updateFailedStatus(updateStatus);
                } catch (Exception e) {
                    LOG.error("Issue with existing CodeScheme found. Message: " + e.getMessage());
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

    private void loadDefaultCodes(final List<CodeScheme> codeSchemes) {
        LOG.info("Loading default Codes...");
        final Stopwatch watch = Stopwatch.createStarted();
        codeSchemes.forEach(codeScheme -> {
            final String identifier = codeScheme.getCodeRegistry().getCodeValue() + "_" + codeScheme.getCodeValue();
            if (updateManager.shouldUpdateData(DATA_CODES, identifier)) {
                LOG.info("Loading Codes from CodeScheme: " + identifier);
                final List<Code> codes = new ArrayList<>();
                final UpdateStatus updateStatus = updateManager.createStatus(DATA_CODES, SOURCE_INTERNAL, identifier, UpdateManager.UPDATE_RUNNING);
                try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_CODES + "/" + identifier + ".csv");) {
                    codes.addAll(codeParser.parseCodesFromCsvInputStream(codeScheme, inputStream));
                } catch (IOException e) {
                    LOG.error("Issue with parsing Code file. Message: " + e.getMessage());
                    updateManager.updateFailedStatus(updateStatus);
                } catch (Exception e) {
                    LOG.error("Issue with existing Code found. Message: " + e.getMessage());
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
        if (updateManager.shouldUpdateData(DATA_PROPERTYTYPES, DEFAULT_PROPERTYTYPE_FILENAME)) {
            final UpdateStatus updateStatus = updateManager.createStatus(DATA_PROPERTYTYPES, SOURCE_INTERNAL, DEFAULT_PROPERTYTYPE_FILENAME, UpdateManager.UPDATE_RUNNING);
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_PROPERTYTYPES + "/" + DEFAULT_PROPERTYTYPE_FILENAME);) {
                final List<PropertyType> propertyTypes = propertyTypeParser.parsePropertyTypesFromCsvInputStream(inputStream);
                LOG.info("PropertyType data loaded: " + propertyTypes.size() + " PropertyTypes in " + watch);
                watch.reset().start();
                propertyTypeRepository.save(propertyTypes);
                LOG.info("PropertyType data persisted in: " + watch);
                if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
                    updateManager.updateSuccessStatus(updateStatus);
                }
            } catch (IOException e) {
                LOG.error("Issue with parsing PropertyType file. Message: " + e.getMessage());
                updateManager.updateFailedStatus(updateStatus);
            }
        } else {
            LOG.info("PropertyTypes already up to date, skipping...");
        }
    }
}
