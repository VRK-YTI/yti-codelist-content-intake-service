package fi.vm.yti.cls.intake.parser;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import fi.vm.yti.cls.common.model.Code;
import fi.vm.yti.cls.common.model.CodeScheme;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.intake.api.ApiConstants;
import fi.vm.yti.cls.intake.api.ApiUtils;
import fi.vm.yti.cls.intake.jpa.CodeRepository;
import fi.vm.yti.cls.intake.util.FileUtils;
import fi.vm.yti.cls.intake.util.Utils;
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Class that handles parsing of codes from source data.
 */
@Service
public class CodeParser {

    private static final Logger LOG = LoggerFactory.getLogger(CodeParser.class);
    private final ApiUtils apiUtils;
    private final CodeRepository codeRepository;

    @Inject
    public CodeParser(final ApiUtils apiUtils,
                      final CodeRepository codeRepository) {
        this.apiUtils = apiUtils;
        this.codeRepository = codeRepository;
    }

    /**
     * Parses the .csv Code-file and returns the codes as an arrayList.
     *
     * @param codeScheme CodeScheme codeValue identifier.
     * @param source Source identifier for the data.
     * @param inputStream The Code -file.
     * @return List of Code objects.
     */
    public List<Code> parseCodesFromClsInputStream(final CodeScheme codeScheme,
                                                   final String source,
                                                   final InputStream inputStream) throws Exception {
        final List<Code> codes = new ArrayList<>();
        if (codeScheme != null) {
            try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 final BufferedReader in = new BufferedReader(inputStreamReader);
                 final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withHeader())) {
                FileUtils.skipBom(in);
                final List<CSVRecord> records = csvParser.getRecords();

                for (final CSVRecord record : records) {
                    final String id = record.get("ID");
                    final String codeValue = Utils.ensureRegionIdPadding(record.get("CODEVALUE"));
                    final String prefLabelFinnish = record.get("PREFLABEL_FI");
                    final String prefLabelSwedish = record.get("PREFLABEL_SE");
                    final String prefLabelEnglish = record.get("PREFLABEL_EN");
                    final String descriptionFinnish = record.get("DESCRIPTION_FI");
                    final String descriptionSwedish = record.get("DESCRIPTION_SE");
                    final String descriptionEnglish = record.get("DESCRIPTION_EN");
                    final String definitionFinnish = record.get("DEFINITION_FI");
                    final String definitionSwedish = record.get("DEFINITION_SE");
                    final String definitionEnglish = record.get("DEFINITION_EN");
                    final String shortName = record.get("SHORTNAME");
                    final Status status = Status.valueOf(record.get("STATUS"));
                    final ISO8601DateFormat dateFormat = new ISO8601DateFormat();
                    Date startDate = null;
                    final String startDateString = record.get("STARTDATE");
                    if (!startDateString.isEmpty()) {
                        try {
                            startDate = dateFormat.parse(startDateString);
                        } catch (ParseException e) {
                            LOG.error("Parsing startDate for code: " + codeValue + " failed from string: " + startDateString);
                        }
                    }
                    Date endDate = null;
                    final String endDateString = record.get("STARTDATE");
                    if (!endDateString.isEmpty()) {
                        try {
                            endDate = dateFormat.parse(endDateString);
                        } catch (ParseException e) {
                            LOG.error("Parsing endDate for code: " + codeValue + " failed from string: " + endDateString);
                        }
                    }
                    final Code code = createOrUpdateCode(codeScheme, id, codeValue, status, source, shortName, startDate, endDate,
                                prefLabelFinnish, prefLabelSwedish, prefLabelEnglish,
                                descriptionFinnish, descriptionSwedish, descriptionEnglish,
                                definitionFinnish, definitionSwedish, definitionEnglish);
                    if (code != null) {
                        codes.add(code);
                    }
                }
            } catch (final IOException e) {
                LOG.error("Parsing codes failed: " + e.getMessage());
            }
        }
        return codes;
    }

    private Code createOrUpdateCode(final CodeScheme codeScheme,
                                    final String id,
                                    final String codeValue,
                                    final Status status,
                                    final String source,
                                    final String shortName,
                                    final Date startDate,
                                    final Date endDate,
                                    final String finnishPrefLabel,
                                    final String swedishPrefLabel,
                                    final String englishPrefLabel,
                                    final String finnishDescription,
                                    final String swedishDescription,
                                    final String englishDescription,
                                    final String finnishDefinition,
                                    final String swedishDefinition,
                                    final String englishDefinition) throws Exception {
        final Date timeStamp = new Date(System.currentTimeMillis());
        Code code = null;
        if (id != null) {
            code = codeRepository.findById(id);
        }
        String uri = null;
        if (Status.VALID == status) {
            uri = apiUtils.createResourceUrl(ApiConstants.API_PATH_CODEREGISTRIES + "/" + codeScheme.getCodeRegistry().getCodeValue() + ApiConstants.API_PATH_CODESCHEMES + "/" + codeScheme.getCodeValue() + ApiConstants.API_PATH_CODES, codeValue);
            final Code existingCode = codeRepository.findByCodeSchemeAndCodeValueAndStatus(codeScheme, codeValue, status.toString());
            if (existingCode != null) {
                LOG.error("Existing value already found, cancel update!");
                throw new Exception("Existing value already found with status VALID for code: " + codeValue + ", cancel update!");
            }
        } else if (id != null && !id.isEmpty()) {
            uri = apiUtils.createResourceUrl(ApiConstants.API_PATH_CODEREGISTRIES + "/" + codeScheme.getCodeRegistry().getCodeValue() + ApiConstants.API_PATH_CODESCHEMES + "/" + codeScheme.getCodeValue() + ApiConstants.API_PATH_CODES, id);
        }
        // Update
        if (code != null) {
            boolean hasChanges = false;
            if (!Objects.equals(code.getStatus(), status.toString())) {
                code.setStatus(status.toString());
                hasChanges = true;
            }
            if (!Objects.equals(code.getCodeScheme(), codeScheme)) {
                code.setCodeScheme(codeScheme);
                hasChanges = true;
            }
            if (!Objects.equals(code.getUri(), uri)) {
                code.setUri(uri);
                hasChanges = true;
            }
            if (!Objects.equals(code.getSource(), source)) {
                code.setSource(source);
                hasChanges = true;
            }
            if (!Objects.equals(code.getShortName(), shortName)) {
                code.setShortName(shortName);
                hasChanges = true;
            }
            if (!Objects.equals(code.getPrefLabel("fi"), finnishPrefLabel)) {
                code.setPrefLabel("fi", finnishPrefLabel);
                hasChanges = true;
            }
            if (!Objects.equals(code.getPrefLabel("se"), swedishPrefLabel)) {
                code.setPrefLabel("se", swedishPrefLabel);
                hasChanges = true;
            }
            if (!Objects.equals(code.getPrefLabel("en"), englishPrefLabel)) {
                code.setPrefLabel("en", englishPrefLabel);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getDefinition("fi"), finnishDefinition)) {
                codeScheme.setDefinition("fi", finnishDefinition);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getDefinition("se"), swedishDefinition)) {
                codeScheme.setDefinition("se", swedishDefinition);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getDefinition("en"), englishDefinition)) {
                codeScheme.setDefinition("en", englishDefinition);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getDescription("fi"), finnishDescription)) {
                codeScheme.setDescription("fi", finnishDescription);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getDescription("se"), swedishDescription)) {
                codeScheme.setDescription("se", swedishDescription);
                hasChanges = true;
            }
            if (!Objects.equals(codeScheme.getDescription("en"), englishDescription)) {
                codeScheme.setDescription("en", englishDescription);
                hasChanges = true;
            }
            if (!Objects.equals(code.getStartDate(), startDate)) {
                code.setStartDate(startDate);
                hasChanges = true;
            }
            if (!Objects.equals(code.getEndDate(), endDate)) {
                code.setEndDate(endDate);
                hasChanges = true;
            }
            if (hasChanges) {
                code.setModified(timeStamp);
            }
        // Create
        } else {
            code = new Code();
            if (id != null && !id.isEmpty()) {
                code.setId(id);
            } else {
                final String uuid = UUID.randomUUID().toString();
                if (status != Status.VALID) {
                    uri = apiUtils.createResourceUrl(ApiConstants.API_PATH_CODEREGISTRIES + "/" + codeScheme.getCodeRegistry().getCodeValue() + ApiConstants.API_PATH_CODESCHEMES + "/" + codeScheme.getCodeValue() + ApiConstants.API_PATH_CODES, uuid);
                }
                code.setId(uuid);
            }
            code.setStatus(status.toString());
            code.setUri(uri);
            code.setCodeScheme(codeScheme);
            code.setCodeValue(codeValue);
            code.setSource(source);
            code.setShortName(shortName);
            code.setModified(timeStamp);
            code.setPrefLabel("fi", finnishPrefLabel);
            code.setPrefLabel("se", swedishPrefLabel);
            code.setPrefLabel("en", englishPrefLabel);
            code.setDefinition("fi", finnishDefinition);
            code.setDefinition("se", swedishDefinition);
            code.setDefinition("en", englishDefinition);
            code.setDescription("fi", finnishDescription);
            code.setDescription("se", swedishDescription);
            code.setDescription("en", englishDescription);
            code.setStartDate(startDate);
            code.setEndDate(endDate);
        }
        return code;
    }

}
