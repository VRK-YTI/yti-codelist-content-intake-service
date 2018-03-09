package fi.vm.yti.codelist.intake.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

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
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.model.PropertyType;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.exception.CsvParsingException;
import fi.vm.yti.codelist.intake.exception.JsonParsingException;
import fi.vm.yti.codelist.intake.exception.ExcelParsingException;
import fi.vm.yti.codelist.intake.jpa.PropertyTypeRepository;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

/**
 * Class that handles parsing of PropertyTypes from source data.
 */
@Service
public class PropertyTypeParser extends AbstractBaseParser {

    private final ApiUtils apiUtils;
    private final PropertyTypeRepository propertyTypeRepository;

    @Inject
    public PropertyTypeParser(final ApiUtils apiUtils,
                              final PropertyTypeRepository codeSchemeRepository) {
        this.apiUtils = apiUtils;
        this.propertyTypeRepository = codeSchemeRepository;
    }

    public PropertyType parsePropertyTypeFromJson(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final PropertyType fromPropertyType;
        try {
            fromPropertyType = mapper.readValue(jsonPayload, PropertyType.class);
        } catch (final IOException e) {
            throw new JsonParsingException("JSON parsing failed");
        }
        return createOrUpdatePropertyType(fromPropertyType);
    }

    public Set<PropertyType> parsePropertyTypesFromJson(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final Set<PropertyType> fromPropertyTypes;
        try {
            fromPropertyTypes = mapper.readValue(jsonPayload, new TypeReference<Set<PropertyType>>() {
            });
        } catch (final IOException e) {
            throw new JsonParsingException("JSON parsing failed");
        }
        final Set<PropertyType> propertyTypes = new HashSet<>();
        for (final PropertyType fromPropertyType : fromPropertyTypes) {
            propertyTypes.add(createOrUpdatePropertyType(fromPropertyType));
        }
        return propertyTypes;
    }

    /**
     * Parses the .csv PropertyType-file and returns the PropertyType as an ArrayList.
     *
     * @param inputStream The PropertyType -file.
     * @return List of PropertyType objects.
     */
    @SuppressFBWarnings("UC_USELESS_OBJECT")
    public Set<PropertyType> parsePropertyTypesFromCsvInputStream(final InputStream inputStream) {
        final Set<PropertyType> propertyTypes = new HashSet<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
            final Map<String, Integer> headerMap = csvParser.getHeaderMap();
            checkForDuplicateHeaders(headerMap);
            final Map<String, Integer> prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
            final Map<String, Integer> definitionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DEFINITION_PREFIX);
            final List<CSVRecord> records = csvParser.getRecords();
            for (final CSVRecord record : records) {
                final PropertyType fromPropertyType = new PropertyType();
                fromPropertyType.setId(parseUUIDFromString(record.get(CONTENT_HEADER_ID)));
                fromPropertyType.setLocalName(record.get(CONTENT_HEADER_LOCALNAME));
                fromPropertyType.setPropertyUri(record.get(CONTENT_HEADER_PROPERTYURI));
                fromPropertyType.setContext(record.get(CONTENT_HEADER_CONTEXT));
                fromPropertyType.setType(record.get(CONTENT_HEADER_TYPE));
                fromPropertyType.setPrefLabel(parseLocalizedValueFromCsvRecord(prefLabelHeaders, record));
                fromPropertyType.setDefinition(parseLocalizedValueFromCsvRecord(definitionHeaders, record));
                final PropertyType propertyType = createOrUpdatePropertyType(fromPropertyType);
                propertyTypes.add(propertyType);
            }
        } catch (final IOException e) {
            throw new CsvParsingException("CSV parsing failed!");
        }
        return propertyTypes;
    }

    /*
     * Parses the .xls PropertyType Excel-file and returns the PropertyTypes as an arrayList.
     *
     * @param codeRegistry CodeRegistry.
     * @param inputStream The Code containing Excel -file.
     * @return List of Code objects.
     */
    public Set<PropertyType> parsePropertyTypesFromExcelInputStream(final InputStream inputStream) {
        final Set<PropertyType> propertyTypes = new HashSet<>();
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
            while (rowIterator.hasNext()) {
                final Row row = rowIterator.next();
                if (firstRow) {
                    firstRow = false;
                    headerMap = resolveHeaderMap(row);
                    prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
                    definitionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DEFINITION_PREFIX);
                } else {
                    final PropertyType fromPropertyType = new PropertyType();
                    fromPropertyType.setId(parseUUIDFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ID)))));
                    fromPropertyType.setLocalName(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_LOCALNAME))));
                    fromPropertyType.setPropertyUri(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_PROPERTYURI))));
                    fromPropertyType.setContext(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CONTEXT))));
                    fromPropertyType.setType(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_TYPE))));
                    fromPropertyType.setPrefLabel(parseLocalizedValueFromExcelRow(prefLabelHeaders, row, formatter));
                    fromPropertyType.setDefinition(parseLocalizedValueFromExcelRow(definitionHeaders, row, formatter));
                    final PropertyType propertyType = createOrUpdatePropertyType(fromPropertyType);
                    if (propertyType != null) {
                        propertyTypes.add(propertyType);
                    }
                }
            }
        } catch (final InvalidFormatException | IOException | POIXMLException e) {
            throw new ExcelParsingException("Error parsing Excel file.");
        }
        return propertyTypes;
    }

    private PropertyType createOrUpdatePropertyType(final PropertyType fromPropertyType) {
        PropertyType existingPropertyType = null;
        if (fromPropertyType.getId() != null) {
            existingPropertyType = propertyTypeRepository.findById(fromPropertyType.getId());
        } else {
            existingPropertyType = null;
        }
        final PropertyType propertyType;
        if (existingPropertyType != null) {
            propertyType = updatePropertyType(existingPropertyType, fromPropertyType);
        } else {
            propertyType = createPropertyType(fromPropertyType);
        }
        return propertyType;
    }

    private PropertyType updatePropertyType(final PropertyType existingPropertyType,
                                            final PropertyType fromPropertyType) {
        final String uri = apiUtils.createResourceUri(API_PATH_PROPERTYTYPES, fromPropertyType.getId().toString());
        if (!Objects.equals(existingPropertyType.getPropertyUri(), fromPropertyType.getPropertyUri())) {
            existingPropertyType.setPropertyUri(fromPropertyType.getPropertyUri());
        }
        if (!Objects.equals(existingPropertyType.getUri(), uri)) {
            existingPropertyType.setUri(uri);
        }
        if (!Objects.equals(existingPropertyType.getContext(), fromPropertyType.getContext())) {
            existingPropertyType.setUri(fromPropertyType.getContext());
        }
        if (!Objects.equals(existingPropertyType.getLocalName(), fromPropertyType.getLocalName())) {
            existingPropertyType.setLocalName(fromPropertyType.getLocalName());
        }
        if (!Objects.equals(existingPropertyType.getType(), fromPropertyType.getType())) {
            existingPropertyType.setType(fromPropertyType.getType());
        }
        for (final Map.Entry<String, String> entry : fromPropertyType.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingPropertyType.getPrefLabel(language), value)) {
                existingPropertyType.setPrefLabel(language, value);
            }
        }
        for (final Map.Entry<String, String> entry : fromPropertyType.getDefinition().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingPropertyType.getDefinition(language), value)) {
                existingPropertyType.setDefinition(language, value);
            }
        }
        return existingPropertyType;
    }

    private PropertyType createPropertyType(final PropertyType fromPropertyType) {
        final PropertyType propertyType = new PropertyType();
        final String uri;
        if (fromPropertyType.getId() != null) {
            propertyType.setId(fromPropertyType.getId());
            uri = apiUtils.createResourceUri(API_PATH_PROPERTYTYPES, fromPropertyType.getId().toString());
        } else {
            final UUID uuid = UUID.randomUUID();
            uri = apiUtils.createResourceUri(API_PATH_PROPERTYTYPES, uuid.toString());
            propertyType.setId(uuid);
        }
        propertyType.setContext(fromPropertyType.getContext());
        propertyType.setLocalName(fromPropertyType.getLocalName());
        propertyType.setType(fromPropertyType.getType());
        propertyType.setUri(uri);
        propertyType.setPropertyUri(fromPropertyType.getPropertyUri());
        for (final Map.Entry<String, String> entry : fromPropertyType.getPrefLabel().entrySet()) {
            propertyType.setPrefLabel(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, String> entry : fromPropertyType.getDefinition().entrySet()) {
            propertyType.setDefinition(entry.getKey(), entry.getValue());
        }
        return propertyType;
    }
}
