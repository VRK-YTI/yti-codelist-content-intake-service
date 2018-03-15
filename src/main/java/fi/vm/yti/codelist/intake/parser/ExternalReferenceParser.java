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
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.ErrorModel;
import fi.vm.yti.codelist.common.model.ExternalReference;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.exception.CsvParsingException;
import fi.vm.yti.codelist.intake.exception.ExcelParsingException;
import fi.vm.yti.codelist.intake.exception.JsonParsingException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.jpa.ExternalReferenceRepository;
import fi.vm.yti.codelist.intake.jpa.PropertyTypeRepository;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

/**
 * Class that handles parsing of ExternalReferences from source data.
 */
@Service
public class ExternalReferenceParser extends AbstractBaseParser {

    private final ApiUtils apiUtils;
    private final ExternalReferenceRepository externalReferenceRepository;
    private final PropertyTypeRepository propertyTypeRepository;
    private final CodeSchemeRepository codeSchemeRepository;

    @Inject
    public ExternalReferenceParser(final ApiUtils apiUtils,
                                   final ExternalReferenceRepository externalReferenceRepository,
                                   final PropertyTypeRepository propertyTypeRepository,
                                   final CodeSchemeRepository codeSchemeRepository) {
        this.apiUtils = apiUtils;
        this.externalReferenceRepository = externalReferenceRepository;
        this.propertyTypeRepository = propertyTypeRepository;
        this.codeSchemeRepository = codeSchemeRepository;
    }

    public ExternalReference parseExternalReferenceFromJson(final String jsonPayload,
                                                            final CodeScheme codeScheme) {
        final ObjectMapper mapper = createObjectMapper();
        final ExternalReference fromExternalReference;
        try {
            fromExternalReference = mapper.readValue(jsonPayload, ExternalReference.class);
        } catch (final IOException e) {
            throw new JsonParsingException(ERR_MSG_USER_406);
        }
        return createOrUpdateExternalReference(fromExternalReference, codeScheme);
    }

    public Set<ExternalReference> parseExternalReferencesFromJson(final String jsonPayload,
                                                                  final CodeScheme codeScheme) {
        final Set<ExternalReference> externalReferences = new HashSet<>();
        final ObjectMapper mapper = createObjectMapper();
        final Set<ExternalReference> fromExternalReferences;
        try {
            fromExternalReferences = mapper.readValue(jsonPayload, new TypeReference<List<ExternalReference>>() {
            });
        } catch (final IOException e) {
            throw new JsonParsingException(ERR_MSG_USER_406);
        }
        for (final ExternalReference fromExternalReference : fromExternalReferences) {
            externalReferences.add(createOrUpdateExternalReference(fromExternalReference, codeScheme));
        }
        return externalReferences;
    }

    /**
     * Parses the .csv ExternalReference-file and returns the ExternalReference as an ArrayList.
     *
     * @param inputStream The ExternalReference -file.
     * @return List of ExternalReference objects.
     */
    @SuppressFBWarnings("UC_USELESS_OBJECT")
    public Set<ExternalReference> parseExternalReferencesFromCsvInputStream(final InputStream inputStream) {
        final Set<ExternalReference> externalReferences = new HashSet<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
            final Map<String, Integer> headerMap = csvParser.getHeaderMap();
            final Map<String, Integer> titleHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_TITLE_PREFIX);
            final Map<String, Integer> descriptionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DESCRIPTION_PREFIX);
            final List<CSVRecord> records = csvParser.getRecords();
            for (final CSVRecord record : records) {
                final ExternalReference fromExternalReference = new ExternalReference();
                final UUID id = parseUUIDFromString(record.get(CONTENT_HEADER_ID));
                fromExternalReference.setId(id);
                final UUID parentCodeSchemeId = parseUUIDFromString(record.get(CONTENT_HEADER_PARENTCODESCHEMEID));
                final CodeScheme parentCodeScheme = codeSchemeRepository.findById(parentCodeSchemeId);
                fromExternalReference.setParentCodeScheme(parentCodeScheme);
                final String propertyTypeLocalName = record.get(CONTENT_HEADER_PROPERTYTYPE);
                fromExternalReference.setPropertyType(propertyTypeRepository.findByLocalName(propertyTypeLocalName));
                final String url = record.get(CONTENT_HEADER_URL);
                fromExternalReference.setUrl(url);
                fromExternalReference.setTitle(parseLocalizedValueFromCsvRecord(titleHeaders, record));
                fromExternalReference.setDescription(parseLocalizedValueFromCsvRecord(descriptionHeaders, record));
                final ExternalReference externalReference = createOrUpdateExternalReference(fromExternalReference, null);
                externalReferences.add(externalReference);
            }
        } catch (final IllegalArgumentException e) {
            throw new CsvParsingException(ERR_MSG_USER_DUPLICATE_HEADER_VALUE);
        } catch (final IOException e) {
            throw new CsvParsingException(ERR_MSG_USER_ERROR_PARSING_CSV_FILE);
        }
        return externalReferences;
    }

    /*
     * Parses the .xls ExternalReference Excel-file and returns the ExternalReferences as an arrayList.
     *
     * @param codeRegistry CodeRegistry.
     * @param inputStream The Code containing Excel -file.
     * @return List of Code objects.
     */
    public Set<ExternalReference> parseExternalReferencesFromExcelInputStream(final InputStream inputStream) {
        final Set<ExternalReference> externalReferences = new HashSet<>();
        try (final Workbook workbook = WorkbookFactory.create(inputStream)) {
            final DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheet(EXCEL_SHEET_EXTERNALREFERENCES);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            final Iterator<Row> rowIterator = sheet.rowIterator();
            Map<String, Integer> headerMap = null;
            Map<String, Integer> titleHeaders = null;
            Map<String, Integer> descriptionHeaders = null;
            boolean firstRow = true;
            while (rowIterator.hasNext()) {
                final Row row = rowIterator.next();
                if (firstRow) {
                    headerMap = resolveHeaderMap(row);
                    titleHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_TITLE_PREFIX);
                    descriptionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DESCRIPTION_PREFIX);
                    firstRow = false;
                } else {
                    final ExternalReference fromExternalReference = new ExternalReference();
                    final UUID id = parseUUIDFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ID))));
                    fromExternalReference.setId(id);
                    final UUID parentCodeSchemeId = parseUUIDFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_PARENTCODESCHEMEID))));
                    final CodeScheme parentCodeScheme = codeSchemeRepository.findById(parentCodeSchemeId);
                    fromExternalReference.setParentCodeScheme(parentCodeScheme);
                    final String url = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_URL)));
                    fromExternalReference.setUrl(url);
                    final String propertyTypeLocalName = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_PROPERTYTYPE)));
                    fromExternalReference.setPropertyType(propertyTypeRepository.findByLocalName(propertyTypeLocalName));
                    fromExternalReference.setTitle(parseLocalizedValueFromExcelRow(titleHeaders, row, formatter));
                    fromExternalReference.setDescription(parseLocalizedValueFromExcelRow(descriptionHeaders, row, formatter));
                    final ExternalReference externalReference = createOrUpdateExternalReference(fromExternalReference, null);
                    if (externalReference != null) {
                        externalReferences.add(externalReference);
                    }
                }
            }
        } catch (final InvalidFormatException | IOException | POIXMLException e) {
            throw new ExcelParsingException(ERR_MSG_USER_ERROR_PARSING_EXCEL_FILE);
        }
        return externalReferences;
    }

    public ExternalReference createOrUpdateExternalReference(final ExternalReference fromExternalReference,
                                                             final CodeScheme codeScheme) {
        final boolean isGlobal = fromExternalReference.getGlobal() != null ? fromExternalReference.getGlobal() : true;
        final ExternalReference existingExternalReference;
        if (fromExternalReference.getId() != null && codeScheme != null && !isGlobal) {
            existingExternalReference = externalReferenceRepository.findByIdAndParentCodeScheme(fromExternalReference.getId(), codeScheme);
        } else if (fromExternalReference.getId() != null && isGlobal) {
            existingExternalReference = externalReferenceRepository.findById(fromExternalReference.getId());
        } else {
            existingExternalReference = null;
        }
        final ExternalReference externalReference;
        if (existingExternalReference != null && isGlobal) {
            externalReference = existingExternalReference;
        } else if (existingExternalReference != null && !isGlobal) {
            externalReference = updateExternalReference(existingExternalReference, fromExternalReference);
        } else if (!isGlobal) {
            externalReference = createExternalReference(fromExternalReference);
        } else if (codeScheme == null) {
            externalReference = createExternalReference(fromExternalReference);
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
        }
        return externalReference;
    }

    private ExternalReference updateExternalReference(final ExternalReference existingExternalReference,
                                                      final ExternalReference fromExternalReference) {
        final String uri = apiUtils.createResourceUrl(API_PATH_EXTERNALREFERENCES, fromExternalReference.getId().toString());
        boolean hasChanges = false;
        if (!Objects.equals(existingExternalReference.getUri(), uri)) {
            existingExternalReference.setUri(uri);
            hasChanges = true;
        }
        if (!Objects.equals(existingExternalReference.getUrl(), fromExternalReference.getUrl())) {
            existingExternalReference.setUrl(fromExternalReference.getUrl());
            hasChanges = true;
        }
        if (!Objects.equals(existingExternalReference.getParentCodeScheme(), fromExternalReference.getParentCodeScheme())) {
            existingExternalReference.setParentCodeScheme(fromExternalReference.getParentCodeScheme());
            existingExternalReference.setGlobal(fromExternalReference.getParentCodeScheme() == null);
            hasChanges = true;
        }
        if (!Objects.equals(existingExternalReference.getPropertyType(), fromExternalReference.getPropertyType())) {
            existingExternalReference.setPropertyType(fromExternalReference.getPropertyType());
            hasChanges = true;
        }
        for (final Map.Entry<String, String> entry : fromExternalReference.getTitle().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingExternalReference.getTitle(language), value)) {
                existingExternalReference.setTitle(language, value);
                hasChanges = true;
            }
        }
        for (final Map.Entry<String, String> entry : fromExternalReference.getDescription().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingExternalReference.getDescription(language), value)) {
                existingExternalReference.setDescription(language, value);
                hasChanges = true;
            }
        }
        if (hasChanges) {
            final Date timeStamp = new Date(System.currentTimeMillis());
            existingExternalReference.setModified(timeStamp);
        }
        return existingExternalReference;
    }

    private ExternalReference createExternalReference(final ExternalReference fromExternalReference) {
        final ExternalReference externalReference = new ExternalReference();
        final String uri;
        if (fromExternalReference.getId() != null) {
            uri = apiUtils.createResourceUrl(API_PATH_EXTERNALREFERENCES, fromExternalReference.getId().toString());
            externalReference.setId(fromExternalReference.getId());
        } else {
            final UUID uuid = UUID.randomUUID();
            uri = apiUtils.createResourceUrl(API_PATH_EXTERNALREFERENCES, uuid.toString());
            externalReference.setId(uuid);
        }
        externalReference.setParentCodeScheme(fromExternalReference.getParentCodeScheme());
        externalReference.setGlobal(fromExternalReference.getParentCodeScheme() == null);
        externalReference.setPropertyType(fromExternalReference.getPropertyType());
        externalReference.setUri(uri);
        externalReference.setUrl(fromExternalReference.getUrl());
        for (final Map.Entry<String, String> entry : fromExternalReference.getTitle().entrySet()) {
            externalReference.setTitle(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, String> entry : fromExternalReference.getDescription().entrySet()) {
            externalReference.setDescription(entry.getKey(), entry.getValue());
        }
        final Date timeStamp = new Date(System.currentTimeMillis());
        externalReference.setModified(timeStamp);
        return externalReference;
    }
}
