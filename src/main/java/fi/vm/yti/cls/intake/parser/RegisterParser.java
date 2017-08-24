package fi.vm.yti.cls.intake.parser;

import fi.vm.yti.cls.common.model.Register;
import fi.vm.yti.cls.common.model.RegisterType;
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
 * Class that handles parsing of registers from source data.
 */
@Service
public class RegisterParser {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterItemParser.class);

    private final ApiUtils m_apiUtils;

    private final ParserUtils m_parserUtils;


    @Inject
    public RegisterParser(final ApiUtils apiUtils,
                              final ParserUtils parserUtils) {

        m_apiUtils = apiUtils;

        m_parserUtils = parserUtils;

    }


    /**
     * Parses the .csv Register-file and returns the registers as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The Municipality -file.
     * @return List of Municipality objects.
     */
    public List<Register> parseRegistersFromClsInputStream(final String source,
                                                           final InputStream inputStream) {

        final List<Register> registers = new ArrayList<>();

        final Map<String, Register> existingRegistersMap = m_parserUtils.getRegistersMap();

        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {

            final BufferedReader in = new BufferedReader(inputStreamReader);
            FileUtils.skipBom(in);

            final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withHeader());

            final List<CSVRecord> records = csvParser.getRecords();

            records.forEach(record -> {
                final String code = record.get("CODE");
                final String nameFinnish = record.get("NAME_FI");
                final String nameSwedish = record.get("NAME_SE");
                final String nameEnglish = record.get("NAME_EN");
                final String version = record.get("VERSION");
                final Status status = Status.valueOf(record.get("STATUS"));
                final RegisterType type = RegisterType.valueOf(record.get("TYPE"));

                final Register register = createOrUpdateRegister(existingRegistersMap, code, nameFinnish, nameSwedish, nameEnglish, version, source, status, type);
                registers.add(register);
            });

        } catch (IOException e) {
            LOG.error("Parsing registers failed: " + e.getMessage());
        }

        return registers;

    }

    
    private Register createOrUpdateRegister(final Map<String, Register> registersMap,
                                            final String code,
                                            final String finnishName,
                                            final String swedishName,
                                            final String englishName,
                                            final String version,
                                            final String source,
                                            final Status status,
                                            final RegisterType type) {

        String url = null;
        if (type == RegisterType.CODELIST) {
            url = m_apiUtils.createResourceUrl(ApiConstants.API_PATH_REGISTERS, code);
        } else {
            url = m_apiUtils.createResourceUrl(ApiConstants.API_PATH_REGISTERS + "/" + code, null);
        }
        final Date timeStamp = new Date(System.currentTimeMillis());

        Register register = registersMap.get(code);

        // Update
        if (register != null) {
            boolean hasChanges = false;
            if (!Objects.equals(register.getStatus(), status.toString())) {
                register.setStatus(status.toString());
                hasChanges = true;
            }
            if (!Objects.equals(register.getUrl(), url)) {
                register.setUrl(url);
                hasChanges = true;
            }
            if (!Objects.equals(register.getSource(), source)) {
                register.setSource(source);
                hasChanges = true;
            }
            if (!Objects.equals(register.getNameFinnish(), finnishName)) {
                register.setNameFinnish(finnishName);
                hasChanges = true;
            }
            if (!Objects.equals(register.getNameSwedish(), swedishName)) {
                register.setNameSwedish(swedishName);
                hasChanges = true;
            }
            if (!Objects.equals(register.getNameEnglish(), englishName)) {
                register.setNameEnglish(englishName);
                hasChanges = true;
            }
            if (!Objects.equals(register.getVersion(), version)) {
                register.setVersion(version);
                hasChanges = true;
            }
            if (!Objects.equals(register.getType(), type.toString())) {
                register.setType(type.toString());
                hasChanges = true;
            }
            if (hasChanges) {
                register.setModified(timeStamp);
            }

        // Create
        } else {
            register = new Register();
            register.setId(UUID.randomUUID().toString());
            register.setUrl(url);
            register.setCode(code);
            register.setSource(source);
            register.setCreated(timeStamp);
            register.setNameFinnish(finnishName);
            register.setNameSwedish(swedishName);
            register.setNameEnglish(englishName);
            register.setVersion(version);
            register.setStatus(status.toString());
            register.setType(type.toString());
        }

        return register;

    }
}
