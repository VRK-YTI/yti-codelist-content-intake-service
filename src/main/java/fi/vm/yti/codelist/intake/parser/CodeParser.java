package fi.vm.yti.codelist.intake.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.ErrorModel;
import fi.vm.yti.codelist.common.model.ExternalReference;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.exception.CodeParsingException;
import fi.vm.yti.codelist.intake.exception.CsvParsingException;
import fi.vm.yti.codelist.intake.exception.ExcelParsingException;
import fi.vm.yti.codelist.intake.exception.ExistingCodeException;
import fi.vm.yti.codelist.intake.exception.JsonParsingException;
import fi.vm.yti.codelist.intake.exception.MissingHeaderCodeValueException;
import fi.vm.yti.codelist.intake.exception.MissingHeaderStatusException;
import fi.vm.yti.codelist.intake.exception.MissingRowValueCodeValueException;
import fi.vm.yti.codelist.intake.exception.MissingRowValueStatusException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.ExternalReferenceRepository;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

/**
 * Class that handles parsing of codes from source data.
 */
@Service
public class CodeParser extends AbstractBaseParser {

    private final ApiUtils apiUtils;
    private final CodeRepository codeRepository;
    private final ExternalReferenceRepository externalReferenceRepository;
    private final ExternalReferenceParser externalReferenceParser;
    private static int flatCount = 0;
    private static Boolean hasFlatOrder = true;

    @Inject
    public CodeParser(final ApiUtils apiUtils,
                      final CodeRepository codeRepository,
                      final ExternalReferenceRepository externalReferenceRepository,
                      final ExternalReferenceParser externalReferenceParser) {
        this.apiUtils = apiUtils;
        this.codeRepository = codeRepository;
        this.externalReferenceRepository = externalReferenceRepository;
        this.externalReferenceParser = externalReferenceParser;
    }

    public Set<Code> parseCodesFromCsvInputStream(final CodeScheme codeScheme,
                                                  final InputStream inputStream) {
        final Map<String, Code> codes = new HashMap<>();
        final Map<String, String> broaderCodeMapping = new HashMap<>();
        if (codeScheme != null) {
            try (final InputStreamReader inputStreamReader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
                 final BufferedReader in = new BufferedReader(inputStreamReader);
                 final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
                final Map<String, Integer> headerMap = csvParser.getHeaderMap();
                final Map<String, Integer> prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
                final Map<String, Integer> definitionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DEFINITION_PREFIX);
                final Map<String, Integer> descriptionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DESCRIPTION_PREFIX);
                validateRequiredCodeHeaders(headerMap);
                final List<CSVRecord> records = csvParser.getRecords();
                for (final CSVRecord record : records) {
                    validateRequiredDataOnRecord(record);
                    final Code fromCode = new Code();
                    fromCode.setId(parseUUIDFromString(record.get(CONTENT_HEADER_ID)));
                    final String codeValue = record.get(CONTENT_HEADER_CODEVALUE);
                    checkForDuplicateCodeValueInImportData(codes, codeValue);
                    fromCode.setCodeValue(codeValue);
                    fromCode.setPrefLabel(parseLocalizedValueFromCsvRecord(prefLabelHeaders, record));
                    fromCode.setDefinition(parseLocalizedValueFromCsvRecord(definitionHeaders, record));
                    fromCode.setDescription(parseLocalizedValueFromCsvRecord(descriptionHeaders, record));
                    fromCode.setShortName(parseShortNameFromCsvRecord(record));
                    fromCode.setFlatOrder(resolveFlatOrderFromCsvRecord(headerMap, record));
                    fromCode.setChildOrder(Integer.parseInt(record.get(CONTENT_HEADER_CHILDORDER)));
                    if (headerMap.containsKey(CONTENT_HEADER_BROADER)) {
                        final String broaderCodeCodeValue = record.get(CONTENT_HEADER_BROADER);
                        if (broaderCodeCodeValue != null && !broaderCodeCodeValue.isEmpty()) {
                            broaderCodeMapping.put(codeValue, broaderCodeCodeValue);
                        } else {
                            broaderCodeMapping.put(codeValue, null);
                        }
                    }
                    fromCode.setHierarchyLevel(resolveHierarchyLevelFromCsvRecord(headerMap, record));
                    fromCode.setStatus(parseStatusValueFromString(record.get(CONTENT_HEADER_STATUS)));
                    fromCode.setStartDate(parseStartDateFromString(record.get(CONTENT_HEADER_STARTDATE), String.valueOf(record.getRecordNumber())));
                    fromCode.setEndDate(parseEndDateString(record.get(CONTENT_HEADER_ENDDATE), String.valueOf(record.getRecordNumber())));
                    final Code code = createOrUpdateCode(codeScheme, fromCode);
                    if (code != null) {
                        codes.put(code.getCodeValue(), code);
                    }
                }
                if (headerMap.containsKey(CONTENT_HEADER_BROADER)) {
                    setBroaderCodesAndEvaluateHierarchyLevels(broaderCodeMapping, codes);
                }
            } catch (final IllegalArgumentException e) {
                throw new CsvParsingException(ERR_MSG_USER_DUPLICATE_HEADER_VALUE);
            } catch (final IOException e) {
                throw new CsvParsingException(ERR_MSG_USER_ERROR_PARSING_CSV_FILE);
            }
        }
        return new HashSet<>(codes.values());
    }

    private Integer resolveHierarchyLevelFromCsvRecord(final Map<String, Integer> headerMap,
                                                       final CSVRecord record) {
        final Integer hierarchyLevel;
        if (headerMap.containsKey(CONTENT_HEADER_HIERARCHYLEVEL) && !headerMap.containsKey(CONTENT_HEADER_BROADER)) {
            hierarchyLevel = resolveHierarchyLevelFromString(record.get(CONTENT_HEADER_HIERARCHYLEVEL));
        } else {
            hierarchyLevel = null;
        }
        return hierarchyLevel;
    }

    private Integer resolveHierarchyLevelFromExcelRow(final Map<String, Integer> headerMap,
                                                      final Row row,
                                                      final DataFormatter formatter) {
        final Integer hierarchyLevel;
        if (headerMap.containsKey(CONTENT_HEADER_HIERARCHYLEVEL) && !headerMap.containsKey(CONTENT_HEADER_BROADER)) {
            hierarchyLevel = resolveHierarchyLevelFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_HIERARCHYLEVEL))));
        } else {
            hierarchyLevel = null;
        }
        return hierarchyLevel;
    }

    private Integer resolveHierarchyLevelFromString(final String hierarchyLevelString) {
        final Integer hierarchyLevel;
        if (!hierarchyLevelString.isEmpty()) {
            try {
                hierarchyLevel = Integer.parseInt(hierarchyLevelString);
            } catch (final NumberFormatException e) {
                throw new CodeParsingException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    ERR_MSG_USER_HIERARCHY_LEVEL_INVALID_VALUE));
            }
        } else {
            hierarchyLevel = null;
        }
        return hierarchyLevel;
    }

    private Integer resolveFlatOrderFromCsvRecord(final Map<String, Integer> headerMap,
                                                       final CSVRecord record) {
        final Integer flatOrder;
        if (hasFlatOrder && headerMap.containsKey(CONTENT_HEADER_FLATORDER)) {
            flatOrder = resolveFlatOrderFromString(record.get(CONTENT_HEADER_FLATORDER));
            flatCount = flatOrder;
        } else {
            flatOrder = null;
        }
        return flatOrder;
    }

    private Integer resolveFlatOrderFromExcelRow(final Map<String, Integer> headerMap,
                                                      final Row row,
                                                      final DataFormatter formatter) {
        final Integer flatOrder;
        if (headerMap.containsKey(CONTENT_HEADER_FLATORDER)) {
            flatOrder = resolveFlatOrderFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_FLATORDER))));
        } else {
            flatOrder = null;
        }
        return flatOrder;
    }

    private Integer resolveFlatOrderFromString(final String flatOrderString) {

        final Integer flatOrder;
        if (!flatOrderString.isEmpty()) {
            try {
               flatOrder = Integer.parseInt(flatOrderString);
            } catch (final NumberFormatException e) {
                throw new CodeParsingException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        ERR_MSG_USER_FLAT_ORDER_INVALID_VALUE));
            }
        } else {
            hasFlatOrder = false;
            final Integer newIndex = flatCount++;
            flatOrder = newIndex;
        }
        return flatOrder;
    }

    private void validateRequiredCodeHeaders(final Map<String, Integer> headerMap) {
        if (!headerMap.containsKey(CONTENT_HEADER_CODEVALUE)) {
            throw new MissingHeaderCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_MISSING_HEADER_CODEVALUE));
        }
        if (!headerMap.containsKey(CONTENT_HEADER_STATUS)) {
            throw new MissingHeaderStatusException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_MISSING_HEADER_STATUS));
        }
    }

    private void validateRequiredDataOnRow(final Row row,
                                           final Map<String, Integer> headerMap,
                                           final DataFormatter formatter) {
        if (formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODEVALUE))) == null ||
            formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODEVALUE))).equals("")) {
            throw new MissingRowValueCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_CODEVALUE, String.valueOf(row.getRowNum() + 1)));
        }
        if (formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STATUS))) == null ||
            formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STATUS))).equals("")) {
            throw new MissingRowValueStatusException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_STATUS, String.valueOf(row.getRowNum() + 1)));
        }
    }

    private void validateRequiredDataOnRecord(final CSVRecord record) {
        // This try-catch block prevents an ugly and large vomit of non-UTF-8 characters from polluting the log.
        // The problem emanates from Apache's CVSRecord itself so hacking around it for now like this.
        // This problem happens for example when the user specifies CSV but gives Excel with certain kind of data.
        try {
            record.get(CONTENT_HEADER_ID);
        } catch (IllegalArgumentException e) {
            throw new CsvParsingException(ERR_MSG_USER_ERROR_PARSING_CSV_FILE);
        }
        if (record.get(CONTENT_HEADER_CODEVALUE) == null || record.get(CONTENT_HEADER_CODEVALUE).equals("")) {
            throw new MissingRowValueCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_CODEVALUE, String.valueOf(record.getRecordNumber() + 1)));
        }
        if (record.get(CONTENT_HEADER_STATUS) == null || record.get(CONTENT_HEADER_STATUS).equals("")) {
            throw new MissingRowValueStatusException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_STATUS, String.valueOf(record.getRecordNumber() + 1)));
        }
    }

    /**
     * Parses the .xls or .xlsx Code-inputstream and returns the codes as a set.
     *
     * @param codeScheme  CodeScheme for which these codes are for.
     * @param inputStream The Code containing Excel -inputstream.
     * @return Set of Code objects.
     */
    public Set<Code> parseCodesFromExcelInputStream(final CodeScheme codeScheme,
                                                    final InputStream inputStream) {
        try (final Workbook workbook = WorkbookFactory.create(inputStream)) {
            return parseCodesFromExcelWorkbook(codeScheme, workbook);
        } catch (final InvalidFormatException | IOException | POIXMLException e) {
            throw new ExcelParsingException(ERR_MSG_USER_ERROR_PARSING_EXCEL_FILE);
        }
    }

    @SuppressFBWarnings("UC_USELESS_OBJECT")
    public Set<Code> parseCodesFromExcelWorkbook(final CodeScheme codeScheme,
                                                 final Workbook workbook) {
        final Map<String, Code> codes = new HashMap<>();
        final Map<String, String> broaderCodeMapping = new HashMap<>();
        if (codeScheme != null) {
            final DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheet(EXCEL_SHEET_CODES);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            final Iterator<Row> rowIterator = sheet.rowIterator();
            boolean firstRow = true;
            Map<String, Integer> headerMap = null;
            Map<String, Integer> prefLabelHeaders = null;
            Map<String, Integer> definitionHeaders = null;
            Map<String, Integer> descriptionHeaders = null;
            while (rowIterator.hasNext()) {
                final Row row = rowIterator.next();
                if (firstRow) {
                    firstRow = false;
                    headerMap = resolveHeaderMap(row);
                    prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
                    definitionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DEFINITION_PREFIX);
                    descriptionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DESCRIPTION_PREFIX);
                    validateRequiredCodeHeaders(headerMap);
                } else if (row.getPhysicalNumberOfCells() > 0 && !isRowEmpty(row)) {
                    validateRequiredDataOnRow(row, headerMap, formatter);
                    final Code fromCode = new Code();
                    final String codeValue = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODEVALUE)));
                    checkForDuplicateCodeValueInImportData(codes, codeValue);
                    fromCode.setCodeValue(codeValue);
                    final UUID id = parseUUIDFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ID))));
                    fromCode.setId(id);
                    fromCode.setPrefLabel(parseLocalizedValueFromExcelRow(prefLabelHeaders, row, formatter));
                    fromCode.setDefinition(parseLocalizedValueFromExcelRow(definitionHeaders, row, formatter));
                    fromCode.setDescription(parseLocalizedValueFromExcelRow(descriptionHeaders, row, formatter));
                    fromCode.setShortName(parseShortNameFromExcelRow(headerMap, row, formatter));
                    fromCode.setHierarchyLevel(resolveHierarchyLevelFromExcelRow(headerMap, row, formatter));
                    fromCode.setFlatOrder(resolveFlatOrderFromExcelRow(headerMap, row, formatter));
                    if (headerMap.containsKey(CONTENT_HEADER_BROADER)) {
                        final String broaderCodeCodeValue = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_BROADER)));
                        if (broaderCodeCodeValue != null && !broaderCodeCodeValue.isEmpty()) {
                            broaderCodeMapping.put(codeValue, broaderCodeCodeValue);
                        }
                    }
                    fromCode.setStatus(parseStatusValueFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STATUS)))));
                    fromCode.setStartDate(parseStartDateFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STARTDATE))), String.valueOf(row.getRowNum())));
                    fromCode.setEndDate(parseEndDateString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ENDDATE))), String.valueOf(row.getRowNum())));
                    final Code code = createOrUpdateCode(codeScheme, fromCode);
                    if (code != null) {
                        codes.put(code.getCodeValue(), code);
                    }
                }
            }
            if (headerMap != null && headerMap.containsKey(CONTENT_HEADER_BROADER)) {
                setBroaderCodesAndEvaluateHierarchyLevels(broaderCodeMapping, codes);
            }
        }
        return new HashSet<>(codes.values());
    }

    public Code parseCodeFromJsonData(final CodeScheme codeScheme,
                                      final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final Code code;
        try {
            final Code fromCode = mapper.readValue(jsonPayload, Code.class);
            code = createOrUpdateCode(codeScheme, fromCode);
            // TODO: Refactor
            final Set<ExternalReference> externalReferences = initializeExternalReferences(fromCode.getExternalReferences(), codeScheme, externalReferenceParser);
            if (!externalReferences.isEmpty()) {
                externalReferenceRepository.save(externalReferences);
                code.setExternalReferences(externalReferences);
            } else {
                code.setExternalReferences(null);
            }
        } catch (final IOException e) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
        }
        return code;
    }

    public Set<Code> parseCodesFromJsonData(final CodeScheme codeScheme,
                                            final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final Map<String, Code> codes = new HashMap<>();
        try {
            final Set<Code> fromCodes = mapper.readValue(jsonPayload, new TypeReference<Set<Code>>() {
            });
            for (final Code fromCode : fromCodes) {
                checkForDuplicateCodeValueInImportData(codes, fromCode.getCodeValue());
                final Code code = createOrUpdateCode(codeScheme, fromCode);
                // TODO: Refactor
                final Set<ExternalReference> externalReferences = initializeExternalReferences(fromCode.getExternalReferences(), codeScheme, externalReferenceParser);
                if (!externalReferences.isEmpty()) {
                    externalReferenceRepository.save(externalReferences);
                    code.setExternalReferences(externalReferences);
                } else {
                    code.setExternalReferences(null);
                }
                codes.put(code.getCodeValue(), code);
            }
        } catch (final IOException e) {
            throw new JsonParsingException(ERR_MSG_USER_406);
        }
        return new HashSet<>(codes.values());
    }

    private Code createOrUpdateCode(final CodeScheme codeScheme,
                                    final Code fromCode) {
        validateCodeForCodeScheme(fromCode);
        if (!startDateIsBeforeEndDateSanityCheck(fromCode.getStartDate(), fromCode.getEndDate())) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_END_BEFORE_START_DATE));
        }
        final Code existingCode;
        if (fromCode.getId() != null) {
            existingCode = codeRepository.findById(fromCode.getId());
            if (existingCode == null) {
                checkForExistingCodeInCodeScheme(codeScheme, fromCode);
            }
        } else {
            existingCode = codeRepository.findByCodeSchemeAndCodeValue(codeScheme, fromCode.getCodeValue());
        }
        final Code code;
        if (existingCode != null) {
            code = updateCode(codeScheme, existingCode, fromCode);
        } else {
            code = createCode(codeScheme, fromCode);
        }
        return code;
    }

    private Code updateCode(final CodeScheme codeScheme,
                            final Code existingCode,
                            final Code fromCode) {
        final String uri = apiUtils.createCodeUri(codeScheme.getCodeRegistry(), codeScheme, existingCode);
        boolean hasChanges = false;
        if (!Objects.equals(existingCode.getStatus(), fromCode.getStatus())) {
            if (Status.valueOf(existingCode.getStatus()).ordinal() >= Status.VALID.ordinal() && Status.valueOf(fromCode.getStatus()).ordinal() < Status.VALID.ordinal()) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_STATUS_CHANGE_NOT_ALLOWED));
            }
            existingCode.setStatus(fromCode.getStatus());
            hasChanges = true;
        }
        if (!Objects.equals(existingCode.getCodeScheme(), codeScheme)) {
            existingCode.setCodeScheme(codeScheme);
            hasChanges = true;
        }
        if (!Objects.equals(existingCode.getUri(), uri)) {
            existingCode.setUri(uri);
            hasChanges = true;
        }
        if (!Objects.equals(existingCode.getShortName(), fromCode.getShortName())) {
            existingCode.setShortName(fromCode.getShortName());
            hasChanges = true;
        }
        if (!Objects.equals(existingCode.getHierarchyLevel(), fromCode.getHierarchyLevel())) {
            existingCode.setHierarchyLevel(fromCode.getHierarchyLevel());
            hasChanges = true;
        }
        if (!Objects.equals(existingCode.getBroaderCodeId(), fromCode.getBroaderCodeId())) {
            existingCode.setBroaderCodeId(fromCode.getBroaderCodeId());
            hasChanges = true;
        }
        for (final Map.Entry<String, String> entry : fromCode.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCode.getPrefLabel(language), value)) {
                existingCode.setPrefLabel(language, value);
                hasChanges = true;
            }
        }
        for (final Map.Entry<String, String> entry : fromCode.getDescription().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCode.getDescription(language), value)) {
                existingCode.setDescription(language, value);
                hasChanges = true;
            }
        }
        for (final Map.Entry<String, String> entry : fromCode.getDefinition().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCode.getDefinition(language), value)) {
                existingCode.setDefinition(language, value);
                hasChanges = true;
            }
        }
        if (!Objects.equals(existingCode.getStartDate(), fromCode.getStartDate())) {
            existingCode.setStartDate(fromCode.getStartDate());
            hasChanges = true;
        }
        if (!Objects.equals(existingCode.getEndDate(), fromCode.getEndDate())) {
            existingCode.setEndDate(fromCode.getEndDate());
            hasChanges = true;
        }
        if (hasChanges) {
            final Date timeStamp = new Date(System.currentTimeMillis());
            existingCode.setModified(timeStamp);
        }
        return existingCode;
    }

    private Code createCode(final CodeScheme codeScheme,
                            final Code fromCode) {
        final Code code = new Code();
        if (fromCode.getId() != null) {
            code.setId(fromCode.getId());
        } else {
            final UUID uuid = UUID.randomUUID();
            code.setId(uuid);
        }
        code.setStatus(fromCode.getStatus());
        code.setCodeScheme(codeScheme);
        code.setCodeValue(fromCode.getCodeValue());
        code.setShortName(fromCode.getShortName());
        code.setHierarchyLevel(fromCode.getHierarchyLevel());
        code.setBroaderCodeId(fromCode.getBroaderCodeId());
        final Date timeStamp = new Date(System.currentTimeMillis());
        code.setModified(timeStamp);
        for (Map.Entry<String, String> entry : fromCode.getPrefLabel().entrySet()) {
            code.setPrefLabel(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : fromCode.getDescription().entrySet()) {
            code.setDescription(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : fromCode.getDefinition().entrySet()) {
            code.setDefinition(entry.getKey(), entry.getValue());
        }
        code.setStartDate(fromCode.getStartDate());
        code.setEndDate(fromCode.getEndDate());
        code.setUri(apiUtils.createCodeUri(codeScheme.getCodeRegistry(), codeScheme, code));
        return code;
    }

    private void setBroaderCodesAndEvaluateHierarchyLevels(final Map<String, String> broaderCodeMapping,
                                                           final Map<String, Code> codes) {
        setBroaderCodes(broaderCodeMapping, codes);
        evaluateAndSetHierarchyLevels(new HashSet<>(codes.values()));
    }

    private void setBroaderCodes(final Map<String, String> broaderCodeMapping,
                                 final Map<String, Code> codes) {
        broaderCodeMapping.forEach((codeCodeValue, broaderCodeCodeValue) -> {
            final Code code = codes.get(codeCodeValue);
            final Code broaderCode = codes.get(broaderCodeCodeValue);
            if (broaderCode == null && broaderCodeCodeValue != null) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_BROADER_CODE_DOES_NOT_EXIST));
            } else if (broaderCode != null && broaderCode.getCodeValue().equalsIgnoreCase(code.getCodeValue())) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_BROADER_CODE_SELF_REFERENCE));
            } else {
                code.setBroaderCodeId(broaderCode != null ? broaderCode.getId() : null);
            }
        });
    }

    public void evaluateAndSetHierarchyLevels(final Set<Code> codes) {
        final Set<Code> codesToEvaluate = new HashSet<>(codes);
        final Map<Integer, Set<UUID>> hierarchyMapping = new HashMap<>();
        int hierarchyLevel = 0;
        while (!codesToEvaluate.isEmpty()) {
            ++hierarchyLevel;
            resolveAndSetCodeHierarchyLevels(codesToEvaluate, hierarchyMapping, hierarchyLevel);
        }
    }

    private void resolveAndSetCodeHierarchyLevels(final Set<Code> codesToEvaluate,
                                                  final Map<Integer, Set<UUID>> hierarchyMapping,
                                                  final Integer hierarchyLevel) {
        final Set<Code> toRemove = new HashSet<>();
        codesToEvaluate.forEach(code -> {
            if (hierarchyLevel == 1 && code.getBroaderCodeId() == null || hierarchyLevel > 1 && code.getBroaderCodeId() != null && hierarchyMapping.get(hierarchyLevel - 1).contains(code.getBroaderCodeId())) {
                code.setHierarchyLevel(hierarchyLevel);
                final Set<UUID> uuids = hierarchyMapping.computeIfAbsent(hierarchyLevel, k -> new HashSet<>());
                uuids.add(code.getId());
                toRemove.add(code);
            }
        });
        codesToEvaluate.removeAll(toRemove);
    }

    private void validateCodeForCodeScheme(final Code code) {
        if (code.getId() != null) {
            final Code existingCode = codeRepository.findById(code.getId());
            if (existingCode != null && !existingCode.getCodeValue().equalsIgnoreCase(code.getCodeValue())) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_EXISTING_CODE_MISMATCH));
            }
        }
    }

    private void checkForExistingCodeInCodeScheme(final CodeScheme codeScheme, final Code fromCode) {
        final Code code = codeRepository.findByCodeSchemeAndCodeValue(codeScheme, fromCode.getCodeValue());
        if (code != null) {
            throw new ExistingCodeException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ALREADY_EXISTING_CODE, code.getCodeValue()));
        }
    }

    private String parseShortNameFromCsvRecord(final CSVRecord record) {
        String shortName;
        try {
            shortName = record.get(CONTENT_HEADER_SHORTNAME);
        } catch (final Exception e) {
            shortName = null;
        }
        return shortName;
    }

    private String parseShortNameFromExcelRow(final Map<String, Integer> genericHeaders,
                                              final Row row,
                                              final DataFormatter formatter) {
        final String shortName;
        if (genericHeaders.get(CONTENT_HEADER_SHORTNAME) != null) {
            shortName = formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_SHORTNAME)));
        } else {
            shortName = null;
        }
        return shortName;
    }
}
