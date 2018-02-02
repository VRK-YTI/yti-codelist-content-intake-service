package fi.vm.yti.codelist.intake.parser;

import fi.vm.yti.codelist.intake.exception.MissingCodeValueException;
import fi.vm.yti.codelist.intake.exception.MissingHeaderException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import fi.vm.yti.codelist.intake.exception.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.exception.CodeParsingException;
import fi.vm.yti.codelist.intake.exception.ExistingCodeException;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
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
     * Parses the .csv Code-file and returns the codes as a set.
     *
     * @param codeScheme  CodeScheme codeValue identifier.
     * @param inputStream The Code -file.
     * @return Set of Code objects.
     */
    public Set<Code> parseCodesFromCsvInputStream(final CodeScheme codeScheme,
                                                  final InputStream inputStream) throws Exception {
        final Map<String, Code> codes = new HashMap<>();
        final Map<String, String> broaderCodeMapping = new HashMap<>();
        if (codeScheme != null) {
            try (final InputStreamReader inputStreamReader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
                 final BufferedReader in = new BufferedReader(inputStreamReader);
                 final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
                final Map<String, Integer> headerMap = csvParser.getHeaderMap();
                if (!headerMap.containsKey(CONTENT_HEADER_CODEVALUE)) {
                    throw new MissingHeaderException("Missing CODEVALUE header.");
                }
                if (!headerMap.containsKey(CONTENT_HEADER_STATUS)) {
                    throw new MissingHeaderException("Missing STATUS header.");
                }
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
                    // This try-catch block prevents an ugly and large vomit of non-UTF-8 characters from polluting the log.
                    // The problem emanates from Apache's CVSRecord itself so hacking around it for now like this.
                    // This problem happens for example when the user specifies CSV but gives Excel with certain kind of data.
                    try {
                        record.get(CONTENT_HEADER_ID);
                    } catch (IllegalArgumentException e) {
                        throw new WebApplicationException("A serious problem with the CSV file (possibly erroneously an Excel-file was used).");
                    }
                    if (record.get(CONTENT_HEADER_CODEVALUE) == null || record.get(CONTENT_HEADER_CODEVALUE).equals("")) {
                        throw new MissingCodeValueException("A row is missing the codevalue.");
                    }
                    if (record.get(CONTENT_HEADER_STATUS) == null || record.get(CONTENT_HEADER_STATUS).equals("")) {
                        throw new MissingCodeValueException("A row is missing the status.");
                    }
                    final UUID id = parseUUIDFromString(record.get(CONTENT_HEADER_ID));
                    final String codeValue = record.get(CONTENT_HEADER_CODEVALUE);
                    final Map<String, String> prefLabel = new LinkedHashMap<>();
                    prefLabelHeaders.forEach((language, header) ->
                        prefLabel.put(language, record.get(header)));
                    final Map<String, String> definition = new LinkedHashMap<>();
                    definitionHeaders.forEach((language, header) ->
                        definition.put(language, record.get(header)));
                    final Map<String, String> description = new LinkedHashMap<>();
                    descriptionHeaders.forEach((language, header) ->
                        description.put(language, record.get(header)));
                    final String shortName;
                    if (headerMap.containsKey(CONTENT_HEADER_SHORTNAME)) {
                        shortName = record.get(CONTENT_HEADER_SHORTNAME);
                    } else {
                        shortName = null;
                    }
                    if (headerMap.containsKey(CONTENT_HEADER_BROADER)) {
                        final String broaderCodeCodeValue = record.get(CONTENT_HEADER_BROADER);
                        if (broaderCodeCodeValue != null && !broaderCodeCodeValue.isEmpty()) {
                            broaderCodeMapping.put(codeValue, broaderCodeCodeValue);
                        }
                    }
                    final String hierarchyLevel;
                    if (headerMap.containsKey(CONTENT_HEADER_HIERARCHYLEVEL)) {
                        hierarchyLevel = record.get(CONTENT_HEADER_HIERARCHYLEVEL);
                    } else {
                        hierarchyLevel = null;
                    }
                    final String statusString = record.get(CONTENT_HEADER_STATUS);
                    final Status status;
                    if (!statusString.isEmpty()) {
                        status = Status.valueOf(statusString);
                    } else {
                        status = Status.DRAFT;
                    }
                    final ISO8601DateFormat dateFormat = new ISO8601DateFormat();
                    Date startDate = null;
                    final String startDateString = record.get(CONTENT_HEADER_STARTDATE);
                    if (!startDateString.isEmpty()) {
                        try {
                            startDate = dateFormat.parse(startDateString);
                        } catch (ParseException e) {
                            LOG.error("Parsing startDate for code: " + codeValue + " failed from string: " + startDateString);
                            throw e;
                        }
                    }
                    Date endDate = null;
                    final String endDateString = record.get(CONTENT_HEADER_ENDDATE);
                    if (!endDateString.isEmpty()) {
                        try {
                            endDate = dateFormat.parse(endDateString);
                        } catch (ParseException e) {
                            LOG.error("Parsing endDate for code: " + codeValue + " failed from string: " + endDateString);
                            throw e;
                        }
                    }
                    final Code code = createOrUpdateCode(codeScheme, id, codeValue, status, shortName, hierarchyLevel, startDate, endDate, prefLabel, description, definition);
                    if (code != null) {
                        codes.put(code.getCodeValue(), code);
                    }
                }
            }
        }
        setBroaderCodes(broaderCodeMapping, codes);
        return new HashSet<>(codes.values());
    }

    /**
     * Parses the .xls or .xlsx Code-inputstream and returns the codes as a set.
     *
     * @param codeScheme  CodeScheme for which these codes are for.
     * @param inputStream The Code containing Excel -inputstream.
     * @return Set of Code objects.
     */
    public Set<Code> parseCodesFromExcelInputStream(final CodeScheme codeScheme,
                                                    final InputStream inputStream) throws Exception {
        try (final Workbook workbook = WorkbookFactory.create(inputStream)) {
            return parseCodesFromExcel(codeScheme, workbook);
        }
    }

    /**
     * Parses the .xls or .xlsx Code-file and returns the codes as a set.
     *
     * @param codeScheme CodeScheme for which these codes are for.
     * @param workbook   The Code containing Excel -file.
     * @return Set of Code objects.
     */
    @SuppressFBWarnings("UC_USELESS_OBJECT")
    public Set<Code> parseCodesFromExcel(final CodeScheme codeScheme,
                                         final Workbook workbook) throws Exception {
        final Map<String, Code> codes = new HashMap<>();
        final Map<String, String> broaderCodeMapping = new HashMap<>();
        if (codeScheme != null) {
            final DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheet(EXCEL_SHEET_CODES);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            final Iterator<Row> rowIterator = sheet.rowIterator();
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
                    if (!genericHeaders.containsKey(CONTENT_HEADER_CODEVALUE) ) {
                        throw new MissingHeaderException("Missing CODEVALUE header.");
                    }
                    if (!genericHeaders.containsKey(CONTENT_HEADER_STATUS)) {
                        throw new MissingHeaderException("Missing STATUS header.");
                    }
                    firstRow = false;
                } else if (row.getPhysicalNumberOfCells() > 0 && !isRowEmpty(row)) {
                    if (formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_CODEVALUE))) == null ||
                            formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_CODEVALUE))).equals("")) {
                        throw new MissingCodeValueException("A row is missing the codevalue.");
                    }
                    if (formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_STATUS))) == null ||
                            formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_STATUS))).equals("")) {
                        throw new MissingCodeValueException("A row is missing the status.");
                    }

                    final String codeValue = formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_CODEVALUE)));
                    final UUID id = parseUUIDFromString(formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_ID))));
                    final Map<String, String> prefLabel = new LinkedHashMap<>();
                    prefLabelHeaders.forEach((language, header) ->
                        prefLabel.put(language, formatter.formatCellValue(row.getCell(header))));
                    final Map<String, String> definition = new LinkedHashMap<>();
                    definitionHeaders.forEach((language, header) ->
                        definition.put(language, formatter.formatCellValue(row.getCell(header))));
                    final Map<String, String> description = new LinkedHashMap<>();
                    descriptionHeaders.forEach((language, header) ->
                        description.put(language, formatter.formatCellValue(row.getCell(header))));
                    final String shortName;
                    if (genericHeaders.containsKey(CONTENT_HEADER_SHORTNAME)) {
                        shortName = formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_SHORTNAME)));
                    } else {
                        shortName = null;
                    }
                    final String hierarchyLevel;
                    if (genericHeaders.containsKey(CONTENT_HEADER_HIERARCHYLEVEL)) {
                        hierarchyLevel = formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_HIERARCHYLEVEL)));
                    } else {
                        hierarchyLevel = null;
                    }
                    if (genericHeaders.containsKey(CONTENT_HEADER_BROADER)) {
                        final String broaderCodeCodeValue = formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_BROADER)));
                        if (broaderCodeCodeValue != null && !broaderCodeCodeValue.isEmpty()) {
                            broaderCodeMapping.put(codeValue, broaderCodeCodeValue);
                        }
                    }
                    final String statusString = formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_STATUS)));
                    final Status status = Status.valueOf(statusString);
                    final ISO8601DateFormat dateFormat = new ISO8601DateFormat();
                    Date startDate = null;
                    final String startDateString = formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_STARTDATE)));
                    if (!startDateString.isEmpty()) {
                        try {
                            startDate = dateFormat.parse(startDateString);
                        } catch (ParseException e) {
                            LOG.error("Parsing startDate for code: " + codeValue + " failed from string: " + startDateString);
                            throw new CodeParsingException("STARTDATE header does not have valid value, import failed!");
                        }
                    }
                    Date endDate = null;
                    final String endDateString = formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_ENDDATE)));
                    if (!endDateString.isEmpty()) {
                        try {
                            endDate = dateFormat.parse(endDateString);
                        } catch (ParseException e) {
                            LOG.error("Parsing endDate for code: " + codeValue + " failed from string: " + endDateString);
                            throw new CodeParsingException("ENDDATE header does not have valid value, import failed!");
                        }
                    }
                    final Code code = createOrUpdateCode(codeScheme, id, codeValue, status, shortName, hierarchyLevel, startDate, endDate, prefLabel, description, definition);
                    if (code != null) {
                        codes.put(code.getCodeValue(), code);
                    }
                }
            }
        }
        setBroaderCodes(broaderCodeMapping, codes);
        return new HashSet<>(codes.values());
    }

    private Code createOrUpdateCode(final CodeScheme codeScheme,
                                    final UUID id,
                                    final String codeValue,
                                    final Status status,
                                    final String shortName,
                                    final String hierarchyLevel,
                                    final Date startDate,
                                    final Date endDate,
                                    final Map<String, String> prefLabel,
                                    final Map<String, String> description,
                                    final Map<String, String> definition) {
        Code code = null;
        if (id != null) {
            code = codeRepository.findById(id);
        }
        if (Status.VALID == status) {
            final Code existingCode = codeRepository.findByCodeSchemeAndCodeValueAndStatus(codeScheme, codeValue, status.toString());
            if (existingCode != code) {
                throw new ExistingCodeException("Existing value already found with status VALID for code: " + codeValue + ", cancel update!");
            }
        }
        if (code != null) {
            final String uri = apiUtils.createCodeUri(codeScheme.getCodeRegistry(), codeScheme, code);
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
            if (!Objects.equals(code.getHierarchyLevel(), hierarchyLevel)) {
                code.setHierarchyLevel(hierarchyLevel);
                hasChanges = true;
            }
            for (final Map.Entry<String, String> entry : prefLabel.entrySet()) {
                final String language = entry.getKey();
                final String value = entry.getValue();
                if (!Objects.equals(code.getPrefLabel(language), value)) {
                    code.setPrefLabel(language, value);
                    hasChanges = true;
                }
            }
            for (final Map.Entry<String, String> entry : description.entrySet()) {
                final String language = entry.getKey();
                final String value = entry.getValue();
                if (!Objects.equals(code.getDescription(language), value)) {
                    code.setDescription(language, value);
                    hasChanges = true;
                }
            }
            for (final Map.Entry<String, String> entry : definition.entrySet()) {
                final String language = entry.getKey();
                final String value = entry.getValue();
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
                code.setId(uuid);
            }
            code.setStatus(status.toString());
            code.setCodeScheme(codeScheme);
            code.setCodeValue(codeValue);
            code.setShortName(shortName);
            code.setHierarchyLevel(hierarchyLevel);
            final Date timeStamp = new Date(System.currentTimeMillis());
            code.setModified(timeStamp);
            for (Map.Entry<String, String> entry : prefLabel.entrySet()) {
                code.setPrefLabel(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, String> entry : description.entrySet()) {
                code.setDescription(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, String> entry : definition.entrySet()) {
                code.setDefinition(entry.getKey(), entry.getValue());
            }
            code.setStartDate(startDate);
            code.setEndDate(endDate);
            code.setUri(apiUtils.createCodeUri(codeScheme.getCodeRegistry(), codeScheme, code));
        }
        return code;
    }

    private void setBroaderCodes(final Map<String, String> broaderCodeMapping,
                                 final Map<String, Code> codes) {
        broaderCodeMapping.forEach((codeCodeValue, broaderCodeCodeValue) -> {
            final Code code = codes.get(codeCodeValue);
            final Code broaderCode = codes.get(broaderCodeCodeValue);
            if (broaderCode != null) {
                code.setBroaderCodeId(broaderCode.getId());
            }
        });
    }
}
