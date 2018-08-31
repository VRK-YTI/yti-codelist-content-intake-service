package fi.vm.yti.codelist.intake.parser.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.intake.exception.CsvParsingException;
import fi.vm.yti.codelist.intake.exception.ExcelParsingException;
import fi.vm.yti.codelist.intake.exception.JsonParsingException;
import fi.vm.yti.codelist.intake.exception.MissingHeaderClassificationException;
import fi.vm.yti.codelist.intake.exception.MissingHeaderCodeValueException;
import fi.vm.yti.codelist.intake.exception.MissingRowValueCodeValueException;
import fi.vm.yti.codelist.intake.exception.MissingRowValueStatusException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.parser.ExtensionSchemeParser;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Component
public class ExtensionSchemeParserImpl extends AbstractBaseParser implements ExtensionSchemeParser {

    private static final Logger LOG = LoggerFactory.getLogger(ExtensionSchemeParserImpl.class);

    public ExtensionSchemeDTO parseExtensionSchemeFromJson(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final ExtensionSchemeDTO extensionScheme;
        try {
            extensionScheme = mapper.readValue(jsonPayload, ExtensionSchemeDTO.class);
            validateStartDateIsBeforeEndDate(extensionScheme);
        } catch (final IOException e) {
            LOG.error("ExtensionScheme parsing failed from JSON!", e);
            throw new JsonParsingException(ERR_MSG_USER_406);
        }
        return extensionScheme;
    }

    @Override
    public Set<ExtensionSchemeDTO> parseExtensionSchemesFromJson(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final Set<ExtensionSchemeDTO> extensionSchemes;
        try {
            extensionSchemes = mapper.readValue(jsonPayload, new TypeReference<Set<ExtensionSchemeDTO>>() {
            });
        } catch (final IOException e) {
            LOG.error("ExtensionSchemes parsing failed from JSON!", e);
            throw new JsonParsingException(ERR_MSG_USER_406);
        }
        extensionSchemes.forEach(this::validateStartDateIsBeforeEndDate);
        return extensionSchemes;
    }

    @Override
    @SuppressFBWarnings("UC_USELESS_OBJECT")
    public Set<ExtensionSchemeDTO> parseExtensionSchemesFromCsvInputStream(final InputStream inputStream) {
        final Set<ExtensionSchemeDTO> extensionSchemes = new HashSet<>();
        final Set<String> codeValues = new HashSet<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
            final Map<String, Integer> headerMap = csvParser.getHeaderMap();
            final Map<String, Integer> prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
            validateRequiredHeaders(headerMap);
            final List<CSVRecord> records = csvParser.getRecords();
            for (final CSVRecord record : records) {
                validateRequiredDataOnRecord(record);
                final ExtensionSchemeDTO extensionScheme = new ExtensionSchemeDTO();
                extensionScheme.setId(parseIdFromRecord(record));
                final String codeValue = parseCodeValueFromRecord(record);
                validateCodeValue(codeValue);
                codeValues.add(codeValue.toLowerCase());
                extensionScheme.setCodeValue(codeValue);
                extensionScheme.setId(parseIdFromRecord(record));
                checkForDuplicateCodeValueInImportData(codeValues, codeValue);
                extensionScheme.setPrefLabel(parseLocalizedValueFromCsvRecord(prefLabelHeaders, record));
                if (headerMap.containsKey(CONTENT_HEADER_CODESCHEMES)) {
                    final Set<CodeSchemeDTO> codeSchemes = new HashSet<>();
                    final List<String> uris = parseCodeSchemesFromRecord(record);
                    uris.forEach(uri -> {
                        final CodeSchemeDTO codeScheme = new CodeSchemeDTO();
                        codeScheme.setUri(uri);
                        codeSchemes.add(codeScheme);
                    });
                    extensionScheme.setCodeSchemes(codeSchemes);
                }
                if (record.isMapped(CONTENT_HEADER_STARTDATE)) {
                    extensionScheme.setStartDate(parseStartDateFromString(parseStartDateStringFromCsvRecord(record), String.valueOf(record.getRecordNumber() + 1)));
                }
                if (record.isMapped(CONTENT_HEADER_ENDDATE)) {
                    extensionScheme.setEndDate(parseEndDateFromString(parseEndDateStringFromCsvRecord(record), String.valueOf(record.getRecordNumber() + 1)));
                }
                validateStartDateIsBeforeEndDate(extensionScheme);
                extensionScheme.setStatus(parseStatusValueFromString(record.get(CONTENT_HEADER_STATUS)));
                final String propertyTypeLocalName = record.get(CONTENT_HEADER_PROPERTYTYPE);
                final PropertyTypeDTO propertyType = new PropertyTypeDTO();
                propertyType.setLocalName(propertyTypeLocalName);
                extensionScheme.setPropertyType(propertyType);
                extensionSchemes.add(extensionScheme);
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

    @Override
    public Set<ExtensionSchemeDTO> parseExtensionSchemesFromExcelInputStream(final InputStream inputStream,
                                                                             final String sheetName,
                                                                             final Map<ExtensionSchemeDTO, String> extensionsSheetNames) {
        try (final Workbook workbook = WorkbookFactory.create(inputStream)) {
            return parseExtensionSchemesFromExcelWorkbook(workbook, sheetName, extensionsSheetNames);
        } catch (final InvalidFormatException | IOException | POIXMLException e) {
            LOG.error("Error parsing Excel file!", e);
            throw new ExcelParsingException(ERR_MSG_USER_ERROR_PARSING_EXCEL_FILE);
        }
    }

    @Override
    public Set<ExtensionSchemeDTO> parseExtensionSchemesFromExcelWorkbook(final Workbook workbook,
                                                                          final String sheetName,
                                                                          final Map<ExtensionSchemeDTO, String> extensionsSheetNames) {
        final Set<ExtensionSchemeDTO> extensionsSchemes = new HashSet<>();
        final Set<String> codeValues = new HashSet<>();
        final DataFormatter formatter = new DataFormatter();
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            sheet = workbook.getSheetAt(0);
        }
        final Iterator<Row> rowIterator = sheet.rowIterator();
        Map<String, Integer> headerMap = null;
        Map<String, Integer> prefLabelHeaders = null;
        boolean firstRow = true;
        while (rowIterator.hasNext()) {
            final Row row = rowIterator.next();
            if (firstRow) {
                firstRow = false;
                headerMap = resolveHeaderMap(row);
                prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
                validateRequiredHeaders(headerMap);
            } else {
                final ExtensionSchemeDTO extensionScheme = new ExtensionSchemeDTO();
                final String codeValue = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODEVALUE))).trim();
                final String status = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STATUS)));
                if (skipEmptyLine(codeValue, status)) {
                    continue;
                }
                validateRequiredDataOnRow(row, headerMap, formatter);
                validateCodeValue(codeValue);
                checkForDuplicateCodeValueInImportData(codeValues, codeValue);
                codeValues.add(codeValue.toLowerCase());
                extensionScheme.setCodeValue(codeValue);
                extensionScheme.setStatus(parseStatusValueFromString(status));
                if (headerMap.containsKey(CONTENT_HEADER_ID)) {
                    extensionScheme.setId(parseUUIDFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ID)))));
                }
                if (headerMap.containsKey(CONTENT_HEADER_CODESCHEMES)) {
                    final Set<CodeSchemeDTO> codeSchemes = new HashSet<>();
                    final List<String> uris = Arrays.asList(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODESCHEMES))).split(";"));
                    uris.forEach(uri -> {
                        if (!uri.isEmpty()) {
                            final CodeSchemeDTO codeScheme = new CodeSchemeDTO();
                            codeScheme.setUri(uri);
                            codeSchemes.add(codeScheme);
                        }
                    });
                    extensionScheme.setCodeSchemes(codeSchemes);
                }
                extensionScheme.setPrefLabel(parseLocalizedValueFromExcelRow(prefLabelHeaders, row, formatter));
                if (headerMap.containsKey(CONTENT_HEADER_STARTDATE)) {
                    extensionScheme.setStartDate(parseStartDateFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STARTDATE))), String.valueOf(row.getRowNum())));
                }
                if (headerMap.containsKey(CONTENT_HEADER_ENDDATE)) {
                    extensionScheme.setEndDate(parseEndDateFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ENDDATE))), String.valueOf(row.getRowNum())));
                }
                validateStartDateIsBeforeEndDate(extensionScheme);
                final String propertyTypeLocalName = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_PROPERTYTYPE)));
                final PropertyTypeDTO propertyType = new PropertyTypeDTO();
                propertyType.setLocalName(propertyTypeLocalName);
                extensionScheme.setPropertyType(propertyType);
                if (headerMap.containsKey(CONTENT_HEADER_EXTENSIONSSHEET)) {
                    final String extensionsSheetName = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_EXTENSIONSSHEET)));
                    if (extensionsSheetName != null && !extensionsSheetName.isEmpty()) {
                        extensionsSheetNames.put(extensionScheme, extensionsSheetName);
                    }
                }
                extensionsSchemes.add(extensionScheme);
            }
        }
        return extensionsSchemes;
    }

    private void validateRequiredDataOnRow(final Row row,
                                           final Map<String, Integer> headerMap,
                                           final DataFormatter formatter) {
        if (formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODEVALUE))) == null ||
            formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODEVALUE))).isEmpty()) {
            throw new MissingRowValueCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_CODEVALUE, String.valueOf(row.getRowNum() + 1)));
        }
    }

    private void validateRequiredDataOnRecord(final CSVRecord record) {
        if (record.get(CONTENT_HEADER_CODEVALUE) == null || record.get(CONTENT_HEADER_CODEVALUE).isEmpty()) {
            throw new MissingRowValueCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_CODEVALUE, String.valueOf(record.getRecordNumber() + 1)));
        }
        if (record.get(CONTENT_HEADER_PROPERTYTYPE) == null || record.get(CONTENT_HEADER_PROPERTYTYPE).isEmpty()) {
            throw new MissingRowValueStatusException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_PROPERTYTYPE, String.valueOf(record.getRecordNumber() + 1)));
        }
    }

    private void validateRequiredHeaders(final Map<String, Integer> headerMap) {
        if (!headerMap.containsKey(CONTENT_HEADER_CODEVALUE)) {
            throw new MissingHeaderCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_MISSING_HEADER_CODEVALUE));
        }
        if (!headerMap.containsKey(CONTENT_HEADER_PROPERTYTYPE)) {
            throw new MissingHeaderClassificationException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_MISSING_HEADER_PROPERTYTYPE));
        }
    }

    private void validateStartDateIsBeforeEndDate(final ExtensionSchemeDTO extensionScheme) {
        if (!startDateIsBeforeEndDateSanityCheck(extensionScheme.getStartDate(), extensionScheme.getEndDate())) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_END_BEFORE_START_DATE));
        }
    }

    private List<String> parseCodeSchemesFromRecord(final CSVRecord record) {
        final List<String> codeSchemeUris = new ArrayList<>();
        if (record.isMapped(CONTENT_HEADER_CODESCHEMES)) {
            codeSchemeUris.addAll(Arrays.asList(parseStringFromCsvRecord(record, CONTENT_HEADER_CODESCHEMES).split(";")));
        }
        return codeSchemeUris;
    }
}
