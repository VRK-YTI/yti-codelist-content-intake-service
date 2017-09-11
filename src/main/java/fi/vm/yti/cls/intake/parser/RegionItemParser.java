package fi.vm.yti.cls.intake.parser;

import fi.vm.yti.cls.common.model.Region;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Class that handles parsing of regions from source data.
 */
@Service
public class RegionItemParser {

    private static final Logger LOG = LoggerFactory.getLogger(RegionItemParser.class);
    private final ApiUtils apiUtils;
    private final ParserUtils parserUtils;

    @Inject
    public RegionItemParser(final ApiUtils apiUtils,
                            final ParserUtils parserUtils) {
        this.apiUtils = apiUtils;
        this.parserUtils = parserUtils;
    }

    /**
     * Parses the .csv Municipality-file and returns the regions as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The Municipality -file.
     * @return List of Municipality objects.
     */
    public List<Region> parseRegionsFromClsInputStream(final String source,
                                                       final InputStream inputStream) {
        final List<Region> regions = new ArrayList<>();
        final Map<String, Region> existingRegionsMap = parserUtils.getRegionsMap();
        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                final BufferedReader in = new BufferedReader(inputStreamReader);
                final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withHeader())) {
            FileUtils.skipBom(in);
            final List<CSVRecord> records = csvParser.getRecords();
            records.forEach(record -> {
                final String code = Utils.ensureRegionIdPadding(record.get("CODEVALUE"));
                final String finnishName = record.get("PREFLABEL_FI");
                final String swedishName = record.get("PREFLABEL_SE");
                final String englishName = record.get("PREFLABEL_EN");
                final Status status = Status.valueOf(record.get("STATUS"));
                final Region region = createOrUpdateRegion(existingRegionsMap, code, status, source, finnishName, swedishName, englishName);
                regions.add(region);
            });
        } catch (IOException e) {
            LOG.error("Parsing regions failed: " + e.getMessage());
        }
        return regions;
    }

    /**
     * Parses the .csv Municipality-file and returns the regions as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The Municipality -file.
     * @return List of Municipality objects.
     */
    public List<Region> parseRegionsFromInputStream(final String source,
                                                    final InputStream inputStream) {
        final Map<String, Region> regionMap = new HashMap<>();
        final Map<String, Region> existingRegionsMap = parserUtils.getRegionsMap();
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
                    final String code = Utils.ensureRegionIdPadding(parts[15]);
                    if (code != null && !code.isEmpty() && !regionMap.containsKey(code)) {
                        final String finnishName = parts[16];
                        final String swedishName = parts[17];
                        final Region region = createOrUpdateRegion(existingRegionsMap, code, Status.VALID, source, finnishName, swedishName, null);
                        regionMap.put(code, region);
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Parsing regions failed: " + e.getMessage());
        }
        final List<Region> regions = new ArrayList<Region>(regionMap.values());
        return regions;
    }

    private Region createOrUpdateRegion(final Map<String, Region> regionsMap,
                                        final String code,
                                        final Status status,
                                        final String source,
                                        final String finnishName,
                                        final String swedishName,
                                        final String englishName) {
        final String url = apiUtils.createResourceUrl(ApiConstants.API_PATH_REGIONS, code);
        final Date timeStamp = new Date(System.currentTimeMillis());
        Region region = regionsMap.get(code);
        // Update
        if (region != null) {
            boolean hasChanges = false;
            if (!Objects.equals(region.getStatus(), status.toString())) {
                region.setStatus(status.toString());
                hasChanges = true;
            }
            if (!Objects.equals(region.getUri(), url)) {
                region.setUri(url);
                hasChanges = true;
            }
            if (!Objects.equals(region.getSource(), source)) {
                region.setSource(source);
                hasChanges = true;
            }
            if (!Objects.equals(region.getPrefLabelFi(), finnishName)) {
                region.setPrefLabelFi(finnishName);
                hasChanges = true;
            }
            if (!Objects.equals(region.getPrefLabelSe(), swedishName)) {
                region.setPrefLabelSe(swedishName);
                hasChanges = true;
            }
            if (!Objects.equals(region.getPrefLabelEn(), englishName)) {
                region.setPrefLabelEn(englishName);
                hasChanges = true;
            }
            if (hasChanges) {
                region.setModified(timeStamp);
            }
        // Create
        } else {
            region = new Region();
            region.setId(UUID.randomUUID().toString());
            region.setStatus(status.toString());
            region.setUri(url);
            region.setCodeValue(code);
            region.setSource(source);
            region.setModified(timeStamp);
            region.setPrefLabelFi(finnishName);
            region.setPrefLabelSe(swedishName);
            region.setPrefLabelEn(englishName);
        }
        return region;
    }

}
