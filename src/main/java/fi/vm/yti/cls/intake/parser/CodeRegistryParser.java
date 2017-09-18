package fi.vm.yti.cls.intake.parser;

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

import fi.vm.yti.cls.common.model.CodeRegistry;
import fi.vm.yti.cls.intake.api.ApiConstants;
import fi.vm.yti.cls.intake.api.ApiUtils;
import fi.vm.yti.cls.intake.util.FileUtils;

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
    public List<CodeRegistry> parseCodeRegistriesFromClsInputStream(final String source,
                                                                    final InputStream inputStream) {
        final List<CodeRegistry> codeRegistries = new ArrayList<>();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withHeader())) {
            FileUtils.skipBom(in);
            final List<CSVRecord> records = csvParser.getRecords();
            records.forEach(record -> {
                final String code = record.get("CODEVALUE");
                final String prefLabelFinnish = record.get("PREFLABEL_FI");
                final String prefLabelSwedish = record.get("PREFLABEL_SE");
                final String prefLabelEnglish = record.get("PREFLABEL_EN");
                final String definitionFinnish = record.get("DEFINITION_FI");
                final String definitionSwedish = record.get("DEFINITION_SE");
                final String definitionEnglish = record.get("DEFINITION_EN");
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
            if (!Objects.equals(codeRegistry.getPrefLabel("fi"), prefLabelFinnish)) {
                codeRegistry.setPrefLabel("fi", prefLabelFinnish);
                hasChanges = true;
            }
            if (!Objects.equals(codeRegistry.getPrefLabel("se"), prefLabelSwedish)) {
                codeRegistry.setPrefLabel("se", prefLabelSwedish);
                hasChanges = true;
            }
            if (!Objects.equals(codeRegistry.getPrefLabel("en"), prefLabelEnglish)) {
                codeRegistry.setPrefLabel("en", prefLabelEnglish);
                hasChanges = true;
            }
            if (!Objects.equals(codeRegistry.getDefinition("fi"), definitionFinnish)) {
                codeRegistry.setDefinition("fi", definitionFinnish);
                hasChanges = true;
            }
            if (!Objects.equals(codeRegistry.getDefinition("se"), definitionSwedish)) {
                codeRegistry.setDefinition("se", prefLabelSwedish);
                hasChanges = true;
            }
            if (!Objects.equals(codeRegistry.getDefinition("en"), definitionEnglish)) {
                codeRegistry.setDefinition("en", prefLabelEnglish);
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
            codeRegistry.setPrefLabel("fi", prefLabelFinnish);
            codeRegistry.setPrefLabel("se", prefLabelSwedish);
            codeRegistry.setPrefLabel("en", prefLabelEnglish);
            codeRegistry.setDefinition("fi", definitionFinnish);
            codeRegistry.setDefinition("se", definitionSwedish);
            codeRegistry.setDefinition("en", definitionEnglish);
        }
        return codeRegistry;
    }

}
