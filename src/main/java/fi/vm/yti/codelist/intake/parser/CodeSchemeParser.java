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

import fi.vm.yti.codelist.common.constants.ApiConstants;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.util.FileUtils;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

/**
 * Class that handles parsing of codeschemes from source data.
 */
@Service
public class CodeSchemeParser {

    private static final Logger LOG = LoggerFactory.getLogger(CodeSchemeParser.class);
    private final ApiUtils apiUtils;
    private final CodeSchemeRepository codeSchemeRepository;

    @Inject
    public CodeSchemeParser(final ApiUtils apiUtils,
                            final CodeSchemeRepository codeSchemeRepository) {
        this.apiUtils = apiUtils;
        this.codeSchemeRepository = codeSchemeRepository;
    }

    /**
     * Parses the .csv CodeScheme-file and returns the codeschemes as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The CodeScheme -file.
     * @return List of CodeScheme objects.
     */
    public List<CodeScheme> parseCodeSchemesFromInputStream(final CodeRegistry codeRegistry,
                                                            final String source,
                                                            final InputStream inputStream) throws Exception {
        final List<CodeScheme> codeSchemes = new ArrayList<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                final BufferedReader in = new BufferedReader(inputStreamReader);
                final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withHeader())) {
            FileUtils.skipBom(in);
            final List<CSVRecord> records = csvParser.getRecords();
            for (final CSVRecord record : records) {
                final String id = record.get(CSV_HEADER_ID);
                final String codeValue = record.get(CSV_HEADER_CODEVALUE);
                final String prefLabelFinnish = record.get(CSV_HEADER_PREFLABEL_FI);
                final String prefLabelSwedish = record.get(CSV_HEADER_PREFLABEL_SE);
                final String prefLabelEnglish = record.get(CSV_HEADER_PREFLABEL_EN);
                final String descriptionFinnish = record.get(CSV_HEADER_DESCRIPTION_FI);
                final String descriptionSwedish = record.get(CSV_HEADER_DESCRIPTION_SE);
                final String descriptionEnglish = record.get(CSV_HEADER_DESCRIPTION_EN);
                final String definitionFinnish = record.get(CSV_HEADER_DEFINITION_FI);
                final String definitionSwedish = record.get(CSV_HEADER_DEFINITION_SE);
                final String definitionEnglish = record.get(CSV_HEADER_DEFINITION_EN);
                final String changeNoteFinnish = record.get(CSV_HEADER_CHANGENOTE_FI);
                final String changeNoteSwedish = record.get(CSV_HEADER_CHANGENOTE_SE);
                final String changeNoteEnglish = record.get(CSV_HEADER_CHANGENOTE_EN);
                final String version = record.get(CSV_HEADER_VERSION);
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
                final CodeScheme codeScheme = createOrUpdateCodeScheme(codeRegistry, id, codeValue,
                        version, source, status, startDate, endDate,
                        prefLabelFinnish, prefLabelSwedish, prefLabelEnglish,
                        descriptionFinnish, descriptionSwedish, descriptionEnglish,
                        definitionFinnish, definitionSwedish, definitionEnglish,
                        changeNoteFinnish, changeNoteSwedish, changeNoteEnglish);
                codeSchemes.add(codeScheme);
            }
        } catch (IOException e) {
            LOG.error("Parsing codeschemes failed: " + e.getMessage());
        }
        return codeSchemes;
    }
    
    private CodeScheme createOrUpdateCodeScheme(final CodeRegistry codeRegistry,
                                                final String id,
                                                final String codeValue,
                                                final String version,
                                                final String source,
                                                final Status status,
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
                                                final String englishDefinition,
                                                final String finnishChangeNote,
                                                final String swedishChangeNote,
                                                final String englishChangeNote) throws Exception {
        final Date timeStamp = new Date(System.currentTimeMillis());
        CodeScheme codeScheme = null;
        if (id != null) {
            codeScheme = codeSchemeRepository.findById(id);
        }
        String uri = null;
        if (Status.VALID == status) {
            uri = apiUtils.createResourceUrl(ApiConstants.API_PATH_CODEREGISTRIES + "/" + codeRegistry.getCodeValue() + ApiConstants.API_PATH_CODESCHEMES, codeValue);
            final CodeScheme existingCodeScheme = codeSchemeRepository.findByCodeValueAndStatusAndCodeRegistry(codeValue, status.toString(), codeRegistry);
            if (existingCodeScheme != null) {
                LOG.error("Existing value already found, cancel update!");
                throw new Exception("Existing value already found with status VALID for code scheme with code value: " + codeValue + ", cancel update!");
            }
        } else if (id != null && !id.isEmpty()) {
            uri = apiUtils.createResourceUrl(ApiConstants.API_PATH_CODEREGISTRIES + "/" + codeRegistry.getCodeValue() + ApiConstants.API_PATH_CODESCHEMES, id);
        }
        // Update
        if (codeScheme != null) {
            boolean hasChanges = false;
            if (!Objects.equals(codeScheme.getStatus(), status.toString())) {
                codeScheme.setStatus(status.toString());
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getCodeRegistry(), codeRegistry)) {
                codeScheme.setCodeRegistry(codeRegistry);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getUri(), uri)) {
                codeScheme.setUri(uri);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getSource(), source)) {
                codeScheme.setSource(source);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getPrefLabel(LANGUAGE_CODE_FI), finnishPrefLabel)) {
                codeScheme.setPrefLabel(LANGUAGE_CODE_FI, finnishPrefLabel);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getPrefLabel(LANGUAGE_CODE_SE), swedishPrefLabel)) {
                codeScheme.setPrefLabel(LANGUAGE_CODE_SE, swedishPrefLabel);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getPrefLabel(LANGUAGE_CODE_EN), englishPrefLabel)) {
                codeScheme.setPrefLabel(LANGUAGE_CODE_EN, englishPrefLabel);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getDefinition(LANGUAGE_CODE_FI), finnishDefinition)) {
                codeScheme.setDefinition(LANGUAGE_CODE_FI, finnishDefinition);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getDefinition(LANGUAGE_CODE_SE), swedishDefinition)) {
                codeScheme.setDefinition(LANGUAGE_CODE_SE, swedishDefinition);
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
            if (!Objects.equals(codeScheme.getDescription(LANGUAGE_CODE_SE), swedishDescription)) {
                codeScheme.setDescription(LANGUAGE_CODE_SE, swedishDescription);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getDescription(LANGUAGE_CODE_EN), englishDescription)) {
                codeScheme.setDescription(LANGUAGE_CODE_EN, englishDescription);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getChangeNote(LANGUAGE_CODE_FI), finnishChangeNote)) {
                codeScheme.setChangeNote(LANGUAGE_CODE_FI, finnishChangeNote);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getChangeNote(LANGUAGE_CODE_SE), swedishChangeNote)) {
                codeScheme.setChangeNote(LANGUAGE_CODE_SE, swedishChangeNote);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getChangeNote(LANGUAGE_CODE_EN), englishChangeNote)) {
                codeScheme.setChangeNote(LANGUAGE_CODE_EN, englishChangeNote);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getVersion(), version)) {
                codeScheme.setVersion(version);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getStartDate(), startDate)) {
                codeScheme.setStartDate(startDate);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getEndDate(), endDate)) {
                codeScheme.setEndDate(endDate);
                hasChanges = true;
            }
            if (hasChanges) {
                codeScheme.setModified(timeStamp);
            }
        // Create
        } else {
            codeScheme = new CodeScheme();
            codeScheme.setId(UUID.randomUUID().toString());
            codeScheme.setCodeRegistry(codeRegistry);
            if (id != null && !id.isEmpty()) {
                codeScheme.setId(id);
            } else {
                final String uuid = UUID.randomUUID().toString();
                if (status != Status.VALID) {
                    uri = apiUtils.createResourceUrl(ApiConstants.API_PATH_CODEREGISTRIES + "/" + codeRegistry.getCodeValue() + ApiConstants.API_PATH_CODESCHEMES, id);
                }
                codeScheme.setId(uuid);
            }
            codeScheme.setUri(uri);
            codeScheme.setCodeValue(codeValue);
            codeScheme.setSource(source);
            codeScheme.setModified(timeStamp);
            codeScheme.setPrefLabel(LANGUAGE_CODE_FI, finnishPrefLabel);
            codeScheme.setPrefLabel(LANGUAGE_CODE_SE, swedishPrefLabel);
            codeScheme.setPrefLabel(LANGUAGE_CODE_EN, englishPrefLabel);
            codeScheme.setDefinition(LANGUAGE_CODE_FI, finnishDefinition);
            codeScheme.setDefinition(LANGUAGE_CODE_SE, swedishDefinition);
            codeScheme.setDefinition(LANGUAGE_CODE_EN, englishDefinition);
            codeScheme.setDescription(LANGUAGE_CODE_FI, finnishDescription);
            codeScheme.setDescription(LANGUAGE_CODE_SE, swedishDescription);
            codeScheme.setDescription(LANGUAGE_CODE_EN, englishDescription);
            codeScheme.setChangeNote(LANGUAGE_CODE_FI, finnishChangeNote);
            codeScheme.setChangeNote(LANGUAGE_CODE_SE, swedishChangeNote);
            codeScheme.setChangeNote(LANGUAGE_CODE_EN, englishChangeNote);
            codeScheme.setVersion(version);
            codeScheme.setStatus(status.toString());
            codeScheme.setStartDate(startDate);
            codeScheme.setEndDate(endDate);
        }
        return codeScheme;
    }

}
