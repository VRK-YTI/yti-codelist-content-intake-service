package fi.vm.yti.cls.intake.parser;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import fi.vm.yti.cls.common.model.Code;
import fi.vm.yti.cls.common.model.CodeScheme;
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;


/**
 * Class that handles parsing of codes from source data.
 */
@Service
public class CodeParser {

    private static final Logger LOG = LoggerFactory.getLogger(CodeParser.class);
    private final ApiUtils apiUtils;
    private final ParserUtils parserUtils;

    @Inject
    public CodeParser(final ApiUtils apiUtils,
                      final ParserUtils parserUtils) {
        this.apiUtils = apiUtils;
        this.parserUtils = parserUtils;
    }

    /**
     * Parses the .csv Code-file and returns the codes as an arrayList.
     *
     * @param codeScheme CodeScheme codeValue identifier.
     * @param source Source identifier for the data.
     * @param inputStream The Code -file.
     * @return List of Code objects.
     */
    public List<Code> parseCodesFromClsInputStream(final CodeScheme codeScheme,
                                                   final String source,
                                                   final InputStream inputStream) {
        final List<Code> codes = new ArrayList<>();
        if (codeScheme != null) {
            try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 final BufferedReader in = new BufferedReader(inputStreamReader);
                 final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withHeader())) {
                FileUtils.skipBom(in);
                final List<CSVRecord> records = csvParser.getRecords();

                records.forEach(record -> {
                    if (codeScheme != null) {
                        final Map<String, Code> existingCodesMap = parserUtils.getCodesMap(codeScheme);
                        final String codeValue = Utils.ensureRegionIdPadding(record.get("CODEVALUE"));
                        final String finnishName = record.get("PREFLABEL_FI");
                        final String swedishName = record.get("PREFLABEL_SE");
                        final String englishName = record.get("PREFLABEL_EN");
                        final String shortName = record.get("SHORTNAME");
                        final String definition = record.get("DEFINITION");
                        final String description = record.get("DESCRIPTION");
                        final Status status = Status.valueOf(record.get("STATUS"));
                        final ISO8601DateFormat dateFormat = new ISO8601DateFormat();
                        Date startDate = null;
                        final String startDateString = record.get("STARTDATE");
                        if (!startDateString.isEmpty()) {
                            try {
                                startDate = dateFormat.parse(startDateString);
                            } catch (ParseException e) {
                                LOG.error("Parsing startDate for code: " + codeValue + " failed from string: " + startDateString);
                            }
                        }
                        Date endDate = null;
                        final String endDateString = record.get("STARTDATE");
                        if (!endDateString.isEmpty()) {
                            try {
                                endDate = dateFormat.parse(endDateString);
                            } catch (ParseException e) {
                                LOG.error("Parsing endDate for code: " + codeValue + " failed from string: " + endDateString);
                            }
                        }
                        final Code code = createOrUpdateCode(existingCodesMap, codeScheme, codeValue, status, source, shortName, definition, description, finnishName, swedishName, englishName, startDate, endDate);
                        codes.add(code);
                    }
                });
            } catch (IOException e) {
                LOG.error("Parsing regions failed: " + e.getMessage());
            }
        }
        return codes;
    }

    private Code createOrUpdateCode(final Map<String, Code> codesMap,
                                    final CodeScheme codeScheme,
                                    final String codeValue,
                                    final Status status,
                                    final String source,
                                    final String shortName,
                                    final String definition,
                                    final String description,
                                    final String finnishName,
                                    final String swedishName,
                                    final String englishName,
                                    final Date startDate,
                                    final Date endDate) {
        final String url = apiUtils.createResourceUrl(ApiConstants.API_PATH_CODEREGISTRIES + "/" + codeScheme.getCodeRegistry().getCodeValue() + ApiConstants.API_PATH_CODESCHEMES + "/" + codeScheme.getCodeValue() + ApiConstants.API_PATH_CODES, codeValue);
        final Date timeStamp = new Date(System.currentTimeMillis());
        Code code = codesMap.get(codeValue);

        // Update
        if (code != null) {
            boolean hasChanges = false;
            if (!Objects.equals(code.getStatus(), status.toString())) {
                code.setStatus(status.toString());
                hasChanges = true;
            }
            if (!Objects.equals(code.getCodeScheme(), codeScheme)) {
                code.setCodeScheme(codeScheme);
                hasChanges = true;
            }
            if (!Objects.equals(code.getUri(), url)) {
                code.setUri(url);
                hasChanges = true;
            }
            if (!Objects.equals(code.getSource(), source)) {
                code.setSource(source);
                hasChanges = true;
            }
            if (!Objects.equals(code.getShortName(), shortName)) {
                code.setShortName(shortName);
                hasChanges = true;
            }
            if (!Objects.equals(code.getDefinition(), definition)) {
                code.setDefinition(definition);
                hasChanges = true;
            }
            if (!Objects.equals(code.getDescription(), description)) {
                code.setDescription(description);
                hasChanges = true;
            }
            if (!Objects.equals(code.getPrefLabel("fi"), finnishName)) {
                code.setPrefLabel("fi", finnishName);
                hasChanges = true;
            }
            if (!Objects.equals(code.getPrefLabel("se"), swedishName)) {
                code.setPrefLabel("se", swedishName);
                hasChanges = true;
            }
            if (!Objects.equals(code.getPrefLabel("en"), englishName)) {
                code.setPrefLabel("en", englishName);
                hasChanges = true;
            }
            if (!Objects.equals(code.getStartDate(), startDate)) {
                code.setStartDate(startDate);
                hasChanges = true;
            }
            if (!Objects.equals(code.getEndDate(), endDate)) {
                code.setEndDate(endDate);
                hasChanges = true;
            }
            if (hasChanges) {
                code.setModified(timeStamp);
            }
        // Create
        } else {
            code = new Code();
            code.setId(UUID.randomUUID().toString());
            code.setStatus(status.toString());
            code.setUri(url);
            code.setCodeScheme(codeScheme);
            code.setCodeValue(codeValue);
            code.setSource(source);
            code.setShortName(shortName);
            code.setDefinition(definition);
            code.setDescription(description);
            code.setModified(timeStamp);
            code.setPrefLabel("fi", finnishName);
            code.setPrefLabel("se", swedishName);
            code.setPrefLabel("en", englishName);
            code.setStartDate(startDate);
            code.setEndDate(endDate);
        }
        return code;
    }

}
