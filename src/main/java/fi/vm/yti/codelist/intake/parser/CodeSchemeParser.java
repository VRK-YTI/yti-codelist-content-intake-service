package fi.vm.yti.codelist.intake.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.util.FileUtils;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

/**
 * Class that handles parsing of CodeSchemes from source data.
 */
@Service
public class CodeSchemeParser extends AbstractBaseParser {

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
     * @param inputStream The CodeScheme -file.
     * @return            List of CodeScheme objects.
     */
    public List<CodeScheme> parseCodeSchemesFromCsvInputStream(final CodeRegistry codeRegistry,
                                                               final InputStream inputStream) throws Exception {
        final List<CodeScheme> codeSchemes = new ArrayList<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
            FileUtils.skipBom(in);
            final Map<String, Integer> headerMap = csvParser.getHeaderMap();
            final Map<String, String> prefLabelHeaders = new LinkedHashMap<>();
            final Map<String, String> descriptionHeaders = new LinkedHashMap<>();
            final Map<String, String> definitionHeaders = new LinkedHashMap<>();
            final Map<String, String> changeNoteHeaders = new LinkedHashMap<>();
            for (final String value : headerMap.keySet()) {
                if (value.startsWith(CONTENT_HEADER_PREFLABEL_PREFIX)) {
                    prefLabelHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_PREFLABEL_PREFIX, value), value);
                } else if (value.startsWith(CONTENT_HEADER_DESCRIPTION_PREFIX)) {
                    descriptionHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_DESCRIPTION_PREFIX, value), value);
                } else if (value.startsWith(CONTENT_HEADER_DEFINITION_PREFIX)) {
                    definitionHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_DEFINITION_PREFIX, value), value);
                } else if (value.startsWith(CONTENT_HEADER_CHANGENOTE_PREFIX)) {
                    changeNoteHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_CHANGENOTE_PREFIX, value), value);
                }
            }
            final List<CSVRecord> records = csvParser.getRecords();
            for (final CSVRecord record : records) {
                final UUID id = parseUUIDFromString(record.get(CONTENT_HEADER_ID));
                final String codeValue = record.get(CONTENT_HEADER_CODEVALUE);
                final Map<String, String> prefLabels = new LinkedHashMap<>();
                prefLabelHeaders.forEach((language, header) -> {
                    prefLabels.put(language, record.get(header));
                });
                final Map<String, String> definitions = new LinkedHashMap<>();
                definitionHeaders.forEach((language, header) -> {
                    definitions.put(language, record.get(header));
                });
                final Map<String, String> descriptions = new LinkedHashMap<>();
                descriptionHeaders.forEach((language, header) -> {
                    descriptions.put(language, record.get(header));
                });
                final Map<String, String> changeNotes = new LinkedHashMap<>();
                changeNoteHeaders.forEach((language, header) -> {
                    changeNotes.put(language, record.get(header));
                });
                final String version = record.get(CONTENT_HEADER_VERSION);
                final Status status = Status.valueOf(record.get(CONTENT_HEADER_STATUS));
                final String legalBase = record.get(CONTENT_HEADER_LEGALBASE);
                final String governancePolicy = record.get(CONTENT_HEADER_GOVERNANCEPOLICY);
                final String license = record.get(CONTENT_HEADER_LICENSE);
                final String source = record.get(CONTENT_HEADER_SOURCE);
                final ISO8601DateFormat dateFormat = new ISO8601DateFormat();
                Date startDate = null;
                final String startDateString = record.get(CONTENT_HEADER_STARTDATE);
                if (!startDateString.isEmpty()) {
                    try {
                        startDate = dateFormat.parse(startDateString);
                    } catch (ParseException e) {
                        LOG.error("Parsing startDate for code: " + codeValue + " failed from string: " + startDateString);
                    }
                }
                Date endDate = null;
                final String endDateString = record.get(CONTENT_HEADER_ENDDATE);
                if (!endDateString.isEmpty()) {
                    try {
                        endDate = dateFormat.parse(endDateString);
                    } catch (ParseException e) {
                        LOG.error("Parsing endDate for code: " + codeValue + " failed from string: " + endDateString);
                    }
                }
                final CodeScheme codeScheme = createOrUpdateCodeScheme(codeRegistry, id, codeValue, version, status,
                    source, legalBase, governancePolicy, license, startDate, endDate, prefLabels, descriptions, definitions, changeNotes);
                codeSchemes.add(codeScheme);
            }
        } catch (IOException e) {
            LOG.error("Parsing codeschemes failed: " + e.getMessage());
        }
        return codeSchemes;
    }

    /*
     * Parses the .xls CodeScheme Excel-file and returns the CodeSchemes as an arrayList.
     *
     * @param codeRegistry CodeRegistry.
     * @param inputStream The Code containing Excel -file.
     * @return List of Code objects.
     */
    public List<CodeScheme> parseCodeSchemesFromExcelInputStream(final CodeRegistry codeRegistry,
                                                                 final InputStream inputStream) throws Exception {
        final List<CodeScheme> codeSchemes = new ArrayList<>();
        if (codeRegistry != null) {
            final Workbook workbook = new XSSFWorkbook(inputStream);
            final Sheet codesSheet = workbook.getSheet(EXCEL_SHEET_CODESCHEMES);
            final Iterator<Row> rowIterator = codesSheet.rowIterator();
            final Map<String, Integer> genericHeaders = new LinkedHashMap<>();
            final Map<String, Integer> prefLabelHeaders = new LinkedHashMap<>();
            final Map<String, Integer> descriptionHeaders = new LinkedHashMap<>();
            final Map<String, Integer> definitionHeaders = new LinkedHashMap<>();
            final Map<String, Integer> changeNoteHeaders = new LinkedHashMap<>();
            boolean firstRow = true;
            while (rowIterator.hasNext()) {
                final Row row = rowIterator.next();
                if (firstRow) {
                    final Iterator<Cell> cellIterator = row.cellIterator();
                    while (cellIterator.hasNext()) {
                        final Cell cell = cellIterator.next();
                        final String value = cell.getStringCellValue();
                        final Integer index = cell.getColumnIndex();
                        if (value.startsWith(CONTENT_HEADER_PREFLABEL_PREFIX)) {
                            prefLabelHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_PREFLABEL_PREFIX, value), index);
                        } else if (value.startsWith(CONTENT_HEADER_DESCRIPTION_PREFIX)) {
                            descriptionHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_DESCRIPTION_PREFIX, value), index);
                        } else if (value.startsWith(CONTENT_HEADER_DEFINITION_PREFIX)) {
                            definitionHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_DEFINITION_PREFIX, value), index);
                        } else if (value.startsWith(CONTENT_HEADER_CHANGENOTE_PREFIX)) {
                            changeNoteHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_CHANGENOTE_PREFIX, value), index);
                        } else {
                            genericHeaders.put(value, index);
                        }
                    }
                    firstRow = false;
                } else {
                    final UUID id = parseUUIDFromString(row.getCell(genericHeaders.get(CONTENT_HEADER_ID)).getStringCellValue());
                    final String codeValue = row.getCell(genericHeaders.get(CONTENT_HEADER_CODEVALUE)).getStringCellValue();
                    final Map<String, String> prefLabels = new LinkedHashMap<>();
                    prefLabelHeaders.forEach((language, header) -> {
                        prefLabels.put(language, row.getCell(header).getStringCellValue());
                    });
                    final Map<String, String> definitions = new LinkedHashMap<>();
                    definitionHeaders.forEach((language, header) -> {
                        definitions.put(language, row.getCell(header).getStringCellValue());
                    });
                    final Map<String, String> descriptions = new LinkedHashMap<>();
                    descriptionHeaders.forEach((language, header) -> {
                        descriptions.put(language, row.getCell(header).getStringCellValue());
                    });
                    final Map<String, String> changeNotes = new LinkedHashMap<>();
                    changeNoteHeaders.forEach((language, header) -> {
                        changeNotes.put(language, row.getCell(header).getStringCellValue());
                    });
                    final String version = row.getCell(genericHeaders.get(CONTENT_HEADER_VERSION)).getStringCellValue();
                    final Status status = Status.valueOf(row.getCell(genericHeaders.get(CONTENT_HEADER_STATUS)).getStringCellValue());
                    final String source = row.getCell(genericHeaders.get(CONTENT_HEADER_SOURCE)).getStringCellValue();
                    final String legalBase = row.getCell(genericHeaders.get(CONTENT_HEADER_LEGALBASE)).getStringCellValue();
                    final String governancePolicy = row.getCell(genericHeaders.get(CONTENT_HEADER_GOVERNANCEPOLICY)).getStringCellValue();
                    final String license = row.getCell(genericHeaders.get(CONTENT_HEADER_LICENSE)).getStringCellValue();
                    final ISO8601DateFormat dateFormat = new ISO8601DateFormat();
                    Date startDate = null;
                    final String startDateString = row.getCell(genericHeaders.get(CONTENT_HEADER_STARTDATE)).getStringCellValue();
                    if (!startDateString.isEmpty()) {
                        try {
                            startDate = dateFormat.parse(startDateString);
                        } catch (ParseException e) {
                            LOG.error("Parsing startDate for code: " + codeValue + " failed from string: " + startDateString);
                        }
                    }
                    Date endDate = null;
                    final String endDateString = row.getCell(genericHeaders.get(CONTENT_HEADER_ENDDATE)).getStringCellValue();
                    if (!endDateString.isEmpty()) {
                        try {
                            endDate = dateFormat.parse(endDateString);
                        } catch (ParseException e) {
                            LOG.error("Parsing endDate for code: " + codeValue + " failed from string: " + endDateString);
                        }
                    }
                    final CodeScheme codeScheme = createOrUpdateCodeScheme(codeRegistry, id, codeValue, version, status, source, legalBase, governancePolicy, license, startDate, endDate, prefLabels, descriptions, definitions, changeNotes);
                    if (codeScheme != null) {
                        codeSchemes.add(codeScheme);
                    }
                }
            }
        }
        return codeSchemes;
    }

    private CodeScheme createOrUpdateCodeScheme(final CodeRegistry codeRegistry,
                                                final UUID id,
                                                final String codeValue,
                                                final String version,
                                                final Status status,
                                                final String source,
                                                final String legalBase,
                                                final String governancePolicy,
                                                final String license,
                                                final Date startDate,
                                                final Date endDate,
                                                final Map<String, String> prefLabels,
                                                final Map<String, String> descriptions,
                                                final Map<String, String> definitions,
                                                final Map<String, String> changeNotes) throws Exception {
        CodeScheme codeScheme = null;
        if (id != null) {
            codeScheme = codeSchemeRepository.findById(id);
        }
        String uri = null;
        if (Status.VALID == status) {
            uri = apiUtils.createResourceUrl(API_PATH_CODEREGISTRIES + "/" + codeRegistry.getCodeValue() + API_PATH_CODESCHEMES, codeValue);
            final CodeScheme existingCodeScheme = codeSchemeRepository.findByCodeValueAndStatusAndCodeRegistry(codeValue, status.toString(), codeRegistry);
            if (existingCodeScheme != codeScheme) {
                LOG.error("Existing value already found, cancel update!");
                throw new Exception("Existing value already found with status VALID for code scheme with code value: " + codeValue + ", cancel update!");
            }
        } else if (id != null) {
            uri = apiUtils.createResourceUrl(API_PATH_CODEREGISTRIES + "/" + codeRegistry.getCodeValue() + API_PATH_CODESCHEMES, id.toString());
        }
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
            if (!Objects.equals(codeScheme.getLegalBase(), legalBase)) {
                codeScheme.setLegalBase(legalBase);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getGovernancePolicy(), governancePolicy)) {
                codeScheme.setGovernancePolicy(governancePolicy);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getLicense(), license)) {
                codeScheme.setLicense(license);
                hasChanges = true;
            }
            for (final String language : prefLabels.keySet()) {
                final String value = prefLabels.get(language);
                if (!Objects.equals(codeScheme.getPrefLabel(language), value)) {
                    codeScheme.setPrefLabel(language, value);
                    hasChanges = true;
                }
            }
            for (final String language : descriptions.keySet()) {
                final String value = descriptions.get(language);
                if (!Objects.equals(codeScheme.getDescription(language), value)) {
                    codeScheme.setDescription(language, value);
                    hasChanges = true;
                }
            }
            for (final String language : definitions.keySet()) {
                final String value = definitions.get(language);
                if (!Objects.equals(codeScheme.getDefinition(language), value)) {
                    codeScheme.setDefinition(language, value);
                    hasChanges = true;
                }
            }
            for (final String language : changeNotes.keySet()) {
                final String value = changeNotes.get(language);
                if (!Objects.equals(codeScheme.getChangeNote(language), value)) {
                    codeScheme.setChangeNote(language, value);
                    hasChanges = true;
                }
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
                final Date timeStamp = new Date(System.currentTimeMillis());
                codeScheme.setModified(timeStamp);
            }
        } else {
            codeScheme = new CodeScheme();
            codeScheme.setId(UUID.randomUUID());
            codeScheme.setCodeRegistry(codeRegistry);
            if (id != null) {
                codeScheme.setId(id);
            } else {
                final UUID uuid = UUID.randomUUID();
                if (status != Status.VALID) {
                    uri = apiUtils.createResourceUrl(API_PATH_CODEREGISTRIES + "/" + codeRegistry.getCodeValue() + API_PATH_CODESCHEMES, uuid.toString());
                }
                codeScheme.setId(uuid);
            }
            codeScheme.setUri(uri);
            codeScheme.setCodeValue(codeValue);
            codeScheme.setSource(source);
            codeScheme.setLegalBase(legalBase);
            codeScheme.setGovernancePolicy(governancePolicy);
            codeScheme.setLicense(license);
            final Date timeStamp = new Date(System.currentTimeMillis());
            codeScheme.setModified(timeStamp);
            for (final String language : prefLabels.keySet()) {
                codeScheme.setPrefLabel(language, prefLabels.get(language));
            }
            for (final String language : descriptions.keySet()) {
                codeScheme.setDescription(language, descriptions.get(language));
            }
            for (final String language : definitions.keySet()) {
                codeScheme.setDefinition(language, definitions.get(language));
            }
            for (final String language : changeNotes.keySet()) {
                codeScheme.setChangeNote(language, changeNotes.get(language));
            }
            codeScheme.setVersion(version);
            codeScheme.setStatus(status.toString());
            codeScheme.setStartDate(startDate);
            codeScheme.setEndDate(endDate);
        }
        return codeScheme;
    }
}
