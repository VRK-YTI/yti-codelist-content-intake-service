package fi.vm.yti.codelist.intake.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
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
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.ErrorModel;
import fi.vm.yti.codelist.common.model.Organization;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.exception.ErrorConstants;
import fi.vm.yti.codelist.intake.exception.MissingHeaderCodeValueException;
import fi.vm.yti.codelist.intake.jpa.OrganizationRepository;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_PATH_CODEREGISTRIES;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CONTENT_HEADER_CODEVALUE;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CONTENT_HEADER_DEFINITION_PREFIX;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CONTENT_HEADER_ORGANIZATION;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CONTENT_HEADER_PREFLABEL_PREFIX;
import static fi.vm.yti.codelist.common.constants.ApiConstants.EXCEL_SHEET_CODEREGISTRIES;

/**
 * Class that handles parsing of CodeRegistries from source data.
 */
@Service
public class CodeRegistryParser extends AbstractBaseParser {

    private final ApiUtils apiUtils;
    private final ParserUtils parserUtils;
    private final OrganizationRepository organizationRepository;

    @Inject
    public CodeRegistryParser(final ApiUtils apiUtils,
                              final ParserUtils parserUtils,
                              final OrganizationRepository organizationRepository) {
        this.apiUtils = apiUtils;
        this.parserUtils = parserUtils;
        this.organizationRepository = organizationRepository;
    }

    public Set<CodeRegistry> parseCodeRegistriesFromJson(final String jsonPayload) throws IOException {
        final ObjectMapper mapper = createObjectMapper();
        final Set<CodeRegistry> fromCodeRegistries = mapper.readValue(jsonPayload, new TypeReference<Set<CodeRegistry>>() {
        });
        final Set<CodeRegistry> codeRegistries = new HashSet<>();
        for (final CodeRegistry fromRegistry : fromCodeRegistries) {
            codeRegistries.add(createOrUpdateCodeRegistry(fromRegistry));
        }
        return codeRegistries;
    }

    /**
     * Parses the .csv CodeRegistry-file and returns the coderegistries as a set.
     *
     * @param inputStream The CodeRegistry-file.
     * @return Set of CodeRegistry objects.
     */
    public Set<CodeRegistry> parseCodeRegistriesFromCsvInputStream(final InputStream inputStream) throws IOException {
        final Set<CodeRegistry> codeRegistries = new HashSet<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
            final Map<String, Integer> headerMap = csvParser.getHeaderMap();
            final Map<String, Integer> prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
            final Map<String, Integer> definitionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DEFINITION_PREFIX);
            final List<CSVRecord> records = csvParser.getRecords();
            records.forEach(record -> {
                final CodeRegistry fromCodeRegistry = new CodeRegistry();
                fromCodeRegistry.setCodeValue(record.get(CONTENT_HEADER_CODEVALUE));
                fromCodeRegistry.setOrganizations(resolveOrganizations(record.get(CONTENT_HEADER_ORGANIZATION)));
                fromCodeRegistry.setPrefLabel(parseLocalizedValueFromCsvRecord(prefLabelHeaders, record));
                fromCodeRegistry.setDefinition(parseLocalizedValueFromCsvRecord(definitionHeaders, record));
                final CodeRegistry codeRegistry = createOrUpdateCodeRegistry(fromCodeRegistry);
                if (codeRegistry != null) {
                    codeRegistries.add(codeRegistry);
                }
            });
        }
        return codeRegistries;
    }

    /**
     * Parses the .xls CodeRegistry Excel-file and returns the CodeRegistries as a set.
     *
     * @param inputStream The CodeRegistry containing Excel -file.
     * @return Set of CodeRegistry objects.
     */
    @SuppressFBWarnings("UC_USELESS_OBJECT")
    public Set<CodeRegistry> parseCodeRegistriesFromExcelInputStream(final InputStream inputStream) throws IOException, InvalidFormatException {
        final Set<CodeRegistry> codeRegistries = new HashSet<>();
        try (final Workbook workbook = WorkbookFactory.create(inputStream)) {
            final DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheet(EXCEL_SHEET_CODEREGISTRIES);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            boolean firstRow = true;
            final Iterator<Row> rowIterator = sheet.rowIterator();
            Map<String, Integer> headerMap = null;
            Map<String, Integer> prefLabelHeaders = null;
            Map<String, Integer> definitionHeaders = null;
            while (rowIterator.hasNext()) {
                final Row row = rowIterator.next();
                if (firstRow) {
                    firstRow = false;
                    headerMap = resolveHeaderMap(row);
                    prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
                    definitionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DEFINITION_PREFIX);
                    validateRequiredCodeHeaders(headerMap);
                } else {
                    final CodeRegistry fromCodeRegistry = new CodeRegistry();
                    final String codeValue = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODEVALUE)));
                    if (codeValue == null || codeValue.trim().isEmpty()) {
                        continue;
                    }
                    fromCodeRegistry.setCodeValue(codeValue);
                    fromCodeRegistry.setOrganizations(resolveOrganizations(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ORGANIZATION)))));
                    fromCodeRegistry.setPrefLabel(parseLocalizedValueFromExcelRow(prefLabelHeaders, row, formatter));
                    fromCodeRegistry.setDefinition(parseLocalizedValueFromExcelRow(definitionHeaders, row, formatter));

                    final CodeRegistry codeRegistry = createOrUpdateCodeRegistry(fromCodeRegistry);
                    codeRegistries.add(codeRegistry);
                }
            }
        }
        return codeRegistries;
    }

    private void validateRequiredCodeHeaders(final Map<String, Integer> headerMap) {
        if (!headerMap.containsKey(CONTENT_HEADER_CODEVALUE)) {
            throw new MissingHeaderCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ErrorConstants.ERR_MSG_USER_MISSING_HEADER_CODEVALUE));
        }
    }

    private CodeRegistry createOrUpdateCodeRegistry(final CodeRegistry fromCodeRegistry) {
        final Map<String, CodeRegistry> existingCodeRegistriesMap = parserUtils.getCodeRegistriesMap();
        final CodeRegistry codeRegistry;
        CodeRegistry existingCodeRegistry = existingCodeRegistriesMap.get(fromCodeRegistry.getCodeValue());
        if (existingCodeRegistry != null) {
            codeRegistry = updateCodeRegistry(existingCodeRegistry, fromCodeRegistry);
        } else {
            codeRegistry = createCodeRegistry(fromCodeRegistry);
        }
        return codeRegistry;
    }

    private CodeRegistry updateCodeRegistry(final CodeRegistry codeRegistry,
                                            final CodeRegistry fromCodeRegistry) {
        final Date timeStamp = new Date(System.currentTimeMillis());
        final String uri = apiUtils.createResourceUri(API_PATH_CODEREGISTRIES, fromCodeRegistry.getCodeValue());
        boolean hasChanges = false;
        if (!Objects.equals(codeRegistry.getUri(), uri)) {
            codeRegistry.setUri(uri);
            hasChanges = true;
        }
        for (final Map.Entry<String, String> entry : fromCodeRegistry.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(codeRegistry.getPrefLabel(language), value)) {
                codeRegistry.setPrefLabel(language, value);
                hasChanges = true;
            }
        }
        for (final Map.Entry<String, String> entry : fromCodeRegistry.getDefinition().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(codeRegistry.getDefinition(language), value)) {
                codeRegistry.setDefinition(language, value);
                hasChanges = true;
            }
        }
        if (hasChanges) {
            codeRegistry.setModified(timeStamp);
        }
        return codeRegistry;
    }

    private CodeRegistry createCodeRegistry(final CodeRegistry fromCodeRegistry) {
        final Date timeStamp = new Date(System.currentTimeMillis());
        final CodeRegistry codeRegistry = new CodeRegistry();
        codeRegistry.setId(UUID.randomUUID());
        codeRegistry.setCodeValue(fromCodeRegistry.getCodeValue());
        codeRegistry.setModified(timeStamp);
        codeRegistry.setOrganizations(fromCodeRegistry.getOrganizations());
        for (Map.Entry<String, String> entry : fromCodeRegistry.getPrefLabel().entrySet()) {
            codeRegistry.setPrefLabel(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : fromCodeRegistry.getDefinition().entrySet()) {
            codeRegistry.setDefinition(entry.getKey(), entry.getValue());
        }
        codeRegistry.setUri(apiUtils.createCodeRegistryUri(codeRegistry));
        return codeRegistry;
    }

    private Set<Organization> resolveOrganizations(final String organizationsString) {
        final Set<Organization> organizations = new HashSet<>();
        if (organizationsString != null && !organizationsString.isEmpty()) {
            for (final String organizationId : organizationsString.split(";")) {
                final Organization organization = organizationRepository.findById(UUID.fromString(organizationId));
                if (organization != null) {
                    organizations.add(organization);
                }
            }
        }
        return organizations;
    }
}
