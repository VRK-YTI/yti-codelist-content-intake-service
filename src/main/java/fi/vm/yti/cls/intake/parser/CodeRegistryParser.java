package fi.vm.yti.cls.intake.parser;

import fi.vm.yti.cls.common.model.CodeRegistry;
import fi.vm.yti.cls.intake.api.ApiConstants;
import fi.vm.yti.cls.intake.api.ApiUtils;
import fi.vm.yti.cls.intake.util.FileUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
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
        final Map<String, CodeRegistry> existingCodeRegistriesMap = parserUtils.getCodeRegistriesMap();

        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             final BufferedReader in = new BufferedReader(inputStreamReader);
             final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withHeader())) {
            FileUtils.skipBom(in);
            final List<CSVRecord> records = csvParser.getRecords();

            records.forEach(record -> {
                final String code = record.get("CODEVALUE");
                final String nameFinnish = record.get("PREFLABEL_FI");
                final String nameSwedish = record.get("PREFLABEL_SE");
                final String nameEnglish = record.get("PREFLABEL_EN");

                final CodeRegistry codeRegistry = createOrUpdateCodeRegistry(existingCodeRegistriesMap, code, nameFinnish, nameSwedish, nameEnglish, source);
                codeRegistries.add(codeRegistry);
            });
        } catch (IOException e) {
            LOG.error("Parsing codeschemes failed: " + e.getMessage());
        }
        return codeRegistries;
    }

    private CodeRegistry createOrUpdateCodeRegistry(final Map<String, CodeRegistry> codeRegistriesMap,
                                                    final String code,
                                                    final String finnishName,
                                                    final String swedishName,
                                                    final String englishName,
                                                    final String source) {

        String url = null;
        url = apiUtils.createResourceUrl(ApiConstants.API_PATH_CODEREGISTRIES, code);
        final Date timeStamp = new Date(System.currentTimeMillis());
        CodeRegistry codeRegistry = codeRegistriesMap.get(code);

        // Update
        if (codeRegistry != null) {
            boolean hasChanges = false;
            if (!Objects.equals(codeRegistry.getUri(), url)) {
                codeRegistry.setUri(url);
                hasChanges = true;
            }
            if (!Objects.equals(codeRegistry.getSource(), source)) {
                codeRegistry.setSource(source);
                hasChanges = true;
            }
            if (!Objects.equals(codeRegistry.getPrefLabel("fi"), finnishName)) {
                codeRegistry.setPrefLabel("fi", finnishName);
                hasChanges = true;
            }
            if (!Objects.equals(codeRegistry.getPrefLabel("se"), swedishName)) {
                codeRegistry.setPrefLabel("se", swedishName);
                hasChanges = true;
            }
            if (!Objects.equals(codeRegistry.getPrefLabel("en"), englishName)) {
                codeRegistry.setPrefLabel("en", englishName);
                hasChanges = true;
            }
            if (hasChanges) {
                codeRegistry.setModified(timeStamp);
            }
        // Create
        } else {
            codeRegistry = new CodeRegistry();
            codeRegistry.setId(UUID.randomUUID().toString());
            codeRegistry.setUri(url);
            codeRegistry.setCodeValue(code);
            codeRegistry.setSource(source);
            codeRegistry.setCreated(timeStamp);
            codeRegistry.setPrefLabel("fi", finnishName);
            codeRegistry.setPrefLabel("se", swedishName);
            codeRegistry.setPrefLabel("en", englishName);
        }

        return codeRegistry;
    }

}
