package fi.vm.yti.cls.intake.data;

import com.google.common.base.Stopwatch;
import fi.vm.yti.cls.common.model.BusinessServiceSubRegion;
import fi.vm.yti.cls.common.model.ElectoralDistrict;
import fi.vm.yti.cls.common.model.HealthCareDistrict;
import fi.vm.yti.cls.common.model.Magistrate;
import fi.vm.yti.cls.common.model.MagistrateServiceUnit;
import fi.vm.yti.cls.common.model.Municipality;
import fi.vm.yti.cls.common.model.Region;
import fi.vm.yti.cls.common.model.UpdateStatus;
import fi.vm.yti.cls.intake.domain.Domain;
import fi.vm.yti.cls.intake.domain.DomainConstants;
import fi.vm.yti.cls.intake.parser.BusinessServiceSubRegionParser;
import fi.vm.yti.cls.intake.parser.ElectoralDistrictParser;
import fi.vm.yti.cls.intake.parser.HealthCareDistrictParser;
import fi.vm.yti.cls.intake.parser.MagistrateParser;
import fi.vm.yti.cls.intake.parser.MagistrateServiceUnitParser;
import fi.vm.yti.cls.intake.parser.MunicipalityParser;
import fi.vm.yti.cls.intake.parser.RegionItemParser;
import fi.vm.yti.cls.intake.update.UpdateManager;
import fi.vm.yti.cls.intake.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static fi.vm.yti.cls.intake.update.UpdateManager.UPDATE_RUNNING;


/**
 * Implementing class for DataAccess interface.
 *
 * This class provides method implementations for accessing source data.
 */
@Service
public class GenericDataAccess implements DataAccess {

    private static final Logger LOG = LoggerFactory.getLogger(GenericDataAccess.class);

    private static final String DEFAULT_MUNICIPALITY_FILENAME = "CUsersA011673.AHKDownloadsKuntaluettelo.csv";

    private static final String DEFAULT_HEALTHCAREDISTRICT_FILENAME = "Shp_jäsenkunnat_2017-modified.xls";

    private static final String DEFAULT_HEALTHCAREDISTRICTNAME_FILENAME = "2017-06-16-healthcaredistrict-names.txt";

    private static final String DEFAULT_BUSINESSSERVICESUBREGION_FILENAME = "seudut_ja_kunnat.xlsx";

    private static final String DEFAULT_CODESCHEME_FILENAME = "v1_codeschemes.csv";

    private static final String DEFAULT_CODEREGISTRY_FILENAME = "v1_coderegistries.csv";

    private static final String DEFAULT_CODE_FILENAME = "v1_codes.csv";

    private static final String DEFAULT_CODEREGISTRY_NAME = "testregistry";

    private static final String DEFAULT_CODESCHEME_NAME = "testscheme";


    private final Domain m_domain;

    private final UpdateManager m_updateManager;

    private final MunicipalityParser m_municipalityParser;

    private final HealthCareDistrictParser m_healthCareDistrictParser;

    private final RegionItemParser m_regionParser;

    private final MagistrateParser m_magistrateParser;

    private final MagistrateServiceUnitParser m_magistrateServiceUnitParser;

    private final ElectoralDistrictParser m_electoralDistrictParser;

    private final BusinessServiceSubRegionParser m_businessServiceSubRegionParser;


    @Inject
    public GenericDataAccess(final Domain domain,
                             final UpdateManager updateManager,
                             final MunicipalityParser municipalityParser,
                             final HealthCareDistrictParser healthCareDistrictParser,
                             final RegionItemParser regionParser,
                             final MagistrateParser magistrateParser,
                             final MagistrateServiceUnitParser magistrateServiceUnitParser,
                             final ElectoralDistrictParser electoralDistrictParser,
                             final BusinessServiceSubRegionParser businessServiceSubRegionParser) {

        m_domain = domain;
        m_updateManager = updateManager;
        m_municipalityParser = municipalityParser;
        m_healthCareDistrictParser = healthCareDistrictParser;
        m_regionParser = regionParser;
        m_magistrateParser = magistrateParser;
        m_magistrateServiceUnitParser = magistrateServiceUnitParser;
        m_electoralDistrictParser = electoralDistrictParser;
        m_businessServiceSubRegionParser = businessServiceSubRegionParser;

    }


    public boolean checkForNewData() {

        // Generic Data Access has only static files now, no need for checking new data.

        return false;

    }


    public void initializeOrRefresh() {

        LOG.info("Initializing generic DataAccess...");

        loadDefaultMagistrates();

        loadDefaultRegions();

        loadDefaultMunicipalities();

        loadDefaultHealthCareDistricts();

        loadDefaultElectoralDistricts();

        loadDefaultMagistrateServiceUnits();

        loadDefaultBusinessServiceSubRegions();

    }


    /**
     * Loads municipalities data from a file inside the JAR.
     */
    private void loadDefaultMunicipalities() {

        LOG.info("Loading municipalities...");

        if (m_updateManager.shouldUpdateData(DomainConstants.DATA_MUNICIPALITIES, DEFAULT_MUNICIPALITY_FILENAME)) {

            final UpdateStatus updateStatus = m_updateManager.createStatus(DomainConstants.DATA_MUNICIPALITIES, DomainConstants.SOURCE_AVOINDATA, DEFAULT_MUNICIPALITY_FILENAME, UPDATE_RUNNING);

            final Stopwatch watch = Stopwatch.createStarted();

            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/municipalities/" + DEFAULT_MUNICIPALITY_FILENAME)) {
                final List<Municipality> municipalities = m_municipalityParser.parseMunicipalitiesFromInputStream(DomainConstants.SOURCE_AVOINDATA, inputStream);
                LOG.info("Municipality data loaded: " + municipalities.size() + " municipalities found in " + watch);
                watch.reset().start();
                m_domain.persistMunicipalities(municipalities);
                LOG.info("Municipality data persisted in: " + watch);

                if (updateStatus.getStatus().equals(UPDATE_RUNNING)) {
                    m_updateManager.updateSuccessStatus(updateStatus);
                }

            } catch (IOException e) {
                m_updateManager.updateSuccessStatus(updateStatus);
                LOG.error("Municipality data loading failed: " + e.getMessage());
            }

        } else {

            LOG.info("Municipalities already up to date, skipping...");

        }

    }

    /**
     * Loads municipalities data from a file inside the JAR.
     */
    private void loadDefaultMagistrates() {

        LOG.info("Loading magistrates...");

        if (m_updateManager.shouldUpdateData(DomainConstants.DATA_MAGISTRATES, DEFAULT_MUNICIPALITY_FILENAME)) {

            final UpdateStatus updateStatus = m_updateManager.createStatus(DomainConstants.DATA_MAGISTRATES, DomainConstants.SOURCE_AVOINDATA, DEFAULT_MUNICIPALITY_FILENAME, UPDATE_RUNNING);

            final Stopwatch watch = Stopwatch.createStarted();

            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/municipalities/" + DEFAULT_MUNICIPALITY_FILENAME)) {
                final List<Magistrate> magistrates = m_magistrateParser.parseMagistratesFromInputStream(DomainConstants.SOURCE_AVOINDATA, inputStream);
                LOG.info("Magistrate data loaded: " + magistrates.size() + " magistrates found in " + watch);
                watch.reset().start();
                m_domain.persistMagistrates(magistrates);
                LOG.info("Magistrate data persisted in: " + watch);

                if (updateStatus.getStatus().equals(UPDATE_RUNNING)) {
                    m_updateManager.updateSuccessStatus(updateStatus);
                }

            } catch (IOException e) {
                m_updateManager.updateFailedStatus(updateStatus);
                LOG.error("Magistrate data loading failed: " + e.getMessage());
            }

        } else {

            LOG.info("Magistrates already up to date, skipping...");

        }

    }


    /**
     * Loads regions data from a static file inside the JAR and persists them to database.
     */
    private void loadDefaultRegions() {

        LOG.info("Loading regions...");

        if (m_updateManager.shouldUpdateData(DomainConstants.DATA_REGIONS, DEFAULT_MUNICIPALITY_FILENAME)) {

            final UpdateStatus updateStatus = m_updateManager.createStatus(DomainConstants.DATA_REGIONS, DomainConstants.SOURCE_AVOINDATA, DEFAULT_MUNICIPALITY_FILENAME, UPDATE_RUNNING);

            final Stopwatch watch = Stopwatch.createStarted();

            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/municipalities/" + DEFAULT_MUNICIPALITY_FILENAME)) {
                final List<Region> regions = m_regionParser.parseRegionsFromInputStream(DomainConstants.SOURCE_AVOINDATA, inputStream);
                LOG.info("Region data loaded: " + regions.size() + " regions found in " + watch);
                watch.reset().start();
                m_domain.persistRegions(regions);
                LOG.info("Region data persisted in " + watch);

                if (updateStatus.getStatus().equals(UPDATE_RUNNING)) {
                    m_updateManager.updateSuccessStatus(updateStatus);
                }

            } catch (IOException e) {
                m_updateManager.updateFailedStatus(updateStatus);
                LOG.error("Region data loading failed: " + e.getMessage());
            }

        } else {

            LOG.info("Regions already up to date, skipping...");

        }

    }


    /**
     * Loads electoraldistricts data from a static file inside the JAR.
     */
    private void loadDefaultElectoralDistricts() {

        LOG.info("Loading electoraldistricts...");

        if (m_updateManager.shouldUpdateData(DomainConstants.DATA_ELECTORALDISTRICTS, DEFAULT_MUNICIPALITY_FILENAME)) {

            final UpdateStatus updateStatus = m_updateManager.createStatus(DomainConstants.DATA_ELECTORALDISTRICTS, DomainConstants.SOURCE_AVOINDATA, DEFAULT_MUNICIPALITY_FILENAME, UPDATE_RUNNING);

            final Stopwatch watch = Stopwatch.createStarted();

            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/municipalities/" + DEFAULT_MUNICIPALITY_FILENAME)) {
                final List<ElectoralDistrict> electoralDistricts = m_electoralDistrictParser.parseElectoralDistrictsFromInputStream(DomainConstants.SOURCE_AVOINDATA, inputStream);
                LOG.info("ElectoralDistrict data loaded: " + electoralDistricts.size() + " electoraldistricts found in " + watch);
                watch.reset().start();
                m_domain.persistElectoralDistricts(electoralDistricts);
                LOG.info("ElectoralDistrict persisted in " + watch);

                if (updateStatus.getStatus().equals(UPDATE_RUNNING)) {
                    m_updateManager.updateSuccessStatus(updateStatus);
                }

            } catch (IOException e) {
                m_updateManager.updateFailedStatus(updateStatus);
                LOG.error("ElectoralDistrict data loading failed: " + e.getMessage());
            }

        } else {

            LOG.info("ElectoralDistricts already up to date, skipping...");

        }

    }


    /**
     * Loads magistrateServiceUnits data from a static file inside the JAR.
     */
    private void loadDefaultMagistrateServiceUnits() {

        LOG.info("Loading magistrateserviceunits...");

        if (m_updateManager.shouldUpdateData(DomainConstants.DATA_MAGISTRATESERVICEUNITS, DEFAULT_MUNICIPALITY_FILENAME)) {

            final UpdateStatus updateStatus = m_updateManager.createStatus(DomainConstants.DATA_MAGISTRATESERVICEUNITS, DomainConstants.SOURCE_AVOINDATA, DEFAULT_MUNICIPALITY_FILENAME, UPDATE_RUNNING);

            final Stopwatch watch = Stopwatch.createStarted();

            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/municipalities/" + DEFAULT_MUNICIPALITY_FILENAME)) {
                final List<MagistrateServiceUnit> magistrateServiceUnits = m_magistrateServiceUnitParser.parseMagistrateServiceUnitsFromInputStream(DomainConstants.SOURCE_AVOINDATA, inputStream);
                LOG.info("MagistrateServiceUnit data loaded: " + magistrateServiceUnits.size() + " magistrateserviceunits found in " + watch);
                watch.reset().start();
                m_domain.persistMagistrateServiceUnits(magistrateServiceUnits);
                LOG.info("MagistrateServiceUnit data persisted in " + watch);

                if (updateStatus.getStatus().equals(UPDATE_RUNNING)) {
                    m_updateManager.updateSuccessStatus(updateStatus);
                }

            } catch (IOException e) {
                m_updateManager.updateFailedStatus(updateStatus);
                LOG.error("MagistrateServiceUnit data loading failed: " + e.getMessage());
            }

        } else {

            LOG.info("MagistrateServiceUnits already up to date, skipping...");

        }

    }


    /**
     * Loads healthcaredistrict data from a static Excel file inside the JAR and persists them to database.
     */
    private void loadDefaultHealthCareDistricts() {

        LOG.info("Loading healthcaredistricts...");

        final String versionString = DEFAULT_HEALTHCAREDISTRICT_FILENAME + "_" + DEFAULT_HEALTHCAREDISTRICTNAME_FILENAME;

        if (m_updateManager.shouldUpdateData(DomainConstants.DATA_HEALTHCAREDISTRICTS, versionString)) {

            final UpdateStatus updateStatus = m_updateManager.createStatus(DomainConstants.DATA_HEALTHCAREDISTRICTS, DomainConstants.SOURCE_KUNNAT_NET, versionString, UPDATE_RUNNING);

            final Stopwatch watch = Stopwatch.createStarted();

            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/healthcaredistricts/" + DEFAULT_HEALTHCAREDISTRICT_FILENAME)) {
                final List<HealthCareDistrict> healthCareDistricts = m_healthCareDistrictParser.parseHealthCareDistrictsFromExcelInputStream(DomainConstants.SOURCE_KUNNAT_NET, inputStream);
                LOG.info("HealthCareDistrict data loaded: " + healthCareDistricts.size() + " healthcaredistricts found in " + watch);
                watch.reset().start();
                m_domain.persistHealthCareDistricts(healthCareDistricts);
                LOG.info("HealthCareDistrict persisted in " + watch);
                ensureDefaultHealthCareDistrictsNames();

                if (updateStatus.getStatus().equals(UPDATE_RUNNING)) {
                    m_updateManager.updateSuccessStatus(updateStatus);
                }

            } catch (IOException e) {
                m_updateManager.updateFailedStatus(updateStatus);
                LOG.error("HealthCareDistrict data loading failed: " + e.getMessage());
            }

        } else {

            LOG.info("HealthCareDistricts already up to date, skipping...");

        }

    }


    /**
     * Updates healthcaredistrict name information from a static .txt file inside the JAR and persists them to database if changes.
     */
    private void ensureDefaultHealthCareDistrictsNames() {

        LOG.info("Updating healthcaredistrict names...");

        try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/healthcaredistricts/" + DEFAULT_HEALTHCAREDISTRICTNAME_FILENAME)) {
            final Stopwatch watch = Stopwatch.createStarted();

            final List<HealthCareDistrict> healthCareDistricts = m_healthCareDistrictParser.parseHealthCareDistrictNamesFromInputStream(DomainConstants.SOURCE_THL_CODESERVICE, inputStream);
            if (!healthCareDistricts.isEmpty()) {
                LOG.info("HealthCareDistrict names updated for: " + healthCareDistricts.size() + " healthcaredistricts in " + watch);
                watch.reset().start();
                m_domain.persistHealthCareDistricts(healthCareDistricts);
                LOG.info("HealthCareDistrict updates persisted in " + watch);
            } else {
                LOG.info("HealthCareDistrict names up to date, checking took " + watch);
            }
        } catch (IOException e) {
            LOG.error("HealthCareDistrict name updating failed: " + e.getMessage());
        }

    }


    /**
     * Loads business service subregion data from a static Excel file inside the JAR and persists them to database.
     */
    private void loadDefaultBusinessServiceSubRegions() {

        LOG.info("Loading businessservicesubregions...");

        if (m_updateManager.shouldUpdateData(DomainConstants.DATA_BUSINESSSERVICESUBREGIONS, DEFAULT_BUSINESSSERVICESUBREGION_FILENAME)) {

            final Stopwatch watch = Stopwatch.createStarted();

            final UpdateStatus updateStatus = m_updateManager.createStatus(DomainConstants.DATA_BUSINESSSERVICESUBREGIONS, DomainConstants.SOURCE_ELY_KESKUS, DEFAULT_BUSINESSSERVICESUBREGION_FILENAME, UPDATE_RUNNING);

            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/businessservicesubregions/" + DEFAULT_BUSINESSSERVICESUBREGION_FILENAME)) {
                final List<BusinessServiceSubRegion> businessServiceSubRegions = m_businessServiceSubRegionParser.parseBusinessServiceSubRegionsFromInputStream(DomainConstants.SOURCE_ELY_KESKUS, inputStream);
                LOG.info("BusinessServiceSubRegion data loaded: " + businessServiceSubRegions.size() + " businessservicesubregions found in " + watch);
                watch.reset().start();
                m_domain.persistBusinessServiceSubRegions(businessServiceSubRegions);
                LOG.info("BusinessServiceSubRegion data persisted in " + watch);

                if (updateStatus.getStatus().equals(UPDATE_RUNNING)) {
                    m_updateManager.updateSuccessStatus(updateStatus);
                }

            } catch (IOException e) {
                m_updateManager.updateFailedStatus(updateStatus);
                LOG.error("BusinessServiceSubRegion data loading failed: " + e.getMessage());
            }

        } else {

            LOG.info("BusinessServiceSubRegions already up to date, skipping...");

        }

    }

}
