package fi.vm.yti.codelist.intake.parser.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
import org.apache.poi.EmptyFileException;
import org.apache.poi.POIXMLException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.dto.ValueTypeDTO;
import fi.vm.yti.codelist.intake.exception.CsvParsingException;
import fi.vm.yti.codelist.intake.exception.ExcelParsingException;
import fi.vm.yti.codelist.intake.exception.JsonParsingException;
import fi.vm.yti.codelist.intake.parser.ValueTypeParser;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Service
public class ValueTypeParserImpl extends AbstractBaseParser implements ValueTypeParser {

    private static final Logger LOG = LoggerFactory.getLogger(ValueTypeParserImpl.class);

    @Override
    public ValueTypeDTO parseValueTypeFromJson(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final ValueTypeDTO fromValueType;
        try {
            fromValueType = mapper.readValue(jsonPayload, ValueTypeDTO.class);
        } catch (final IOException e) {
            LOG.error("ValueType parsing failed from JSON!", e);
            throw new JsonParsingException(ERR_MSG_USER_VALUETYPE_PARSING_FAILED);
        }
        return fromValueType;
    }

    @Override
    public Set<ValueTypeDTO> parseValueTypesFromJson(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final Set<ValueTypeDTO> valueTypes;
        try {
            valueTypes = mapper.readValue(jsonPayload, new TypeReference<Set<ValueTypeDTO>>() {
            });
        } catch (final IOException e) {
            LOG.error("ValueTypes parsing failed from JSON!", e);
            throw new JsonParsingException(ERR_MSG_USER_VALUETYPE_PARSING_FAILED);
        }
        return valueTypes;
    }

    @Override
    @SuppressFBWarnings("UC_USELESS_OBJECT")
    public Set<ValueTypeDTO> parseValueTypesFromCsvInputStream(final InputStream inputStream) {
        final Set<ValueTypeDTO> valueTypes = new HashSet<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
            final Map<String, Integer> headerMap = csvParser.getHeaderMap();
            final Map<String, Integer> prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
            final List<CSVRecord> records = csvParser.getRecords();
            for (final CSVRecord record : records) {
                final ValueTypeDTO valueType = new ValueTypeDTO();
                valueType.setId(parseUUIDFromString(record.get(CONTENT_HEADER_ID)));
                valueType.setLocalName(record.get(CONTENT_HEADER_LOCALNAME));
                valueType.setRegexp(record.get(CONTENT_HEADER_REGEXP));
                valueType.setTypeUri(record.get(CONTENT_HEADER_TYPEURI));
                valueType.setUri(record.get(CONTENT_HEADER_URI));
                valueType.setRequired(Boolean.valueOf(record.get(CONTENT_HEADER_REQUIRED)));
                valueType.setPrefLabel(parseLocalizedValueFromCsvRecord(prefLabelHeaders, record));
                valueTypes.add(valueType);
            }
        } catch (final IllegalArgumentException e) {
            LOG.error("Duplicate header value found in CSV!", e);
            throw new CsvParsingException(ERR_MSG_USER_DUPLICATE_HEADER_VALUE);
        } catch (final IOException e) {
            LOG.error("Error parsing CSV file!", e);
            throw new CsvParsingException(ERR_MSG_USER_ERROR_PARSING_CSV_FILE);
        }
        return valueTypes;
    }

    @Override
    public Set<ValueTypeDTO> parseValueTypesFromExcelInputStream(final InputStream inputStream) {
        final Set<ValueTypeDTO> valueTypes = new HashSet<>();
        try (final Workbook workbook = WorkbookFactory.create(inputStream)) {
            final DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheet(EXCEL_SHEET_VALUETYPES);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            final Iterator<Row> rowIterator = sheet.rowIterator();
            Map<String, Integer> headerMap = null;
            Map<String, Integer> prefLabelHeaders = null;
            boolean firstRow = true;
            checkIfExcelEmpty(rowIterator);
            while (rowIterator.hasNext()) {
                final Row row = rowIterator.next();
                if (firstRow) {
                    firstRow = false;
                    headerMap = resolveHeaderMap(row);
                    prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
                } else if (!checkIfRowIsEmpty(row)) {
                    final ValueTypeDTO valueType = new ValueTypeDTO();
                    valueType.setId(parseUUIDFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ID)))));
                    valueType.setLocalName(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_LOCALNAME))));
                    valueType.setRegexp(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_REGEXP))));
                    valueType.setTypeUri(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_TYPEURI))));
                    valueType.setUri(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_URI))));
                    valueType.setRequired(Boolean.valueOf(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_REQUIRED)))));
                    valueType.setPrefLabel(parseLocalizedValueFromExcelRow(prefLabelHeaders, row, formatter));
                    valueTypes.add(valueType);
                }
            }
        } catch (final InvalidFormatException | EmptyFileException | IOException | POIXMLException e) {
            LOG.error("Error parsing Excel file!", e);
            throw new ExcelParsingException(ERR_MSG_USER_ERROR_PARSING_EXCEL_FILE);
        }
        return valueTypes;
    }
}
