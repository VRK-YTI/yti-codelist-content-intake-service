package fi.vm.yti.codelist.intake.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.UpdateStatus;
import fi.vm.yti.codelist.intake.domain.Domain;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.parser.CodeParser;
import fi.vm.yti.codelist.intake.parser.CodeRegistryParser;
import fi.vm.yti.codelist.intake.parser.CodeSchemeParser;
import fi.vm.yti.codelist.intake.update.UpdateManager;
import fi.vm.yti.codelist.intake.util.FileUtils;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

/**
 * Implementing class for DataAccess interface.
 * <p>
 * This class provides method implementations for accessing YTI specific source data.
 */
@Service
public class YtiDataAccess implements DataAccess {

    private static final Logger LOG = LoggerFactory.getLogger(YtiDataAccess.class);

    private final Domain domain;
    private final UpdateManager updateManager;
    private final CodeRegistryParser codeRegistryParser;
    private final CodeSchemeParser codeSchemeParser;
    private final CodeParser codeParser;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeSchemeRepository codeSchemeRepository;

    @Inject
    public YtiDataAccess(final Domain domain,
                         final UpdateManager updateManager,
                         final CodeSchemeParser codeSchemeParser,
                         final CodeRegistryParser codeRegistryParser,
                         final CodeParser codeParser,
                         final CodeRegistryRepository codeRegistryRepository,
                         final CodeSchemeRepository codeSchemeRepository) {
        this.domain = domain;
        this.updateManager = updateManager;
        this.codeSchemeParser = codeSchemeParser;
        this.codeRegistryParser = codeRegistryParser;
        this.codeParser = codeParser;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
    }

    public boolean checkForNewData() {
        // YTI Data Access has only static files now, no need for checking new data.
        return false;
    }

    public void initializeOrRefresh() {
        LOG.info("Initializing YTI DataAccess with test data...");
        loadDefaultCodeRegistries();
        loadDefaultCodeSchemes();
        loadDefaultCodes();
    }

    private void loadDefaultCodeRegistries() {
        LOG.info("Loading default coderegistries...");
        final Stopwatch watch = Stopwatch.createStarted();
        if (updateManager.shouldUpdateData(DATA_CODEREGISTRIES, DEFAULT_CODEREGISTRY_FILENAME)) {
            final UpdateStatus updateStatus = updateManager.createStatus(DATA_CODEREGISTRIES, SOURCE_INTERNAL, DEFAULT_CODEREGISTRY_FILENAME, UpdateManager.UPDATE_RUNNING);
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/coderegistries/" + DEFAULT_CODEREGISTRY_FILENAME);) {
                final List<CodeRegistry> codeRegistries = codeRegistryParser.parseCodeRegistriesFromInputStream(SOURCE_INTERNAL, inputStream);
                LOG.info("CodeRegistry data loaded: " + codeRegistries.size() + " coderegistries in " + watch);
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
    }

    private void loadDefaultCodeSchemes() {
        LOG.info("Loading default codeschemes...");
        final Stopwatch watch = Stopwatch.createStarted();
        if (updateManager.shouldUpdateData(DATA_CODESCHEMES, DEFAULT_CODESCHEME_FILENAME)) {
            final List<CodeScheme> codeSchemes = new ArrayList<>();
            final UpdateStatus updateStatus = updateManager.createStatus(DATA_CODESCHEMES, SOURCE_INTERNAL, DEFAULT_CODESCHEME_FILENAME, UpdateManager.UPDATE_RUNNING);
            final Set<CodeRegistry> defaultCodeRegistries = codeRegistryRepository.findAll();
            defaultCodeRegistries.forEach(codeRegistry -> {
                if (codeRegistry.getCodeValue().startsWith(DEFAULT_CODEREGISTRY_NAME_PREFIX)) {
                    try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/codeschemes/" + DEFAULT_CODESCHEME_FILENAME);) {
                        codeSchemes.addAll(codeSchemeParser.parseCodeSchemesFromInputStream(codeRegistry, SOURCE_INTERNAL, inputStream));
                    } catch (IOException e) {
                        LOG.error("Issue with parsing CodeScheme file. Message: " + e.getMessage());
                        updateManager.updateFailedStatus(updateStatus);
                    } catch (Exception e) {
                        LOG.error("Issue with existing code found. Message: " + e.getMessage());
                        updateManager.updateFailedStatus(updateStatus);
                    }
                }
            });
            if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
                LOG.info("CodeScheme data loaded: " + codeSchemes.size() + " codeschemes in " + watch);
                watch.reset().start();
                domain.persistCodeSchemes(codeSchemes);
                LOG.info("CodeScheme data persisted in: " + watch);
                updateManager.updateSuccessStatus(updateStatus);
            }
        } else {
            LOG.info("CodeSchemes already up to date, skipping...");
        }
    }

    private void loadDefaultCodes() {
        LOG.info("Loading default codes...");
        final Stopwatch watch = Stopwatch.createStarted();
        if (updateManager.shouldUpdateData(DATA_CODES, DEFAULT_CODE_FILENAME)) {
            final List<Code> codes = new ArrayList<>();
            final UpdateStatus updateStatus = updateManager.createStatus(DATA_CODES, SOURCE_INTERNAL, DEFAULT_CODE_FILENAME, UpdateManager.UPDATE_RUNNING);
            final Set<CodeRegistry> defaultCodeRegistries = codeRegistryRepository.findAll();
            defaultCodeRegistries.forEach(codeRegistry -> {
                if (codeRegistry.getCodeValue().startsWith(DEFAULT_CODEREGISTRY_NAME_PREFIX)) {
                    final Set<CodeScheme> defaultCodeSchemes = codeSchemeRepository.findByCodeRegistry(codeRegistry);
                    defaultCodeSchemes.forEach(codeScheme -> {
                        try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/codes/" + DEFAULT_CODE_FILENAME);) {
                            codes.addAll(codeParser.parseCodesFromInputStream(codeScheme, SOURCE_INTERNAL, inputStream));
                        } catch (IOException e) {
                            LOG.error("Issue with parsing Code file. Message: " + e.getMessage());
                            updateManager.updateFailedStatus(updateStatus);
                        } catch (Exception e) {
                            LOG.error("Issue with existing code found. Message: " + e.getMessage());
                            updateManager.updateFailedStatus(updateStatus);
                        }
                    });
                    if (defaultCodeSchemes.isEmpty()) {
                        LOG.error("Loading default test scheme with name: " + DEFAULT_CODESCHEME_NAME + " failed!");
                        updateManager.updateFailedStatus(updateStatus);
                    }
                } else {
                    LOG.error("Loading default test registry with name: " + DEFAULT_CODEREGISTRY_NAME_PREFIX + " failed!");
                    updateManager.updateFailedStatus(updateStatus);
                }
            });
            if (updateStatus.getStatus().equals(UpdateManager.UPDATE_RUNNING)) {
                LOG.info("Code data loaded: " + codes.size() + " codes in " + watch);
                domain.persistCodes(codes);
                LOG.info("Code data persisted in: " + watch);
                updateManager.updateSuccessStatus(updateStatus);
            }
        } else {
            LOG.info("Code already up to date, skipping...");
        }
    }
}
