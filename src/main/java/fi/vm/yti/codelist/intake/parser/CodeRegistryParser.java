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

import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.util.FileUtils;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

/**
 * Class that handles parsing of CodeRegistries from source data.
 */
@Service
public class CodeRegistryParser {

    private static final Logger LOG = LoggerFactory.getLogger(CodeRegistryParser.class);
    private final ApiUtils apiUtils;
    private final ParserUtils parserUtils;

    @Inject
    public CodeRegistryParser(final ApiUtils apiUtils,
                              final ParserUtils parserUtils) {
        this.apiUtils = apiUtils;
        this.parserUtils = parserUtils;
    }

    /**
     * Parses the .csv CodeRegistry-file and returns the coderegistries as an arrayList.
     *
     * @param source      Source identifier for the data.
     * @param inputStream The CodeRegistry-file.
     * @return            List of CodeRegistry objects.
     */
    public List<CodeRegistry> parseCodeRegistriesFromCsvInputStream(final String source,
                                                                    final InputStream inputStream) {
        final List<CodeRegistry> codeRegistries = new ArrayList<>();
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
            records.forEach(record -> {
                final String code = record.get(CONTENT_HEADER_CODEVALUE);
                final Map<String, String> prefLabels = new LinkedHashMap<>();
                for (final String language : prefLabelHeaders.keySet()) {
                    prefLabels.put(language, record.get(prefLabelHeaders.get(language)));
                }
                final Map<String, String> definitions = new LinkedHashMap<>();
                for (final String language : definitionHeaders.keySet()) {
                    definitions.put(language, record.get(definitionHeaders.get(language)));
                }
                final CodeRegistry codeRegistry = createOrUpdateCodeRegistry(code, source, prefLabels, definitions);
                if (codeRegistry != null) {
                    codeRegistries.add(codeRegistry);
                }
            });
        } catch (IOException e) {
            LOG.error("Parsing coderegistries failed: " + e.getMessage());
        }
        return codeRegistries;
    }

    /* Parses the .xls CodeResistry Excel-file and returns the CodeRegistries as an arrayList.
     *
     * @param source      Source identifier for the data.
     * @param inputStream The CodeRegistry containing Excel -file.
     * @return            List of CodeRegistry objects.
     */
    public List<CodeRegistry> parseCodeRegistriesFromExcelInputStream(final String source,
                                                                      final InputStream inputStream) throws Exception {
        final List<CodeRegistry> codeRegistries = new ArrayList<>();
        final Workbook workbook = new XSSFWorkbook(inputStream);
        final Sheet codesSheet = workbook.getSheet(EXCEL_SHEET_CODESCHEMES);
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
                        prefLabelHeaders.put(value.substring(value.indexOf(CONTENT_HEADER_DESCRIPTION_PREFIX)).toLowerCase(), index);
                    } else if (value.startsWith(CONTENT_HEADER_DEFINITION_PREFIX)) {
                        definitionHeaders.put(value.substring(value.indexOf(CONTENT_HEADER_DEFINITION_PREFIX)).toLowerCase(), index);
                    } else {
                        genericHeaders.put(value, index);
                    }
                }
                firstRow = false;
            } else {
                final String codeValue = row.getCell(genericHeaders.get(CONTENT_HEADER_CODEVALUE)).getStringCellValue();
                final Map<String, String> prefLabels = new LinkedHashMap<>();
                prefLabelHeaders.forEach((language, header) -> {
                    prefLabels.put(language, row.getCell(prefLabelHeaders.get(language)).getStringCellValue());
                });
                final Map<String, String> definitions = new LinkedHashMap<>();
                definitionHeaders.forEach((language, header) -> {
                    definitions.put(language, row.getCell(definitionHeaders.get(language)).getStringCellValue());
                });
                final CodeRegistry codeRegistry = createOrUpdateCodeRegistry(codeValue, source, prefLabels, definitions);
                if (codeRegistry != null) {
                    codeRegistries.add(codeRegistry);
                }
            }
        }
        return codeRegistries;
    }

    private CodeRegistry createOrUpdateCodeRegistry(final String codeValue,
                                                    final String source,
                                                    final Map<String, String> prefLabels,
                                                    final Map<String, String> definitions) {
        final Map<String, CodeRegistry> existingCodeRegistriesMap = parserUtils.getCodeRegistriesMap();
        final String uri = apiUtils.createResourceUrl(API_PATH_CODEREGISTRIES, codeValue);
        final Date timeStamp = new Date(System.currentTimeMillis());
        CodeRegistry codeRegistry = existingCodeRegistriesMap.get(codeValue);
        if (codeRegistry != null) {
            boolean hasChanges = false;
            if (!Objects.equals(codeRegistry.getUri(), uri)) {
                codeRegistry.setUri(uri);
                hasChanges = true;
            }
            if (!Objects.equals(codeRegistry.getSource(), source)) {
                codeRegistry.setSource(source);
                hasChanges = true;
            }
            for (final String language : prefLabels.keySet()) {
                final String value = prefLabels.get(language);
                if (!Objects.equals(codeRegistry.getPrefLabel(language), value)) {
                    codeRegistry.setPrefLabel(language, value);
                    hasChanges = true;
                }
            }
            for (final String language : definitions.keySet()) {
                final String value = definitions.get(language);
                if (!Objects.equals(codeRegistry.getDefinition(language), value)) {
                    codeRegistry.setDefinition(language, value);
                    hasChanges = true;
                }
            }
            if (hasChanges) {
                codeRegistry.setModified(timeStamp);
            }
        } else {
            codeRegistry = new CodeRegistry();
            codeRegistry.setId(UUID.randomUUID().toString());
            codeRegistry.setUri(uri);
            codeRegistry.setCodeValue(codeValue);
            codeRegistry.setSource(source);
            codeRegistry.setModified(timeStamp);
            for (final String language : prefLabels.keySet()) {
                codeRegistry.setPrefLabel(language, prefLabels.get(language));
            }
            for (final String language : definitions.keySet()) {
                codeRegistry.setDefinition(language, definitions.get(language));
            }
        }
        return codeRegistry;
    }
}
