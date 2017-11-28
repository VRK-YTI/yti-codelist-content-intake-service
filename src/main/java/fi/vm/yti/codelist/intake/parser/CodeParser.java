package fi.vm.yti.codelist.intake.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
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

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.util.FileUtils;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

/**
 * Class that handles parsing of codes from source data.
 */
@Service
public class CodeParser extends AbstractBaseParser {

    private static final Logger LOG = LoggerFactory.getLogger(CodeParser.class);
    private final ApiUtils apiUtils;
    private final CodeRepository codeRepository;

    @Inject
    public CodeParser(final ApiUtils apiUtils,
                      final CodeRepository codeRepository) {
        this.apiUtils = apiUtils;
        this.codeRepository = codeRepository;
    }

    /**
     * Parses the .csv Code-file and returns the codes as an arrayList.
     *
     * @param codeScheme  CodeScheme codeValue identifier.
     * @param inputStream The Code -file.
     * @return List of Code objects.
     */
    public List<Code> parseCodesFromCsvInputStream(final CodeScheme codeScheme,
                                                   final InputStream inputStream) throws Exception {
        final List<Code> codes = new ArrayList<>();
        if (codeScheme != null) {
            try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 final BufferedReader in = new BufferedReader(inputStreamReader);
                 final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
                FileUtils.skipBom(in);
                final Map<String, Integer> headerMap = csvParser.getHeaderMap();
                final Map<String, String> prefLabelHeaders = new LinkedHashMap<>();
                final Map<String, String> descriptionHeaders = new LinkedHashMap<>();
                final Map<String, String> definitionHeaders = new LinkedHashMap<>();
                headerMap.keySet().forEach(header -> {
                    if (header.startsWith(CONTENT_HEADER_PREFLABEL_PREFIX)) {
                        prefLabelHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_PREFLABEL_PREFIX, header), header);
                    } else if (header.startsWith(CONTENT_HEADER_DESCRIPTION_PREFIX)) {
                        descriptionHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_DESCRIPTION_PREFIX, header), header);
                    } else if (header.startsWith(CONTENT_HEADER_DEFINITION_PREFIX)) {
                        definitionHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_DEFINITION_PREFIX, header), header);
                    }
                });
                final List<CSVRecord> records = csvParser.getRecords();
                for (final CSVRecord record : records) {
                    final UUID id = parseUUIDFromString(record.get(CONTENT_HEADER_ID));
                    final String codeValue = record.get(CONTENT_HEADER_CODEVALUE);
                    final Map<String, String> prefLabel = new LinkedHashMap<>();
                    prefLabelHeaders.forEach((language, header) -> {
                        prefLabel.put(language, record.get(header));
                    });
                    final Map<String, String> definition = new LinkedHashMap<>();
                    definitionHeaders.forEach((language, header) -> {
                        definition.put(language, record.get(header));
                    });
                    final Map<String, String> description = new LinkedHashMap<>();
                    descriptionHeaders.forEach((language, header) -> {
                        description.put(language, record.get(header));
                    });
                    final String shortName = record.get(CONTENT_HEADER_SHORTNAME);
                    final Status status = Status.valueOf(record.get(CONTENT_HEADER_STATUS));
                    final ISO8601DateFormat dateFormat = new ISO8601DateFormat();
                    Date startDate = null;
                    final String startDateString = record.get(CONTENT_HEADER_STARTDATE);
                    if (!startDateString.isEmpty()) {
                        try {
                            startDate = dateFormat.parse(startDateString);
                        } catch (ParseException e) {
                            LOG.error("Parsing startDate for code: " + codeValue + " failed from string: " + startDateString);
                        }
                    }
                    Date endDate = null;
                    final String endDateString = record.get(CONTENT_HEADER_ENDDATE);
                    if (!endDateString.isEmpty()) {
                        try {
                            endDate = dateFormat.parse(endDateString);
                        } catch (ParseException e) {
                            LOG.error("Parsing endDate for code: " + codeValue + " failed from string: " + endDateString);
                        }
                    }
                    final Code code = createOrUpdateCode(codeScheme, id, codeValue, status, shortName, startDate, endDate, prefLabel, description, definition);
                    if (code != null) {
                        codes.add(code);
                    }
                }
            } catch (final IOException e) {
                LOG.error("Parsing codes failed: " + e.getMessage());
            }
        }
        return codes;
    }

    /**
     * Parses the .xls Code-file and returns the codes as an arrayList.
     *
     * @param codeScheme  CodeScheme for which these codes are for.
     * @param inputStream The Code containing Excel -file.
     * @return List of Code objects.
     */
    public List<Code> parseCodesFromExcelInputStream(final CodeScheme codeScheme,
                                                     final InputStream inputStream) throws Exception {
        final List<Code> codes = new ArrayList<>();
        if (codeScheme != null) {
            try (final Workbook workbook = new XSSFWorkbook(inputStream)) {
                final Sheet codesSheet = workbook.getSheet(EXCEL_SHEET_CODES);
                final Iterator<Row> rowIterator = codesSheet.rowIterator();
                final Map<String, Integer> genericHeaders = new LinkedHashMap<>();
                final Map<String, Integer> prefLabelHeaders = new LinkedHashMap<>();
                final Map<String, Integer> descriptionHeaders = new LinkedHashMap<>();
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
                            } else if (value.startsWith(CONTENT_HEADER_DESCRIPTION_PREFIX)) {
                                descriptionHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_DESCRIPTION_PREFIX, value), index);
                            } else if (value.startsWith(CONTENT_HEADER_DEFINITION_PREFIX)) {
                                definitionHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_DEFINITION_PREFIX, value), index);
                            } else {
                                genericHeaders.put(value, index);
                            }
                        }
                        firstRow = false;
                    } else {
                        final UUID id = parseUUIDFromString(row.getCell(genericHeaders.get(CONTENT_HEADER_ID)).getStringCellValue());
                        final String codeValue = row.getCell(genericHeaders.get(CONTENT_HEADER_CODEVALUE)).getStringCellValue();
                        final Map<String, String> prefLabel = new LinkedHashMap<>();
                        prefLabelHeaders.forEach((language, haeder) -> {
                            prefLabel.put(language, row.getCell(prefLabelHeaders.get(language)).getStringCellValue());
                        });
                        final Map<String, String> definition = new LinkedHashMap<>();
                        definitionHeaders.forEach((language, header) -> {
                            definition.put(language, row.getCell(header).getStringCellValue());
                        });
                        final Map<String, String> description = new LinkedHashMap<>();
                        descriptionHeaders.forEach((language, header) -> {
                            description.put(language, row.getCell(header).getStringCellValue());
                        });
                        final String shortName = row.getCell(genericHeaders.get(CONTENT_HEADER_SHORTNAME)).getStringCellValue();
                        final Status status = Status.valueOf(row.getCell(genericHeaders.get(CONTENT_HEADER_STATUS)).getStringCellValue());
                        final ISO8601DateFormat dateFormat = new ISO8601DateFormat();
                        Date startDate = null;
                        final String startDateString = row.getCell(genericHeaders.get(CONTENT_HEADER_STARTDATE)).getStringCellValue();
                        if (!startDateString.isEmpty()) {
                            try {
                                startDate = dateFormat.parse(startDateString);
                            } catch (ParseException e) {
                                LOG.error("Parsing startDate for code: " + codeValue + " failed from string: " + startDateString);
                            }
                        }
                        Date endDate = null;
                        final String endDateString = row.getCell(genericHeaders.get(CONTENT_HEADER_ENDDATE)).getStringCellValue();
                        if (!endDateString.isEmpty()) {
                            try {
                                endDate = dateFormat.parse(endDateString);
                            } catch (ParseException e) {
                                LOG.error("Parsing endDate for code: " + codeValue + " failed from string: " + endDateString);
                            }
                        }
                        final Code code = createOrUpdateCode(codeScheme, id, codeValue, status, shortName, startDate, endDate, prefLabel, description, definition);
                        if (code != null) {
                            codes.add(code);
                        }
                    }
                }
            }
        }
        return codes;
    }

    private Code createOrUpdateCode(final CodeScheme codeScheme,
                                    final UUID id,
                                    final String codeValue,
                                    final Status status,
                                    final String shortName,
                                    final Date startDate,
                                    final Date endDate,
                                    final Map<String, String> prefLabel,
                                    final Map<String, String> description,
                                    final Map<String, String> definition) throws Exception {
        Code code = null;
        if (id != null) {
            code = codeRepository.findById(id);
        }
        String uri = null;
        if (Status.VALID == status) {
            uri = apiUtils.createResourceUrl(API_PATH_CODEREGISTRIES + "/" + codeScheme.getCodeRegistry().getCodeValue() + API_PATH_CODESCHEMES + "/" + codeScheme.getCodeValue() + API_PATH_CODES, codeValue);
            final Code existingCode = codeRepository.findByCodeSchemeAndCodeValueAndStatus(codeScheme, codeValue, status.toString());
            if (existingCode != code) {
                LOG.error("Existing value already found, cancel update!");
                throw new Exception("Existing value already found with status VALID for code: " + codeValue + ", cancel update!");
            }
        } else if (id != null) {
            uri = apiUtils.createResourceUrl(API_PATH_CODEREGISTRIES + "/" + codeScheme.getCodeRegistry().getCodeValue() + API_PATH_CODESCHEMES + "/" + codeScheme.getCodeValue() + API_PATH_CODES, id.toString());
        }
        if (code != null) {
            boolean hasChanges = false;
            if (!Objects.equals(code.getStatus(), status.toString())) {
                code.setStatus(status.toString());
                hasChanges = true;
            }
            if (!Objects.equals(code.getCodeScheme(), codeScheme)) {
                code.setCodeScheme(codeScheme);
                hasChanges = true;
            }
            if (!Objects.equals(code.getUri(), uri)) {
                code.setUri(uri);
                hasChanges = true;
            }
            if (!Objects.equals(code.getShortName(), shortName)) {
                code.setShortName(shortName);
                hasChanges = true;
            }
            for (final String language : prefLabel.keySet()) {
                final String value = prefLabel.get(language);
                if (!Objects.equals(code.getPrefLabel(language), value)) {
                    code.setPrefLabel(language, value);
                    hasChanges = true;
                }
            }
            for (final String language : description.keySet()) {
                final String value = description.get(language);
                if (!Objects.equals(code.getDescription(language), value)) {
                    code.setDescription(language, value);
                    hasChanges = true;
                }
            }
            for (final String language : definition.keySet()) {
                final String value = definition.get(language);
                if (!Objects.equals(code.getDefinition(language), value)) {
                    code.setDefinition(language, value);
                    hasChanges = true;
                }
            }
            if (!Objects.equals(code.getStartDate(), startDate)) {
                code.setStartDate(startDate);
                hasChanges = true;
            }
            if (!Objects.equals(code.getEndDate(), endDate)) {
                code.setEndDate(endDate);
                hasChanges = true;
            }
            if (hasChanges) {
                final Date timeStamp = new Date(System.currentTimeMillis());
                code.setModified(timeStamp);
            }
        } else {
            code = new Code();
            if (id != null) {
                code.setId(id);
            } else {
                final UUID uuid = UUID.randomUUID();
                if (status != Status.VALID) {
                    uri = apiUtils.createResourceUrl(API_PATH_CODEREGISTRIES + "/" + codeScheme.getCodeRegistry().getCodeValue() + API_PATH_CODESCHEMES + "/" + codeScheme.getCodeValue() + API_PATH_CODES, uuid.toString());
                }
                code.setId(uuid);
            }
            code.setStatus(status.toString());
            code.setUri(uri);
            code.setCodeScheme(codeScheme);
            code.setCodeValue(codeValue);
            code.setShortName(shortName);
            final Date timeStamp = new Date(System.currentTimeMillis());
            code.setModified(timeStamp);
            for (final String language : prefLabel.keySet()) {
                code.setPrefLabel(language, prefLabel.get(language));
            }
            for (final String language : description.keySet()) {
                code.setDescription(language, description.get(language));
            }
            for (final String language : definition.keySet()) {
                code.setDefinition(language, definition.get(language));
            }
            code.setStartDate(startDate);
            code.setEndDate(endDate);
        }
        return code;
    }
}
