package fi.vm.yti.codelist.intake.parser.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.intake.exception.BadClassificationException;
import fi.vm.yti.codelist.intake.exception.CsvParsingException;
import fi.vm.yti.codelist.intake.exception.JsonParsingException;
import fi.vm.yti.codelist.intake.exception.MissingHeaderClassificationException;
import fi.vm.yti.codelist.intake.exception.MissingHeaderCodeValueException;
import fi.vm.yti.codelist.intake.exception.MissingHeaderStatusException;
import fi.vm.yti.codelist.intake.exception.MissingRowValueCodeValueException;
import fi.vm.yti.codelist.intake.exception.MissingRowValueStatusException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.parser.CodeSchemeParser;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Service
public class CodeSchemeParserImpl extends AbstractBaseParser implements CodeSchemeParser {

    private static final Logger LOG = LoggerFactory.getLogger(CodeSchemeParserImpl.class);

    @Override
    public CodeSchemeDTO parseCodeSchemeFromJsonData(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final CodeSchemeDTO codeScheme;
        try {
            codeScheme = mapper.readValue(jsonPayload, CodeSchemeDTO.class);
            validateStartDateIsBeforeEndDate(codeScheme);
        } catch (final IOException e) {
            LOG.error("CodeScheme parsing failed from JSON!", e);
            throw new JsonParsingException(ERR_MSG_USER_406);
        }
        return codeScheme;
    }

    @Override
    public Set<CodeSchemeDTO> parseCodeSchemesFromJsonData(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final Set<CodeSchemeDTO> codeSchemes;
        final Set<String> codeValues = new HashSet<>();
        try {
            codeSchemes = mapper.readValue(jsonPayload, new TypeReference<Set<CodeSchemeDTO>>() {
            });
        } catch (final IOException e) {
            LOG.error("CodeSchemes parsing failed from JSON!", e);
            throw new JsonParsingException(ERR_MSG_USER_406);
        }
        for (final CodeSchemeDTO codeScheme : codeSchemes) {
            final String codeValue = codeScheme.getCodeValue();
            checkForDuplicateCodeValueInImportData(codeValues, codeValue);
            codeValues.add(codeValue.toLowerCase());
            validateStartDateIsBeforeEndDate(codeScheme);
        }
        return codeSchemes;
    }

    @Override
    @SuppressFBWarnings("UC_USELESS_OBJECT")
    public Set<CodeSchemeDTO> parseCodeSchemesFromCsvInputStream(final CodeRegistry codeRegistry,
                                                                 final InputStream inputStream) {
        final Set<CodeSchemeDTO> codeSchemes = new HashSet<>();
        final Set<String> codeValues = new HashSet<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
            final Map<String, Integer> headerMap = csvParser.getHeaderMap();
            final Map<String, Integer> prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
            final Map<String, Integer> definitionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DEFINITION_PREFIX);
            final Map<String, Integer> descriptionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DESCRIPTION_PREFIX);
            final Map<String, Integer> changeNoteHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_CHANGENOTE_PREFIX);
            validateRequiredHeaders(headerMap);
            final List<CSVRecord> records = csvParser.getRecords();
            for (final CSVRecord record : records) {
                validateRequiredDataOnRecord(record);
                final CodeSchemeDTO codeScheme = new CodeSchemeDTO();
                final String codeValue = parseCodeValueFromRecord(record);
                validateCodeValue(codeValue);
                checkForDuplicateCodeValueInImportData(codeValues, codeValue);
                codeValues.add(codeValue.toLowerCase());
                codeScheme.setCodeValue(codeValue);
                codeScheme.setId(parseIdFromRecord(record));
                codeScheme.setPrefLabel(parseLocalizedValueFromCsvRecord(prefLabelHeaders, record));
                codeScheme.setDefinition(parseLocalizedValueFromCsvRecord(definitionHeaders, record));
                codeScheme.setDescription(parseLocalizedValueFromCsvRecord(descriptionHeaders, record));
                codeScheme.setChangeNote(parseLocalizedValueFromCsvRecord(changeNoteHeaders, record));
                if (!codeValue.equals(YTI_DATACLASSIFICATION_CODESCHEME) && !codeRegistry.getCodeValue().equals(JUPO_REGISTRY)) {
                    codeScheme.setDataClassifications(resolveDataClassificationsFromString(parseStringFromCsvRecord(record, CONTENT_HEADER_CLASSIFICATION)));
                }
                codeScheme.setStatus(parseStatusValueFromString(record.get(CONTENT_HEADER_STATUS)));
                codeScheme.setVersion(parseVersionFromCsvRecord(record));
                codeScheme.setLegalBase(parseLegalBaseFromCsvRecord(record));
                codeScheme.setGovernancePolicy(parseGovernancePolicyFromCsvRecord(record));
                codeScheme.setConceptUriInVocabularies(parseConceptUriFromCsvRecord(record));
                codeScheme.setSource(parseSourceFromCsvRecord(record));
                if (headerMap.containsKey(CONTENT_HEADER_DEFAULTCODE)) {
                    final String defaultCodeCodeValue = parseDefaultCodeFromCsvRecord(record);
                    if (defaultCodeCodeValue != null && !defaultCodeCodeValue.isEmpty()) {
                        final CodeDTO defaultCode = new CodeDTO();
                        defaultCode.setCodeValue(defaultCodeCodeValue);
                        codeScheme.setDefaultCode(defaultCode);
                    }
                }
                if (record.isMapped(CONTENT_HEADER_STARTDATE)) {
                    codeScheme.setStartDate(parseStartDateFromString(parseStartDateStringFromCsvRecord(record), String.valueOf(record.getRecordNumber() + 1)));
                }
                if (record.isMapped(CONTENT_HEADER_ENDDATE)) {
                    codeScheme.setEndDate(parseEndDateFromString(parseEndDateStringFromCsvRecord(record), String.valueOf(record.getRecordNumber() + 1)));
                }
                validateStartDateIsBeforeEndDate(codeScheme);
                codeSchemes.add(codeScheme);
            }
        } catch (final IllegalArgumentException e) {
            LOG.error("Duplicate header value found in CSV!", e);
            throw new CsvParsingException(ERR_MSG_USER_DUPLICATE_HEADER_VALUE);
        } catch (final IOException e) {
            LOG.error("Error parsing CSV file!", e);
            throw new CsvParsingException(ERR_MSG_USER_ERROR_PARSING_CSV_FILE);
        }
        return codeSchemes;
    }

    @Override
    @SuppressFBWarnings("UC_USELESS_OBJECT")
    public Set<CodeSchemeDTO> parseCodeSchemesFromExcelWorkbook(final CodeRegistry codeRegistry,
                                                                final Workbook workbook,
                                                                final Map<CodeSchemeDTO, String> codesSheetNames,
                                                                final Map<CodeSchemeDTO, String> extensionSchemesSheetNames) {
        final Set<CodeSchemeDTO> codeSchemes = new HashSet<>();
        final Set<String> codeValues = new HashSet<>();
        final DataFormatter formatter = new DataFormatter();
        Sheet sheet = workbook.getSheet(EXCEL_SHEET_CODESCHEMES);
        if (sheet == null) {
            sheet = workbook.getSheetAt(0);
        }
        final Iterator<Row> rowIterator = sheet.rowIterator();
        Map<String, Integer> headerMap = null;
        Map<String, Integer> prefLabelHeaders = null;
        Map<String, Integer> definitionHeaders = null;
        Map<String, Integer> descriptionHeaders = null;
        Map<String, Integer> changeNoteHeaders = null;
        boolean firstRow = true;
        while (rowIterator.hasNext()) {
            final Row row = rowIterator.next();
            if (firstRow) {
                firstRow = false;
                headerMap = resolveHeaderMap(row);
                prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
                definitionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DEFINITION_PREFIX);
                descriptionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DESCRIPTION_PREFIX);
                changeNoteHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_CHANGENOTE_PREFIX);
                validateRequiredHeaders(headerMap);
            } else if (row.getPhysicalNumberOfCells() > 0 && !isRowEmpty(row)) {
                final CodeSchemeDTO codeScheme = new CodeSchemeDTO();
                final String codeValue = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODEVALUE)));
                final String status = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STATUS)));
                if (skipEmptyLine(codeValue, status)) {
                    continue;
                }
                validateRequiredDataOnRow(row, headerMap, formatter);
                validateCodeValue(codeValue);
                checkForDuplicateCodeValueInImportData(codeValues, codeValue);
                codeValues.add(codeValue.toLowerCase());
                codeScheme.setCodeValue(codeValue);
                codeScheme.setStatus(parseStatusValueFromString(status));
                if (headerMap.containsKey(CONTENT_HEADER_ID)) {
                    codeScheme.setId(parseUUIDFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ID)))));
                }
                if (!codeValue.equals(YTI_DATACLASSIFICATION_CODESCHEME) && !codeRegistry.getCodeValue().equals(JUPO_REGISTRY)) {
                    codeScheme.setDataClassifications(resolveDataClassificationsFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CLASSIFICATION)))));
                }
                codeScheme.setPrefLabel(parseLocalizedValueFromExcelRow(prefLabelHeaders, row, formatter));
                codeScheme.setDefinition(parseLocalizedValueFromExcelRow(definitionHeaders, row, formatter));
                codeScheme.setDescription(parseLocalizedValueFromExcelRow(descriptionHeaders, row, formatter));
                codeScheme.setChangeNote(parseLocalizedValueFromExcelRow(changeNoteHeaders, row, formatter));
                if (headerMap.containsKey(CONTENT_HEADER_VERSION)) {
                    codeScheme.setVersion(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_VERSION))));
                }
                if (headerMap.containsKey(CONTENT_HEADER_SOURCE)) {
                    codeScheme.setSource(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_SOURCE))));
                }
                if (headerMap.containsKey(CONTENT_HEADER_LEGALBASE)) {
                    codeScheme.setLegalBase(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_LEGALBASE))));
                }
                if (headerMap.containsKey(CONTENT_HEADER_GOVERNANCEPOLICY)) {
                    codeScheme.setGovernancePolicy(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_GOVERNANCEPOLICY))));
                }
                if (headerMap.containsKey(CONTENT_HEADER_CONCEPTURI)) {
                    codeScheme.setConceptUriInVocabularies(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CONCEPTURI))));
                }
                if (headerMap.containsKey(CONTENT_HEADER_DEFAULTCODE)) {
                    final String defaultCodeCodeValue = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_DEFAULTCODE)));
                    if (defaultCodeCodeValue != null && !defaultCodeCodeValue.isEmpty()) {
                        final CodeDTO defaultCode = new CodeDTO();
                        defaultCode.setCodeValue(defaultCodeCodeValue);
                        codeScheme.setDefaultCode(defaultCode);
                    }
                }
                if (headerMap.containsKey(CONTENT_HEADER_STARTDATE)) {
                    codeScheme.setStartDate(parseStartDateFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STARTDATE))), String.valueOf(row.getRowNum())));
                }
                if (headerMap.containsKey(CONTENT_HEADER_ENDDATE)) {
                    codeScheme.setEndDate(parseEndDateFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ENDDATE))), String.valueOf(row.getRowNum())));
                }
                if (headerMap.containsKey(CONTENT_HEADER_EXTENSIONSCHEMESSHEET)) {
                    final String extensionSchemesSheetName = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_EXTENSIONSCHEMESSHEET)));
                    if (extensionSchemesSheetName != null && !extensionSchemesSheetName.isEmpty()) {
                        extensionSchemesSheetNames.put(codeScheme, extensionSchemesSheetName);
                    }
                }
                if (headerMap.containsKey(CONTENT_HEADER_CODESSHEET)) {
                    final String codesSheetName = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODESSHEET)));
                    if (codesSheetName != null && !codesSheetName.isEmpty()) {
                        codesSheetNames.put(codeScheme, codesSheetName);
                    }
                }
                validateStartDateIsBeforeEndDate(codeScheme);
                codeSchemes.add(codeScheme);
            }
        }
        return codeSchemes;
    }

    private Set<CodeDTO> resolveDataClassificationsFromString(final String dataClassificationCodes) {
        final Set<CodeDTO> classifications = new HashSet<>();
        if (dataClassificationCodes == null || dataClassificationCodes.isEmpty()) {
            throw new BadClassificationException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_BAD_CLASSIFICATION));
        }
        final List<String> codes = Arrays.asList(dataClassificationCodes.split(";"));
        codes.forEach(code -> {
            if (!code.isEmpty()) {
                final CodeDTO classification = new CodeDTO();
                classification.setCodeValue(code);
                classifications.add(classification);
            }
        });
        return classifications;
    }

    private void validateStartDateIsBeforeEndDate(final CodeSchemeDTO codeScheme) {
        if (!startDateIsBeforeEndDateSanityCheck(codeScheme.getStartDate(), codeScheme.getEndDate())) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_END_BEFORE_START_DATE));
        }
    }

    private void validateRequiredDataOnRow(final Row row,
                                           final Map<String, Integer> headerMap,
                                           final DataFormatter formatter) {
        if (formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODEVALUE))) == null ||
            formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODEVALUE))).isEmpty()) {
            throw new MissingRowValueCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_CODEVALUE, String.valueOf(row.getRowNum() + 1)));
        }
        if (formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STATUS))) == null ||
            formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STATUS))).isEmpty()) {
            throw new MissingRowValueStatusException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_STATUS, String.valueOf(row.getRowNum() + 1)));
        }
    }

    private void validateRequiredDataOnRecord(final CSVRecord record) {
        if (record.get(CONTENT_HEADER_CODEVALUE) == null || record.get(CONTENT_HEADER_CODEVALUE).isEmpty()) {
            throw new MissingRowValueCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_CODEVALUE, String.valueOf(record.getRecordNumber() + 1)));
        }
        if (record.get(CONTENT_HEADER_STATUS) == null || record.get(CONTENT_HEADER_STATUS).isEmpty()) {
            throw new MissingRowValueStatusException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_STATUS, String.valueOf(record.getRecordNumber() + 1)));
        }
    }

    private void validateRequiredHeaders(final Map<String, Integer> headerMap) {
        if (!headerMap.containsKey(CONTENT_HEADER_CODEVALUE)) {
            throw new MissingHeaderCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_MISSING_HEADER_CODEVALUE));
        }
        if (!headerMap.containsKey(CONTENT_HEADER_CLASSIFICATION)) {
            throw new MissingHeaderClassificationException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_MISSING_HEADER_CLASSIFICATION));
        }
        if (!headerMap.containsKey(CONTENT_HEADER_STATUS)) {
            throw new MissingHeaderStatusException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_MISSING_HEADER_STATUS));
        }
    }

    private String parseVersionFromCsvRecord(final CSVRecord record) {
        return parseStringFromCsvRecord(record, CONTENT_HEADER_VERSION);
    }

    private String parseLegalBaseFromCsvRecord(final CSVRecord record) {
        return parseStringFromCsvRecord(record, CONTENT_HEADER_LEGALBASE);
    }

    private String parseDefaultCodeFromCsvRecord(final CSVRecord record) {
        return parseStringFromCsvRecord(record, CONTENT_HEADER_DEFAULTCODE);
    }

    private String parseGovernancePolicyFromCsvRecord(final CSVRecord record) {
        return parseStringFromCsvRecord(record, CONTENT_HEADER_GOVERNANCEPOLICY);
    }

    private String parseSourceFromCsvRecord(final CSVRecord record) {
        return parseStringFromCsvRecord(record, CONTENT_HEADER_SOURCE);
    }
}
