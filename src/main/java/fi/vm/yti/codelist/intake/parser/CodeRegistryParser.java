package fi.vm.yti.codelist.intake.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.common.constants.ApiConstants;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.util.FileUtils;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

/**
 * Class that handles parsing of coderegistries from source data.
 */
@Service
public class CodeRegistryParser {

    private static final Logger LOG = LoggerFactory.getLogger(CodeRegistryParser.class);
    private final ApiUtils apiUtils;
    private final ParserUtils parserUtils;

    @Inject
    public CodeRegistryParser(final ApiUtils apiUtils,
                            final ParserUtils parserUtils) {
        this.apiUtils = apiUtils;
        this.parserUtils = parserUtils;
    }

    /**
     * Parses the .csv CodeRegistry-file and returns the coderegistries as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The CodeRegistry-file.
     * @return List of CodeRegistry objects.
     */
    public List<CodeRegistry> parseCodeRegistriesFromInputStream(final String source,
                                                                 final InputStream inputStream) {
        final List<CodeRegistry> codeRegistries = new ArrayList<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withHeader())) {
            FileUtils.skipBom(in);
            final List<CSVRecord> records = csvParser.getRecords();
            records.forEach(record -> {
                final String code = record.get(CSV_HEADER_CODEVALUE);
                final String prefLabelFinnish = record.get(CSV_HEADER_PREFLABEL_FI);
                final String prefLabelSwedish = record.get(CSV_HEADER_PREFLABEL_SE);
                final String prefLabelEnglish = record.get(CSV_HEADER_PREFLABEL_EN);
                final String definitionFinnish = record.get(CSV_HEADER_DEFINITION_FI);
                final String definitionSwedish = record.get(CSV_HEADER_DEFINITION_SE);
                final String definitionEnglish = record.get(CSV_HEADER_DEFINITION_EN);
                final CodeRegistry codeRegistry = createOrUpdateCodeRegistry(code, source, prefLabelFinnish, prefLabelSwedish, prefLabelEnglish, definitionFinnish, definitionSwedish, definitionEnglish);
                if (codeRegistry != null) {
                    codeRegistries.add(codeRegistry);
                }
            });
        } catch (IOException e) {
            LOG.error("Parsing coderegistries failed: " + e.getMessage());
        }
        return codeRegistries;
    }

    private CodeRegistry createOrUpdateCodeRegistry(final String code,
                                                    final String prefLabelFinnish,
                                                    final String prefLabelSwedish,
                                                    final String prefLabelEnglish,
                                                    final String definitionFinnish,
                                                    final String definitionSwedish,
                                                    final String definitionEnglish,
                                                    final String source) {
        final Map<String, CodeRegistry> existingCodeRegistriesMap = parserUtils.getCodeRegistriesMap();
        String uri = apiUtils.createResourceUrl(ApiConstants.API_PATH_CODEREGISTRIES, code);
        final Date timeStamp = new Date(System.currentTimeMillis());
        CodeRegistry codeRegistry = existingCodeRegistriesMap.get(code);
        // Update
        if (codeRegistry != null) {
            boolean hasChanges = false;
            if (!Objects.equals(codeRegistry.getUri(), uri)) {
                codeRegistry.setUri(uri);
                hasChanges = true;
            }
            if (!Objects.equals(codeRegistry.getSource(), source)) {
                codeRegistry.setSource(source);
                hasChanges = true;
            }
            if (!Objects.equals(codeRegistry.getPrefLabel(LANGUAGE_CODE_FI), prefLabelFinnish)) {
                codeRegistry.setPrefLabel(LANGUAGE_CODE_FI, prefLabelFinnish);
                hasChanges = true;
            }
            if (!Objects.equals(codeRegistry.getPrefLabel(LANGUAGE_CODE_SE), prefLabelSwedish)) {
                codeRegistry.setPrefLabel(LANGUAGE_CODE_SE, prefLabelSwedish);
                hasChanges = true;
            }
            if (!Objects.equals(codeRegistry.getPrefLabel(LANGUAGE_CODE_EN), prefLabelEnglish)) {
                codeRegistry.setPrefLabel(LANGUAGE_CODE_EN, prefLabelEnglish);
                hasChanges = true;
            }
            if (!Objects.equals(codeRegistry.getDefinition(LANGUAGE_CODE_FI), definitionFinnish)) {
                codeRegistry.setDefinition(LANGUAGE_CODE_FI, definitionFinnish);
                hasChanges = true;
            }
            if (!Objects.equals(codeRegistry.getDefinition(LANGUAGE_CODE_SE), definitionSwedish)) {
                codeRegistry.setDefinition(LANGUAGE_CODE_SE, prefLabelSwedish);
                hasChanges = true;
            }
            if (!Objects.equals(codeRegistry.getDefinition(LANGUAGE_CODE_EN), definitionEnglish)) {
                codeRegistry.setDefinition(LANGUAGE_CODE_EN, prefLabelEnglish);
                hasChanges = true;
            }
            if (hasChanges) {
                codeRegistry.setModified(timeStamp);
            }
        // Create
        } else {
            codeRegistry = new CodeRegistry();
            codeRegistry.setId(UUID.randomUUID().toString());
            codeRegistry.setUri(uri);
            codeRegistry.setCodeValue(code);
            codeRegistry.setSource(source);
            codeRegistry.setModified(timeStamp);
            codeRegistry.setPrefLabel(LANGUAGE_CODE_FI, prefLabelFinnish);
            codeRegistry.setPrefLabel(LANGUAGE_CODE_SE, prefLabelSwedish);
            codeRegistry.setPrefLabel(LANGUAGE_CODE_EN, prefLabelEnglish);
            codeRegistry.setDefinition(LANGUAGE_CODE_FI, definitionFinnish);
            codeRegistry.setDefinition(LANGUAGE_CODE_SE, definitionSwedish);
            codeRegistry.setDefinition(LANGUAGE_CODE_EN, definitionEnglish);
        }
        return codeRegistry;
    }

}
