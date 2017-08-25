package fi.vm.yti.cls.intake.parser;

import fi.vm.yti.cls.common.model.PostManagementDistrict;
import fi.vm.yti.cls.common.model.PostalCode;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.intake.api.ApiConstants;
import fi.vm.yti.cls.intake.api.ApiUtils;
import fi.vm.yti.cls.intake.jpa.PostManagementDistrictRepository;
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
 * Class that handles parsing of postmanagementdistricts from source data.
 */
@Service
public class PostManagementDistrictParser {

    private static final Logger LOG = LoggerFactory.getLogger(PostManagementDistrictParser.class);

    private final ApiUtils m_apiUtils;

    private final ParserUtils m_parserUtils;

    private final PostManagementDistrictRepository m_postManagementDistrictRepository;


    @Inject
    public PostManagementDistrictParser(final ApiUtils apiUtils,
                                        final ParserUtils parserUtils,
                                        final PostManagementDistrictRepository postManagementDistrictRepository) {

        m_apiUtils = apiUtils;

        m_parserUtils = parserUtils;

        m_postManagementDistrictRepository = postManagementDistrictRepository;

    }


    /**
     * Parses the .csv source and returns the PostManagementDistricts as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The PostManagementDistrict -file input stream.
     * @return List of PostManagementDistrict objects.
     */
    public List<PostManagementDistrict> parsePostManagementDistrictsFromClsInputStream(final String source,
                                                                                       final InputStream inputStream) {

        final Map<String, PostManagementDistrict> postManagementDistrictMap = new HashMap<>();

        final Map<String, PostManagementDistrict> existingPostManagementDistrictsMap = m_parserUtils.getPostManagementDistrictsMap();

        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                final BufferedReader in = new BufferedReader(inputStreamReader);
                final CSVParser csvParser = new CSVParser(in, CSVFormat.newFormat(',').withHeader())) {

            FileUtils.skipBom(in);

            final List<CSVRecord> records = csvParser.getRecords();

            records.forEach(record -> {

                final String code = Utils.ensurePostManagementDistrictIdPadding(record.get("CODE"));

                if (!postManagementDistrictMap.containsKey(code)) {
                    final String finnishName = record.get("NAME_FI");
                    final String swedishName = record.get("NAME_SE");
                    final String englishName = record.get("NAME_EN");
                    final Status status = Status.valueOf(record.get("STATUS"));

                    final PostManagementDistrict postManagementDistrict = createOrUpdatePostManagementDistrict(existingPostManagementDistrictsMap, code, status, source, finnishName, swedishName, englishName);
                    postManagementDistrictMap.put(code, postManagementDistrict);
                }
            });

        } catch (IOException e) {
            LOG.error("Parsing postalmanagementdistricts failed: " + e.getMessage());
        }

        final List<PostManagementDistrict> postManagementDistricts = new ArrayList<PostManagementDistrict>(postManagementDistrictMap.values());
        return postManagementDistricts;

    }


    /**
     * Parses the .dat File and returns the PostManagementDistricts as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The PostManagementDistrict -file input stream.
     * @return List of PostManagementDistrict objects.
     */
    public List<PostManagementDistrict> parsePostManagementDistrictsFromInputStream(final String source,
                                                                                    final InputStream inputStream) {

        final Map<String, PostManagementDistrict> postManagementDistrictMap = new HashMap<>();

        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1)) {
            final BufferedReader in = new BufferedReader(inputStreamReader);
            FileUtils.skipBom(in);

            String line = null;
            boolean skipFirstLine = true;

            final Map<String, PostManagementDistrict> existingPostManagementDistrictsMap = m_parserUtils.getPostManagementDistrictsMap();

            while ((line = in.readLine()) != null) {
                if (skipFirstLine) {
                    skipFirstLine = false;
                } else {

                    final String code = Utils.ensurePostManagementDistrictIdPadding(line.substring(111, 116).trim());

                    if (!postManagementDistrictMap.containsKey(code)) {
                        final String finnishName = line.substring(116, 146).trim();
                        final String swedishName = line.substring(146, 176).trim();

                        final PostManagementDistrict postManagementDistrict = createOrUpdatePostManagementDistrict(existingPostManagementDistrictsMap, code, Status.VALID, source, finnishName, swedishName, null);
                        postManagementDistrictMap.put(code, postManagementDistrict);
                    }
                }
            }

        } catch (IOException e) {
            LOG.error("Parsing postalmanagementdistricts failed: " + e.getMessage());
        }

        final List<PostManagementDistrict> postManagementDistricts = new ArrayList<PostManagementDistrict>(postManagementDistrictMap.values());
        return postManagementDistricts;

    }


    private PostManagementDistrict createOrUpdatePostManagementDistrict(final Map<String, PostManagementDistrict> existingPostManagementDistrictsMap,
                                                                        final String code,
                                                                        final Status status,
                                                                        final String source,
                                                                        final String finnishName,
                                                                        final String swedishName,
                                                                        final String englishName) {

        PostManagementDistrict postManagementDistrict = existingPostManagementDistrictsMap.get(code);

        final String url = m_apiUtils.createResourceUrl(ApiConstants.API_PATH_POSTMANAGEMENTDISTRICTS, code);
        final Date timeStamp = new Date(System.currentTimeMillis());

        // Update
        if (postManagementDistrict != null) {
            boolean hasChanges = false;
            if (!Objects.equals(postManagementDistrict.getStatus(), status.toString())) {
                postManagementDistrict.setStatus(status.toString());
                hasChanges = true;
            }
            if (!Objects.equals(postManagementDistrict.getUrl(), url)) {
                postManagementDistrict.setUrl(url);
                hasChanges = true;
            }
            if (!Objects.equals(postManagementDistrict.getSource(), source)) {
                postManagementDistrict.setSource(source);
                hasChanges = true;
            }
            if (!Objects.equals(postManagementDistrict.getNameFinnish(), finnishName)) {
                postManagementDistrict.setNameFinnish(finnishName);
                hasChanges = true;
            }
            if (!Objects.equals(postManagementDistrict.getNameSwedish(), swedishName)) {
                postManagementDistrict.setNameSwedish(swedishName);
                hasChanges = true;
            }
            if (!Objects.equals(postManagementDistrict.getNameEnglish(), englishName)) {
                postManagementDistrict.setNameEnglish(englishName);
                hasChanges = true;
            }
            if (hasChanges) {
                postManagementDistrict.setModified(timeStamp);
            }

        // Create
        } else {
            postManagementDistrict = new PostManagementDistrict();
            postManagementDistrict.setId(UUID.randomUUID().toString());
            postManagementDistrict.setStatus(status.toString());
            postManagementDistrict.setUrl(url);
            postManagementDistrict.setSource(source);
            postManagementDistrict.setCreated(timeStamp);
            postManagementDistrict.setCode(code);
            postManagementDistrict.setNameFinnish(finnishName);
            postManagementDistrict.setNameSwedish(swedishName);
            postManagementDistrict.setNameEnglish(englishName);
        }

        return postManagementDistrict;

    }


    /**
     * Parses the .dat File and returns the PostManagementDistricts as an arrayList.
     *
     * @param inputStream The PostManagementDistrict -file input stream.
     * @return List of PostManagementDistrict objects.
     */
    public void ensurePostalCodePostManagementDistrictRelations(final InputStream inputStream) {

        final Map<String, PostalCode> existingPostalCodesMap = m_parserUtils.getPostalCodesMap();

        final Map<String, PostManagementDistrict> existingPostManagementDistrictsMap = m_parserUtils.getPostManagementDistrictsMap();

        // PostManagementDistrict and it's list of PostalCodes
        final Map<String, List<PostalCode>> map = new HashMap<>();

        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1)) {
            final BufferedReader in = new BufferedReader(inputStreamReader);
            FileUtils.skipBom(in);

            String line = null;
            while ((line = in.readLine()) != null) {
                final String postalCodeCode = Utils.ensurePostalCodeIdPadding(line.substring(13, 18).trim());

                final String postManagementDistrictCode = Utils.ensurePostManagementDistrictIdPadding(line.substring(111, 116).trim());

                if (map.containsKey(postManagementDistrictCode)) {
                    final PostalCode postalCode = existingPostalCodesMap.get(postalCodeCode);
                    map.get(postManagementDistrictCode).add(postalCode);
                } else {
                    final List<PostalCode> postalCodes = new ArrayList<>();
                    final PostalCode postalCode = existingPostalCodesMap.get(postalCodeCode);
                    postalCodes.add(postalCode);
                    map.put(postManagementDistrictCode, postalCodes);
                }
            }

        } catch (IOException e) {
            LOG.error("Creating relations between postlcodes and postalmanagementdistricts failed: " + e.getMessage());
        }

        map.keySet().forEach(postManageMentDistrictCode -> {
            final PostManagementDistrict district = existingPostManagementDistrictsMap.get(postManageMentDistrictCode);
            district.setPostalCodes(map.get(postManageMentDistrictCode));
            m_postManagementDistrictRepository.save(district);
        });

    }

}
