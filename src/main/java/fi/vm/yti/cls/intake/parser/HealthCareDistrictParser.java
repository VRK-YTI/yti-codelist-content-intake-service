package fi.vm.yti.cls.intake.parser;

import fi.vm.yti.cls.common.model.HealthCareDistrict;
import fi.vm.yti.cls.common.model.Municipality;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.intake.api.ApiConstants;
import fi.vm.yti.cls.intake.api.ApiUtils;
import fi.vm.yti.cls.intake.util.FileUtils;
import fi.vm.yti.cls.intake.util.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Class that handles parsing of healthcaredistricts from source data.
 */
@Service
public class HealthCareDistrictParser {

    private static final Logger LOG = LoggerFactory.getLogger(HealthCareDistrictParser.class);
    private final ApiUtils apiUtils;
    private final ParserUtils parserUtils;

    @Inject
    public HealthCareDistrictParser(final ApiUtils apiUtils,
                                    final ParserUtils parserUtils) {
        this.apiUtils = apiUtils;
        this.parserUtils = parserUtils;
    }

    /**
     * Parses the .csv data file and returns the HealthCareDistricts as an arrayList.
     *
     * @param source      source identifier for the data.
     * @param inputStream The HealthCareDistrict -file.
     * @return List of HealthCareDistrict objects.
     */
    public List<HealthCareDistrict> parseHealthCareDistrictsFromClsInputStream(final String source,
                                                                               final InputStream inputStream) {
        final Map<String, HealthCareDistrict> existingHealthCareDistrictMap = parserUtils.getHealthCareDistrictsMap();
        final Map<String, Municipality> existingMunicipalitiesMap = parserUtils.getMunicipalitiesMap();
        final Map<String, HealthCareDistrict> healthCareDistrictMap = new HashMap<>();

        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withHeader())) {
            FileUtils.skipBom(in);
            final List<CSVRecord> records = csvParser.getRecords();
            records.forEach(record -> {
                final String code = Utils.ensureHealthCareDistrictIdPadding(record.get("CODEVALUE"));
                if (code != null && !code.isEmpty()) {
                    HealthCareDistrict healthCareDistrict = null;

                    if (!healthCareDistrictMap.containsKey(code)) {

                        final String finnishName = record.get("PREFLABEL_FI");
                        final String swedishName = record.get("PREFLABEL_SE");
                        final String englishName = record.get("PREFLABEL_EN");
                        final Status status = Status.valueOf(record.get("STATUS"));
                        final String abbr = record.get("ABBR");
                        final String specialAreaOfResponsibilityCode = record.get("SPECIAL_AREA_CODE");

                        healthCareDistrict = createOrUpdateHealthCareDistrict(existingHealthCareDistrictMap, code, status, source, finnishName, swedishName, englishName, abbr, specialAreaOfResponsibilityCode);

                        healthCareDistrictMap.put(code, healthCareDistrict);
                    } else {
                        healthCareDistrict = healthCareDistrictMap.get(code);
                    }

                    final String municipalityCode = Utils.ensureMunicipalityIdPadding(record.get("REF_MUNICIPALITY"));
                    final Municipality municipality = existingMunicipalitiesMap.get(municipalityCode);

                    List<Municipality> municipalities = healthCareDistrict.getMunicipalities();

                    if (municipality != null) {
                        if (municipalities != null) {
                            boolean skip = false;
                            for (final Municipality munic : municipalities) {
                                if (municipality.getCodeValue().equalsIgnoreCase(munic.getCodeValue())) {
                                    skip = true;
                                }
                            }
                            if (!skip) {
                                municipalities.add(municipality);
                            }
                        } else {
                            municipalities = new ArrayList<>();
                            municipalities.add(municipality);
                            healthCareDistrict.setMunicipalities(municipalities);
                        }
                    } else {
                        LOG.error("HealthCareDistrictparser municipality not found for code: " + municipalityCode);
                    }
                }
            });
        } catch (IOException e) {
            LOG.error("Parsing healthcaredistricts failed. " + e.getMessage());
        }

        final List<HealthCareDistrict> healthCareDistricts = new ArrayList<HealthCareDistrict>(healthCareDistrictMap.values());
        return healthCareDistricts;
    }

    /**
     * Parses the .txt file that contains health care districts names and returns the modified list of HealthCareDistrics as an arrayList.
     *
     * @param source      Source identifier for the data.
     * @param inputStream The HealthCareDistrict -file.
     * @return List of HealthCareDistrict objects.
     */
    public List<HealthCareDistrict> parseHealthCareDistrictNamesFromInputStream(final String source,
                                                                                final InputStream inputStream) {
        final List<HealthCareDistrict> updatedHealthCareDistricts = new ArrayList<>();
        final Map<String, HealthCareDistrict> existingHealthCareDistrictMap = parserUtils.getHealthCareDistrictsMap();

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

                    String code = Utils.ensureHealthCareDistrictIdPadding(parts[0]);

                    // Reformatting handling for earlier code '22' to more recent value '00' for Åland.
                    if (code.equals("22")) {
                        code = "00";
                    }

                    final HealthCareDistrict healthCareDistrict = existingHealthCareDistrictMap.get(code);

                    if (healthCareDistrict != null) {
                        boolean hasChanges = false;

                        final String abbr = parts[1];
                        final String finnishName = parts[2];
                        final String swedishName = parts[12];

                        if (!Objects.equals(healthCareDistrict.getPrefLabelFi(), finnishName)) {
                            healthCareDistrict.setPrefLabelFi(finnishName);
                            hasChanges = true;
                        }
                        if (!Objects.equals(healthCareDistrict.getPrefLabelSe(), swedishName)) {
                            healthCareDistrict.setPrefLabelSe(swedishName);
                            hasChanges = true;
                        }
                        if (!Objects.equals(healthCareDistrict.getAbbreviation(), abbr)) {
                            healthCareDistrict.setAbbreviation(abbr);
                            hasChanges = true;
                        }
                        if (hasChanges) {
                            updatedHealthCareDistricts.add(healthCareDistrict);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Parsing health care district names failed: " + e.getMessage());
        }

        return updatedHealthCareDistricts;

    }

    /**
     * Parses the .xls Excel-file and returns the HealthCareDistricts as an arrayList.
     *
     * @param source      Source identifier for the data.
     * @param inputStream The HealthCareDistrict -file.
     * @return List of HealthCareDistrict objects.
     */
    public List<HealthCareDistrict> parseHealthCareDistrictsFromExcelInputStream(final String source,
                                                                                 final InputStream inputStream) {
        final Map<String, HealthCareDistrict> healthCareDistrictMap = new HashMap<>();
        final Workbook workbook;

        try {
            workbook = new HSSFWorkbook(inputStream);
            final Sheet memberMunicipalities = workbook.getSheet("shp_jäsenkunnat_2017_lkm");
            final Map<String, HealthCareDistrict> existingHealthCareDistrictMap = parserUtils.getHealthCareDistrictsMap();
            final Map<String, Municipality> existingMunicipalitiesMap = parserUtils.getMunicipalitiesMap();

            for (int i = 5; i < 316; i++) {
                final Row row = memberMunicipalities.getRow(i);

                final Cell codeCell = row.getCell(2);
                codeCell.setCellType(CellType.STRING);
                final String code = Utils.ensureHealthCareDistrictIdPadding(codeCell.getStringCellValue());

                if (code != null && !code.isEmpty()) {

                    HealthCareDistrict healthCareDistrict = null;

                    if (!healthCareDistrictMap.containsKey(code)) {

                        final String specialAreaOfResponsibilityCode = row.getCell(4).getStringCellValue();
                        final String finnishName = row.getCell(3).getStringCellValue();

                        healthCareDistrict = createOrUpdateHealthCareDistrict(existingHealthCareDistrictMap, code, Status.VALID, source, finnishName, null, null, null, specialAreaOfResponsibilityCode);

                        healthCareDistrictMap.put(code, healthCareDistrict);
                    } else {
                        healthCareDistrict = healthCareDistrictMap.get(code);
                    }

                    final Cell municipalityCodeCell = row.getCell(0);
                    municipalityCodeCell.setCellType(CellType.STRING);
                    final String municipalityCode = Utils.ensureMunicipalityIdPadding(municipalityCodeCell.getStringCellValue());
                    final Municipality municipality = existingMunicipalitiesMap.get(municipalityCode);

                    List<Municipality> municipalities = healthCareDistrict.getMunicipalities();

                    if (municipality != null) {
                        if (municipalities != null) {
                            boolean skip = false;
                            for (final Municipality munic : municipalities) {
                                if (municipality.getCodeValue().equalsIgnoreCase(munic.getCodeValue())) {
                                    skip = true;
                                }
                            }
                            if (!skip) {
                                municipalities.add(municipality);
                            }
                        } else {
                            municipalities = new ArrayList<>();
                            municipalities.add(municipality);
                            healthCareDistrict.setMunicipalities(municipalities);
                        }
                    } else {
                        LOG.error("HealthCareDistrictparser municipality not found for code: " + municipalityCode);
                    }

                }

            }
        } catch (IOException e) {
            LOG.error("Parsing healthcaredistricts failed. " + e.getMessage());
        }

        final List<HealthCareDistrict> healthCareDistricts = new ArrayList<HealthCareDistrict>(healthCareDistrictMap.values());
        return healthCareDistricts;
    }

    private HealthCareDistrict createOrUpdateHealthCareDistrict(final Map<String, HealthCareDistrict> existingHealthCareDistrictMap,
                                                                final String code,
                                                                final Status status,
                                                                final String source,
                                                                final String finnishName,
                                                                final String swedishName,
                                                                final String englishName,
                                                                final String abbr,
                                                                final String specialAreaOfResponsibilityCode) {
        HealthCareDistrict healthCareDistrict = existingHealthCareDistrictMap.get(code);
        final String url = apiUtils.createResourceUrl(ApiConstants.API_PATH_HEALTHCAREDISTRICTS, code);
        final Date timeStamp = new Date(System.currentTimeMillis());

        // Update
        if (healthCareDistrict != null) {
            boolean hasChanges = false;
            if (!Objects.equals(healthCareDistrict.getStatus(), status.toString())) {
                healthCareDistrict.setStatus(status.toString());
                hasChanges = true;
            }
            if (!Objects.equals(healthCareDistrict.getUri(), url)) {
                healthCareDistrict.setUri(url);
                hasChanges = true;
            }
            if (!Objects.equals(healthCareDistrict.getSource(), source)) {
                healthCareDistrict.setSource(source);
                hasChanges = true;
            }
            if (!Objects.equals(healthCareDistrict.getPrefLabelFi(), finnishName)) {
                healthCareDistrict.setPrefLabelFi(finnishName);
                hasChanges = true;
            }
            if (!Objects.equals(healthCareDistrict.getPrefLabelSe(), swedishName)) {
                healthCareDistrict.setPrefLabelSe(swedishName);
                hasChanges = true;
            }
            if (!Objects.equals(healthCareDistrict.getPrefLabelEn(), englishName)) {
                healthCareDistrict.setPrefLabelEn(englishName);
                hasChanges = true;
            }
            if (!Objects.equals(healthCareDistrict.getAbbreviation(), abbr)) {
                healthCareDistrict.setAbbreviation(abbr);
                hasChanges = true;
            }
            if (!Objects.equals(healthCareDistrict.getSpecialAreaOfResponsibility(), specialAreaOfResponsibilityCode)) {
                healthCareDistrict.setSpecialAreaOfResponsibility(specialAreaOfResponsibilityCode);
                hasChanges = true;
            }
            if (hasChanges) {
                healthCareDistrict.setModified(timeStamp);
            }
        // Create
        } else {
            healthCareDistrict = new HealthCareDistrict();
            healthCareDistrict.setId(UUID.randomUUID().toString());
            healthCareDistrict.setUri(url);
            healthCareDistrict.setStatus(status.toString());
            healthCareDistrict.setSource(source);
            healthCareDistrict.setModified(timeStamp);
            healthCareDistrict.setCodeValue(code);
            healthCareDistrict.setPrefLabelFi(finnishName);
            healthCareDistrict.setPrefLabelSe(swedishName);
            healthCareDistrict.setPrefLabelEn(englishName);
            healthCareDistrict.setAbbreviation(abbr);
            if (specialAreaOfResponsibilityCode != null) {
                healthCareDistrict.setSpecialAreaOfResponsibility(specialAreaOfResponsibilityCode);
            }
        }
        return healthCareDistrict;
    }

}
