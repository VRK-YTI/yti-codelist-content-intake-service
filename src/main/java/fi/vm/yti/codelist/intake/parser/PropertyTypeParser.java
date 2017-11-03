package fi.vm.yti.codelist.intake.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

import fi.vm.yti.codelist.common.model.PropertyType;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.jpa.PropertyTypeRepository;
import fi.vm.yti.codelist.intake.util.FileUtils;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

/**
 * Class that handles parsing of PropertyTypes from source data.
 */
@Service
public class PropertyTypeParser {

    private static final Logger LOG = LoggerFactory.getLogger(CodeSchemeParser.class);
    private final ApiUtils apiUtils;
    private final PropertyTypeRepository propertyTypeRepository;

    @Inject
    public PropertyTypeParser(final ApiUtils apiUtils,
                              final PropertyTypeRepository codeSchemeRepository) {
        this.apiUtils = apiUtils;
        this.propertyTypeRepository = codeSchemeRepository;
    }

    /**
     * Parses the .csv PropertyType-file and returns the PropertyType as an ArrayList.
     *
     * @param source      Source identifier for the data.
     * @param inputStream The PropertyType -file.
     * @return List of PropertyType objects.
     */
    public List<PropertyType> parsePropertyTypeFromCsvInputStream(final String source,
                                                                  final InputStream inputStream) {
        final List<PropertyType> propertyTypes = new ArrayList<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
            FileUtils.skipBom(in);
            final Map<String, Integer> headerMap = csvParser.getHeaderMap();
            final Map<String, String> prefLabelHeaders = new LinkedHashMap<>();
            final Map<String, String> definitionHeaders = new LinkedHashMap<>();
            for (final String value : headerMap.keySet()) {
                if (value.startsWith(CONTENT_HEADER_PREFLABEL_PREFIX)) {
                    prefLabelHeaders.put(value.substring(value.indexOf(CONTENT_HEADER_PREFLABEL_PREFIX)).toLowerCase(), value);
                } else if (value.startsWith(CONTENT_HEADER_DEFINITION_PREFIX)) {
                    definitionHeaders.put(value.substring(value.indexOf(CONTENT_HEADER_DEFINITION_PREFIX)).toLowerCase(), value);
                }
            }
            final List<CSVRecord> records = csvParser.getRecords();
            for (final CSVRecord record : records) {
                final String id = record.get(CONTENT_HEADER_ID);
                final String notation = record.get(CONTENT_HEADER_NOTATION);
                final Map<String, String> prefLabels = new LinkedHashMap<>();
                prefLabelHeaders.forEach((language, header) -> {
                    prefLabels.put(language, record.get(header));
                });
                final Map<String, String> definitions = new LinkedHashMap<>();
                definitionHeaders.forEach((language, header) -> {
                    definitions.put(language, record.get(header));
                });
                final PropertyType propertyType = createOrUpdatePropertyType(id, notation, prefLabels, definitions);
                propertyTypes.add(propertyType);
            }
        } catch (IOException e) {
            LOG.error("Parsing codeschemes failed: " + e.getMessage());
        }
        return propertyTypes;
    }

    /*
     * Parses the .xls PropertyType Excel-file and returns the PropertyTypes as an arrayList.
     *
     * @param codeRegistry CodeRegistry.
     * @param source      Source identifier for the data.
     * @param inputStream The Code containing Excel -file.
     * @return List of Code objects.
     */
    public List<PropertyType> parseCodeSchemesFromExcelInputStream(final String source,
                                                                   final InputStream inputStream) throws Exception {
        final List<PropertyType> propertyTypes = new ArrayList<>();
        final Workbook workbook = new XSSFWorkbook(inputStream);
        final Sheet codesSheet = workbook.getSheet(EXCEL_SHEET_PROPERTYTYPES);
        final Iterator<Row> rowIterator = codesSheet.rowIterator();
        final Map<String, Integer> genericHeaders = new LinkedHashMap<>();
        final Map<String, Integer> prefLabelHeaders = new LinkedHashMap<>();
        final Map<String, Integer> definitionHeaders = new LinkedHashMap<>();
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
                        prefLabelHeaders.put(value.substring(value.indexOf(CONTENT_HEADER_PREFLABEL_PREFIX)).toLowerCase(), index);
                    } else if (value.startsWith(CONTENT_HEADER_DEFINITION_PREFIX)) {
                        definitionHeaders.put(value.substring(value.indexOf(CONTENT_HEADER_DEFINITION_PREFIX)).toLowerCase(), index);
                    } else {
                        genericHeaders.put(value, index);
                    }
                }
                firstRow = false;
            } else {
                final String id = row.getCell(genericHeaders.get(CONTENT_HEADER_ID)).getStringCellValue();
                final String notation = row.getCell(genericHeaders.get(CONTENT_HEADER_NOTATION)).getStringCellValue();
                final Map<String, String> prefLabels = new LinkedHashMap<>();
                for (final String language : prefLabelHeaders.keySet()) {
                    prefLabels.put(language, row.getCell(prefLabelHeaders.get(language)).getStringCellValue());
                }
                final Map<String, String> definitions = new LinkedHashMap<>();
                for (final String language : definitionHeaders.keySet()) {
                    definitions.put(language, row.getCell(definitionHeaders.get(language)).getStringCellValue());
                }
                final PropertyType propertyType = createOrUpdatePropertyType(id, notation, prefLabels, definitions);
                if (propertyType != null) {
                    propertyTypes.add(propertyType);
                }
            }
        }
        return propertyTypes;
    }

    private PropertyType createOrUpdatePropertyType(final String id,
                                                    final String notation,
                                                    final Map<String, String> prefLabels,
                                                    final Map<String, String> definitions) {
        final Date timeStamp = new Date(System.currentTimeMillis());
        PropertyType propertyType = null;
        if (id != null && !id.isEmpty()) {
            propertyType = propertyTypeRepository.findById(id);
        }
        String uri = apiUtils.createResourceUrl(API_PATH_PROPERTYTYPES, id);
        if (propertyType != null) {
            boolean hasChanges = false;
            if (!Objects.equals(propertyType.getUri(), uri)) {
                propertyType.setUri(uri);
                hasChanges = true;
            }
            if (!Objects.equals(propertyType.getNotation(), notation)) {
                propertyType.setNotation(uri);
                hasChanges = true;
            }
            for (final String language : prefLabels.keySet()) {
                final String value = prefLabels.get(language);
                if (!Objects.equals(propertyType.getPrefLabel(language), value)) {
                    propertyType.setPrefLabel(language, value);
                    hasChanges = true;
                }
            }
            for (final String language : definitions.keySet()) {
                final String value = definitions.get(language);
                if (!Objects.equals(propertyType.getDefinition(language), value)) {
                    propertyType.setDefinition(language, value);
                    hasChanges = true;
                }
            }
            if (hasChanges) {
                propertyType.setModified(timeStamp);
            }
        } else {
            propertyType = new PropertyType();
            if (id != null && !id.isEmpty()) {
                propertyType.setId(id);
            } else {
                final String uuid = UUID.randomUUID().toString();
                uri = apiUtils.createResourceUrl(API_PATH_PROPERTYTYPES, uuid);
                propertyType.setId(uuid);
            }
            propertyType.setNotation(notation);
            propertyType.setUri(uri);
            propertyType.setModified(timeStamp);
            for (final String language : prefLabels.keySet()) {
                propertyType.setPrefLabel(language, prefLabels.get(language));
            }
            for (final String language : definitions.keySet()) {
                propertyType.setDefinition(language, definitions.get(language));
            }
        }
        return propertyType;
    }
}
