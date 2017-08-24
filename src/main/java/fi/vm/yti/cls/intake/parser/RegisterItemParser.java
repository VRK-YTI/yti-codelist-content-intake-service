package fi.vm.yti.cls.intake.parser;

import fi.vm.yti.cls.common.model.RegisterItem;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.intake.api.ApiConstants;
import fi.vm.yti.cls.intake.api.ApiUtils;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;


/**
 * Class that handles parsing of registerItems from source data.
 */
@Service
public class RegisterItemParser {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterItemParser.class);

    private final ApiUtils m_apiUtils;

    private final ParserUtils m_parserUtils;


    @Inject
    public RegisterItemParser(final ApiUtils apiUtils,
                              final ParserUtils parserUtils) {

        m_apiUtils = apiUtils;

        m_parserUtils = parserUtils;

    }


    /**
     * Parses the .csv RegisterItem-file and returns the registerItems as an arrayList.
     *
     * @param registerCode Register code identifier for the register.
     * @param source Source identifier for the data.
     * @param inputStream The Municipality -file.
     * @return List of Municipality objects.
     */
    public List<RegisterItem> parseRegisterItemsFromClsInputStream(final String registerCode,
                                                                   final String source,
                                                                   final InputStream inputStream) {

        final List<RegisterItem> registerItems = new ArrayList<>();

        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {

            final BufferedReader in = new BufferedReader(inputStreamReader);
            FileUtils.skipBom(in);

            final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withHeader());

            final List<CSVRecord> records = csvParser.getRecords();

            records.forEach(record -> {
                final Map<String, RegisterItem> existingRegisterItemsMap = m_parserUtils.getRegisterItemsMap(registerCode);

                final String code = Utils.ensureRegionIdPadding(record.get("CODE"));
                final String finnishName = record.get("NAME_FI");
                final String swedishName = record.get("NAME_SE");
                final String englishName = record.get("NAME_EN");
                final Status status = Status.valueOf(record.get("STATUS"));

                final RegisterItem registerItem = createOrUpdateRegisterItem(existingRegisterItemsMap, registerCode, code, status, source, finnishName, swedishName, englishName);
                registerItems.add(registerItem);
            });

        } catch (IOException e) {
            LOG.error("Parsing regions failed: " + e.getMessage());
        }

        return registerItems;

    }


    private RegisterItem createOrUpdateRegisterItem(final Map<String, RegisterItem> registerItemsMap,
                                                    final String registerCode,
                                                    final String code,
                                                    final Status status,
                                                    final String source,
                                                    final String finnishName,
                                                    final String swedishName,
                                                    final String englishName) {

        final String url = m_apiUtils.createResourceUrl(ApiConstants.API_PATH_REGISTERS + "/" + registerCode, code);
        final Date timeStamp = new Date(System.currentTimeMillis());

        RegisterItem registerItem = registerItemsMap.get(code);

        // Update
        if (registerItem != null) {
            boolean hasChanges = false;
            if (!Objects.equals(registerItem.getStatus(), status.toString())) {
                registerItem.setStatus(status.toString());
                hasChanges = true;
            }
            if (!Objects.equals(registerItem.getRegister(), registerCode)) {
                registerItem.setRegister(registerCode);
                hasChanges = true;
            }
            if (!Objects.equals(registerItem.getUrl(), url)) {
                registerItem.setUrl(url);
                hasChanges = true;
            }
            if (!Objects.equals(registerItem.getSource(), source)) {
                registerItem.setSource(source);
                hasChanges = true;
            }
            if (!Objects.equals(registerItem.getNameFinnish(), finnishName)) {
                registerItem.setNameFinnish(finnishName);
                hasChanges = true;
            }
            if (!Objects.equals(registerItem.getNameSwedish(), swedishName)) {
                registerItem.setNameSwedish(swedishName);
                hasChanges = true;
            }
            if (!Objects.equals(registerItem.getNameEnglish(), englishName)) {
                registerItem.setNameEnglish(englishName);
                hasChanges = true;
            }
            if (hasChanges) {
                registerItem.setModified(timeStamp);
            }

        // Create
        } else {
            registerItem = new RegisterItem();
            registerItem.setId(UUID.randomUUID().toString());
            registerItem.setStatus(status.toString());
            registerItem.setUrl(url);
            registerItem.setRegister(registerCode);
            registerItem.setCode(code);
            registerItem.setSource(source);
            registerItem.setCreated(timeStamp);
            registerItem.setNameFinnish(finnishName);
            registerItem.setNameSwedish(swedishName);
            registerItem.setNameEnglish(englishName);
        }

        return registerItem;

    }

}
