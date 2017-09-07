package fi.vm.yti.cls.intake.data;

import com.google.common.base.Stopwatch;
import fi.vm.yti.cls.common.model.Code;
import fi.vm.yti.cls.common.model.CodeRegistry;
import fi.vm.yti.cls.common.model.CodeScheme;
import fi.vm.yti.cls.intake.domain.Domain;
import fi.vm.yti.cls.intake.domain.DomainConstants;
import fi.vm.yti.cls.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.cls.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.cls.intake.parser.CodeParser;
import fi.vm.yti.cls.intake.parser.CodeRegistryParser;
import fi.vm.yti.cls.intake.parser.CodeSchemeParser;
import fi.vm.yti.cls.intake.update.UpdateManager;
import fi.vm.yti.cls.intake.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static fi.vm.yti.cls.intake.domain.DomainConstants.SOURCE_INTERNAL;


/**
 * Implementing class for DataAccess interface.
 *
 * This class provides method implementations for accessing YTI specific source data.
 */
@Service
public class YtiDataAccess implements DataAccess {

    private static final Logger LOG = LoggerFactory.getLogger(YtiDataAccess.class);

    private static final String DEFAULT_CODESCHEME_FILENAME = "v1_codeschemes.csv";

    private static final String DEFAULT_CODEREGISTRY_FILENAME = "v1_coderegistries.csv";

    private static final String DEFAULT_CODE_FILENAME = "v1_codes.csv";

    private static final String DEFAULT_CODEREGISTRY_NAME = "testregistry";

    private static final String DEFAULT_CODESCHEME_NAME = "testscheme";


    private final Domain m_domain;

    private final UpdateManager m_updateManager;

    private final CodeRegistryParser m_codeRegistryParser;

    private final CodeSchemeParser m_codeSchemeParser;

    private final CodeParser m_codeParser;

    private final CodeRegistryRepository m_codeRegistryRepository;

    private final CodeSchemeRepository m_codeSchemeRepository;


    @Inject
    public YtiDataAccess(final Domain domain,
                             final UpdateManager updateManager,
                             final CodeSchemeParser codeSchemeParser,
                             final CodeRegistryParser codeRegistryParser,
                             final CodeParser codeParser,
                             final CodeRegistryRepository codeRegistryRepository,
                             final CodeSchemeRepository codeSchemeRepository) {

        m_domain = domain;
        m_updateManager = updateManager;
        m_codeSchemeParser = codeSchemeParser;
        m_codeRegistryParser = codeRegistryParser;
        m_codeParser = codeParser;
        m_codeRegistryRepository = codeRegistryRepository;
        m_codeSchemeRepository = codeSchemeRepository;

    }


    public boolean checkForNewData() {

        // Generic Data Access has only static files now, no need for checking new data.

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

        if (m_updateManager.shouldUpdateData(DomainConstants.DATA_CODEREGISTRIES, DEFAULT_CODEREGISTRY_FILENAME)) {
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/coderegistries/" + DEFAULT_CODEREGISTRY_FILENAME);) {
                final List<CodeRegistry> codeRegistries = m_codeRegistryParser.parseCodeRegistriesFromClsInputStream(SOURCE_INTERNAL, inputStream);
                LOG.info("CodeRegistry data loaded: " + codeRegistries.size() + " coderegistries in " + watch);
                watch.reset().start();
                m_domain.persistCodeRegistries(codeRegistries);
                LOG.info("CodeRegistry data persisted in: " + watch);
            } catch (IOException e) {
                LOG.error("Issue with parsing CodeRegistry file. Message: " + e.getMessage());
            }
        } else {
            LOG.info("CodeRegistries already up to date, skipping...");
        }

    }


    private void loadDefaultCodeSchemes() {

        LOG.info("Loading default codeschemes...");

        final Stopwatch watch = Stopwatch.createStarted();

        if (m_updateManager.shouldUpdateData(DomainConstants.DATA_CODESCHEMES, DEFAULT_CODESCHEME_FILENAME)) {
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/codeschemes/" + DEFAULT_CODESCHEME_FILENAME);) {
                final CodeRegistry defaultCodeRegistry = m_codeRegistryRepository.findByCodeValue(DEFAULT_CODEREGISTRY_NAME);
                if (defaultCodeRegistry != null) {
                    final List<CodeScheme> codeSchemes = m_codeSchemeParser.parseCodeSchemesFromClsInputStream(defaultCodeRegistry, SOURCE_INTERNAL, inputStream);
                    LOG.info("CodeScheme data loaded: " + codeSchemes.size() + " codeschemes in " + watch);
                    watch.reset().start();
                    m_domain.persistCodeSchemes(codeSchemes);
                    LOG.info("CodeScheme data persisted in: " + watch);
                }
            } catch (IOException e) {
                LOG.error("Issue with parsing CodeScheme file. Message: " + e.getMessage());
            }
        } else {
            LOG.info("CodeSchemes already up to date, skipping...");
        }

    }


    private void loadDefaultCodes() {

        LOG.info("Loading default codes...");

        final Stopwatch watch = Stopwatch.createStarted();

        if (m_updateManager.shouldUpdateData(DomainConstants.DATA_CODES, DEFAULT_CODE_FILENAME)) {
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/codes/" + DEFAULT_CODE_FILENAME);) {
                final CodeRegistry defaultCodeRegistry = m_codeRegistryRepository.findByCodeValue(DEFAULT_CODEREGISTRY_NAME);
                if (defaultCodeRegistry != null) {
                    final CodeScheme defaultCodeScheme = m_codeSchemeRepository.findByCodeRegistryAndCodeValue(defaultCodeRegistry, DEFAULT_CODESCHEME_NAME);
                    if (defaultCodeScheme != null) {
                        final List<Code> codes = m_codeParser.parseCodesFromClsInputStream(defaultCodeScheme, SOURCE_INTERNAL, inputStream);
                        LOG.info("Code data loaded: " + codes.size() + " codes in " + watch);
                        watch.reset().start();
                        m_domain.persistCodes(codes);
                        LOG.info("Code data persisted in: " + watch);
                    }
                }
            } catch (IOException e) {
                LOG.error("Issue with parsing Code file. Message: " + e.getMessage());
            }
        } else {
            LOG.info("Code already up to date, skipping...");
        }

    }

}
