package fi.vm.yti.codelist.intake.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
public class CodeRegistryParser extends AbstractBaseParser {

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
     * Parses the .csv CodeRegistry-file and returns the coderegistries as a set.
     *
     * @param inputStream The CodeRegistry-file.
     * @return Set of CodeRegistry objects.
     */
    public Set<CodeRegistry> parseCodeRegistriesFromCsvInputStream(final InputStream inputStream) {
        final Set<CodeRegistry> codeRegistries = new HashSet<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
            FileUtils.skipBom(in);
            final Map<String, Integer> headerMap = csvParser.getHeaderMap();
            final Map<String, String> prefLabelHeaders = new LinkedHashMap<>();
            final Map<String, String> definitionHeaders = new LinkedHashMap<>();
            for (final String value : headerMap.keySet()) {
                if (value.startsWith(CONTENT_HEADER_PREFLABEL_PREFIX)) {
                    prefLabelHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_PREFLABEL_PREFIX, value), value);
                } else if (value.startsWith(CONTENT_HEADER_DEFINITION_PREFIX)) {
                    definitionHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_DEFINITION_PREFIX, value), value);
                }
            }
            final List<CSVRecord> records = csvParser.getRecords();
            records.forEach(record -> {
                final String code = record.get(CONTENT_HEADER_CODEVALUE);
                final Map<String, String> prefLabel = new LinkedHashMap<>();
                prefLabelHeaders.forEach((language, header) -> {
                    prefLabel.put(language, record.get(header));
                });
                final Map<String, String> definition = new LinkedHashMap<>();
                definitionHeaders.forEach((language, header) -> {
                    definition.put(language, record.get(header));
                });
                final CodeRegistry codeRegistry = createOrUpdateCodeRegistry(code, prefLabel, definition);
                if (codeRegistry != null) {
                    codeRegistries.add(codeRegistry);
                }
            });
        } catch (IOException e) {
            LOG.error("Parsing coderegistries failed: " + e.getMessage());
        }
        return codeRegistries;
    }

    /**
     * Parses the .xls CodeRegistry Excel-file and returns the CodeRegistries as a set.
     *
     * @param inputStream The CodeRegistry containing Excel -file.
     * @return Set of CodeRegistry objects.
     */
    public Set<CodeRegistry> parseCodeRegistriesFromExcelInputStream(final InputStream inputStream) throws Exception {
        final Set<CodeRegistry> codeRegistries = new HashSet<>();
        try (final Workbook workbook = new XSSFWorkbook(inputStream)) {
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
                            prefLabelHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_PREFLABEL_PREFIX, value), index);
                        } else if (value.startsWith(CONTENT_HEADER_DEFINITION_PREFIX)) {
                            definitionHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_DEFINITION_PREFIX, value), index);
                        } else {
                            genericHeaders.put(value, index);
                        }
                    }
                    firstRow = false;
                } else {
                    final String codeValue = row.getCell(genericHeaders.get(CONTENT_HEADER_CODEVALUE)).getStringCellValue();
                    final Map<String, String> prefLabel = new LinkedHashMap<>();
                    prefLabelHeaders.forEach((language, header) -> {
                        prefLabel.put(language, row.getCell(prefLabelHeaders.get(language)).getStringCellValue());
                    });
                    final Map<String, String> definition = new LinkedHashMap<>();
                    definitionHeaders.forEach((language, header) -> {
                        definition.put(language, row.getCell(definitionHeaders.get(language)).getStringCellValue());
                    });
                    final CodeRegistry codeRegistry = createOrUpdateCodeRegistry(codeValue, prefLabel, definition);
                    if (codeRegistry != null) {
                        codeRegistries.add(codeRegistry);
                    }
                }
            }
        }
        return codeRegistries;
    }

    private CodeRegistry createOrUpdateCodeRegistry(final String codeValue,
                                                    final Map<String, String> prefLabel,
                                                    final Map<String, String> definition) {
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
            for (final String language : prefLabel.keySet()) {
                final String value = prefLabel.get(language);
                if (!Objects.equals(codeRegistry.getPrefLabel(language), value)) {
                    codeRegistry.setPrefLabel(language, value);
                    hasChanges = true;
                }
            }
            for (final String language : definition.keySet()) {
                final String value = definition.get(language);
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
            codeRegistry.setId(UUID.randomUUID());
            codeRegistry.setUri(uri);
            codeRegistry.setCodeValue(codeValue);
            codeRegistry.setModified(timeStamp);
            for (final String language : prefLabel.keySet()) {
                codeRegistry.setPrefLabel(language, prefLabel.get(language));
            }
            for (final String language : definition.keySet()) {
                codeRegistry.setDefinition(language, definition.get(language));
            }
        }
        return codeRegistry;
    }
}
