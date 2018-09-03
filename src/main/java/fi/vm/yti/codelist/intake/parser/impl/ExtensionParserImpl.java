package fi.vm.yti.codelist.intake.parser.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.intake.exception.CsvParsingException;
import fi.vm.yti.codelist.intake.exception.ExcelParsingException;
import fi.vm.yti.codelist.intake.exception.JsonParsingException;
import fi.vm.yti.codelist.intake.exception.MissingHeaderCodeValueException;
import fi.vm.yti.codelist.intake.exception.MissingRowValueCodeValueException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;
import fi.vm.yti.codelist.intake.parser.ExtensionParser;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Component
public class ExtensionParserImpl extends AbstractBaseParser implements ExtensionParser {

    private static final Logger LOG = LoggerFactory.getLogger(ExtensionParserImpl.class);
    private static final String TYPE_CALCULATION_HIERARCHY = "calculationHierarchy";

    public ExtensionDTO parseExtensionFromJson(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final ExtensionDTO extension;
        try {
            extension = mapper.readValue(jsonPayload, ExtensionDTO.class);
        } catch (final IOException e) {
            LOG.error("Extension parsing failed from JSON!", e);
            throw new JsonParsingException(ERR_MSG_USER_406);
        }
        return extension;
    }

    public Set<ExtensionDTO> parseExtensionsFromJson(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final Set<ExtensionDTO> extensions;
        try {
            extensions = mapper.readValue(jsonPayload, new TypeReference<Set<ExtensionDTO>>() {
            });
        } catch (final IOException e) {
            LOG.error("Extension parsing failed from JSON!", e);
            throw new JsonParsingException(ERR_MSG_USER_406);
        }
        return extensions;
    }

    @SuppressFBWarnings("UC_USELESS_OBJECT")
    public Set<ExtensionDTO> parseExtensionsFromCsvInputStream(final ExtensionScheme extensionScheme,
                                                               final InputStream inputStream) {
        final boolean requiresExtensionValue = hasExtensionValue(extensionScheme);
        final Set<ExtensionDTO> extensionSchemes = new LinkedHashSet<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
            final Map<String, Integer> headerMap = csvParser.getHeaderMap();
            final Map<String, Integer> prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
            validateRequiredHeaders(requiresExtensionValue, headerMap);
            final List<CSVRecord> records = csvParser.getRecords();
            for (final CSVRecord record : records) {
                validateRequiredDataOnRecord(requiresExtensionValue, record);
                final ExtensionDTO extension = new ExtensionDTO();
                extension.setId(parseIdFromRecord(record));
                extension.setOrder(resolveOrderFromCsvRecord(record));
                extension.setPrefLabel(parseLocalizedValueFromCsvRecord(prefLabelHeaders, record));
                if (requiresExtensionValue) {
                    extension.setExtensionValue(parseExtensionValueFromCsvRecord(record));
                }
                extension.setCode(createCodeUsingIdentifier(parseCodeIdentifierFromCsvRecord(record)));
                final String relationCodeValue = parseExtensionRelationFromCsvRecord(record);
                if (relationCodeValue != null) {
                    extension.setExtension(createExtensionWithCodeValue(relationCodeValue));
                }
                if (record.isMapped(CONTENT_HEADER_STARTDATE)) {
                    extension.setStartDate(parseStartDateFromString(parseStartDateStringFromCsvRecord(record), String.valueOf(record.getRecordNumber() + 1)));
                }
                if (record.isMapped(CONTENT_HEADER_ENDDATE)) {
                    extension.setEndDate(parseEndDateFromString(parseEndDateStringFromCsvRecord(record), String.valueOf(record.getRecordNumber() + 1)));
                }
                validateStartDateIsBeforeEndDate(extension);
                extensionSchemes.add(extension);
            }
        } catch (final IllegalArgumentException e) {
            LOG.error("Duplicate header value found in CSV!", e);
            throw new CsvParsingException(ERR_MSG_USER_DUPLICATE_HEADER_VALUE);
        } catch (final IOException e) {
            LOG.error("Error parsing CSV file!", e);
            throw new CsvParsingException(ERR_MSG_USER_ERROR_PARSING_CSV_FILE);
        }
        return extensionSchemes;
    }

    private ExtensionDTO createExtensionWithCodeValue(final String codeValue) {
        final ExtensionDTO refExtension = new ExtensionDTO();
        final CodeDTO refCode = new CodeDTO();
        refCode.setCodeValue(codeValue);
        refExtension.setCode(refCode);
        return refExtension;
    }

    public Set<ExtensionDTO> parseExtensionsFromExcelInputStream(final ExtensionScheme extensionScheme,
                                                                 final InputStream inputStream,
                                                                 final String sheetName) {
        try (final Workbook workbook = WorkbookFactory.create(inputStream)) {
            return parseExtensionsFromExcelWorkbook(extensionScheme, workbook, sheetName);
        } catch (final InvalidFormatException | IOException | POIXMLException e) {
            LOG.error("Error parsing Excel file!", e);
            throw new ExcelParsingException(ERR_MSG_USER_ERROR_PARSING_EXCEL_FILE);
        }
    }

    private boolean hasExtensionValue(final ExtensionScheme extensionScheme) {
        return extensionScheme.getPropertyType().getLocalName().equalsIgnoreCase(TYPE_CALCULATION_HIERARCHY);
    }

    public Set<ExtensionDTO> parseExtensionsFromExcelWorkbook(final ExtensionScheme extensionScheme,
                                                              final Workbook workbook,
                                                              final String sheetName) {
        final boolean requireExtensionValue = hasExtensionValue(extensionScheme);
        final Set<ExtensionDTO> extensions = new LinkedHashSet<>();
        final DataFormatter formatter = new DataFormatter();
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new ExcelParsingException(ERR_MSG_USER_ERROR_PARSING_EXCEL_FILE_SHEET_NOT_FOUND);
        }
        final Iterator<Row> rowIterator = sheet.rowIterator();
        Map<String, Integer> headerMap = null;
        Map<String, Integer> prefLabelHeaders = null;
        boolean firstRow = true;
        while (rowIterator.hasNext()) {
            final Row row = rowIterator.next();
            if (row == null) {
                continue;
            }
            if (firstRow) {
                firstRow = false;
                headerMap = resolveHeaderMap(row);
                prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
                validateRequiredHeaders(requireExtensionValue, headerMap);
            } else {
                final ExtensionDTO extension = new ExtensionDTO();
                final String codeIdentifier = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODE)));
                validateRequiredDataOnRow(requireExtensionValue, row, headerMap, formatter);
                extension.setCode(createCodeUsingIdentifier(codeIdentifier));
                if (headerMap.containsKey(CONTENT_HEADER_ID)) {
                    extension.setId(parseUUIDFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ID)))));
                }
                extension.setPrefLabel(parseLocalizedValueFromExcelRow(prefLabelHeaders, row, formatter));
                extension.setOrder(resolveOrderFromExcelRow(headerMap, row, formatter));
                if (requireExtensionValue) {
                    extension.setExtensionValue(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_EXTENSIONVALUE))));
                }
                if (headerMap.containsKey(CONTENT_HEADER_RELATION)) {
                    final String relationCodeValue = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_RELATION)));
                    if (relationCodeValue != null && !relationCodeValue.isEmpty()) {
                        extension.setExtension(createExtensionWithCodeValue(relationCodeValue));
                    }
                }
                if (headerMap.containsKey(CONTENT_HEADER_STARTDATE)) {
                    extension.setStartDate(parseStartDateFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STARTDATE))), String.valueOf(row.getRowNum())));
                }
                if (headerMap.containsKey(CONTENT_HEADER_ENDDATE)) {
                    extension.setEndDate(parseEndDateFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ENDDATE))), String.valueOf(row.getRowNum())));
                }
                validateStartDateIsBeforeEndDate(extension);
                extensions.add(extension);
            }
        }
        return extensions;
    }

    private void validateRequiredDataOnRow(final boolean requireExtensionValue,
                                           final Row row,
                                           final Map<String, Integer> headerMap,
                                           final DataFormatter formatter) {
        if (requireExtensionValue && (formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_EXTENSIONVALUE))) == null ||
            formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_EXTENSIONVALUE))).isEmpty())) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_EXTENSIONVALUE, String.valueOf(row.getRowNum() + 1)));
        }
        if (formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODE))) == null ||
            formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODE))).isEmpty()) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_CODE, String.valueOf(row.getRowNum() + 1)));
        }
    }

    private void validateRequiredDataOnRecord(final boolean requireExtensionValue,
                                              final CSVRecord record) {
        if (requireExtensionValue && (record.get(CONTENT_HEADER_EXTENSIONVALUE) == null || record.get(CONTENT_HEADER_EXTENSIONVALUE).isEmpty())) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_EXTENSIONVALUE, String.valueOf(record.getRecordNumber() + 1)));
        }
        if (record.get(CONTENT_HEADER_CODE) == null || record.get(CONTENT_HEADER_CODE).isEmpty()) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_CODE, String.valueOf(record.getRecordNumber() + 1)));
        }
    }

    private void validateRequiredHeaders(final boolean requiresExtensionValue,
                                         final Map<String, Integer> headerMap) {
        if (requiresExtensionValue && !headerMap.containsKey(CONTENT_HEADER_EXTENSIONVALUE)) {
            throw new MissingHeaderCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_MISSING_HEADER_EXTENSIONVALUE));
        }
        if (!headerMap.containsKey(CONTENT_HEADER_ORDER)) {
            throw new MissingHeaderCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_MISSING_HEADER_ORDER));
        }
        if (!headerMap.containsKey(CONTENT_HEADER_CODE)) {
            throw new MissingHeaderCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_MISSING_HEADER_CODE));
        }
    }

    private String parseCodeIdentifierFromCsvRecord(final CSVRecord record) {
        return parseStringFromCsvRecord(record, CONTENT_HEADER_CODE);
    }

    private CodeDTO createCodeUsingIdentifier(final String identifier) {
        final CodeDTO code = new CodeDTO();
        if (identifier == null || identifier.isEmpty()) {
            throw new MissingRowValueCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_ROW_MISSING_CODE));
        } else if (identifier.startsWith("http://uri.suomi.fi/codelist/")) {
            code.setUri(identifier);
        } else {
            code.setCodeValue(identifier);
        }
        return code;
    }

    private String parseExtensionValueFromCsvRecord(final CSVRecord record) {
        return parseStringFromCsvRecord(record, CONTENT_HEADER_EXTENSIONVALUE);
    }

    private String parseExtensionRelationFromCsvRecord(final CSVRecord record) {
        return parseStringFromCsvRecord(record, CONTENT_HEADER_RELATION);
    }

    private void validateStartDateIsBeforeEndDate(final ExtensionDTO extension) {
        if (!startDateIsBeforeEndDateSanityCheck(extension.getStartDate(), extension.getEndDate())) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_END_BEFORE_START_DATE));
        }
    }
}
