package fi.vm.yti.codelist.intake.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.input.BOMInputStream;
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
import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.ErrorModel;
import fi.vm.yti.codelist.common.model.ExternalReference;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.exception.CodeParsingException;
import fi.vm.yti.codelist.intake.exception.ErrorConstants;
import fi.vm.yti.codelist.intake.exception.ExistingCodeException;
import fi.vm.yti.codelist.intake.exception.MissingHeaderClassificationException;
import fi.vm.yti.codelist.intake.exception.MissingHeaderCodeValueException;
import fi.vm.yti.codelist.intake.exception.MissingHeaderStatusException;
import fi.vm.yti.codelist.intake.exception.MissingRowValueCodeValueException;
import fi.vm.yti.codelist.intake.exception.MissingRowValueStatusException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.jpa.ExternalReferenceRepository;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CONTENT_HEADER_CHANGENOTE_PREFIX;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CONTENT_HEADER_CLASSIFICATION;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CONTENT_HEADER_CODEVALUE;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CONTENT_HEADER_DEFINITION_PREFIX;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CONTENT_HEADER_DESCRIPTION_PREFIX;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CONTENT_HEADER_ENDDATE;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CONTENT_HEADER_GOVERNANCEPOLICY;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CONTENT_HEADER_ID;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CONTENT_HEADER_LEGALBASE;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CONTENT_HEADER_PREFLABEL_PREFIX;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CONTENT_HEADER_SOURCE;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CONTENT_HEADER_STARTDATE;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CONTENT_HEADER_STATUS;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CONTENT_HEADER_VERSION;
import static fi.vm.yti.codelist.common.constants.ApiConstants.EXCEL_SHEET_CODESCHEMES;

/**
 * Class that handles parsing of CodeSchemes from source data.
 */
@Service
public class CodeSchemeParser extends AbstractBaseParser {

    private static final Logger LOG = LoggerFactory.getLogger(CodeSchemeParser.class);
    private final ApiUtils apiUtils;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRepository codeRepository;
    private final CodeRegistryRepository codeRegistryRepository;
    private final ExternalReferenceRepository externalReferenceRepository;
    private final ExternalReferenceParser externalReferenceParser;

    @Inject
    public CodeSchemeParser(final ApiUtils apiUtils,
                            final CodeRegistryRepository codeRegistryRepository,
                            final CodeSchemeRepository codeSchemeRepository,
                            final CodeRepository codeRepository,
                            final ExternalReferenceRepository externalReferenceRepository,
                            final ExternalReferenceParser externalReferenceParser) {
        this.apiUtils = apiUtils;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
        this.externalReferenceRepository = externalReferenceRepository;
        this.externalReferenceParser = externalReferenceParser;
    }

    public CodeScheme parseCodeSchemeFromJsonInput(final CodeRegistry codeRegistry,
                                                   final String jsonPayload) throws IOException {
        final ObjectMapper mapper = createObjectMapper();
        final CodeScheme codeScheme;
        final CodeScheme fromCodeScheme = mapper.readValue(jsonPayload, CodeScheme.class);
        codeScheme = createOrUpdateCodeScheme(codeRegistry, fromCodeScheme);
        updateExternalReferences(fromCodeScheme, codeScheme);
        return codeScheme;
    }

    public Set<CodeScheme> parseCodeSchemesFromJsonInput(final CodeRegistry codeRegistry,
                                                         final String jsonPayload) throws IOException {
        final ObjectMapper mapper = createObjectMapper();
        final Set<CodeScheme> codeSchemes = new HashSet<>();
        final Set<CodeScheme> fromCodeSchemes = mapper.readValue(jsonPayload, new TypeReference<Set<CodeScheme>>() {
        });
        for (final CodeScheme fromCodeScheme : fromCodeSchemes) {
            final CodeScheme codeScheme = createOrUpdateCodeScheme(codeRegistry, fromCodeScheme);
            codeSchemes.add(codeScheme);
            updateExternalReferences(fromCodeScheme, codeScheme);
        }
        return codeSchemes;
    }

    private void updateExternalReferences(final CodeScheme fromCodeScheme,
                                          final CodeScheme codeScheme) {
        if (fromCodeScheme.getExternalReferences() != null) {
            final Set<ExternalReference> externalReferences = initializeExternalReferences(fromCodeScheme.getExternalReferences(), codeScheme, externalReferenceParser);
            if (!externalReferences.isEmpty()) {
                externalReferenceRepository.save(externalReferences);
                codeScheme.setExternalReferences(externalReferences);
            } else {
                codeScheme.setExternalReferences(null);
            }
        }
    }

    /**
     * Parses the .csv CodeScheme-file and returns the codeschemes as a set.
     *
     * @param inputStream The CodeScheme -file.
     * @return List of CodeScheme objects.
     */
    @SuppressFBWarnings("UC_USELESS_OBJECT")
    public Set<CodeScheme> parseCodeSchemesFromCsvInputStream(final CodeRegistry codeRegistry,
                                                              final InputStream inputStream) {
        final Set<CodeScheme> codeSchemes = new HashSet<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
            final Map<String, Integer> headerMap = csvParser.getHeaderMap();
            final Map<String, Integer> prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
            final Map<String, Integer> definitionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DEFINITION_PREFIX);
            final Map<String, Integer> descriptionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DESCRIPTION_PREFIX);
            final Map<String, Integer> changeNoteHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_CHANGENOTE_PREFIX);
            validateRequiredSchemeHeaders(headerMap);
            final List<CSVRecord> records = csvParser.getRecords();
            for (final CSVRecord record : records) {
                validateRequiredDataOnRecord(record);
                final CodeScheme fromCodeScheme = new CodeScheme();
                final String codeValue = record.get(CONTENT_HEADER_CODEVALUE);
                fromCodeScheme.setCodeValue(codeValue);
                fromCodeScheme.setId(parseUUIDFromString(record.get(CONTENT_HEADER_ID)));
                fromCodeScheme.setPrefLabel(parseLocalizedValueFromCsvRecord(prefLabelHeaders, record));
                fromCodeScheme.setDefinition(parseLocalizedValueFromCsvRecord(definitionHeaders, record));
                fromCodeScheme.setDescription(parseLocalizedValueFromCsvRecord(descriptionHeaders, record));
                fromCodeScheme.setChangeNote(parseLocalizedValueFromCsvRecord(changeNoteHeaders, record));
                final String dataClassificationCodes = record.get(CONTENT_HEADER_CLASSIFICATION);
                final Set<Code> dataClassifications = resolveDataClassificationsFromString(dataClassificationCodes);
                if (dataClassifications.isEmpty() && !codeValue.equals(YTI_DATACLASSIFICATION_CODESCHEME) && !codeRegistry.getCodeValue().equals(JUPO_REGISTRY)) {
                    LOG.error("Parsing dataClassifications for codeScheme: " + codeValue + " failed");
                    throw new CodeParsingException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                        ErrorConstants.ERR_MSG_USER_MISSING_HEADER_CLASSIFICATION));
                }
                fromCodeScheme.setVersion(record.get(CONTENT_HEADER_VERSION));
                fromCodeScheme.setStatus(Status.valueOf(record.get(CONTENT_HEADER_STATUS)).toString());
                fromCodeScheme.setLegalBase(record.get(CONTENT_HEADER_LEGALBASE));
                fromCodeScheme.setGovernancePolicy(record.get(CONTENT_HEADER_GOVERNANCEPOLICY));
                fromCodeScheme.setSource(record.get(CONTENT_HEADER_SOURCE));
                fromCodeScheme.setStartDate(parseStartDateFromString(record.get(CONTENT_HEADER_STARTDATE)));
                fromCodeScheme.setEndDate(parseEndDateString(record.get(CONTENT_HEADER_ENDDATE)));
                final CodeScheme codeScheme = createOrUpdateCodeScheme(codeRegistry, fromCodeScheme);
                codeSchemes.add(codeScheme);
            }
        } catch (IOException e) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorConstants.ERR_MSG_USER_500));
        }
        return codeSchemes;
    }

    /*
     * Parses the .xls or .xlsx CodeScheme Excel-inputstream and returns the CodeSchemes as a set.
     *
     * @param codeRegistry CodeRegistry.
     * @param inputStream The Code containing Excel -inputstream.
     * @return List of Code objects.
     */
    public Set<CodeScheme> parseCodeSchemesFromExcelInputStream(final CodeRegistry codeRegistry,
                                                                final InputStream inputStream) throws Exception {
        try (final Workbook workbook = WorkbookFactory.create(inputStream)) {
            return parseCodeSchemesFromExcel(codeRegistry, workbook);
        }
    }

    /*
     * Parses the .xls or .xlsx CodeScheme Excel-file and returns the CodeSchemes as a set.
     *
     * @param codeRegistry CodeRegistry.
     * @param inputStream The Code containing Excel -file.
     * @return List of Code objects.
     */
    @SuppressFBWarnings("UC_USELESS_OBJECT")
    public Set<CodeScheme> parseCodeSchemesFromExcel(final CodeRegistry codeRegistry,
                                                     final Workbook workbook) throws YtiCodeListException {
        final Set<CodeScheme> codeSchemes = new HashSet<>();
        if (codeRegistry != null) {
            final DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheet(EXCEL_SHEET_CODESCHEMES);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            final Iterator<Row> rowIterator = sheet.rowIterator();
            Map<String, Integer> headerMap = null;
            Map<String, Integer> prefLabelHeaders = null;
            Map<String, Integer> definitionHeaders = null;
            Map<String, Integer> descriptionHeaders = null;
            Map<String, Integer> changeNoteHeaders = null;
            boolean firstRow = true;
            while (rowIterator.hasNext()) {
                final Row row = rowIterator.next();
                if (firstRow) {
                    firstRow = false;
                    headerMap = resolveHeaderMap(row);
                    prefLabelHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_PREFLABEL_PREFIX);
                    definitionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DEFINITION_PREFIX);
                    descriptionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DESCRIPTION_PREFIX);
                    changeNoteHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_CHANGENOTE_PREFIX);
                    validateRequiredSchemeHeaders(headerMap);
                } else if (row.getPhysicalNumberOfCells() > 0 && !isRowEmpty(row)) {
                    validateRequiredDataOnRow(row, headerMap, formatter);
                    final CodeScheme fromCodeScheme = new CodeScheme();
                    final String codeValue = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODEVALUE)));
                    if (codeValue == null || codeValue.trim().isEmpty()) {
                        continue;
                    }
                    fromCodeScheme.setCodeValue(codeValue);
                    fromCodeScheme.setId(parseUUIDFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ID)))));
                    final String dataClassificationCodes;
                    try {
                        dataClassificationCodes = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CLASSIFICATION)));
                    } catch (NullPointerException e) {
                        LOG.error("Parsing dataClassifications for codeScheme: " + codeValue + " failed");
                        throw new CodeParsingException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                            ErrorConstants.ERR_MSG_USER_MISSING_HEADER_CLASSIFICATION));
                    }
                    final Set<Code> dataClassifications = resolveDataClassificationsFromString(dataClassificationCodes);
                    if (dataClassifications.isEmpty() && !codeValue.equals(YTI_DATACLASSIFICATION_CODESCHEME) && !codeRegistry.getCodeValue().equals(JUPO_REGISTRY)) {
                        LOG.error("Parsing dataClassifications for codeScheme: " + codeValue + " failed");
                        throw new CodeParsingException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                            ErrorConstants.ERR_MSG_USER_MISSING_HEADER_CLASSIFICATION));
                    }
                    fromCodeScheme.setDataClassifications(dataClassifications);
                    fromCodeScheme.setPrefLabel(parseLocalizedValueFromExcelRow(prefLabelHeaders, row, formatter));
                    fromCodeScheme.setDefinition(parseLocalizedValueFromExcelRow(definitionHeaders, row, formatter));
                    fromCodeScheme.setDescription(parseLocalizedValueFromExcelRow(descriptionHeaders, row, formatter));
                    fromCodeScheme.setChangeNote(parseLocalizedValueFromExcelRow(changeNoteHeaders, row, formatter));
                    fromCodeScheme.setVersion(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_VERSION))));
                    fromCodeScheme.setStatus(parseStatus(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STATUS)))).toString());
                    fromCodeScheme.setSource(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_SOURCE))));
                    fromCodeScheme.setLegalBase(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_LEGALBASE))));
                    fromCodeScheme.setGovernancePolicy(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_GOVERNANCEPOLICY))));
                    fromCodeScheme.setStartDate(parseStartDateFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STARTDATE)))));
                    fromCodeScheme.setEndDate(parseEndDateString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ENDDATE)))));
                    final CodeScheme codeScheme = createOrUpdateCodeScheme(codeRegistry, fromCodeScheme);
                    if (codeScheme != null) {
                        codeSchemes.add(codeScheme);
                    }
                }
            }
        }
        return codeSchemes;
    }

    private void validateRequiredDataOnRow(final Row row,
                                           final Map<String, Integer> headerMap,
                                           final DataFormatter formatter) {
        if (formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODEVALUE))) == null ||
            formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODEVALUE))).equals("")) {
            throw new MissingRowValueCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ErrorConstants.ERR_MSG_USER_ROW_MISSING_CODEVALUE));
        }
        if (formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STATUS))) == null ||
            formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STATUS))).equals("")) {
            throw new MissingRowValueStatusException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ErrorConstants.ERR_MSG_USER_ROW_MISSING_STATUS));
        }
    }

    private void validateRequiredDataOnRecord(final CSVRecord record) {
        if (record.get(CONTENT_HEADER_CODEVALUE) == null || record.get(CONTENT_HEADER_CODEVALUE).equals("")) {
            throw new MissingRowValueCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ErrorConstants.ERR_MSG_USER_ROW_MISSING_CODEVALUE));
        }
        if (record.get(CONTENT_HEADER_STATUS) == null || record.get(CONTENT_HEADER_STATUS).equals("")) {
            throw new MissingRowValueStatusException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ErrorConstants.ERR_MSG_USER_ROW_MISSING_STATUS));
        }
    }

    private void validateRequiredSchemeHeaders(final Map<String, Integer> headerMap) {
        if (!headerMap.containsKey(CONTENT_HEADER_CODEVALUE)) {
            throw new MissingHeaderCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ErrorConstants.ERR_MSG_USER_MISSING_HEADER_CODEVALUE));
        }
        if (!headerMap.containsKey(CONTENT_HEADER_CLASSIFICATION)) {
            throw new MissingHeaderClassificationException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ErrorConstants.ERR_MSG_USER_MISSING_HEADER_CLASSIFICATION));
        }
        if (!headerMap.containsKey(CONTENT_HEADER_STATUS)) {
            throw new MissingHeaderStatusException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ErrorConstants.ERR_MSG_USER_MISSING_HEADER_STATUS));
        }
    }

    private CodeScheme createOrUpdateCodeScheme(final CodeRegistry codeRegistry,
                                                final CodeScheme fromCodeScheme) {
        validateCodeSchemeForCodeRegistry(codeRegistry, fromCodeScheme);
        if (!startDateIsBeforeEndDateSanityCheck(fromCodeScheme.getStartDate(), fromCodeScheme.getEndDate())) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ErrorConstants.ERR_MSG_USER_END_BEFORE_START_DATE));
        }
        final CodeScheme existingCodeScheme;
        if (fromCodeScheme.getId() != null) {
            existingCodeScheme = codeSchemeRepository.findById(fromCodeScheme.getId());
        } else {
            checkForExistingCodeSchemeInRegistry(codeRegistry, fromCodeScheme);
            existingCodeScheme = null;
        }
        final CodeScheme codeScheme;
        if (existingCodeScheme != null) {
            codeScheme = updateCodeScheme(codeRegistry, existingCodeScheme, fromCodeScheme);
        } else {
            codeScheme = createCodeScheme(codeRegistry, fromCodeScheme);
        }
        return codeScheme;
    }

    private void checkForExistingCodeSchemeInRegistry(final CodeRegistry codeRegistry,
                                                      final CodeScheme codeScheme) {
        final CodeScheme existingCodeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValue(codeRegistry, codeScheme.getCodeValue());
        if (existingCodeScheme != null) {
            throw new ExistingCodeException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ErrorConstants.ERR_MSG_USER_ALREADY_EXISTING_CODE_SCHEME));
        }
    }

    private CodeScheme updateCodeScheme(final CodeRegistry codeRegistry,
                                        final CodeScheme existingCodeScheme,
                                        final CodeScheme fromCodeScheme) {
        final String uri = apiUtils.createCodeSchemeUri(codeRegistry, existingCodeScheme);
        boolean hasChanges = false;
        if (!Objects.equals(existingCodeScheme.getStatus(), fromCodeScheme.getStatus().toString())) {
            if (Status.valueOf(existingCodeScheme.getStatus()).ordinal() >= Status.VALID.ordinal() && Status.valueOf(fromCodeScheme.getStatus()).ordinal() < Status.VALID.ordinal()) {
                throw new WebApplicationException("Trying to update content with status: " + existingCodeScheme.getStatus() + " to status: " + fromCodeScheme.getStatus());
            }
            existingCodeScheme.setStatus(fromCodeScheme.getStatus().toString());
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getCodeRegistry(), codeRegistry)) {
            existingCodeScheme.setCodeRegistry(codeRegistry);
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getDataClassifications(), fromCodeScheme.getDataClassifications())) {
            if (fromCodeScheme.getDataClassifications() != null && !fromCodeScheme.getDataClassifications().isEmpty()) {
                existingCodeScheme.setDataClassifications(resolveClassificationsFromCodes(fromCodeScheme.getDataClassifications()));
            } else {
                existingCodeScheme.setDataClassifications(null);
            }
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getUri(), uri)) {
            existingCodeScheme.setUri(uri);
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getSource(), fromCodeScheme.getSource())) {
            existingCodeScheme.setSource(fromCodeScheme.getSource());
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getLegalBase(), fromCodeScheme.getLegalBase())) {
            existingCodeScheme.setLegalBase(fromCodeScheme.getLegalBase());
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getGovernancePolicy(), fromCodeScheme.getGovernancePolicy())) {
            existingCodeScheme.setGovernancePolicy(fromCodeScheme.getGovernancePolicy());
            hasChanges = true;
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCodeScheme.getPrefLabel(language), value)) {
                existingCodeScheme.setPrefLabel(language, value);
                hasChanges = true;
            }
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getDescription().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCodeScheme.getDescription(language), value)) {
                existingCodeScheme.setDescription(language, value);
                hasChanges = true;
            }
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getDefinition().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCodeScheme.getDefinition(language), value)) {
                existingCodeScheme.setDefinition(language, value);
                hasChanges = true;
            }
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getChangeNote().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCodeScheme.getChangeNote(language), value)) {
                existingCodeScheme.setChangeNote(language, value);
                hasChanges = true;
            }
        }
        if (!Objects.equals(existingCodeScheme.getVersion(), fromCodeScheme.getVersion())) {
            existingCodeScheme.setVersion(fromCodeScheme.getVersion());
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getStartDate(), fromCodeScheme.getStartDate())) {
            existingCodeScheme.setStartDate(fromCodeScheme.getStartDate());
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getEndDate(), fromCodeScheme.getEndDate())) {
            existingCodeScheme.setEndDate(fromCodeScheme.getEndDate());
            hasChanges = true;
        }
        if (hasChanges) {
            final Date timeStamp = new Date(System.currentTimeMillis());
            existingCodeScheme.setModified(timeStamp);
        }
        return existingCodeScheme;
    }

    private CodeScheme createCodeScheme(final CodeRegistry codeRegistry,
                                        final CodeScheme fromCodeScheme) {
        final CodeScheme codeScheme = new CodeScheme();
        codeScheme.setCodeRegistry(codeRegistry);
        codeScheme.setDataClassifications(fromCodeScheme.getDataClassifications());
        if (fromCodeScheme.getId() != null) {
            codeScheme.setId(fromCodeScheme.getId());
        } else {
            final UUID uuid = UUID.randomUUID();
            codeScheme.setId(uuid);
        }
        codeScheme.setCodeValue(fromCodeScheme.getCodeValue());
        codeScheme.setSource(fromCodeScheme.getSource());
        codeScheme.setLegalBase(fromCodeScheme.getLegalBase());
        codeScheme.setGovernancePolicy(fromCodeScheme.getGovernancePolicy());
        final Date timeStamp = new Date(System.currentTimeMillis());
        codeScheme.setModified(timeStamp);
        for (final Map.Entry<String, String> entry : fromCodeScheme.getPrefLabel().entrySet()) {
            codeScheme.setPrefLabel(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getDescription().entrySet()) {
            codeScheme.setDescription(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getDefinition().entrySet()) {
            codeScheme.setDefinition(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getChangeNote().entrySet()) {
            codeScheme.setChangeNote(entry.getKey(), entry.getValue());
        }
        codeScheme.setVersion(fromCodeScheme.getVersion());
        codeScheme.setStatus(fromCodeScheme.getStatus().toString());
        codeScheme.setStartDate(fromCodeScheme.getStartDate());
        codeScheme.setEndDate(fromCodeScheme.getEndDate());
        codeScheme.setUri(apiUtils.createCodeSchemeUri(codeRegistry, codeScheme));
        return codeScheme;
    }

    private Set<Code> resolveDataClassificationsFromString(final String dataClassificationCodes) {
        final List<String> codes = Arrays.asList(dataClassificationCodes.split(";"));
        return resolveDataClassificationsFromCodeValues(codes);
    }

    private Set<Code> resolveDataClassificationsFromCodeValues(final List<String> codes) {
        final Set<Code> dataClassifications = new HashSet<>();
        final CodeRegistry ytiRegistry = codeRegistryRepository.findByCodeValue(JUPO_REGISTRY);
        if (ytiRegistry != null) {
            final CodeScheme dataClassificationScheme = codeSchemeRepository.findByCodeRegistryAndCodeValue(ytiRegistry, YTI_DATACLASSIFICATION_CODESCHEME);
            if (dataClassificationScheme != null) {
                codes.forEach(dataClassificationCode -> {
                    final Code code = codeRepository.findByCodeSchemeAndCodeValueAndBroaderCodeId(dataClassificationScheme, dataClassificationCode, null);
                    if (code != null) {
                        dataClassifications.add(code);
                    }
                });
            }
        }
        return dataClassifications;
    }

    private void validateCodeSchemeForCodeRegistry(final CodeRegistry codeRegistry, final CodeScheme codeScheme) {
        if (codeScheme.getId() != null) {
            final CodeScheme existingCodeScheme = codeSchemeRepository.findById(codeScheme.getId());
            if (existingCodeScheme != null && !existingCodeScheme.getCodeValue().equalsIgnoreCase(codeScheme.getCodeValue())) {
                throw new WebApplicationException("CodeScheme codeValue does not match existing values in the database!");
            }
        } else if (codeSchemeRepository.findByCodeRegistryAndCodeValue(codeRegistry, codeScheme.getCodeValue()) != null) {
            throw new WebApplicationException("CodeScheme with CodeValue already found in Registry!");
        }
    }

    private Set<Code> resolveClassificationsFromCodes(final Set<Code> fromClassifications) {
        final List<String> codes = new ArrayList<>();
        for (final Code fromClassification : fromClassifications) {
            codes.add(fromClassification.getCodeValue());
        }
        return resolveDataClassificationsFromCodeValues(codes);
    }
}
