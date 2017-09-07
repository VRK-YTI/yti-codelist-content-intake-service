package fi.vm.yti.cls.intake.parser;

import fi.vm.yti.cls.common.model.ElectoralDistrict;
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
 * Class that handles parsing of electoraldistricts from source data.
 */
@Service
public class ElectoralDistrictParser {

    private final ApiUtils apiUtils;
    private final ParserUtils parserUtils;
    private static final Logger LOG = LoggerFactory.getLogger(ElectoralDistrictParser.class);

    @Inject
    public ElectoralDistrictParser(final ApiUtils apiUtils,
                                   final ParserUtils parserUtils) {
        this.apiUtils = apiUtils;
        this.parserUtils = parserUtils;
    }

    /**
     * Parses the .csv data file and returns the ElectoralDistricts as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The ElectoralDistrict -file.
     * @return List of ElectoralDistrict objects.
     */
    public List<ElectoralDistrict> parseElectoralDistrictsFromClsInputStream(final String source,
                                                                             final InputStream inputStream) {

        final Map<String, ElectoralDistrict> electoralDistrictsMap = new HashMap<>();
        final Map<String, ElectoralDistrict> existingElectoralDistrictsMap = parserUtils.getElectoralDistrictsMap();
        final Map<String, Municipality> existingMunicipalitiesMap = parserUtils.getMunicipalitiesMap();

        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withHeader())) {
            FileUtils.skipBom(in);
            final List<CSVRecord> records = csvParser.getRecords();

            for (final CSVRecord record : records) {
                final String code = Utils.ensureElectoralDistrictIdPadding(record.get("CODEVALUE"));
                ElectoralDistrict electoralDistrict = null;
                if (!electoralDistrictsMap.containsKey(code)) {
                    final String finnishName = record.get("PREFLABEL_FI");
                    final String swedishName = record.get("PREFLABEL_SE");
                    final String englishName = record.get("PREFLABEL_EN");
                    final Status status = Status.valueOf(record.get("STATUS"));
                    electoralDistrict = createOrUpdateElectoralDistrict(existingElectoralDistrictsMap, code, status, source, finnishName, swedishName, englishName);
                    electoralDistrictsMap.put(code, electoralDistrict);
                } else {
                    electoralDistrict = electoralDistrictsMap.get(code);
                }

                final String municipalityCode = Utils.ensureMunicipalityIdPadding(record.get("REF_MUNICIPALITY"));
                final Municipality municipality = existingMunicipalitiesMap.get(municipalityCode);

                List<Municipality> municipalities = electoralDistrict.getMunicipalities();

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
                        electoralDistrict.setMunicipalities(municipalities);
                    }
                } else {
                    LOG.error("ElectoralDistrictParser municipality not found for code: " + municipalityCode);
                }
            }
        } catch (IOException e) {
            LOG.error("Parsing electoraldistricts failed. " + e.getMessage());
        }
        final List<ElectoralDistrict> electoralDistricts = new ArrayList<ElectoralDistrict>(electoralDistrictsMap.values());
        return electoralDistricts;
    }

    /**
     * Parses the .xls Excel-file and returns the ElectoralDistricts as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The ElectoralDistrict -file.
     * @return List of ElectoralDistrict objects.
     */
    public List<ElectoralDistrict> parseElectoralDistrictsFromInputStream(final String source,
                                                                          final InputStream inputStream) {
        final Map<String, ElectoralDistrict> electoralDistrictsMap = new HashMap<>();
        final Map<String, ElectoralDistrict> existingElectoralDistrictsMap = parserUtils.getElectoralDistrictsMap();
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
                    final String code = Utils.ensureElectoralDistrictIdPadding(parts[6]);

                    ElectoralDistrict electoralDistrict = null;

                    if (!electoralDistrictsMap.containsKey(code)) {
                        final String finnishName = parts[7];
                        final String swedishName = parts[8];
                        electoralDistrict = createOrUpdateElectoralDistrict(existingElectoralDistrictsMap, code, Status.VALID, source, finnishName, swedishName, null);
                        electoralDistrictsMap.put(code, electoralDistrict);
                    } else {
                        electoralDistrict = electoralDistrictsMap.get(code);
                    }

                    final String municipalityCode = Utils.ensureMunicipalityIdPadding(parts[0]);
                    final Municipality municipality = existingMunicipalitiesMap.get(municipalityCode);

                    List<Municipality> municipalities = electoralDistrict.getMunicipalities();

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
                            electoralDistrict.setMunicipalities(municipalities);
                        }
                    } else {
                        LOG.error("ElectoralDistrictParser municipality not found for code: " + municipalityCode);
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Parsing electoraldistricts failed. " + e.getMessage());
        }

        final List<ElectoralDistrict> electoralDistricts = new ArrayList<ElectoralDistrict>(electoralDistrictsMap.values());
        return electoralDistricts;
    }

    private ElectoralDistrict createOrUpdateElectoralDistrict(final Map<String, ElectoralDistrict> electoralDistrictsMap,
                                                              final String code,
                                                              final Status status,
                                                              final String source,
                                                              final String finnishName,
                                                              final String swedishName,
                                                              final String englishName) {
        final String url = apiUtils.createResourceUrl(ApiConstants.API_PATH_ELECTORALDISTRICTS, code);
        final Date timeStamp = new Date(System.currentTimeMillis());

        ElectoralDistrict electoralDistrict = electoralDistrictsMap.get(code);

        // Update
        if (electoralDistrict != null) {
            boolean hasChanges = false;
            if (!Objects.equals(electoralDistrict.getStatus(), status.toString())) {
                electoralDistrict.setStatus(status.toString());
                hasChanges = true;
            }
            if (!Objects.equals(electoralDistrict.getUri(), url)) {
                electoralDistrict.setUri(url);
                hasChanges = true;
            }
            if (!Objects.equals(electoralDistrict.getSource(), source)) {
                electoralDistrict.setSource(source);
                hasChanges = true;
            }
            if (!Objects.equals(electoralDistrict.getPrefLabelFi(), finnishName)) {
                electoralDistrict.setPrefLabelFi(finnishName);
                hasChanges = true;
            }
            if (!Objects.equals(electoralDistrict.getPrefLabelSe(), swedishName)) {
                electoralDistrict.setPrefLabelSe(swedishName);
                hasChanges = true;
            }
            if (!Objects.equals(electoralDistrict.getPrefLabelEn(), englishName)) {
                electoralDistrict.setPrefLabelEn(englishName);
                hasChanges = true;
            }
            if (hasChanges) {
                electoralDistrict.setModified(timeStamp);
            }
        // Create
        } else {
            electoralDistrict = new ElectoralDistrict();
            electoralDistrict.setId(UUID.randomUUID().toString());
            electoralDistrict.setUri(url);
            electoralDistrict.setSource(source);
            electoralDistrict.setStatus(status.toString());
            electoralDistrict.setCreated(timeStamp);
            electoralDistrict.setCodeValue(code);
            electoralDistrict.setPrefLabelFi(finnishName);
            electoralDistrict.setPrefLabelSe(swedishName);
            electoralDistrict.setPrefLabelEn(englishName);
        }

        return electoralDistrict;
    }

}
