package fi.vm.yti.cls.intake.parser;

import fi.vm.yti.cls.common.model.Municipality;
import fi.vm.yti.cls.common.model.PostOfficeType;
import fi.vm.yti.cls.common.model.PostalCode;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Class that handles parsing of postal codes from source data.
 */
@Service
public class PostalCodeParser {

    private static final Logger LOG = LoggerFactory.getLogger(PostalCodeParser.class);
    private final ApiUtils apiUtils;
    private final ParserUtils parserUtils;

    @Inject
    public PostalCodeParser(final ApiUtils apiUtils,
                            final ParserUtils parserUtils) {
        this.apiUtils = apiUtils;
        this.parserUtils = parserUtils;
    }

    /**
     * Parses the .csv PostalCode-file and returns PostalCodes as an arrayList.
     *
     * @param source The source idenfitifer for the data.
     * @param inputStream The PostalCode file input stream.
     * @return List of PostalCode objects.
     */
    public List<PostalCode> parsePostalCodesFromClsInputStream(final String source,
                                                               final InputStream inputStream) {
        final List<PostalCode> postalCodes = new ArrayList<>();
        final Map<String, PostalCode> existingPostalCodesMap = parserUtils.getPostalCodesMap();
        final Map<String, Municipality> existingMunicipalitiesMap = parserUtils.getMunicipalitiesMap();

        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                final BufferedReader in = new BufferedReader(inputStreamReader);
                final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withHeader())) {
            FileUtils.skipBom(in);
            final List<CSVRecord> records = csvParser.getRecords();
            records.forEach(record -> {
                final String code = Utils.ensurePostalCodeIdPadding(record.get("CODEVALUE"));
                final String finnishName = record.get("PREFLABEL_FI");
                final String swedishName = record.get("PREFLABEL_SE");
                final String englishName = record.get("PREFLABEL_EN");
                final Status status = Status.valueOf(record.get("STATUS"));
                final String finnishAbbr = record.get("ABBR_FI");
                final String swedishAbbr = record.get("ABBR_SE");
                final String englishAbbr = record.get("ABBR_EN");
                final Integer typeCode = Integer.parseInt(record.get("TYPE"));
                final String type = PostOfficeType.valueOf(typeCode - 1).getName();
                final String municipalityCode = Utils.ensureMunicipalityIdPadding(record.get("REF_MUNICIPALITY"));
                final PostalCode postalCode = createOrUpdatePostalCode(existingPostalCodesMap, existingMunicipalitiesMap, code, status, source, finnishName, swedishName, englishName, finnishAbbr, swedishAbbr, englishAbbr, type, municipalityCode);
                postalCodes.add(postalCode);
            });
        } catch (IOException e) {
            LOG.error("Parsing postalcodes failed: " + e.getMessage());
        }

        return postalCodes;
    }

    /**
     * Parses the .DAT PostalCode-file and returns the PostalCodes as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The PostalCode file input stream.
     * @return List of PostalCode objects.
     */
    public List<PostalCode> parsePostalCodesFromInputStream(final String source,
                                                            final InputStream inputStream) {
        final List<PostalCode> postalCodes = new ArrayList<>();
        final Map<String, PostalCode> existingPostalCodesMap = parserUtils.getPostalCodesMap();
        final Map<String, Municipality> existingMunicipalitiesMap = parserUtils.getMunicipalitiesMap();

        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1)) {
            final BufferedReader in = new BufferedReader(inputStreamReader);
            FileUtils.skipBom(in);
            String line = null;
            while ((line = in.readLine()) != null) {
                final String code = Utils.ensurePostalCodeIdPadding(line.substring(13, 18).trim());
                final String finnishName = line.substring(18, 48).trim();
                final String swedishName = line.substring(48, 78).trim();
                final String finnishAbbr = line.substring(78, 90).trim();
                final String swedishAbbr = line.substring(90, 102).trim();
                final Integer typeCode = Integer.parseInt(line.substring(110, 111).trim());
                final String type = PostOfficeType.valueOf(typeCode - 1).getName();
                final String municipalityCode = Utils.ensureMunicipalityIdPadding(line.substring(176, 179).trim());
                final PostalCode postalCode = createOrUpdatePostalCode(existingPostalCodesMap, existingMunicipalitiesMap, code, Status.VALID, source, finnishName, swedishName, null, finnishAbbr, swedishAbbr, null, type, municipalityCode);
                postalCodes.add(postalCode);
            }
        } catch (IOException e) {
            LOG.error("Parsing postalcodes failed: " + e.getMessage());
        }
        return postalCodes;
    }

    private PostalCode createOrUpdatePostalCode(final Map<String, PostalCode> existingPostalCodesMap,
                                                final Map<String, Municipality> existingMunicipalitiesMap,
                                                final String code,
                                                final Status status,
                                                final String source,
                                                final String finnishName,
                                                final String swedishName,
                                                final String englishName,
                                                final String finnishAbbr,
                                                final String swedishAbbr,
                                                final String englishAbbr,
                                                final String type,
                                                final String municipalityCode) {
        final Date timeStamp = new Date(System.currentTimeMillis());
        final String url = apiUtils.createResourceUrl(ApiConstants.API_PATH_POSTALCODES, code);
        Municipality municipality = null;
        if (municipalityCode != null && !municipalityCode.isEmpty()) {
            municipality = existingMunicipalitiesMap.get(municipalityCode);
        }
        PostalCode postalCode = existingPostalCodesMap.get(code);

        // Update
        if (postalCode != null) {

            boolean hasChanges = false;

            if (!Objects.equals(postalCode.getStatus(), status.toString())) {
                postalCode.setStatus(status.toString());
                hasChanges = true;
            }
            if (!Objects.equals(postalCode.getUri(), url)) {
                postalCode.setUri(url);
                hasChanges = true;
            }
            if (!Objects.equals(postalCode.getSource(), source)) {
                postalCode.setSource(source);
                hasChanges = true;
            }
            if (!Objects.equals(postalCode.getPrefLabelFi(), finnishName)) {
                postalCode.setPrefLabelFi(finnishName);
                hasChanges = true;
            }
            if (!Objects.equals(postalCode.getPrefLabelSe(), swedishName)) {
                postalCode.setPrefLabelSe(swedishName);
                hasChanges = true;
            }
            if (!Objects.equals(postalCode.getPrefLabelEn(), englishName)) {
                postalCode.setPrefLabelEn(englishName);
                hasChanges = true;
            }
            if (!Objects.equals(postalCode.getNameAbbrFinnish(), finnishAbbr)) {
                postalCode.setNameAbbrFinnish(finnishAbbr);
                hasChanges = true;
            }
            if (!Objects.equals(postalCode.getNameAbbrSwedish(), swedishAbbr)) {
                postalCode.setNameAbbrSwedish(swedishAbbr);
                hasChanges = true;
            }
            if (!Objects.equals(postalCode.getNameAbbrEnglish(), englishAbbr)) {
                postalCode.setNameAbbrEnglish(englishAbbr);
                hasChanges = true;
            }
            if (!Objects.equals(postalCode.getTypeName(), type)) {
                postalCode.setTypeName(type);
                hasChanges = true;
            }
            if (postalCode.getMunicipality() != municipality) {
                postalCode.setMunicipality(municipality);
                hasChanges = true;
            }
            if (hasChanges) {
                postalCode.setModified(timeStamp);
            }
        // Create
        } else {
            postalCode = new PostalCode();
            postalCode.setId(UUID.randomUUID().toString());
            postalCode.setStatus(status.toString());
            postalCode.setUri(url);
            postalCode.setSource(source);
            postalCode.setCreated(timeStamp);
            postalCode.setCodeValue(code);
            postalCode.setPrefLabelFi(finnishName);
            postalCode.setPrefLabelSe(swedishName);
            postalCode.setPrefLabelEn(englishName);
            postalCode.setNameAbbrFinnish(finnishAbbr);
            postalCode.setNameAbbrSwedish(swedishAbbr);
            postalCode.setNameAbbrEnglish(englishAbbr);
            postalCode.setTypeName(type);
            postalCode.setMunicipality(municipality);
        }
        return postalCode;
    }

}
