package fi.vm.yti.cls.intake.parser;

import fi.vm.yti.cls.common.model.MagistrateServiceUnit;
import fi.vm.yti.cls.common.model.Municipality;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Class that handles parsing of magistrateserviceunits from source data.
 */
@Service
public class MagistrateServiceUnitParser {

    private static final Logger LOG = LoggerFactory.getLogger(MagistrateServiceUnitParser.class);
    private final ApiUtils apiUtils;
    private final ParserUtils parserUtils;

    @Inject
    public MagistrateServiceUnitParser(final ApiUtils apiUtils,
                                       final ParserUtils parserUtils) {
        this.apiUtils = apiUtils;
        this.parserUtils = parserUtils;
    }

    /**
     * Parses the CLS-spec .csv data-file and returns the MagistrateServiceUnits as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The MagistrateServiceUnit -file.
     * @return List of MagistrateServiceUnit objects.
     */
    public List<MagistrateServiceUnit> parseMagistrateServiceUnitsFromClsInputStream(final String source,
                                                                                     final InputStream inputStream) {
        final Map<String, MagistrateServiceUnit> magistrateServiceUnitMap = new HashMap<>();
        final Map<String, MagistrateServiceUnit> existingMagistrateServiceUnitsMap = parserUtils.getMagistrateServiceUnitsMap();
        final Map<String, Municipality> existingMunicipalitiesMap = parserUtils.getMunicipalitiesMap();

        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                final BufferedReader in = new BufferedReader(inputStreamReader);
                final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withHeader())) {

               FileUtils.skipBom(in);

            final List<CSVRecord> records = csvParser.getRecords();

            records.forEach(record -> {
                final String code = Utils.ensureMagistrateServiceUnitIdPadding(record.get("CODEVALUE"));
                MagistrateServiceUnit magistrateServiceUnit = null;
                if (!magistrateServiceUnitMap.containsKey(code)) {
                    final String finnishName = record.get("PREFLABEL_FI");
                    final String swedishName = record.get("PREFLABEL_SE");
                    final String englishName = record.get("PREFLABEL_EN");
                    final Status status = Status.valueOf(record.get("STATUS"));
                    magistrateServiceUnit = createOrUpdateMagistrateServiceUnit(existingMagistrateServiceUnitsMap, code, status, source, finnishName, swedishName, englishName);
                    magistrateServiceUnitMap.put(code, magistrateServiceUnit);
                } else {
                    magistrateServiceUnit = magistrateServiceUnitMap.get(code);
                }

                final String municipalityCode = Utils.ensureMunicipalityIdPadding(record.get("REF_MUNICIPALITY"));
                final Municipality municipality = existingMunicipalitiesMap.get(municipalityCode);
                List<Municipality> municipalities = magistrateServiceUnit.getMunicipalities();

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
                        magistrateServiceUnit.setMunicipalities(municipalities);
                    }
                } else {
                    LOG.error("MagistrateServiceUnitParser municipality not found for code: " + municipalityCode);
                }
            });
        } catch (IOException e) {
            LOG.error("Parsing magistrateserviceunits failed. " + e.getMessage());
        }

        final List<MagistrateServiceUnit> magistrateServiceUnits = new ArrayList<MagistrateServiceUnit>(magistrateServiceUnitMap.values());
        return magistrateServiceUnits;
    }

    /**
     * Parses the .xls Excel-file and returns the MagistrateServiceUnits as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The MagistrateServiceUnit -file.
     * @return List of MagistrateServiceUnit objects.
     */
    public List<MagistrateServiceUnit> parseMagistrateServiceUnitsFromInputStream(final String source,
                                                                                  final InputStream inputStream) {
        final Map<String, MagistrateServiceUnit> magistrateServiceUnitMap = new HashMap<>();
        final Map<String, MagistrateServiceUnit> existingMagistrateServiceUnitsMap = parserUtils.getMagistrateServiceUnitsMap();
        final Map<String, Municipality> existingMunicipalitiesMap = parserUtils.getMunicipalitiesMap();

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
                    final String code = Utils.ensureMagistrateServiceUnitIdPadding(parts[12]);

                    // Skip 0 code data
                    if ("0".equalsIgnoreCase(code)) {
                        continue;
                    }

                    MagistrateServiceUnit magistrateServiceUnit = null;

                    if (!magistrateServiceUnitMap.containsKey(code)) {
                        final String finnishName = parts[13];
                        final String swedishName = parts[14];
                        magistrateServiceUnit = createOrUpdateMagistrateServiceUnit(existingMagistrateServiceUnitsMap, code, Status.VALID, source, finnishName, swedishName, null);
                        magistrateServiceUnitMap.put(code, magistrateServiceUnit);
                    } else {
                        magistrateServiceUnit = magistrateServiceUnitMap.get(code);
                    }

                    final String municipalityCode = Utils.ensureMunicipalityIdPadding(parts[0]);
                    final Municipality municipality = existingMunicipalitiesMap.get(municipalityCode);

                    List<Municipality> municipalities = magistrateServiceUnit.getMunicipalities();

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
                            magistrateServiceUnit.setMunicipalities(municipalities);
                        }
                    } else {
                        LOG.error("MagistrateServiceUnitParser municipality not found for code: " + municipalityCode);
                    }

                }
            }
        } catch (IOException e) {
            LOG.error("Parsing magistrateserviceunits failed. " + e.getMessage());
        }

        final List<MagistrateServiceUnit> magistrateServiceUnits = new ArrayList<MagistrateServiceUnit>(magistrateServiceUnitMap.values());
        return magistrateServiceUnits;
    }

    private MagistrateServiceUnit createOrUpdateMagistrateServiceUnit(final Map<String, MagistrateServiceUnit> magistrateServiceUnitsMap,
                                                                      final String code,
                                                                      final Status status,
                                                                      final String source,
                                                                      final String finnishName,
                                                                      final String swedishName,
                                                                      final String englishName) {
        final String url = apiUtils.createResourceUrl(ApiConstants.API_PATH_MAGISTRATESERVICEUNITS, code);
        final Date timeStamp = new Date(System.currentTimeMillis());

        MagistrateServiceUnit magistrateServiceUnit = magistrateServiceUnitsMap.get(code);

        // Update
        if (magistrateServiceUnit != null) {
            boolean hasChanges = false;
            if (!Objects.equals(magistrateServiceUnit.getStatus(), status.toString())) {
                magistrateServiceUnit.setStatus(status.toString());
                hasChanges = true;
            }
            if (!Objects.equals(magistrateServiceUnit.getUri(), url)) {
                magistrateServiceUnit.setUri(url);
                hasChanges = true;
            }
            if (!Objects.equals(magistrateServiceUnit.getSource(), source)) {
                magistrateServiceUnit.setSource(source);
                hasChanges = true;
            }
            if (!Objects.equals(magistrateServiceUnit.getPrefLabelFi(), finnishName)) {
                magistrateServiceUnit.setPrefLabelFi(finnishName);
                hasChanges = true;
            }
            if (!Objects.equals(magistrateServiceUnit.getPrefLabelSe(), swedishName)) {
                magistrateServiceUnit.setPrefLabelSe(swedishName);
                hasChanges = true;
            }
            if (!Objects.equals(magistrateServiceUnit.getPrefLabelEn(), englishName)) {
                magistrateServiceUnit.setPrefLabelEn(englishName);
                hasChanges = true;
            }
            if (hasChanges) {
                magistrateServiceUnit.setModified(timeStamp);
            }
        // Create
        } else {
            magistrateServiceUnit = new MagistrateServiceUnit();
            magistrateServiceUnit.setId(UUID.randomUUID().toString());
            magistrateServiceUnit.setStatus(status.toString());
            magistrateServiceUnit.setUri(url);
            magistrateServiceUnit.setSource(source);
            magistrateServiceUnit.setModified(timeStamp);
            magistrateServiceUnit.setCodeValue(code);
            magistrateServiceUnit.setPrefLabelFi(finnishName);
            magistrateServiceUnit.setPrefLabelSe(swedishName);
            magistrateServiceUnit.setPrefLabelEn(englishName);
        }

        return magistrateServiceUnit;
    }

}
