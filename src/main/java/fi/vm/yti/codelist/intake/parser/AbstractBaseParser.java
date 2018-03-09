package fi.vm.yti.codelist.intake.parser;

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
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.ErrorModel;
import fi.vm.yti.codelist.common.model.ExternalReference;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.exception.CodeParsingException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

public abstract class AbstractBaseParser {

    public static final String JUPO_REGISTRY = "jupo";
    public static final String YTI_DATACLASSIFICATION_CODESCHEME = "serviceclassification";
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBaseParser.class);

    public static boolean isRowEmpty(final Row row) {
        for (int cellIndex = row.getFirstCellNum(); cellIndex < row.getLastCellNum(); cellIndex++) {
            final Cell cell = row.getCell(cellIndex);
            if (cell != null && cell.getCellTypeEnum() != CellType.BLANK)
                return false;
        }
        return true;
    }

    public String resolveLanguageFromHeader(final String prefix,
                                            final String header) {
        return header.substring(header.indexOf(prefix) + prefix.length()).toLowerCase();
    }

    public UUID parseUUIDFromString(final String uuidString) {
        final UUID uuid;
        if (uuidString == null || uuidString.isEmpty()) {
            uuid = null;
        } else {
            uuid = UUID.fromString(uuidString);
        }
        return uuid;
    }

    public ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        return mapper;
    }

    public Date parseStartDateFromString(final String dateString, final String rowIdentifier) {
        Date date = null;
        final ISO8601DateFormat dateFormat = new ISO8601DateFormat();
        if (!dateString.isEmpty()) {
            try {
                date = dateFormat.parse(dateString);
            } catch (ParseException e) {
                LOG.error("Parsing startDate failed from string: " + dateString);
                throw new CodeParsingException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                    ERR_MSG_USER_ERRONEOUS_START_DATE, rowIdentifier));
            }
        }
        return date;
    }

    public Date parseEndDateString(final String dateString, final String rowIdentifier) {
        Date date = null;
        final ISO8601DateFormat dateFormat = new ISO8601DateFormat();
        if (!dateString.isEmpty()) {
            try {
                date = dateFormat.parse(dateString);
            } catch (ParseException e) {
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
        } catch (ParseException e) {
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

    public Set<ExternalReference> initializeExternalReferences(final Set<ExternalReference> fromExternalReferences,
                                                               final CodeScheme codeScheme,
                                                               final ExternalReferenceParser externalReferenceParser) {
        final Set<ExternalReference> externalReferences = new HashSet<>();
        if (fromExternalReferences != null) {
            fromExternalReferences.forEach(fromExternalReference -> {
                if (!fromExternalReference.getGlobal()) {
                    fromExternalReference.setParentCodeScheme(codeScheme);
                }
                final ExternalReference externalReference = externalReferenceParser.createOrUpdateExternalReference(fromExternalReference, codeScheme);
                externalReferences.add(externalReference);
            });
        }
        return externalReferences;
    }

    public <T> void checkForDuplicateCodeValueInImportData(final Map<String, T> entityMap,
                                                           final String codeValue) {
        if (entityMap.containsKey(codeValue)) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "Duplicate value found in import data, failing"));
        }
    }
}
