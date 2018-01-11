package fi.vm.yti.codelist.intake.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.ExternalReference;
import fi.vm.yti.codelist.common.model.PropertyType;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.jpa.ExternalReferenceRepository;
import fi.vm.yti.codelist.intake.jpa.PropertyTypeRepository;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

/**
 * Class that handles parsing of ExternalReferences from source data.
 */
@Service
public class ExternalReferenceParser extends AbstractBaseParser {

    private static final Logger LOG = LoggerFactory.getLogger(CodeSchemeParser.class);
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

    /**
     * Parses the .csv ExternalReference-file and returns the ExternalReference as an ArrayList.
     *
     * @param inputStream The ExternalReference -file.
     * @return List of ExternalReference objects.
     */
    public List<ExternalReference> parseExternalReferencesFromCsvInputStream(final InputStream inputStream) {
        final List<ExternalReference> externalReferences = new ArrayList<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
            final Map<String, Integer> headerMap = csvParser.getHeaderMap();
            final Map<String, String> titleHeaders = new LinkedHashMap<>();
            final Map<String, String> descriptionHeaders = new LinkedHashMap<>();
            for (final String value : headerMap.keySet()) {
                if (value.startsWith(CONTENT_HEADER_TITLE_PREFIX)) {
                    titleHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_TITLE_PREFIX, value), value);
                } else if (value.startsWith(CONTENT_HEADER_DESCRIPTION_PREFIX)) {
                    descriptionHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_DESCRIPTION_PREFIX, value), value);
                }
            }
            final List<CSVRecord> records = csvParser.getRecords();
            for (final CSVRecord record : records) {
                final UUID id = parseUUIDFromString(record.get(CONTENT_HEADER_ID));
                final UUID parentCodeSchemeId = parseUUIDFromString(record.get(CONTENT_HEADER_PARENTCODESCHEMEID));
                final CodeScheme parentCodeScheme = codeSchemeRepository.findById(parentCodeSchemeId);
                final String propertyTypeLocalName = record.get(CONTENT_HEADER_PROPERTYTYPE);
                final PropertyType propertyType = propertyTypeRepository.findByLocalName(propertyTypeLocalName);
                final String url = record.get(CONTENT_HEADER_URL);
                final Map<String, String> title = new LinkedHashMap<>();
                titleHeaders.forEach((language, header) -> {
                    title.put(language, record.get(header));
                });
                final Map<String, String> description = new LinkedHashMap<>();
                descriptionHeaders.forEach((language, header) -> {
                    description.put(language, record.get(header));
                });
                final ExternalReference externalReference = createOrUpdateExternalReference(id, propertyType, url, parentCodeScheme, title, description);
                externalReferences.add(externalReference);
            }
        } catch (IOException e) {
            LOG.error("Parsing ExternalReferences failed: " + e.getMessage());
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
    public List<ExternalReference> parseExternalReferencesFromExcelInputStream(final InputStream inputStream) throws Exception {
        final List<ExternalReference> externalReferences = new ArrayList<>();
        try (final Workbook workbook = WorkbookFactory.create(inputStream)) {
            final DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheet(EXCEL_SHEET_EXTERNALREFERENCES);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            final Iterator<Row> rowIterator = sheet.rowIterator();
            final Map<String, Integer> genericHeaders = new LinkedHashMap<>();
            final Map<String, Integer> titleHeaders = new LinkedHashMap<>();
            final Map<String, Integer> descriptionHeaders = new LinkedHashMap<>();
            boolean firstRow = true;
            while (rowIterator.hasNext()) {
                final Row row = rowIterator.next();
                if (firstRow) {
                    final Iterator<Cell> cellIterator = row.cellIterator();
                    while (cellIterator.hasNext()) {
                        final Cell cell = cellIterator.next();
                        final String value = cell.getStringCellValue();
                        final Integer index = cell.getColumnIndex();
                        if (value.startsWith(CONTENT_HEADER_TITLE_PREFIX)) {
                            titleHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_TITLE_PREFIX, value), index);
                        } else if (value.startsWith(CONTENT_HEADER_DESCRIPTION_PREFIX)) {
                            descriptionHeaders.put(resolveLanguageFromHeader(CONTENT_HEADER_DESCRIPTION_PREFIX, value), index);
                        } else {
                            genericHeaders.put(value, index);
                        }
                    }
                    firstRow = false;
                } else {
                    final UUID id = parseUUIDFromString(formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_ID))));
                    final UUID parentCodeSchemeId = parseUUIDFromString(formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_PARENTCODESCHEMEID))));
                    final CodeScheme parentCodeScheme = codeSchemeRepository.findById(parentCodeSchemeId);
                    final String url = formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_URL)));
                    final String propertyTypeLocalName = formatter.formatCellValue(row.getCell(genericHeaders.get(CONTENT_HEADER_PROPERTYTYPE)));
                    final PropertyType propertyType = propertyTypeRepository.findByLocalName(propertyTypeLocalName);
                    final Map<String, String> title = new LinkedHashMap<>();
                    for (final String language : titleHeaders.keySet()) {
                        title.put(language, formatter.formatCellValue(row.getCell(titleHeaders.get(language))));
                    }
                    final Map<String, String> description = new LinkedHashMap<>();
                    for (final String language : descriptionHeaders.keySet()) {
                        description.put(language, formatter.formatCellValue(row.getCell(descriptionHeaders.get(language))));
                    }
                    final ExternalReference externalReference = createOrUpdateExternalReference(id, propertyType, url, parentCodeScheme, title, description);
                    if (externalReference != null) {
                        externalReferences.add(externalReference);
                    }
                }
            }
        }
        return externalReferences;
    }

    private ExternalReference createOrUpdateExternalReference(final UUID id,
                                                              final PropertyType propertyType,
                                                              final String url,
                                                              final CodeScheme parentCodeScheme,
                                                              final Map<String, String> title,
                                                              final Map<String, String> description) {
        ExternalReference externalReference = null;
        String uri = null;
        if (id != null) {
            externalReference = externalReferenceRepository.findById(id);
            uri = apiUtils.createResourceUri(API_PATH_EXTERNALREFERENCES, id.toString());
        }
        if (externalReference != null) {
            boolean hasChanges = false;
            if (!Objects.equals(externalReference.getUri(), uri)) {
                externalReference.setUri(uri);
                hasChanges = true;
            }
            if (!Objects.equals(externalReference.getUrl(), url)) {
                externalReference.setUrl(url);
                hasChanges = true;
            }
            if (!Objects.equals(externalReference.getParentCodeScheme(), parentCodeScheme)) {
                externalReference.setParentCodeScheme(parentCodeScheme);
                externalReference.setGlobal(parentCodeScheme == null);
                hasChanges = true;
            }
            if (!Objects.equals(externalReference.getPropertyType(), propertyType)) {
                externalReference.setPropertyType(propertyType);
                hasChanges = true;
            }
            for (final String language : title.keySet()) {
                final String value = title.get(language);
                if (!Objects.equals(externalReference.getTitle(language), value)) {
                    externalReference.setTitle(language, value);
                    hasChanges = true;
                }
            }
            for (final String language : description.keySet()) {
                final String value = description.get(language);
                if (!Objects.equals(externalReference.getDescription(language), value)) {
                    externalReference.setDescription(language, value);
                    hasChanges = true;
                }
            }
            if (hasChanges) {
                final Date timeStamp = new Date(System.currentTimeMillis());
                externalReference.setModified(timeStamp);
            }
        } else {
            externalReference = new ExternalReference();
            if (id != null) {
                externalReference.setId(id);
            } else {
                final UUID uuid = UUID.randomUUID();
                uri = apiUtils.createResourceUri(API_PATH_EXTERNALREFERENCES, uuid.toString());
                externalReference.setId(uuid);
            }
            externalReference.setParentCodeScheme(parentCodeScheme);
            externalReference.setGlobal(parentCodeScheme == null);
            externalReference.setPropertyType(propertyType);
            externalReference.setUri(uri);
            externalReference.setUrl(url);
            for (final String language : title.keySet()) {
                externalReference.setTitle(language, title.get(language));
            }
            for (final String language : description.keySet()) {
                externalReference.setDescription(language, description.get(language));
            }
            final Date timeStamp = new Date(System.currentTimeMillis());
            externalReference.setModified(timeStamp);
        }
        return externalReference;
    }
}
