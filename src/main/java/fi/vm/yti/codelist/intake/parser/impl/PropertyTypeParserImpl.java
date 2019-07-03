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
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.common.dto.ValueTypeDTO;
import fi.vm.yti.codelist.intake.exception.CsvParsingException;
import fi.vm.yti.codelist.intake.exception.ExcelParsingException;
import fi.vm.yti.codelist.intake.exception.JsonParsingException;
import fi.vm.yti.codelist.intake.parser.PropertyTypeParser;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;
import static java.util.Arrays.asList;

@Service
public class PropertyTypeParserImpl extends AbstractBaseParser implements PropertyTypeParser {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyTypeParserImpl.class);

    @Override
    public PropertyTypeDTO parsePropertyTypeFromJson(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final PropertyTypeDTO fromPropertyType;
        try {
            fromPropertyType = mapper.readValue(jsonPayload, PropertyTypeDTO.class);
        } catch (final IOException e) {
            LOG.error("PropertyType parsing failed from JSON!", e);
            throw new JsonParsingException(ERR_MSG_USER_PROPERTYTYPE_PARSING_FAILED);
        }
        return fromPropertyType;
    }

    @Override
    public Set<PropertyTypeDTO> parsePropertyTypesFromJson(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final Set<PropertyTypeDTO> propertyTypes;
        try {
            propertyTypes = mapper.readValue(jsonPayload, new TypeReference<Set<PropertyTypeDTO>>() {
            });
        } catch (final IOException e) {
            LOG.error("PropertyTypes parsing failed from JSON!", e);
            throw new JsonParsingException(ERR_MSG_USER_PROPERTYTYPE_PARSING_FAILED);
        }
        return propertyTypes;
    }

    @Override
    @SuppressFBWarnings("UC_USELESS_OBJECT")
    public Set<PropertyTypeDTO> parsePropertyTypesFromCsvInputStream(final InputStream inputStream) {
        final Set<PropertyTypeDTO> propertyTypes = new HashSet<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
            final Map<String, Integer> headerMap = csvParser.getHeaderMap();
            final Map<String, Integer> prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
            final Map<String, Integer> definitionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DEFINITION_PREFIX);
            final List<CSVRecord> records = csvParser.getRecords();
            for (final CSVRecord record : records) {
                final PropertyTypeDTO propertyType = new PropertyTypeDTO();
                propertyType.setId(parseUUIDFromString(record.get(CONTENT_HEADER_ID)));
                propertyType.setLocalName(record.get(CONTENT_HEADER_LOCALNAME));
                propertyType.setUri(record.get(CONTENT_HEADER_URI));
                propertyType.setContext(record.get(CONTENT_HEADER_CONTEXT));
                if (record.get(CONTENT_HEADER_VALUETYPE) != null) {
                    propertyType.setValueTypes(parseValueTypesFromString(record.get(CONTENT_HEADER_VALUETYPE)));
                }
                propertyType.setPrefLabel(parseLocalizedValueFromCsvRecord(prefLabelHeaders, record));
                propertyType.setDefinition(parseLocalizedValueFromCsvRecord(definitionHeaders, record));
                propertyTypes.add(propertyType);
            }
        } catch (final IllegalArgumentException e) {
            LOG.error("Duplicate header value found in CSV!", e);
            throw new CsvParsingException(ERR_MSG_USER_DUPLICATE_HEADER_VALUE);
        } catch (final IOException e) {
            LOG.error("Error parsing CSV file!", e);
            throw new CsvParsingException(ERR_MSG_USER_ERROR_PARSING_CSV_FILE);
        }
        return propertyTypes;
    }

    private Set<ValueTypeDTO> parseValueTypesFromString(final String valueTypeString) {
        final Set<ValueTypeDTO> valueTypes = new HashSet<>();
        final List<String> valueTypeLocalNames = valueTypeString == null || valueTypeString.isEmpty() ? null : asList(valueTypeString.split(";"));
        if (valueTypeLocalNames != null && !valueTypeLocalNames.isEmpty()) {
            valueTypeLocalNames.forEach(valueTypeLocalName -> {
                final ValueTypeDTO valueType = new ValueTypeDTO();
                valueType.setLocalName(trimWhiteSpaceFromString(valueTypeLocalName));
                valueTypes.add(valueType);
            });
        }
        return valueTypes;
    }

    @Override
    public Set<PropertyTypeDTO> parsePropertyTypesFromExcelInputStream(final InputStream inputStream) {
        final Set<PropertyTypeDTO> propertyTypes = new HashSet<>();
        try (final Workbook workbook = WorkbookFactory.create(inputStream)) {
            final DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheet(EXCEL_SHEET_PROPERTYTYPES);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            final Iterator<Row> rowIterator = sheet.rowIterator();
            Map<String, Integer> headerMap = null;
            Map<String, Integer> prefLabelHeaders = null;
            Map<String, Integer> definitionHeaders = null;
            boolean firstRow = true;
            checkIfExcelEmpty(rowIterator);
            while (rowIterator.hasNext()) {
                final Row row = rowIterator.next();
                if (firstRow) {
                    firstRow = false;
                    headerMap = resolveHeaderMap(row);
                    prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
                    definitionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DEFINITION_PREFIX);
                } else if (!checkIfRowIsEmpty(row)) {
                    final PropertyTypeDTO propertyType = new PropertyTypeDTO();
                    propertyType.setId(parseUUIDFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ID)))));
                    propertyType.setLocalName(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_LOCALNAME))));
                    propertyType.setUri(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_URI))));
                    propertyType.setContext(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CONTEXT))));
                    propertyType.setPrefLabel(parseLocalizedValueFromExcelRow(prefLabelHeaders, row, formatter));
                    propertyType.setDefinition(parseLocalizedValueFromExcelRow(definitionHeaders, row, formatter));
                    if (row.getCell(headerMap.get(CONTENT_HEADER_VALUETYPE)) != null) {
                        propertyType.setValueTypes(parseValueTypesFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_VALUETYPE)))));
                    }
                    propertyTypes.add(propertyType);
                }
            }
        } catch (final InvalidFormatException | EmptyFileException | IOException | POIXMLException e) {
            LOG.error("Error parsing Excel file!", e);
            throw new ExcelParsingException(ERR_MSG_USER_ERROR_PARSING_EXCEL_FILE);
        }
        return propertyTypes;
    }
}
