package fi.vm.yti.codelist.intake.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.util.FileUtils;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

/**
 * Class that handles parsing of codes from source data.
 */
@Service
public class CodeParser {

    private static final Logger LOG = LoggerFactory.getLogger(CodeParser.class);
    private final ApiUtils apiUtils;
    private final CodeRepository codeRepository;

    @Inject
    public CodeParser(final ApiUtils apiUtils,
                      final CodeRepository codeRepository) {
        this.apiUtils = apiUtils;
        this.codeRepository = codeRepository;
    }

    /**
     * Parses the .csv Code-file and returns the codes as an arrayList.
     *
     * @param codeScheme  CodeScheme codeValue identifier.
     * @param source      Source identifier for the data.
     * @param inputStream The Code -file.
     * @return List of Code objects.
     */
    public List<Code> parseCodesFromInputStream(final CodeScheme codeScheme,
                                                final String source,
                                                final InputStream inputStream) throws Exception {
        final List<Code> codes = new ArrayList<>();
        if (codeScheme != null) {
            try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 final BufferedReader in = new BufferedReader(inputStreamReader);
                 final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withHeader())) {
                FileUtils.skipBom(in);
                final List<CSVRecord> records = csvParser.getRecords();

                for (final CSVRecord record : records) {
                    final String id = record.get(CSV_HEADER_ID);
                    final String codeValue = record.get(CSV_HEADER_CODEVALUE);
                    final String prefLabelFinnish = record.get(CSV_HEADER_PREFLABEL_FI);
                    final String prefLabelSwedish = record.get(CSV_HEADER_PREFLABEL_SV);
                    final String prefLabelEnglish = record.get(CSV_HEADER_PREFLABEL_EN);
                    final String descriptionFinnish = record.get(CSV_HEADER_DESCRIPTION_FI);
                    final String descriptionSwedish = record.get(CSV_HEADER_DESCRIPTION_SV);
                    final String descriptionEnglish = record.get(CSV_HEADER_DESCRIPTION_EN);
                    final String definitionFinnish = record.get(CSV_HEADER_DEFINITION_FI);
                    final String definitionSwedish = record.get(CSV_HEADER_DEFINITION_SV);
                    final String definitionEnglish = record.get(CSV_HEADER_DEFINITION_EN);
                    final String shortName = record.get(CSV_HEADER_SHORTNAME);
                    final Status status = Status.valueOf(record.get(CSV_HEADER_STATUS));
                    final ISO8601DateFormat dateFormat = new ISO8601DateFormat();
                    Date startDate = null;
                    final String startDateString = record.get(CSV_HEADER_STARTDATE);
                    if (!startDateString.isEmpty()) {
                        try {
                            startDate = dateFormat.parse(startDateString);
                        } catch (ParseException e) {
                            LOG.error("Parsing startDate for code: " + codeValue + " failed from string: " + startDateString);
                        }
                    }
                    Date endDate = null;
                    final String endDateString = record.get(CSV_HEADER_ENDDATE);
                    if (!endDateString.isEmpty()) {
                        try {
                            endDate = dateFormat.parse(endDateString);
                        } catch (ParseException e) {
                            LOG.error("Parsing endDate for code: " + codeValue + " failed from string: " + endDateString);
                        }
                    }
                    final Code code = createOrUpdateCode(codeScheme, id, codeValue, status, source, shortName, startDate, endDate,
                        prefLabelFinnish, prefLabelSwedish, prefLabelEnglish,
                        descriptionFinnish, descriptionSwedish, descriptionEnglish,
                        definitionFinnish, definitionSwedish, definitionEnglish);
                    if (code != null) {
                        codes.add(code);
                    }
                }
            } catch (final IOException e) {
                LOG.error("Parsing codes failed: " + e.getMessage());
            }
        }
        return codes;
    }

    private Code createOrUpdateCode(final CodeScheme codeScheme,
                                    final String id,
                                    final String codeValue,
                                    final Status status,
                                    final String source,
                                    final String shortName,
                                    final Date startDate,
                                    final Date endDate,
                                    final String finnishPrefLabel,
                                    final String swedishPrefLabel,
                                    final String englishPrefLabel,
                                    final String finnishDescription,
                                    final String swedishDescription,
                                    final String englishDescription,
                                    final String finnishDefinition,
                                    final String swedishDefinition,
                                    final String englishDefinition) throws Exception {
        final Date timeStamp = new Date(System.currentTimeMillis());
        Code code = null;
        if (id != null) {
            code = codeRepository.findById(id);
        }
        String uri = null;
        if (Status.VALID == status) {
            uri = apiUtils.createResourceUrl(API_PATH_CODEREGISTRIES + "/" + codeScheme.getCodeRegistry().getCodeValue() + API_PATH_CODESCHEMES + "/" + codeScheme.getCodeValue() + API_PATH_CODES, codeValue);
            final Code existingCode = codeRepository.findByCodeSchemeAndCodeValueAndStatus(codeScheme, codeValue, status.toString());
            if (existingCode != code) {
                LOG.error("Existing value already found, cancel update!");
                throw new Exception("Existing value already found with status VALID for code: " + codeValue + ", cancel update!");
            }
        } else if (id != null && !id.isEmpty()) {
            uri = apiUtils.createResourceUrl(API_PATH_CODEREGISTRIES + "/" + codeScheme.getCodeRegistry().getCodeValue() + API_PATH_CODESCHEMES + "/" + codeScheme.getCodeValue() + API_PATH_CODES, id);
        }
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
            if (!Objects.equals(code.getUri(), uri)) {
                code.setUri(uri);
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
            if (!Objects.equals(code.getPrefLabel(LANGUAGE_CODE_FI), finnishPrefLabel)) {
                code.setPrefLabel(LANGUAGE_CODE_FI, finnishPrefLabel);
                hasChanges = true;
            }
            if (!Objects.equals(code.getPrefLabel(LANGUAGE_CODE_SV), swedishPrefLabel)) {
                code.setPrefLabel(LANGUAGE_CODE_SV, swedishPrefLabel);
                hasChanges = true;
            }
            if (!Objects.equals(code.getPrefLabel(LANGUAGE_CODE_EN), englishPrefLabel)) {
                code.setPrefLabel(LANGUAGE_CODE_EN, englishPrefLabel);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getDefinition(LANGUAGE_CODE_FI), finnishDefinition)) {
                codeScheme.setDefinition(LANGUAGE_CODE_FI, finnishDefinition);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getDefinition(LANGUAGE_CODE_SV), swedishDefinition)) {
                codeScheme.setDefinition(LANGUAGE_CODE_SV, swedishDefinition);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getDefinition(LANGUAGE_CODE_EN), englishDefinition)) {
                codeScheme.setDefinition(LANGUAGE_CODE_EN, englishDefinition);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getDescription(LANGUAGE_CODE_FI), finnishDescription)) {
                codeScheme.setDescription(LANGUAGE_CODE_FI, finnishDescription);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getDescription(LANGUAGE_CODE_SV), swedishDescription)) {
                codeScheme.setDescription(LANGUAGE_CODE_SV, swedishDescription);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getDescription(LANGUAGE_CODE_EN), englishDescription)) {
                codeScheme.setDescription(LANGUAGE_CODE_EN, englishDescription);
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
            if (id != null && !id.isEmpty()) {
                code.setId(id);
            } else {
                final String uuid = UUID.randomUUID().toString();
                if (status != Status.VALID) {
                    uri = apiUtils.createResourceUrl(API_PATH_CODEREGISTRIES + "/" + codeScheme.getCodeRegistry().getCodeValue() + API_PATH_CODESCHEMES + "/" + codeScheme.getCodeValue() + API_PATH_CODES, uuid);
                }
                code.setId(uuid);
            }
            code.setStatus(status.toString());
            code.setUri(uri);
            code.setCodeScheme(codeScheme);
            code.setCodeValue(codeValue);
            code.setSource(source);
            code.setShortName(shortName);
            code.setModified(timeStamp);
            code.setPrefLabel(LANGUAGE_CODE_FI, finnishPrefLabel);
            code.setPrefLabel(LANGUAGE_CODE_SV, swedishPrefLabel);
            code.setPrefLabel(LANGUAGE_CODE_EN, englishPrefLabel);
            code.setDefinition(LANGUAGE_CODE_FI, finnishDefinition);
            code.setDefinition(LANGUAGE_CODE_SV, swedishDefinition);
            code.setDefinition(LANGUAGE_CODE_EN, englishDefinition);
            code.setDescription(LANGUAGE_CODE_FI, finnishDescription);
            code.setDescription(LANGUAGE_CODE_SV, swedishDescription);
            code.setDescription(LANGUAGE_CODE_EN, englishDescription);
            code.setStartDate(startDate);
            code.setEndDate(endDate);
        }
        return code;
    }
}
