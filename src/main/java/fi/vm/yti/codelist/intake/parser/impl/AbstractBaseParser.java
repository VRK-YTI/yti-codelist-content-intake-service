package fi.vm.yti.codelist.intake.parser.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.exception.CodeParsingException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

public abstract class AbstractBaseParser {

    public static final String JUPO_REGISTRY = "jupo";
    public static final String YTI_DATACLASSIFICATION_CODESCHEME = "serviceclassification";
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBaseParser.class);
    private static final String CODE_CODEVALUE_VALIDATOR = "^[a-zA-Z0-9_\\-\\.\\+\\&\\#\\*]*$";
    private static final String CODESCHEME_CODEVALUE_VALIDATOR = "^[a-zA-Z0-9_\\-]*$";

    public static boolean isRowEmpty(final Row row) {
        for (int cellIndex = row.getFirstCellNum(); cellIndex < row.getLastCellNum(); cellIndex++) {
            final Cell cell = row.getCell(cellIndex);
            if (cell != null && cell.getCellTypeEnum() != CellType.BLANK)
                return false;
        }
        return true;
    }

    public static void validateCodeCodeValue(final String codeValue) {
        if (codeValue == null || !codeValue.matches(CODE_CODEVALUE_VALIDATOR)) {
            LOG.error("Error with code: " + codeValue);
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_INVALID_CODE_CODEVALUE));
        }
    }

    public static void validateCodeValue(final String codeValue) {
        if (!codeValue.matches(CODESCHEME_CODEVALUE_VALIDATOR)) {
            LOG.error("Error with code: " + codeValue);
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_INVALID_CODEVALUE));
        }
    }

    public String resolveLanguageFromHeader(final String prefix,
                                            final String header) {
        return header.substring(header.indexOf(prefix) + prefix.length()).toLowerCase();
    }

    public ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        return mapper;
    }

    public Date parseStartDateFromString(final String dateString,
                                         final String rowIdentifier) {
        Date date = null;
        final ISO8601DateFormat dateFormat = new ISO8601DateFormat();
        if (!dateString.isEmpty()) {
            try {
                date = dateFormat.parse(dateString);
            } catch (final ParseException e) {
                LOG.error("Parsing startDate failed from string: " + dateString);
                throw new CodeParsingException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                    ERR_MSG_USER_ERRONEOUS_START_DATE, rowIdentifier));
            }
        }
        return date;
    }

    public Date parseEndDateFromString(final String dateString,
                                       final String rowIdentifier) {
        Date date = null;
        final ISO8601DateFormat dateFormat = new ISO8601DateFormat();
        if (!dateString.isEmpty()) {
            try {
                date = dateFormat.parse(dateString);
            } catch (final ParseException e) {
                LOG.error("Parsing endDate failed from string: " + dateString);
                throw new CodeParsingException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                    ERR_MSG_USER_ERRONEOUS_END_DATE, rowIdentifier));
            }
        }
        return date;
    }

    public String parseStatusValueFromString(final String statusString) {
        try {
            return Status.valueOf(statusString).toString();
        } catch (final Exception e) {
            LOG.error("Caught exception in parseStatusValueFromString.", e);
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_STATUS_NOT_VALID));
        }
    }

    public Map<String, String> parseLocalizedValueFromCsvRecord(final Map<String, Integer> valueHeaders,
                                                                final CSVRecord record) {
        final Map<String, String> value = new LinkedHashMap<>();
        valueHeaders.forEach((language, header) ->
            value.put(language, record.get(header)));
        return value;
    }

    public Map<String, Integer> parseHeadersWithPrefix(final Map<String, Integer> headerMap,
                                                       final String headerPrefix) {
        final Map<String, Integer> valueHeaders = new LinkedHashMap<>();
        headerMap.forEach((header, index) -> {
            if (header.startsWith(headerPrefix)) {
                valueHeaders.put(resolveLanguageFromHeader(headerPrefix, header), index);
            }
        });
        return valueHeaders;
    }

    public Map<String, String> parseLocalizedValueFromExcelRow(final Map<String, Integer> valueHeaders,
                                                               final Row row,
                                                               final DataFormatter formatter) {
        final Map<String, String> value = new LinkedHashMap<>();
        valueHeaders.forEach((language, header) ->
            value.put(language, formatter.formatCellValue(row.getCell(header))));
        return value;
    }

    protected boolean startDateIsBeforeEndDateSanityCheck(final Date startDate,
                                                          final Date endDate) {
        if (startDate == null || endDate == null) {
            return true; // if either one is null, everything is OK
        }
        return startDate.before(endDate) || startAndEndDatesAreOnTheSameDay(startDate, endDate);
    }

    /**
     * This is needed to allow start and end date on the same day - the users might want to enable a code or
     * a codescheme for one day.
     */
    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    private boolean startAndEndDatesAreOnTheSameDay(final Date startDate,
                                                    final Date endDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        final Date startDateWithoutTime;
        final Date endDateWithoutTime;
        try {
            startDateWithoutTime = sdf.parse(sdf.format(startDate));
            endDateWithoutTime = sdf.parse(sdf.format(endDate));
        } catch (final ParseException e) {
            return true; // should never ever happen, dates are never null here and are coming from datepicker
        }
        return startDateWithoutTime.compareTo(endDateWithoutTime) == 0;
    }

    public Map<String, Integer> resolveHeaderMap(final Row row) {
        final Map<String, Integer> headerMap = new LinkedHashMap<>();
        final Iterator<Cell> cellIterator = row.cellIterator();
        while (cellIterator.hasNext()) {
            final Cell cell = cellIterator.next();
            final String value = cell.getStringCellValue();
            final Integer index = cell.getColumnIndex();
            if (headerMap.get(value) != null) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_DUPLICATE_HEADER_VALUE));
            }
            headerMap.put(value, index);
        }
        return headerMap;
    }

    public void checkForDuplicateCodeValueInImportData(final Set<String> values,
                                                       final String codeValue) {
        if (values.contains(codeValue.toLowerCase())) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_DUPLICATE_CODE_VALUE));
        }
    }

    public void checkOrdersForDuplicateValues(final Set<CodeDTO> codes) {
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

    public String parseStringFromCsvRecord(final CSVRecord record,
                                           final String columnName) {
        final String value;
        if (record.isMapped(columnName)) {
            value = record.get(columnName);
        } else {
            value = null;
        }
        return value;
    }

    public UUID parseUUIDFromString(final String uuidString) {
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

    public UUID parseIdFromRecord(final CSVRecord record) {
        final UUID id;
        if (record.isMapped(CONTENT_HEADER_ID)) {
            id = parseUUIDFromString(record.get(CONTENT_HEADER_ID));
        } else {
            id = null;
        }
        return id;
    }

    public String parseCodeValueFromRecord(final CSVRecord record) {
        final String codeValue;
        if (record.isMapped(CONTENT_HEADER_CODEVALUE)) {
            codeValue = parseStringFromCsvRecord(record, CONTENT_HEADER_CODEVALUE);
        } else {
            codeValue = null;
        }
        return codeValue;
    }

    public String parseStartDateStringFromCsvRecord(final CSVRecord record) {
        return parseStringFromCsvRecord(record, CONTENT_HEADER_STARTDATE);
    }

    public String parseEndDateStringFromCsvRecord(final CSVRecord record) {
        return parseStringFromCsvRecord(record, CONTENT_HEADER_ENDDATE);
    }

    public String parseConceptUriFromCsvRecord(final CSVRecord record) {
        return parseStringFromCsvRecord(record, CONTENT_HEADER_CONCEPTURI);
    }

    public boolean skipEmptyLine(final String codeValue,
                                 final String status) {
        if ((codeValue == null || codeValue.trim().isEmpty()) && (status == null || status.trim().isEmpty())) {
            return true;
        }
        return false;
    }
}
