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
import java.util.Set;
import java.util.UUID;

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
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.OrganizationDTO;
import fi.vm.yti.codelist.intake.exception.CsvParsingException;
import fi.vm.yti.codelist.intake.exception.ExcelParsingException;
import fi.vm.yti.codelist.intake.exception.JsonParsingException;
import fi.vm.yti.codelist.intake.exception.MissingHeaderCodeValueException;
import fi.vm.yti.codelist.intake.model.ErrorModel;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Service
public class CodeRegistryParser extends AbstractBaseParser {

    private static final Logger LOG = LoggerFactory.getLogger(CodeRegistryParser.class);

    public CodeRegistryDTO parseCodeRegistryFromJsonData(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final CodeRegistryDTO fromCodeRegistry;
        try {
            fromCodeRegistry = mapper.readValue(jsonPayload, CodeRegistryDTO.class);
        } catch (final IOException e) {
            LOG.error("CodeRegistry parsing failed from JSON!", e);
            throw new JsonParsingException(ERR_MSG_USER_406);
        }
        return fromCodeRegistry;
    }

    public Set<CodeRegistryDTO> parseCodeRegistriesFromJsonData(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final Set<CodeRegistryDTO> fromCodeRegistries;
        final Set<String> codeValues = new HashSet<>();
        try {
            fromCodeRegistries = mapper.readValue(jsonPayload, new TypeReference<Set<CodeRegistryDTO>>() {
            });
        } catch (final IOException e) {
            LOG.error("CodeRegistries parsing failed from JSON!", e);
            throw new JsonParsingException(ERR_MSG_USER_406);
        }
        for (final CodeRegistryDTO fromCodeRegistry : fromCodeRegistries) {
            checkForDuplicateCodeValueInImportData(codeValues, fromCodeRegistry.getCodeValue());
            codeValues.add(fromCodeRegistry.getCodeValue());
        }
        return fromCodeRegistries;
    }

    public Set<CodeRegistryDTO> parseCodeRegistriesFromCsvInputStream(final InputStream inputStream) {
        final Set<CodeRegistryDTO> codeRegistries = new HashSet<>();
        final Set<String> codeValues = new HashSet<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
            final Map<String, Integer> headerMap = csvParser.getHeaderMap();
            final Map<String, Integer> prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
            final Map<String, Integer> definitionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DEFINITION_PREFIX);
            final List<CSVRecord> records = csvParser.getRecords();
            records.forEach(record -> {
                final CodeRegistryDTO fromCodeRegistry = new CodeRegistryDTO();
                final String codeValue = parseCodeValueFromRecord(record);
                checkForDuplicateCodeValueInImportData(codeValues, codeValue);
                codeValues.add(codeValue);
                fromCodeRegistry.setCodeValue(codeValue);
                fromCodeRegistry.setOrganizations(resolveOrganizations(record.get(CONTENT_HEADER_ORGANIZATION)));
                fromCodeRegistry.setPrefLabel(parseLocalizedValueFromCsvRecord(prefLabelHeaders, record));
                fromCodeRegistry.setDefinition(parseLocalizedValueFromCsvRecord(definitionHeaders, record));
                codeRegistries.add(fromCodeRegistry);
            });
        } catch (final IllegalArgumentException e) {
            LOG.error("Duplicate header value found in CSV!", e);
            throw new CsvParsingException(ERR_MSG_USER_DUPLICATE_HEADER_VALUE);
        } catch (final IOException e) {
            LOG.error("Error parsing CSV file!", e);
            throw new CsvParsingException(ERR_MSG_USER_ERROR_PARSING_CSV_FILE);
        }
        return codeRegistries;
    }

    @SuppressFBWarnings("UC_USELESS_OBJECT")
    public Set<CodeRegistryDTO> parseCodeRegistriesFromExcelInputStream(final InputStream inputStream) {
        final Set<CodeRegistryDTO> codeRegistries = new HashSet<>();
        final Set<String> codeValues = new HashSet<>();
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
                    final CodeRegistryDTO fromCodeRegistry = new CodeRegistryDTO();
                    final String codeValue = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODEVALUE)));
                    if (codeValue == null || codeValue.trim().isEmpty()) {
                        continue;
                    }
                    checkForDuplicateCodeValueInImportData(codeValues, codeValue);
                    codeValues.add(codeValue);
                    fromCodeRegistry.setCodeValue(codeValue);
                    fromCodeRegistry.setOrganizations(resolveOrganizations(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ORGANIZATION)))));
                    fromCodeRegistry.setPrefLabel(parseLocalizedValueFromExcelRow(prefLabelHeaders, row, formatter));
                    fromCodeRegistry.setDefinition(parseLocalizedValueFromExcelRow(definitionHeaders, row, formatter));
                    codeRegistries.add(fromCodeRegistry);
                }
            }
        } catch (final InvalidFormatException | IOException | POIXMLException e) {
            LOG.error("Error parsing Excel file!", e);
            throw new ExcelParsingException(ERR_MSG_USER_ERROR_PARSING_EXCEL_FILE);
        }
        return codeRegistries;
    }

    private void validateRequiredCodeHeaders(final Map<String, Integer> headerMap) {
        if (!headerMap.containsKey(CONTENT_HEADER_CODEVALUE)) {
            throw new MissingHeaderCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_MISSING_HEADER_CODEVALUE));
        }
    }

    private Set<OrganizationDTO> resolveOrganizations(final String organizationsString) {
        final Set<OrganizationDTO> organizations = new HashSet<>();
        if (organizationsString != null && !organizationsString.isEmpty()) {
            for (final String organizationId : organizationsString.split(";")) {
                final OrganizationDTO organization = new OrganizationDTO();
                organization.setId(UUID.fromString(organizationId));
            }
        }
        return organizations;
    }
}
