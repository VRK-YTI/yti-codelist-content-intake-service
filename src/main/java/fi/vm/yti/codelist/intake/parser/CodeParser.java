package fi.vm.yti.codelist.intake.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import org.apache.poi.POIXMLException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.intake.exception.CodeParsingException;
import fi.vm.yti.codelist.intake.exception.CsvParsingException;
import fi.vm.yti.codelist.intake.exception.ExcelParsingException;
import fi.vm.yti.codelist.intake.exception.JsonParsingException;
import fi.vm.yti.codelist.intake.exception.MissingHeaderCodeValueException;
import fi.vm.yti.codelist.intake.exception.MissingHeaderStatusException;
import fi.vm.yti.codelist.intake.exception.MissingRowValueCodeValueException;
import fi.vm.yti.codelist.intake.exception.MissingRowValueStatusException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.ErrorModel;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Service
public class CodeParser extends AbstractBaseParser {

    private static final Logger LOG = LoggerFactory.getLogger(CodeParser.class);
    private int maxFlatValue = 0;

    public Set<CodeDTO> parseCodesFromCsvInputStream(final InputStream inputStream,
                                                     final Map<String, String> broaderCodeMapping) {
        final Set<CodeDTO> codes = new HashSet<>();
        final Set<String> codeValues = new HashSet<>();
        final List<String> codeValuelist = new ArrayList<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
            final Map<String, Integer> headerMap = csvParser.getHeaderMap();
            final Map<String, Integer> prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
            final Map<String, Integer> definitionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DEFINITION_PREFIX);
            final Map<String, Integer> descriptionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DESCRIPTION_PREFIX);
            validateRequiredCodeHeaders(headerMap);
            String previousBroader = null;
            int childOrderIndex = 1;
            final List<CSVRecord> records = csvParser.getRecords();
            for (final CSVRecord record : records) {
                String broaderCode = null;
                validateRequiredDataOnRecord(record);
                final CodeDTO code = new CodeDTO();
                code.setId(parseIdFromRecord(record));
                final String codeValue = parseCodeValueFromRecord(record);
                validateCodeCodeValue(codeValue);
                checkForDuplicateCodeValueInImportData(codeValues, codeValue);
                codeValues.add(codeValue);
                codeValuelist.add(codeValue);
                code.setCodeValue(codeValue);
                code.setPrefLabel(parseLocalizedValueFromCsvRecord(prefLabelHeaders, record));
                code.setDefinition(parseLocalizedValueFromCsvRecord(definitionHeaders, record));
                code.setDescription(parseLocalizedValueFromCsvRecord(descriptionHeaders, record));
                code.setShortName(parseShortNameFromCsvRecord(record));
                code.setFlatOrder(resolveFlatOrderFromCsvRecord(record));
                if (record.isMapped(CONTENT_HEADER_BROADER)) {
                    final String broaderCodeCodeValue = record.get(CONTENT_HEADER_BROADER);
                    if (broaderCodeCodeValue != null && !broaderCodeCodeValue.isEmpty()) {
                        broaderCodeMapping.put(codeValue.toLowerCase(), broaderCodeCodeValue.toLowerCase());
                        broaderCode = broaderCodeCodeValue;
                    } else {
                        broaderCodeMapping.put(codeValue.toLowerCase(), null);
                    }
                }
                code.setHierarchyLevel(resolveHierarchyLevelFromCsvRecord(record));
                code.setStatus(parseStatusValueFromString(record.get(CONTENT_HEADER_STATUS)));
                if (record.isMapped(CONTENT_HEADER_STARTDATE)) {
                    code.setStartDate(parseStartDateFromString(parseStartDateStringFromCsvRecord(record), String.valueOf(record.getRecordNumber() + 1)));
                }
                if (record.isMapped(CONTENT_HEADER_ENDDATE)) {
                    code.setEndDate(parseEndDateFromString(parseEndDateStringFromCsvRecord(record), String.valueOf(record.getRecordNumber() + 1)));
                }
                validateStartDateIsBeforeEndDate(code);

                final Integer intChildOrder = parseChildOrderFromCsvRecord(record);
                if (codeValues.contains(broaderCode)) {
                    String parentCode = codeValuelist.get(codeValuelist.indexOf(code.getCodeValue()));
                    if (parentCode != null && !parentCode.isEmpty()) {
                        if (broaderCode != null && previousBroader != null && broaderCode.equalsIgnoreCase(previousBroader)) {
                            code.setChildOrder(childOrderIndex);
                            childOrderIndex++;
                        } else {
                            childOrderIndex = 1;
                        }
                    }
                } else {
                    code.setChildOrder(intChildOrder);
                }
                previousBroader = broaderCode;
                codes.add(code);
            }
        } catch (final IllegalArgumentException e) {
            LOG.error("Duplicate header value found in CSV!", e);
            throw new CsvParsingException(ERR_MSG_USER_DUPLICATE_HEADER_VALUE);
        } catch (final IOException e) {
            LOG.error("Error parsing CSV file!", e);
            throw new CsvParsingException(ERR_MSG_USER_ERROR_PARSING_CSV_FILE);
        }
        return codes;
    }

    public Set<CodeDTO> parseCodesFromExcelInputStream(final InputStream inputStream,
                                                       final Map<String, String> broaderCodeMapping) {
        try (final Workbook workbook = WorkbookFactory.create(inputStream)) {
            return parseCodesFromExcelWorkbook(workbook, broaderCodeMapping);
        } catch (final InvalidFormatException | IOException | POIXMLException e) {
            LOG.error("Error parsing Excel file!", e);
            throw new ExcelParsingException(ERR_MSG_USER_ERROR_PARSING_EXCEL_FILE);
        }
    }

    @SuppressFBWarnings("UC_USELESS_OBJECT")
    public Set<CodeDTO> parseCodesFromExcelWorkbook(final Workbook workbook,
                                                    final Map<String, String> broaderCodeMapping) {
        final Set<CodeDTO> codes = new HashSet<>();
        final Set<String> codeValues = new HashSet<>();
        final DataFormatter formatter = new DataFormatter();
        Sheet sheet = workbook.getSheet(EXCEL_SHEET_CODES);
        if (sheet == null) {
            sheet = workbook.getSheetAt(0);
        }
        final Iterator<Row> rowIterator = sheet.rowIterator();
        boolean firstRow = true;
        Map<String, Integer> headerMap = null;
        Map<String, Integer> prefLabelHeaders = null;
        Map<String, Integer> definitionHeaders = null;
        Map<String, Integer> descriptionHeaders = null;
        while (rowIterator.hasNext()) {
            final Row row = rowIterator.next();
            if (firstRow) {
                firstRow = false;
                headerMap = resolveHeaderMap(row);
                prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
                definitionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DEFINITION_PREFIX);
                descriptionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DESCRIPTION_PREFIX);
                validateRequiredCodeHeaders(headerMap);
            } else if (row.getPhysicalNumberOfCells() > 0 && !isRowEmpty(row)) {
                validateRequiredDataOnRow(row, headerMap, formatter);
                final CodeDTO code = new CodeDTO();
                final String codeValue = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODEVALUE)));
                if (codeValue == null || codeValue.trim().isEmpty()) {
                    continue;
                }
                validateCodeCodeValue(codeValue);
                checkForDuplicateCodeValueInImportData(codeValues, codeValue);
                codeValues.add(codeValue);
                code.setCodeValue(codeValue);
                if (headerMap.containsKey(CONTENT_HEADER_ID)) {
                    code.setId(parseUUIDFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ID)))));
                }
                code.setPrefLabel(parseLocalizedValueFromExcelRow(prefLabelHeaders, row, formatter));
                code.setDefinition(parseLocalizedValueFromExcelRow(definitionHeaders, row, formatter));
                code.setDescription(parseLocalizedValueFromExcelRow(descriptionHeaders, row, formatter));
                code.setShortName(parseShortNameFromExcelRow(headerMap, row, formatter));
                code.setHierarchyLevel(resolveHierarchyLevelFromExcelRow(headerMap, row, formatter));
                code.setFlatOrder(resolveFlatOrderFromExcelRow(headerMap, row, formatter));
                code.setChildOrder(resolveChildOrderFromExcelRow(headerMap, row, formatter));
                if (headerMap.containsKey(CONTENT_HEADER_BROADER)) {
                    final String broaderCodeCodeValue = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_BROADER)));
                    if (broaderCodeCodeValue != null && !broaderCodeCodeValue.isEmpty()) {
                        broaderCodeMapping.put(codeValue.toLowerCase(), broaderCodeCodeValue.toLowerCase());
                    } else {
                        broaderCodeMapping.put(codeValue.toLowerCase(), null);
                    }
                }
                code.setStatus(parseStatusValueFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STATUS)))));
                code.setStartDate(parseStartDateFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STARTDATE))), String.valueOf(row.getRowNum() + 1)));
                code.setEndDate(parseEndDateFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ENDDATE))), String.valueOf(row.getRowNum() + 1)));
                validateStartDateIsBeforeEndDate(code);
                codes.add(code);
            }
        }
        return codes;
    }

    public CodeDTO parseCodeFromJsonData(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final CodeDTO code;
        try {
            code = mapper.readValue(jsonPayload, CodeDTO.class);
            validateStartDateIsBeforeEndDate(code);
        } catch (final IOException e) {
            LOG.error("Code parsing failed from JSON!", e);
            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
        }
        return code;
    }

    public Set<CodeDTO> parseCodesFromJsonData(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final Set<CodeDTO> codes;
        final Set<String> codeValues = new HashSet<>();
        try {
            codes = mapper.readValue(jsonPayload, new TypeReference<Set<CodeDTO>>() {
            });
            for (final CodeDTO code : codes) {
                checkForDuplicateCodeValueInImportData(codeValues, code.getCodeValue());
                validateStartDateIsBeforeEndDate(code);
                codeValues.add(code.getCodeValue());
            }
        } catch (final IOException e) {
            LOG.error("Codes parsing failed from JSON!", e);
            throw new JsonParsingException(ERR_MSG_USER_406);
        }
        return codes;
    }

    private Integer resolveHierarchyLevelFromCsvRecord(final CSVRecord record) {
        final Integer hierarchyLevel;
        if (record.isMapped(CONTENT_HEADER_HIERARCHYLEVEL) && !record.isMapped(CONTENT_HEADER_BROADER)) {
            hierarchyLevel = resolveHierarchyLevelFromString(record.get(CONTENT_HEADER_HIERARCHYLEVEL));
        } else {
            hierarchyLevel = 1;
        }
        return hierarchyLevel;
    }

    private Integer resolveHierarchyLevelFromExcelRow(final Map<String, Integer> headerMap,
                                                      final Row row,
                                                      final DataFormatter formatter) {
        final Integer hierarchyLevel;
        if (headerMap.containsKey(CONTENT_HEADER_HIERARCHYLEVEL) && !headerMap.containsKey(CONTENT_HEADER_BROADER)) {
            hierarchyLevel = resolveHierarchyLevelFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_HIERARCHYLEVEL))));
        } else {
            hierarchyLevel = 1;
        }
        return hierarchyLevel;
    }

    private Integer resolveHierarchyLevelFromString(final String hierarchyLevelString) {
        final Integer hierarchyLevel;
        if (!hierarchyLevelString.isEmpty()) {
            try {
                hierarchyLevel = Integer.parseInt(hierarchyLevelString);
            } catch (final NumberFormatException e) {
                LOG.error("Error parsing hierarchyLevel value from: " + hierarchyLevelString, e);
                throw new CodeParsingException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    ERR_MSG_USER_HIERARCHY_LEVEL_INVALID_VALUE));
            }
        } else {
            hierarchyLevel = 1;
        }
        return hierarchyLevel;
    }

    private Integer resolveFlatOrderFromCsvRecord(final CSVRecord record) {
        Integer flatOrder;
        if (record.isMapped(CONTENT_HEADER_FLATORDER)) {
            flatOrder = resolveFlatOrderFromString(record.get(CONTENT_HEADER_FLATORDER));
            if (flatOrder != null && (maxFlatValue > flatOrder)) {
                maxFlatValue = flatOrder;
            } else {
                flatOrder = ++maxFlatValue;
            }
        } else {
            flatOrder = ++maxFlatValue;
        }
        return flatOrder;
    }

    private Integer resolveFlatOrderFromExcelRow(final Map<String, Integer> headerMap,
                                                 final Row row,
                                                 final DataFormatter formatter) {
        Integer flatOrder;
        if (headerMap.containsKey(CONTENT_HEADER_FLATORDER)) {
            flatOrder = resolveFlatOrderFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_FLATORDER))));
            if (flatOrder != null && (flatOrder > maxFlatValue)) {
                maxFlatValue = flatOrder;
            } else {
                flatOrder = ++maxFlatValue;
            }
        } else {
            flatOrder = ++maxFlatValue;
        }
        return flatOrder;
    }

    private Integer resolveChildOrderFromExcelRow(final Map<String, Integer> headerMap,
                                                  final Row row,
                                                  final DataFormatter formatter) {
        final Integer order;
        if (headerMap.containsKey(CONTENT_HEADER_CHILDORDER)) {
            order = resolveChildOrderFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CHILDORDER))));
        } else {
            if (headerMap.containsKey(CONTENT_HEADER_BROADER)) {
                final String broader = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_BROADER)));
                if (broader == null || broader.isEmpty()) {
                    order = 1;
                } else order = null; // TODO get the actual childorder
            } else order = null;
        }
        return order;
    }

    private Integer resolveFlatOrderFromString(final String flatOrderString) {
        final Integer flatOrder;
        if (!flatOrderString.isEmpty()) {
            try {
                flatOrder = Integer.parseInt(flatOrderString);
            } catch (final NumberFormatException e) {
                LOG.error("Error parsing flatOrder from: " + flatOrderString, e);
                throw new CodeParsingException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    ERR_MSG_USER_FLAT_ORDER_INVALID_VALUE));
            }
        } else {
            flatOrder = null;
        }
        return flatOrder;
    }

    private Integer resolveChildOrderFromString(final String childOrderString) {
        final Integer childOrder;
        if (!childOrderString.isEmpty()) {
            try {
                childOrder = Integer.parseInt(childOrderString);
            } catch (final NumberFormatException e) {
                throw new CodeParsingException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    ERR_MSG_USER_CHILD_ORDER_INVALID_VALUE));
            }
        } else {
            childOrder = null;
        }
        return childOrder;
    }

    private void validateRequiredCodeHeaders(final Map<String, Integer> headerMap) {
        if (!headerMap.containsKey(CONTENT_HEADER_CODEVALUE)) {
            throw new MissingHeaderCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_MISSING_HEADER_CODEVALUE));
        }
        if (!headerMap.containsKey(CONTENT_HEADER_STATUS)) {
            throw new MissingHeaderStatusException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_MISSING_HEADER_STATUS));
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
        // This try-catch block prevents an ugly and large vomit of non-UTF-8 characters from polluting the log.
        // The problem emanates from Apache's CVSRecord itself so hacking around it for now like this.
        // This problem happens for example when the user specifies CSV but gives Excel with certain kind of data.
        try {
            record.get(CONTENT_HEADER_ID);
        } catch (final IllegalArgumentException e) {
            LOG.error("ID header not found on CSV file!", e);
            throw new CsvParsingException(ERR_MSG_USER_ERROR_PARSING_CSV_FILE);
        }
        if (record.get(CONTENT_HEADER_CODEVALUE) == null || record.get(CONTENT_HEADER_CODEVALUE).isEmpty()) {
            LOG.error("CODEVALUE header not found or value empty in CSV file!");
            throw new MissingRowValueCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_CODEVALUE, String.valueOf(record.getRecordNumber() + 1)));
        }
        if (record.get(CONTENT_HEADER_STATUS) == null || record.get(CONTENT_HEADER_STATUS).isEmpty()) {
            LOG.error("STATUS header not found or value empty in CSV file!");
            throw new MissingRowValueStatusException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_STATUS, String.valueOf(record.getRecordNumber() + 1)));
        }
    }

    private void validateStartDateIsBeforeEndDate(final CodeDTO code) {
        if (!startDateIsBeforeEndDateSanityCheck(code.getStartDate(), code.getEndDate())) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_END_BEFORE_START_DATE));
        }
    }

    private String parseShortNameFromCsvRecord(final CSVRecord record) {
        return parseStringFromCsvRecord(record, CONTENT_HEADER_SHORTNAME);
    }

    private Integer parseChildOrderFromCsvRecord(final CSVRecord record) {
        final String childOrderString = parseStringFromCsvRecord(record, CONTENT_HEADER_CHILDORDER);
        if (childOrderString != null && !childOrderString.isEmpty()) {
            try {
                return Integer.parseInt(childOrderString);
            } catch (final NumberFormatException e) {
                LOG.error("Child order parsing failed from: " + childOrderString, e);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CHILD_ORDER_INVALID_VALUE));
            }
        } else {
            final String broader = parseStringFromCsvRecord(record, CONTENT_HEADER_BROADER);
            if (broader == null || broader.isEmpty())
                return 1;
        }
        return null;
    }

    private String parseShortNameFromExcelRow(final Map<String, Integer> genericHeaders,
                                              final Row row,
                                              final DataFormatter formatter) {
        final String shortName;
        if (genericHeaders.get(CONTENT_HEADER_SHORTNAME) != null) {
            shortName = formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_SHORTNAME)));
        } else {
            shortName = null;
        }
        return shortName;
    }
}
