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
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.intake.exception.CsvParsingException;
import fi.vm.yti.codelist.intake.exception.ExcelParsingException;
import fi.vm.yti.codelist.intake.exception.JsonParsingException;
import fi.vm.yti.codelist.intake.exception.MissingHeaderClassificationException;
import fi.vm.yti.codelist.intake.exception.MissingHeaderCodeValueException;
import fi.vm.yti.codelist.intake.exception.MissingRowValueCodeValueException;
import fi.vm.yti.codelist.intake.exception.MissingRowValueStatusException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.parser.ExtensionParser;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Component
public class ExtensionParserImpl extends AbstractBaseParser implements ExtensionParser {

    private static final Logger LOG = LoggerFactory.getLogger(ExtensionParserImpl.class);

    public ExtensionDTO parseExtensionFromJson(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final ExtensionDTO extension;
        try {
            extension = mapper.readValue(jsonPayload, ExtensionDTO.class);
            validateStartDateIsBeforeEndDate(extension);
        } catch (final IOException e) {
            LOG.error("Extension parsing failed from JSON!", e);
            throw new JsonParsingException(ERR_MSG_USER_406);
        }
        return extension;
    }

    @Override
    public Set<ExtensionDTO> parseExtensionsFromJson(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final Set<ExtensionDTO> extensions;
        try {
            extensions = mapper.readValue(jsonPayload, new TypeReference<Set<ExtensionDTO>>() {
            });
        } catch (final IOException e) {
            LOG.error("Extensions parsing failed from JSON!", e);
            throw new JsonParsingException(ERR_MSG_USER_406);
        }
        extensions.forEach(this::validateStartDateIsBeforeEndDate);
        return extensions;
    }

    @Override
    @SuppressFBWarnings("UC_USELESS_OBJECT")
    public Set<ExtensionDTO> parseExtensionsFromCsvInputStream(final InputStream inputStream) {
        final Set<ExtensionDTO> extensions = new HashSet<>();
        final Set<String> codeValues = new HashSet<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
            final Map<String, Integer> headerMap = csvParser.getHeaderMap();
            final Map<String, Integer> prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
            validateRequiredHeaders(headerMap);
            final List<CSVRecord> records = csvParser.getRecords();
            for (final CSVRecord record : records) {
                final String recordIdentifier = getRecordIdentifier(record);
                validateRequiredDataOnRecord(record);
                final ExtensionDTO extension = new ExtensionDTO();
                extension.setId(parseIdFromRecord(record));
                final String codeValue = parseCodeValueFromRecord(record);
                validateCodeValue(codeValue, recordIdentifier);
                codeValues.add(codeValue.toLowerCase());
                extension.setCodeValue(codeValue);
                extension.setId(parseIdFromRecord(record));
                checkForDuplicateCodeValueInImportData(codeValues, codeValue);
                extension.setPrefLabel(parseLocalizedValueFromCsvRecord(prefLabelHeaders, record));
                if (headerMap.containsKey(CONTENT_HEADER_CODESCHEMES)) {
                    final Set<CodeSchemeDTO> codeSchemes = new HashSet<>();
                    final List<String> uris = parseCodeSchemesFromRecord(record);
                    uris.forEach(uri -> {
                        final CodeSchemeDTO codeScheme = new CodeSchemeDTO();
                        codeScheme.setUri(uri);
                        codeSchemes.add(codeScheme);
                    });
                    extension.setCodeSchemes(codeSchemes);
                }
                if (record.isMapped(CONTENT_HEADER_STARTDATE)) {
                    extension.setStartDate(parseStartDateFromString(parseStartDateStringFromCsvRecord(record), recordIdentifier));
                }
                if (record.isMapped(CONTENT_HEADER_ENDDATE)) {
                    extension.setEndDate(parseEndDateFromString(parseEndDateStringFromCsvRecord(record), recordIdentifier));
                }
                validateStartDateIsBeforeEndDate(extension);
                extension.setStatus(parseStatusValueFromString(record.get(CONTENT_HEADER_STATUS)));
                final String propertyTypeLocalName = record.get(CONTENT_HEADER_PROPERTYTYPE);
                final PropertyTypeDTO propertyType = new PropertyTypeDTO();
                propertyType.setLocalName(propertyTypeLocalName);
                extension.setPropertyType(propertyType);
                extensions.add(extension);
            }
        } catch (final IllegalArgumentException e) {
            LOG.error("Duplicate header value found in CSV!", e);
            throw new CsvParsingException(ERR_MSG_USER_DUPLICATE_HEADER_VALUE);
        } catch (final IOException e) {
            LOG.error("Error parsing CSV file!", e);
            throw new CsvParsingException(ERR_MSG_USER_ERROR_PARSING_CSV_FILE);
        }
        return extensions;
    }

    @Override
    public Set<ExtensionDTO> parseExtensionsFromExcelInputStream(final InputStream inputStream,
                                                                 final String sheetName,
                                                                 final Map<ExtensionDTO, String> membersSheetNames) {
        try (final Workbook workbook = WorkbookFactory.create(inputStream)) {
            return parseExtensionsFromExcelWorkbook(workbook, sheetName, membersSheetNames);
        } catch (final InvalidFormatException | IOException | POIXMLException e) {
            LOG.error("Error parsing Excel file!", e);
            throw new ExcelParsingException(ERR_MSG_USER_ERROR_PARSING_EXCEL_FILE);
        }
    }

    @Override
    public Set<ExtensionDTO> parseExtensionsFromExcelWorkbook(final Workbook workbook,
                                                              final String sheetName,
                                                              final Map<ExtensionDTO, String> membersSheetNames) {
        final Set<ExtensionDTO> extensionsSchemes = new HashSet<>();
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
            } else if (!checkIfRowIsEmpty(row)) {
                final ExtensionDTO extension = new ExtensionDTO();
                final String codeValue = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODEVALUE))).trim();
                final String status = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STATUS)));
                validateRequiredDataOnRow(row, headerMap, formatter);
                validateCodeValue(codeValue, getRowIdentifier(row));
                checkForDuplicateCodeValueInImportData(codeValues, codeValue);
                codeValues.add(codeValue.toLowerCase());
                extension.setCodeValue(codeValue);
                extension.setStatus(parseStatusValueFromString(status));
                if (headerMap.containsKey(CONTENT_HEADER_ID)) {
                    extension.setId(parseUUIDFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ID)))));
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
                    extension.setCodeSchemes(codeSchemes);
                }
                extension.setPrefLabel(parseLocalizedValueFromExcelRow(prefLabelHeaders, row, formatter));
                if (headerMap.containsKey(CONTENT_HEADER_STARTDATE)) {
                    extension.setStartDate(parseStartDateFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STARTDATE))), getRowIdentifier(row)));
                }
                if (headerMap.containsKey(CONTENT_HEADER_ENDDATE)) {
                    extension.setEndDate(parseEndDateFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ENDDATE))), getRowIdentifier(row)));
                }
                validateStartDateIsBeforeEndDate(extension);
                final String propertyTypeLocalName = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_PROPERTYTYPE)));
                final PropertyTypeDTO propertyType = new PropertyTypeDTO();
                propertyType.setLocalName(propertyTypeLocalName);
                extension.setPropertyType(propertyType);
                if (headerMap.containsKey(CONTENT_HEADER_MEMBERSSHEET)) {
                    final String membersSheetName = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_MEMBERSSHEET)));
                    if (membersSheetName != null && !membersSheetName.isEmpty()) {
                        membersSheetNames.put(extension, membersSheetName);
                    }
                }
                extensionsSchemes.add(extension);
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
                ERR_MSG_USER_ROW_MISSING_CODEVALUE, getRowIdentifier(row)));
        }
    }

    private void validateRequiredDataOnRecord(final CSVRecord record) {
        if (record.get(CONTENT_HEADER_CODEVALUE) == null || record.get(CONTENT_HEADER_CODEVALUE).isEmpty()) {
            throw new MissingRowValueCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_CODEVALUE, getRecordIdentifier(record)));
        }
        if (record.get(CONTENT_HEADER_PROPERTYTYPE) == null || record.get(CONTENT_HEADER_PROPERTYTYPE).isEmpty()) {
            throw new MissingRowValueStatusException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_PROPERTYTYPE, getRecordIdentifier(record)));
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

    private void validateStartDateIsBeforeEndDate(final ExtensionDTO extension) {
        if (!startDateIsBeforeEndDateSanityCheck(extension.getStartDate(), extension.getEndDate())) {
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
