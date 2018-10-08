package fi.vm.yti.codelist.intake.parser.impl;

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
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.intake.exception.CsvParsingException;
import fi.vm.yti.codelist.intake.exception.ExcelParsingException;
import fi.vm.yti.codelist.intake.exception.JsonParsingException;
import fi.vm.yti.codelist.intake.parser.ExternalReferenceParser;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Service
public class ExternalReferenceParserImpl extends AbstractBaseParser implements ExternalReferenceParser {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalReferenceParserImpl.class);

    @Override
    public ExternalReferenceDTO parseExternalReferenceFromJson(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final ExternalReferenceDTO externalReference;
        try {
            externalReference = mapper.readValue(jsonPayload, ExternalReferenceDTO.class);
        } catch (final IOException e) {
            LOG.error("ExternalReference parsing failed from JSON!", e);
            throw new JsonParsingException(ERR_MSG_USER_406);
        }
        return externalReference;
    }

    @Override
    public Set<ExternalReferenceDTO> parseExternalReferencesFromJson(final String jsonPayload) {
        final ObjectMapper mapper = createObjectMapper();
        final Set<ExternalReferenceDTO> externalReferences;
        try {
            externalReferences = mapper.readValue(jsonPayload, new TypeReference<List<ExternalReferenceDTO>>() {
            });
        } catch (final IOException e) {
            LOG.error("ExternalReferences parsing failed from JSON!", e);
            throw new JsonParsingException(ERR_MSG_USER_406);
        }
        return externalReferences;
    }

    @Override
    @SuppressFBWarnings("UC_USELESS_OBJECT")
    public Set<ExternalReferenceDTO> parseExternalReferencesFromCsvInputStream(final InputStream inputStream) {
        final Set<ExternalReferenceDTO> externalReferences = new HashSet<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withQuote('"').withQuoteMode(QuoteMode.MINIMAL).withHeader())) {
            final Map<String, Integer> headerMap = csvParser.getHeaderMap();
            final Map<String, Integer> titleHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_TITLE_PREFIX);
            final Map<String, Integer> descriptionHeaders = parseHeadersWithPrefix(headerMap, CONTENT_HEADER_DESCRIPTION_PREFIX);
            final List<CSVRecord> records = csvParser.getRecords();
            for (final CSVRecord record : records) {
                final ExternalReferenceDTO externalReference = new ExternalReferenceDTO();
                final UUID id = parseUUIDFromString(record.get(CONTENT_HEADER_ID));
                externalReference.setId(id);
                final UUID parentCodeSchemeId = parseUUIDFromString(record.get(CONTENT_HEADER_PARENTCODESCHEMEID));
                final CodeSchemeDTO parentCodeScheme = new CodeSchemeDTO();
                parentCodeScheme.setId(parentCodeSchemeId);
                externalReference.setParentCodeScheme(parentCodeScheme);
                final String propertyTypeLocalName = record.get(CONTENT_HEADER_PROPERTYTYPE);
                final PropertyTypeDTO propertyType = new PropertyTypeDTO();
                propertyType.setLocalName(propertyTypeLocalName);
                externalReference.setPropertyType(propertyType);
                final String href = record.get(CONTENT_HEADER_HREF);
                externalReference.setHref(href);
                externalReference.setTitle(parseLocalizedValueFromCsvRecord(titleHeaders, record));
                externalReference.setDescription(parseLocalizedValueFromCsvRecord(descriptionHeaders, record));
                externalReferences.add(externalReference);
            }
        } catch (final IllegalArgumentException e) {
            LOG.error("Duplicate header value found in CSV!", e);
            throw new CsvParsingException(ERR_MSG_USER_DUPLICATE_HEADER_VALUE);
        } catch (final IOException e) {
            LOG.error("Error parsing CSV file!", e);
            throw new CsvParsingException(ERR_MSG_USER_ERROR_PARSING_CSV_FILE);
        }
        return externalReferences;
    }

    @Override
    public Set<ExternalReferenceDTO> parseExternalReferencesFromExcelInputStream(final InputStream inputStream) {
        final Set<ExternalReferenceDTO> externalReferences = new HashSet<>();
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
                } else if (!checkIfRowIsEmpty(row)) {
                    final ExternalReferenceDTO externalReference = new ExternalReferenceDTO();
                    final UUID id = parseUUIDFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_ID))));
                    externalReference.setId(id);
                    final UUID parentCodeSchemeId = parseUUIDFromString(formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_PARENTCODESCHEMEID))));
                    final CodeSchemeDTO parentCodeScheme = new CodeSchemeDTO();
                    parentCodeScheme.setId(parentCodeSchemeId);
                    externalReference.setParentCodeScheme(parentCodeScheme);
                    final String href = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_HREF)));
                    externalReference.setHref(href);
                    final String propertyTypeLocalName = formatter.formatCellValue(row.getCell(headerMap.get(CONTENT_HEADER_PROPERTYTYPE)));
                    final PropertyTypeDTO propertyType = new PropertyTypeDTO();
                    propertyType.setLocalName(propertyTypeLocalName);
                    externalReference.setPropertyType(propertyType);
                    externalReference.setTitle(parseLocalizedValueFromExcelRow(titleHeaders, row, formatter));
                    externalReference.setDescription(parseLocalizedValueFromExcelRow(descriptionHeaders, row, formatter));
                    externalReferences.add(externalReference);
                }
            }
        } catch (final InvalidFormatException | IOException | POIXMLException e) {
            LOG.error("Error parsing Excel file!", e);
            throw new ExcelParsingException(ERR_MSG_USER_ERROR_PARSING_EXCEL_FILE);
        }
        return externalReferences;
    }
}
