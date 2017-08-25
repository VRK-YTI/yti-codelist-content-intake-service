package fi.vm.yti.cls.intake.parser;

import fi.vm.yti.cls.common.model.Magistrate;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.intake.api.ApiConstants;
import fi.vm.yti.cls.intake.api.ApiUtils;
import fi.vm.yti.cls.intake.jpa.MagistrateRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;


/**
 * Class that handles parsing of magistrates from source data.
 */
@Service
public class MagistrateParser {


    private static final Logger LOG = LoggerFactory.getLogger(MagistrateParser.class);

    private final MagistrateRepository m_magistrateRepository;

    private final ApiUtils m_apiUtils;


    @Inject
    private MagistrateParser(final ApiUtils apiUtils,
                             final MagistrateRepository magistrateRepository) {

        m_apiUtils = apiUtils;

        m_magistrateRepository = magistrateRepository;

    }


    /**
     * Parses the CLS spec .csv data-file and returns the magistrates as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The Municipality -file.
     * @return List of Municipality objects.
     */
    public List<Magistrate> parseMagistratesFromClsInputStream(final String source,
                                                               final InputStream inputStream) {

        final List<Magistrate> magistrates = new ArrayList<>();

        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                final BufferedReader in = new BufferedReader(inputStreamReader);
                final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withHeader())) {
            FileUtils.skipBom(in);

            final List<CSVRecord> records = csvParser.getRecords();

            records.forEach(record -> {

                final String code = Utils.ensureMagistrateIdPadding(record.get("CODE"));
                final String finnishName = record.get("NAME_FI");
                final String swedishName = record.get("NAME_SE");
                final String englishName = record.get("NAME_EN");
                final Status status = Status.valueOf(record.get("STATUS"));

                final Magistrate magistrate = createOrUpdateMagistrate(code, status, source, finnishName, swedishName, englishName);
                magistrates.add(magistrate);
            });

        } catch (IOException e) {
            LOG.error("Parsing magistrates failed. " + e.getMessage());
        }

        return magistrates;

    }


    /**
     * Parses the .csv Municipality-file and returns the magistrates as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The Municipality -file.
     * @return List of Municipality objects.
     */
    public List<Magistrate> parseMagistratesFromInputStream(final String source,
                                                            final InputStream inputStream) {

        final Map<String, Magistrate> magistratesMap = new HashMap<>();

        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1)) {
            final BufferedReader in = new BufferedReader(inputStreamReader);
            FileUtils.skipBom(in);

            String line = null;
            boolean skipFirstLine = true;

            while ((line = in.readLine()) != null) {
                if (skipFirstLine) {
                    skipFirstLine = false;
                } else {
                    final String[] parts = line.split(";");

                    final String code = Utils.ensureMagistrateIdPadding(parts[9]);
                    final String finnishName = parts[10];
                    final String swedishName = parts[11];

                    if (!magistratesMap.containsKey(code)) {
                        final Magistrate magistrate = createOrUpdateMagistrate(code, Status.VALID, source, finnishName, swedishName, null);
                        magistratesMap.put(code, magistrate);
                    }
                }
            }

        } catch (IOException e) {
            LOG.error("Parsing magistrates failed. " + e.getMessage());
        }

        final List<Magistrate> magistrates = new ArrayList<Magistrate>(magistratesMap.values());
        return magistrates;

    }


    private Magistrate createOrUpdateMagistrate(final String code,
                                                final Status status,
                                                final String source,
                                                final String finnishName,
                                                final String swedishName,
                                                final String englishName) {

        Magistrate magistrate = m_magistrateRepository.findByCode(code);

        final String url = m_apiUtils.createResourceUrl(ApiConstants.API_PATH_MAGISTRATES, code);
        final Date timeStamp = new Date(System.currentTimeMillis());

        // Update
        if (magistrate != null) {
            boolean hasChanges = false;
            if (!Objects.equals(magistrate.getStatus(), status.toString())) {
                magistrate.setStatus(status.toString());
                hasChanges = true;
            }
            if (!Objects.equals(magistrate.getUrl(), url)) {
                magistrate.setUrl(url);
                hasChanges = true;
            }
            if (!Objects.equals(magistrate.getSource(), source)) {
                magistrate.setSource(source);
                hasChanges = true;
            }
            if (!Objects.equals(magistrate.getNameFinnish(), finnishName)) {
                magistrate.setNameFinnish(finnishName);
                hasChanges = true;
            }
            if (!Objects.equals(magistrate.getNameSwedish(), swedishName)) {
                magistrate.setNameSwedish(swedishName);
                hasChanges = true;
            }
            if (!Objects.equals(magistrate.getNameEnglish(), englishName)) {
                magistrate.setNameEnglish(englishName);
                hasChanges = true;
            }
            if (hasChanges) {
                magistrate.setModified(timeStamp);
            }

        // Create
        } else {
            magistrate = new Magistrate();
            magistrate.setId(UUID.randomUUID().toString());
            magistrate.setUrl(url);
            magistrate.setStatus(status.toString());
            magistrate.setSource(source);
            magistrate.setCreated(timeStamp);
            magistrate.setCode(code);
            magistrate.setNameFinnish(finnishName);
            magistrate.setNameSwedish(swedishName);
            magistrate.setNameEnglish(englishName);
        }

        return magistrate;

    }

}
