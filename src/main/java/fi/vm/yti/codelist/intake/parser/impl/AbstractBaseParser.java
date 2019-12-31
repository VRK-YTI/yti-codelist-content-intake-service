package fi.vm.yti.codelist.intake.parser.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.vm.yti.codelist.common.dto.AbstractHistoricalCodeDTO;
import fi.vm.yti.codelist.common.dto.AbstractHistoricalIdentifyableCodeWithStatusDTO;
import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.common.dto.OrganizationDTO;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.exception.CodeParsingException;
import fi.vm.yti.codelist.intake.exception.MissingRowValueCodeValueException;
import fi.vm.yti.codelist.intake.exception.MissingRowValuePrefLabelException;
import fi.vm.yti.codelist.intake.exception.MissingRowValueStatusException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

public abstract class AbstractBaseParser {

    public static final String JUPO_REGISTRY = "jupo";
    public static final String YTI_REGISTRY = "interoperabilityplatform";
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBaseParser.class);
    private static final String CODE_CODEVALUE_VALIDATOR = "^[a-zA-Z0-9_\\-\\.\\+\\&\\#\\*]*$";
    private static final String CODESCHEME_CODEVALUE_VALIDATOR = "^[a-zA-Z0-9_\\-]*$";
    private static final String SUGGESTED_STATUS = "SUGGESTED";
    public static final Pattern URL_PATTERN = Pattern.compile("^https?://(?:[^\\s/@]+@)?(:?localhost|\\[[a-fA-F0-9:.]+\\]|[^\\s/@:.?#\\[\\]]+(?:\\.[^\\s/@:.?#\\[\\]]+)+)(?::\\d+)?(?:/\\S*)?$");

    public static void validateCodeCodeValue(final String codeValue) {
        validateCodeCodeValue(codeValue, null);
    }

    public static void validateCodeCodeValue(final String codeValue,
                                             final String entityIdentifier) {
        if (codeValue == null || !codeValue.matches(CODE_CODEVALUE_VALIDATOR)) {
            LOG.error(String.format("Error with code: %s", codeValue));
            if (entityIdentifier != null) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_INVALID_CODE_CODEVALUE_WITH_IDENTIFIER, entityIdentifier));
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_INVALID_CODE_CODEVALUE));
            }
        }
    }

    public static void validateCodeValue(final String codeValue) {
        validateCodeValue(codeValue, null);
    }

    public static void validateCodeValue(final String codeValue,
                                         final String entityIdentifier) {
        if (!codeValue.matches(CODESCHEME_CODEVALUE_VALIDATOR)) {
            LOG.error(String.format("Error with code: %s", codeValue));
            if (entityIdentifier != null) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_INVALID_CODEVALUE_WITH_IDENTIFIER, entityIdentifier));
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_INVALID_CODEVALUE));
            }
        }
    }

    public ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }

    boolean isRowEmpty(final Row row) {
        for (int cellIndex = row.getFirstCellNum(); cellIndex < row.getLastCellNum(); cellIndex++) {
            final Cell cell = row.getCell(cellIndex);
            if (cell != null && cell.getCellType() != CellType.BLANK)
                return false;
        }
        return true;
    }

    private String resolveLanguageFromHeader(final String prefix,
                                             final String header) {
        return header.substring(header.indexOf(prefix) + prefix.length()).toLowerCase();
    }

    LocalDate parseStartDateFromString(final String dateString,
                                       final String rowIdentifier) {
        LocalDate date = null;
        if (!dateString.isEmpty()) {
            try {
                date = LocalDate.parse(dateString);
            } catch (final DateTimeParseException e) {
                LOG.error(String.format("Parsing startDate failed from string: %s", dateString));
                throw new CodeParsingException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                    ERR_MSG_USER_ERRONEOUS_START_DATE, rowIdentifier));
            }
        }
        return date;
    }

    LocalDate parseEndDateFromString(final String dateString,
                                     final String rowIdentifier) {
        LocalDate date = null;
        if (!dateString.isEmpty()) {
            try {
                date = LocalDate.parse(dateString);
            } catch (final DateTimeParseException e) {
                LOG.error(String.format("Parsing endDate failed from string: %s", dateString));
                throw new CodeParsingException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                    ERR_MSG_USER_ERRONEOUS_END_DATE, rowIdentifier));
            }
        }
        return date;
    }

    String parseStatusValueFromString(final String statusString) {
        final String status;
        try {
            status = Status.valueOf(statusString.replaceAll(" ", "").trim().toUpperCase()).toString();
        } catch (final Exception e) {
            LOG.error("Caught exception in parseStatusValueFromString.", e);
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_STATUS_NOT_VALID, statusString));
        }
        if (SUGGESTED_STATUS.equalsIgnoreCase(status)) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_SUGGESTED_STATUS_NOT_ALLOWED));
        }
        return status;
    }

    Map<String, String> parseLocalizedValueFromCsvRecord(final Map<String, Integer> valueHeaders,
                                                         final CSVRecord record) {
        final Map<String, String> value = new LinkedHashMap<>();
        valueHeaders.forEach((language, header) -> {
            final String localizedValue = record.get(header);
            if (localizedValue != null && !localizedValue.trim().isEmpty()) {
                value.put(language, localizedValue.trim());
            }
        });
        return value;
    }

    Map<String, Integer> parseHeadersWithPrefix(final Map<String, Integer> headerMap,
                                                final String headerPrefix) {
        final Map<String, Integer> valueHeaders = new LinkedHashMap<>();
        headerMap.forEach((header, index) -> {
            if (header.startsWith(headerPrefix)) {
                valueHeaders.put(resolveLanguageFromHeader(headerPrefix, header), index);
            }
        });
        return valueHeaders;
    }

    Map<String, String> parseLocalizedValueFromExcelRow(final Map<String, Integer> valueHeaders,
                                                        final Row row,
                                                        final DataFormatter formatter) {
        final Map<String, String> value = new LinkedHashMap<>();
        valueHeaders.forEach((language, header) -> {
            final String localizedValue = formatter.formatCellValue(row.getCell(header));
            if (!localizedValue.trim().isEmpty()) {
                value.put(language, localizedValue.trim());
            }
        });
        return value;
    }

    boolean startDateIsBeforeEndDateSanityCheck(final LocalDate startDate,
                                                final LocalDate endDate) {
        // if either one is null, everything is OK
        return startDate == null || endDate == null || startDate.isBefore(endDate) || startDate.compareTo(endDate) == 0;
    }

    Map<String, Integer> resolveHeaderMap(final Row row) {
        final Map<String, Integer> headerMap = new LinkedHashMap<>();
        final Iterator<Cell> cellIterator = row.cellIterator();
        while (cellIterator.hasNext()) {
            final Cell cell = cellIterator.next();
            final String value = cell.getStringCellValue();
            final Integer index = cell.getColumnIndex();
            if (headerMap.get(value) != null) {
                LOG.error("Duplicate header " + value);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_DUPLICATE_HEADER_VALUE_WITH_IDENTIFIER, value));
            }
            if (value != null && !value.trim().isEmpty()) {
                headerMap.put(value, index);
            }
        }
        return headerMap;
    }

    void checkForDuplicateCodeValueInImportData(final Set<String> values,
                                                final String codeValue) {
        if (values.contains(codeValue.toLowerCase())) {
            LOG.warn(String.format("Duplicate code: %s", codeValue));
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_DUPLICATE_CODE_VALUE));
        }
    }

    void checkOrdersForDuplicateValues(final Set<CodeDTO> codes) {
        final Set<Integer> orders = new HashSet<>();
        codes.forEach(code -> {
            final Integer order = code.getOrder();
            if (order != null) {
                if (!orders.contains(order)) {
                    orders.add(order);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_CODE_ORDER_CONTAINS_DUPLICATE_VALUES));
                }
            }
        });
    }

    String parseStringFromCsvRecord(final CSVRecord record,
                                    final String columnName) {
        final String value;
        try {
            if (record.isMapped(columnName)) {
                value = record.get(columnName);
            } else {
                value = null;
            }
            return value;
        } catch (final Exception e) {
            LOG.error("error in row: " + getRecordIdentifier(record), e);
        }
        return null;
    }

    UUID parseUUIDFromString(final String uuidString) {
        final UUID uuid;
        if (uuidString == null || uuidString.isEmpty()) {
            uuid = null;
        } else {
            try {
                uuid = UUID.fromString(uuidString);
            } catch (final IllegalArgumentException e) {
                LOG.error("UUID parsing failed from: " + uuidString, e);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_INVALID_ID));
            }
        }
        return uuid;
    }

    UUID parseIdFromRecord(final CSVRecord record) {
        final UUID id;
        if (record.isMapped(CONTENT_HEADER_ID)) {
            id = parseUUIDFromString(record.get(CONTENT_HEADER_ID));
        } else {
            id = null;
        }
        return id;
    }

    String parseCodeValueFromRecord(final CSVRecord record) {
        final String codeValue;
        if (record.isMapped(CONTENT_HEADER_CODEVALUE)) {
            codeValue = parseStringFromCsvRecord(record, CONTENT_HEADER_CODEVALUE).trim();
        } else {
            codeValue = null;
        }
        return codeValue;
    }

    String parseStartDateStringFromCsvRecord(final CSVRecord record) {
        return parseStringFromCsvRecord(record, CONTENT_HEADER_STARTDATE);
    }

    String parseEndDateStringFromCsvRecord(final CSVRecord record) {
        return parseStringFromCsvRecord(record, CONTENT_HEADER_ENDDATE);
    }

    String parseConceptUriFromCsvRecord(final CSVRecord record) {
        return parseStringFromCsvRecord(record, CONTENT_HEADER_CONCEPTURI);
    }

    Integer resolveOrderFromExcelRow(final Map<String, Integer> headerMap,
                                     final Row row,
                                     final DataFormatter formatter) {
        final Integer order;
        if (headerMap.containsKey(CONTENT_HEADER_ORDER)) {
            order = resolveOrderFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ORDER))), String.valueOf(row.getRowNum()));
        } else {
            order = null;
        }
        return order;
    }

    Integer resolveSequenceIdFromExcelRow(final Map<String, Integer> headerMap,
                                          final Row row,
                                          final DataFormatter formatter) {
        final Integer sequenceId;
        if (headerMap.containsKey(CONTENT_HEADER_MEMBER_ID)) {
            sequenceId = resolveSequenceIdFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_MEMBER_ID))), String.valueOf(row.getRowNum()));
        } else {
            sequenceId = null;
        }
        return sequenceId;
    }

    Integer resolveOrderFromCsvRecord(final CSVRecord record) {
        final Integer order;
        if (record.isMapped(CONTENT_HEADER_ORDER)) {
            order = resolveOrderFromString(record.get(CONTENT_HEADER_ORDER), String.valueOf(record.getRecordNumber()));
        } else {
            order = null;
        }
        return order;
    }

    Integer resolveSequenceIdFromCsvRecord(final CSVRecord record) {
        final Integer order;
        if (record.isMapped(CONTENT_HEADER_MEMBER_ID)) {
            order = resolveSequenceIdFromString(record.get(CONTENT_HEADER_MEMBER_ID), String.valueOf(record.getRecordNumber()));
        } else {
            order = null;
        }
        return order;
    }

    private Integer resolveOrderFromString(final String orderString,
                                           final String rowIdentifier) {
        final Integer order;
        if (!orderString.isEmpty()) {
            try {
                order = Integer.parseInt(orderString);
            } catch (final NumberFormatException e) {
                LOG.error("Error parsing order from: " + orderString, e);
                throw new CodeParsingException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    ERR_MSG_USER_ORDER_INVALID_VALUE, rowIdentifier));
            }
        } else {
            order = null;
        }
        return order;
    }

    private Integer resolveSequenceIdFromString(final String sequenceIdString,
                                                final String rowIdentifier) {
        final Integer sequenceId;
        if (!sequenceIdString.isEmpty()) {
            try {
                sequenceId = Integer.parseInt(sequenceIdString);
            } catch (final NumberFormatException e) {
                LOG.error("Error parsing sequenceId from: " + sequenceIdString, e);
                throw new CodeParsingException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    ERR_MSG_USER_MEMBER_ID_INVALID_VALUE, rowIdentifier));
            }
        } else {
            sequenceId = null;
        }
        return sequenceId;
    }

    Set<OrganizationDTO> resolveOrganizations(final String organizationsString) {
        final Set<OrganizationDTO> organizations = new HashSet<>();
        if (organizationsString != null && !organizationsString.isEmpty()) {
            for (final String organizationId : organizationsString.split(";")) {
                final OrganizationDTO organization = new OrganizationDTO();
                organization.setId(UUID.fromString(trimWhiteSpaceFromString(organizationId)));
                organizations.add(organization);
            }
        }
        return organizations;
    }

    public String trimWhiteSpaceFromString(final String string) {
        return string.replaceAll(" ", "").trim();
    }

    Set<ExternalReferenceDTO> resolveHrefs(final String externalReferencesString) {
        final Set<ExternalReferenceDTO> externalReferences = new HashSet<>();
        if (externalReferencesString != null && !externalReferencesString.isEmpty()) {
            for (final String externalReferenceIdentifier : externalReferencesString.split("\\|")) {
                final ExternalReferenceDTO externalReference = new ExternalReferenceDTO();
                UUID uuid = null;
                try {
                    uuid = UUID.fromString(trimWhiteSpaceFromString(externalReferenceIdentifier));
                } catch (final Exception e) {
                    // Nothing on purpose
                }
                if (uuid != null) {
                    externalReference.setId(uuid);
                } else {
                    externalReference.setHref(parseAndValidateExternalReferenceHrefFromString(externalReferenceIdentifier));
                }
                externalReferences.add(externalReference);
            }
        }
        return externalReferences;
    }

    String parseAndValidateExternalReferenceHrefFromString(final String externalReferenceHref) {
        final String trimmedHref = trimWhiteSpaceFromString(externalReferenceHref);
        if (!URL_PATTERN.matcher(trimmedHref).matches()) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_IMPORTED_DATA_CONTAINS_INVALID_URLS_IN_LINKS));
        }
        return trimmedHref;
    }

    String getRowIdentifier(final Row row) {
        return String.valueOf(row.getRowNum() + 1);
    }

    String getRecordIdentifier(final CSVRecord record) {
        return String.valueOf(record.getRecordNumber() + 1);
    }

    boolean checkIfRowIsEmpty(final Row row) {
        if (row == null) {
            return true;
        }
        if (row.getLastCellNum() <= 0) {
            return true;
        }
        for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
            final Cell cell = row.getCell(cellNum);
            if (cell != null && cell.getCellType() != CellType.BLANK && StringUtils.isNotBlank(trimWhiteSpaceFromString(cell.toString()))) {
                return false;
            }
        }
        return true;
    }

    void checkIfExcelEmpty(final Iterator<Row> rowIterator) {
        if (!rowIterator.hasNext()) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EMPTY_EXCEL));
        }
    }

    protected boolean headerMapContainsAtLeastOneHeaderWhichStartsWithPrefLabel(final Map<String, Integer> headerMap) {
        for (String key : headerMap.keySet()) {
            if (key.startsWith(CONTENT_HEADER_PREFLABEL_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Due to the realities of multiple different class hiearchies in the DTOs there are 3 differing targets of setting the dates in this method which represent an
     * either/or type of situation, only one target is used at any one time. If targetDto is empty, set the dates to alternateTargetDto, or if that too is empty, set the
     * dates to MemberDTO (TargetDto used with code and codescheme, alternateTargetDto is used with extension, and MemberDTO is used directly in case of Members).
     */
    protected void parseDateFromExcel(final DataFormatter formatter,
                                      final Map<String, Integer> headerMap,
                                      final Row row,
                                      final String rowIdentifier,
                                      final AbstractHistoricalCodeDTO targetDto,
                                      final AbstractHistoricalIdentifyableCodeWithStatusDTO alternateTargetDto,
                                      final MemberDTO memberDTO,
                                      final String theContentHeader,
                                      final String theErrorMessage) {
        Cell cell = row.getCell(headerMap.get(theContentHeader));
        if (cell == null) {
            return;
        }
        CellType cellType = cell.getCellType();
        if (cellType.compareTo(CellType.STRING) == 0) {
            if (theContentHeader != null && theContentHeader.equals(CONTENT_HEADER_STARTDATE)) {
                if (targetDto != null) {
                    targetDto.setStartDate(parseStartDateFromString(formatter.formatCellValue(row.getCell(headerMap.get(theContentHeader))), rowIdentifier));
                } else if (alternateTargetDto != null) {
                    alternateTargetDto.setStartDate(parseStartDateFromString(formatter.formatCellValue(row.getCell(headerMap.get(theContentHeader))), rowIdentifier));
                } else {
                    memberDTO.setStartDate(parseStartDateFromString(formatter.formatCellValue(row.getCell(headerMap.get(theContentHeader))), rowIdentifier));
                }
            } else if (theContentHeader != null && theContentHeader.equals(CONTENT_HEADER_ENDDATE)) {
                if (targetDto != null) {
                    targetDto.setEndDate(parseEndDateFromString(formatter.formatCellValue(row.getCell(headerMap.get(theContentHeader))), rowIdentifier));
                } else if (alternateTargetDto != null) {
                    alternateTargetDto.setEndDate(parseEndDateFromString(formatter.formatCellValue(row.getCell(headerMap.get(theContentHeader))), rowIdentifier));
                } else {
                    memberDTO.setEndDate(parseEndDateFromString(formatter.formatCellValue(row.getCell(headerMap.get(theContentHeader))), rowIdentifier));
                }
            }
        } else if (cellType.compareTo(CellType.NUMERIC) == 0) {
            try {
                if (DateUtil.isCellDateFormatted(cell)) {
                    if (theContentHeader != null && theContentHeader.equals(CONTENT_HEADER_STARTDATE)) {
                        if (targetDto != null) {
                            targetDto.setStartDate(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                        } else if (alternateTargetDto != null) {
                            alternateTargetDto.setStartDate(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                        } else {
                            memberDTO.setStartDate(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                        }
                    } else if (theContentHeader != null && theContentHeader.equals(CONTENT_HEADER_ENDDATE)) {
                        if (targetDto != null) {
                            targetDto.setEndDate(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                        } else if (alternateTargetDto != null) {
                            alternateTargetDto.setEndDate(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                        } else {
                            memberDTO.setEndDate(cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                        }

                    }
                }
            } catch (Exception e) {
                LOG.error(String.format("Parsing " + theContentHeader + " failed from non-string Cell with contents: %s", cell.getDateCellValue().toString()));
                throw new CodeParsingException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                    theErrorMessage, rowIdentifier));
            }
        }
    }

    protected void validateRequiredDataOnRow(final Row row,
                                             final Map<String, Integer> headerMap,
                                             final DataFormatter formatter) {
        if (formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODEVALUE))) == null ||
            formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_CODEVALUE))).isEmpty()) {
            throw new MissingRowValueCodeValueException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_CODEVALUE, getRowIdentifier(row)));
        }
        if (formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STATUS))) == null ||
            formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_STATUS))).isEmpty()) {
            throw new MissingRowValueStatusException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_STATUS, getRowIdentifier(row)));
        }
        boolean foundAtLeastOnePrefLabelFromTheRow = false;
        List<String> columnNamesWhichStartWithPrefLabelPrefix = new ArrayList<>();
        headerMap.keySet().forEach(key -> {
            if (key.startsWith(CONTENT_HEADER_PREFLABEL_PREFIX)) {
                columnNamesWhichStartWithPrefLabelPrefix.add(key);
            }
        });
        for (String prefLabelColumnName : columnNamesWhichStartWithPrefLabelPrefix) {
            if (formatter.formatCellValue(row.getCell(headerMap.get(prefLabelColumnName))) != null &&
                !formatter.formatCellValue(row.getCell(headerMap.get(prefLabelColumnName))).isEmpty()) {
                foundAtLeastOnePrefLabelFromTheRow = true;
            }
        }
        if (!foundAtLeastOnePrefLabelFromTheRow) {
            throw new MissingRowValuePrefLabelException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ROW_MISSING_PREFLABEL_VALUE, getRowIdentifier(row)));
        }
    }

    protected String parseOperationFromExcelRow(final Map<String, Integer> genericHeaders,
                                                final Row row,
                                                final DataFormatter formatter) {
        final String operation;
        if (genericHeaders.get(CONTENT_HEADER_OPERATION) != null) {
            operation = formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_OPERATION)));
        } else {
            operation = null;
        }
        return operation;
    }
}
