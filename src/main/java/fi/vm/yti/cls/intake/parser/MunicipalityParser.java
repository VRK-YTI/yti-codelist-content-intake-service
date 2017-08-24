package fi.vm.yti.cls.intake.parser;

import fi.vm.yti.cls.common.model.BusinessServiceSubRegion;
import fi.vm.yti.cls.common.model.ElectoralDistrict;
import fi.vm.yti.cls.common.model.HealthCareDistrict;
import fi.vm.yti.cls.common.model.Magistrate;
import fi.vm.yti.cls.common.model.MagistrateServiceUnit;
import fi.vm.yti.cls.common.model.Municipality;
import fi.vm.yti.cls.common.model.Region;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.intake.api.ApiConstants;
import fi.vm.yti.cls.intake.api.ApiUtils;
import fi.vm.yti.cls.intake.util.FileUtils;
import fi.vm.yti.cls.intake.util.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;


/**
 * Class that handles parsing of municipalities from source data.
 */
@Service
public class MunicipalityParser {

    private static final Logger LOG = LoggerFactory.getLogger(MunicipalityParser.class);

    private static final String LANGUAGE_FI = "fi";

    private static final String LANGUAGE_SE = "se";

    private static final String TYPE_CITY = "city";

    private static final String TYPE_MUNICIPALITY = "municipality";

    private final ApiUtils m_apiUtils;

    private final ParserUtils m_parserUtils;


    @Inject
    public MunicipalityParser(final ApiUtils apiUtils,
                              final ParserUtils parserUtils) {

        m_apiUtils = apiUtils;

        m_parserUtils = parserUtils;

    }


    /**
     * Parses the .csv Municipality-file and returns the municipalities as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The Municipality -file.
     * @return List of Municipality objects.
     */
    public List<Municipality> parseMunicipalitiesFromClsInputStream(final String source,
                                                                    final InputStream inputStream) {

        final Map<String, Region> existingRegionsMap = m_parserUtils.getRegionsMap();

        final Map<String, Magistrate> existingMagistratesMap = m_parserUtils.getMagistratesMap();

        final Map<String, MagistrateServiceUnit> existingMagistrateServiceUnitsMap = m_parserUtils.getMagistrateServiceUnitsMap();

        final Map<String, Municipality> existingMunicipalitiesMap = m_parserUtils.getMunicipalitiesMap();

        final Map<String, BusinessServiceSubRegion> existingBusinessServiceSubRegionsMap = m_parserUtils.getBusinessServiceSubRegionsMap();

        final Map<String, ElectoralDistrict> existingElectoralDistrictsMap = m_parserUtils.getElectoralDistrictsMap();

        final Map<String, HealthCareDistrict> existingHealthCareDistrictsMap = m_parserUtils.getHealthCareDistrictsMap();

        final List<Municipality> m_municipalities = new ArrayList<>();

        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            final BufferedReader in = new BufferedReader(inputStreamReader);
            FileUtils.skipBom(in);

            final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withHeader());

            final List<CSVRecord> records = csvParser.getRecords();

            records.forEach(record -> {
                final String code = Utils.ensureMunicipalityIdPadding(record.get("CODE"));
                final String finnishName = record.get("NAME_FI");
                final String swedishName = record.get("NAME_SE");
                final String englishName = record.get("NAME_EN");
                final Status status = Status.valueOf(record.get("STATUS"));
                final String type = resolveType(record.get("TYPE"));
                final Set<String> languages = new HashSet<>(Arrays.asList(record.get("LANGUAGES").toLowerCase().split("-")));
                final String regionCode = Utils.ensureRegionIdPadding(record.get("REF_REGION"));
                final String magistrateCode = Utils.ensureMagistrateIdPadding(record.get("REF_MAGISTRATE"));
                final String magistrateServiceUnitCode = Utils.ensureMagistrateServiceUnitIdPadding(record.get("REF_MAGISTRATESERVICEUNIT"));
                final String healthCareDistrictCode = Utils.ensureHealthCareDistrictIdPadding(record.get("REF_HEALTHCAREDISTRICT"));
                final String electoralDistrictCode = Utils.ensureElectoralDistrictIdPadding(record.get("REF_ELECTORALDISTRICT"));
                final String businessServiceSubRegionCode = Utils.ensureBusinessServiceSubRegionIdPadding(record.get("REF_BUSINESSSERVICESUBREGION"));
                final String url = m_apiUtils.createResourceUrl(ApiConstants.API_PATH_MUNICIPALITIES, code);
                final Date timeStamp = new Date(System.currentTimeMillis());

                final Municipality municipality = createOrUpdateMunicipality(existingRegionsMap, existingMagistratesMap, existingMagistrateServiceUnitsMap, existingMunicipalitiesMap, existingHealthCareDistrictsMap, existingElectoralDistrictsMap, existingBusinessServiceSubRegionsMap, code, status, url, source, finnishName, swedishName, englishName, type, languages, regionCode, magistrateCode, magistrateServiceUnitCode, healthCareDistrictCode, electoralDistrictCode, businessServiceSubRegionCode, timeStamp);

                m_municipalities.add(municipality);
            });

        } catch (IOException e) {
            LOG.error("Parsing magistrateserviceunits failed. " + e.getMessage());
        }

        return m_municipalities;

    }


    /**
     * Parses the .csv Municipality-file and returns the municipalities as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The Municipality -file.
     * @return List of Municipality objects.
     */
    public List<Municipality> parseMunicipalitiesFromInputStream(final String source,
                                                                 final InputStream inputStream) {

        final List<Municipality> m_municipalities = new ArrayList<>();

        final Map<String, Region> existingRegionsMap = m_parserUtils.getRegionsMap();

        final Map<String, Magistrate> existingMagistratesMap = m_parserUtils.getMagistratesMap();

        final Map<String, Municipality> existingMunicipalitiesMap = m_parserUtils.getMunicipalitiesMap();

        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1)) {
            final BufferedReader in = new BufferedReader(inputStreamReader);
            FileUtils.skipBom(in);

            String line = null;
            boolean skipFirstLine = true;

            while ((line = in.readLine()) != null) {
                if (skipFirstLine) {
                    skipFirstLine = false;
                } else {
                    final String[] parts = line.split(";");
                    final String code = Utils.ensureMunicipalityIdPadding(parts[0]);

                    final String finnishName = parts[1];
                    final String swedishName = parts[2];
                    final String type = resolveType(parts[3]);
                    final Set<String> languages = resolveLanguages(parts[5]);
                    final String regionCode = Utils.ensureRegionIdPadding(parts[15]);
                    final String magistrateCode = Utils.ensureMagistrateIdPadding(parts[9]);

                    final String url = m_apiUtils.createResourceUrl(ApiConstants.API_PATH_MUNICIPALITIES, code);
                    final Date timeStamp = new Date(System.currentTimeMillis());

                    final Municipality municipality = createOrUpdateMunicipality(existingRegionsMap, existingMagistratesMap, null, existingMunicipalitiesMap, null, null, null, code, Status.VALID, url, source, finnishName, swedishName, null, type, languages, regionCode, magistrateCode, null, null, null, null, timeStamp);

                    m_municipalities.add(municipality);
                }
            }

        } catch (IOException e) {
            LOG.error("Parsing magistrateserviceunits failed. " + e.getMessage());
        }

        return m_municipalities;

    }


    private Municipality createOrUpdateMunicipality(final Map<String, Region> existingRegionsMap,
                                                    final Map<String, Magistrate> existingMagistratesMap,
                                                    final Map<String, MagistrateServiceUnit> existingMagistrateServiceUnitsMap,
                                                    final Map<String, Municipality> existingMunicipalitiesMap,
                                                    final Map<String, HealthCareDistrict> existingHealthCareDistrictsMap,
                                                    final Map<String, ElectoralDistrict> existingElectoralDistrictsMap,
                                                    final Map<String, BusinessServiceSubRegion> existingBusinessServiceSubRegionsMap,
                                                    final String code,
                                                    final Status status,
                                                    final String url,
                                                    final String source,
                                                    final String finnishName,
                                                    final String swedishName,
                                                    final String englishName,
                                                    final String type,
                                                    final Set<String> languages,
                                                    final String regionCode,
                                                    final String magistrateCode,
                                                    final String magistrateServiceUnitCode,
                                                    final String electoralDistrictCode,
                                                    final String healthCareDistrictCode,
                                                    final String businessServiceSubRegionCode,
                                                    final Date timeStamp) {

        Region region = null;
        if (regionCode != null) {
            region = existingRegionsMap.get(regionCode);
        }

        Magistrate magistrate = null;
        if (magistrateCode != null && existingMagistratesMap != null) {
            magistrate = existingMagistratesMap.get(magistrateCode);
        }

        MagistrateServiceUnit magistrateServiceUnit = null;
        if (magistrateServiceUnitCode != null && existingMagistrateServiceUnitsMap != null) {
            magistrateServiceUnit = existingMagistrateServiceUnitsMap.get(magistrateServiceUnitCode);
        }

        ElectoralDistrict electoralDistrict = null;
        if (electoralDistrictCode != null && existingElectoralDistrictsMap != null) {
            electoralDistrict = existingElectoralDistrictsMap.get(electoralDistrictCode);
        }

        HealthCareDistrict healthCareDistrict = null;
        if (healthCareDistrictCode != null && existingHealthCareDistrictsMap != null) {
            healthCareDistrict = existingHealthCareDistrictsMap.get(healthCareDistrictCode);
        }

        BusinessServiceSubRegion businessServiceSubRegion = null;
        if (businessServiceSubRegionCode != null && existingBusinessServiceSubRegionsMap != null) {
            businessServiceSubRegion = existingBusinessServiceSubRegionsMap.get(businessServiceSubRegionCode);
        }

        Municipality municipality = existingMunicipalitiesMap.get(code);

        // Update
        if (municipality != null) {
            boolean hasChanges = false;
            if (!Objects.equals(municipality.getStatus(), status.toString())) {
                municipality.setStatus(status.toString());
                hasChanges = true;
            }
            if (!Objects.equals(municipality.getUrl(), url)) {
                municipality.setUrl(url);
                hasChanges = true;
            }
            if (!Objects.equals(municipality.getSource(), source)) {
                municipality.setSource(source);
                hasChanges = true;
            }
            if (!Objects.equals(municipality.getNameFinnish(), finnishName)) {
                municipality.setNameFinnish(finnishName);
                hasChanges = true;
            }
            if (!Objects.equals(municipality.getNameSwedish(), swedishName)) {
                municipality.setNameSwedish(swedishName);
                hasChanges = true;
            }
            if (!Objects.equals(municipality.getNameEnglish(), englishName)) {
                municipality.setNameEnglish(englishName);
                hasChanges = true;
            }
            if (!Objects.equals(municipality.getType(), type)) {
                municipality.setType(type);
                hasChanges = true;
            }
            if (!municipality.getLanguages().equals(languages)) {
                municipality.setLanguages(languages);
                hasChanges = true;
            }
            if (existingRegionsMap != null && !Objects.equals(municipality.getRegion() != null ? municipality.getRegion().getCode() : null, region != null ? region.getCode() : null)) {
                municipality.setRegion(region);
                hasChanges = true;
            }
            if (existingMagistratesMap != null && !Objects.equals(municipality.getMagistrate() != null ? municipality.getMagistrate().getCode() : null, magistrate != null ? magistrate.getCode() : null)) {
                municipality.setMagistrate(magistrate);
                hasChanges = true;
            }
            if (existingMagistrateServiceUnitsMap != null && !Objects.equals(municipality.getMagistrateServiceUnit() != null ? municipality.getMagistrateServiceUnit().getCode() : null, magistrateServiceUnit != null ? magistrateServiceUnit.getCode() : null)) {
                municipality.setMagistrateServiceUnit(magistrateServiceUnit);
                hasChanges = true;
            }
            if (existingHealthCareDistrictsMap != null && !Objects.equals(municipality.getHealthCareDistrict() != null ? municipality.getHealthCareDistrict().getCode() : null, healthCareDistrict != null ? healthCareDistrict.getCode() : null)) {
                municipality.setHealthCareDistrict(healthCareDistrict);
                hasChanges = true;
            }
            if (existingElectoralDistrictsMap != null && !Objects.equals(municipality.getElectoralDistrict() != null ? municipality.getElectoralDistrict().getCode() : null, electoralDistrict != null ? electoralDistrict.getCode() : null)) {
                municipality.setElectoralDistrict(electoralDistrict);
                hasChanges = true;
            }
            if (existingBusinessServiceSubRegionsMap != null && !Objects.equals(municipality.getBusinessServiceSubRegion() != null ? municipality.getBusinessServiceSubRegion().getCode() : null, businessServiceSubRegion != null ? businessServiceSubRegion.getCode() : null)) {
                municipality.setBusinessServiceSubRegion(businessServiceSubRegion);
                hasChanges = true;
            }
            if (hasChanges) {
                municipality.setModified(timeStamp);
            }

        // Create
        } else {
            municipality = new Municipality();
            municipality.setId(UUID.randomUUID().toString());
            municipality.setStatus(status.toString());
            municipality.setUrl(url);
            municipality.setSource(source);
            municipality.setCreated(timeStamp);
            municipality.setCode(code);
            municipality.setNameFinnish(finnishName);
            municipality.setNameSwedish(swedishName);
            municipality.setNameEnglish(englishName);
            municipality.setType(type);
            municipality.setLanguages(languages);
            if (existingRegionsMap != null) {
                municipality.setRegion(region);
            }
            if (existingMagistrateServiceUnitsMap != null) {
                municipality.setMagistrate(magistrate);
            }
            if (existingMagistrateServiceUnitsMap != null) {
                municipality.setMagistrateServiceUnit(magistrateServiceUnit);
            }
            if (existingElectoralDistrictsMap != null) {
                municipality.setElectoralDistrict(electoralDistrict);
            }
            if (existingHealthCareDistrictsMap != null) {
                municipality.setHealthCareDistrict(healthCareDistrict);
            }
            if (existingBusinessServiceSubRegionsMap != null) {
                municipality.setBusinessServiceSubRegion(businessServiceSubRegion);
            }
        }

        return municipality;

    }


    private Set<String> resolveLanguages(final String languagesString) {

        final List<String> parsedLanguages = Arrays.asList(languagesString.split(" - "));
        final Set<String> languages = new HashSet<>();

        for (final String language : parsedLanguages) {
            if (language.equalsIgnoreCase("suomi")) {
                languages.add(LANGUAGE_FI);
            } else if (language.equalsIgnoreCase("ruotsi")) {
                languages.add(LANGUAGE_SE);
            }
        }

        return languages;

    }


    private String resolveType(final String type) {

        if (type.equalsIgnoreCase("kaupunki")) {
            return TYPE_CITY;
        } else if (type.equalsIgnoreCase("kunta")) {
            return TYPE_MUNICIPALITY;
        }

        LOG.error("Unknown type in municipality parser: " + type);
        return type;

    }

}
