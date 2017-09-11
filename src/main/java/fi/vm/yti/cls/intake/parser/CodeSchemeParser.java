package fi.vm.yti.cls.intake.parser;

import fi.vm.yti.cls.common.model.CodeRegistry;
import fi.vm.yti.cls.common.model.CodeScheme;
import fi.vm.yti.cls.common.model.CodeSchemeType;
import fi.vm.yti.cls.common.model.Status;
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
 * Class that handles parsing of codeschemes from source data.
 */
@Service
public class CodeSchemeParser {

    private static final Logger LOG = LoggerFactory.getLogger(CodeSchemeParser.class);
    private final ApiUtils apiUtils;
    private final ParserUtils parserUtils;

    @Inject
    public CodeSchemeParser(final ApiUtils apiUtils,
                            final ParserUtils parserUtils) {
        this.apiUtils = apiUtils;
        this.parserUtils = parserUtils;
    }

    /**
     * Parses the .csv CodeScheme-file and returns the codeschemes as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The CodeScheme -file.
     * @return List of CodeScheme objects.
     */
    public List<CodeScheme> parseCodeSchemesFromClsInputStream(final CodeRegistry codeRegistry,
                                                               final String source,
                                                               final InputStream inputStream) {
        final List<CodeScheme> codeSchemes = new ArrayList<>();
        final Map<String, CodeScheme> existingCodeSchemesMap = parserUtils.getCodeSchemesMap();

        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                final BufferedReader in = new BufferedReader(inputStreamReader);
                final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withHeader())) {
            FileUtils.skipBom(in);
            final List<CSVRecord> records = csvParser.getRecords();

            records.forEach(record -> {
                final String codeValue = record.get("CODEVALUE");
                final String nameFinnish = record.get("PREFLABEL_FI");
                final String nameSwedish = record.get("PREFLABEL_SE");
                final String nameEnglish = record.get("PREFLABEL_EN");
                final String version = record.get("VERSION");
                final String definition = record.get("DEFINITION");
                final String description = record.get("DESCRIPTION");
                final String changeNote = record.get("CHANGENOTE");
                final Status status = Status.valueOf(record.get("STATUS"));
                final CodeSchemeType type = CodeSchemeType.valueOf(record.get("TYPE"));

                final CodeScheme register = createOrUpdateCodeScheme(existingCodeSchemesMap, codeRegistry, codeValue, nameFinnish, nameSwedish, nameEnglish, version, source, definition, description, changeNote, status, type);
                codeSchemes.add(register);
            });
        } catch (IOException e) {
            LOG.error("Parsing codeschemes failed: " + e.getMessage());
        }
        return codeSchemes;
    }
    
    private CodeScheme createOrUpdateCodeScheme(final Map<String, CodeScheme> codeSchemesMap,
                                                final CodeRegistry codeRegistry,
                                                final String codeValue,
                                                final String finnishName,
                                                final String swedishName,
                                                final String englishName,
                                                final String version,
                                                final String source,
                                                final String definition,
                                                final String description,
                                                final String changeNote,
                                                final Status status,
                                                final CodeSchemeType type) {
        String url = null;
        if (type == CodeSchemeType.CODELIST) {
            url = apiUtils.createResourceUrl(ApiConstants.API_PATH_CODEREGISTRIES + "/" + codeRegistry.getCodeValue() + ApiConstants.API_PATH_CODESCHEMES, codeValue);
        } else {
            url = apiUtils.createResourceUrl("/" + codeValue, null);
        }
        final Date timeStamp = new Date(System.currentTimeMillis());
        CodeScheme codeScheme = codeSchemesMap.get(codeValue);

        // Update
        if (codeScheme != null) {
            boolean hasChanges = false;
            if (!Objects.equals(codeScheme.getStatus(), status.toString())) {
                codeScheme.setStatus(status.toString());
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getCodeRegistry(), codeRegistry)) {
                codeScheme.setCodeRegistry(codeRegistry);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getUri(), url)) {
                codeScheme.setUri(url);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getSource(), source)) {
                codeScheme.setSource(source);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getDefinition(), definition)) {
                codeScheme.setDefinition(definition);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getDescription(), description)) {
                codeScheme.setDescription(description);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getChangeNote(), changeNote)) {
                codeScheme.setChangeNote(changeNote);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getPrefLabel("fi"), finnishName)) {
                codeScheme.setPrefLabel("fi", finnishName);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getPrefLabel("se"), swedishName)) {
                codeScheme.setPrefLabel("se", swedishName);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getPrefLabel("en"), englishName)) {
                codeScheme.setPrefLabel("en", englishName);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getVersion(), version)) {
                codeScheme.setVersion(version);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getType(), type.toString())) {
                codeScheme.setType(type.toString());
                hasChanges = true;
            }
            if (hasChanges) {
                codeScheme.setModified(timeStamp);
            }
        // Create
        } else {
            codeScheme = new CodeScheme();
            codeScheme.setId(UUID.randomUUID().toString());
            codeScheme.setCodeRegistry(codeRegistry);
            codeScheme.setUri(url);
            codeScheme.setCodeValue(codeValue);
            codeScheme.setSource(source);
            codeScheme.setDefinition(definition);
            codeScheme.setDescription(description);
            codeScheme.setChangeNote(changeNote);
            codeScheme.setCreated(timeStamp);
            codeScheme.setPrefLabel("fi", finnishName);
            codeScheme.setPrefLabel("se", swedishName);
            codeScheme.setPrefLabel("en", englishName);
            codeScheme.setVersion(version);
            codeScheme.setStatus(status.toString());
            codeScheme.setType(type.toString());
        }
        return codeScheme;
    }

}
