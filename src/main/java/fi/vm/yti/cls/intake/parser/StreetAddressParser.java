package fi.vm.yti.cls.intake.parser;

import fi.vm.yti.cls.common.model.Municipality;
import fi.vm.yti.cls.common.model.PostalCode;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.common.model.StreetAddress;
import fi.vm.yti.cls.common.model.StreetNumber;
import fi.vm.yti.cls.intake.api.ApiConstants;
import fi.vm.yti.cls.intake.api.ApiUtils;
import fi.vm.yti.cls.intake.util.FileUtils;
import fi.vm.yti.cls.intake.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;


/**
 * Class that handles parsing of street addresses from source data.
 */
@Service
public class StreetAddressParser {

    private static final Logger LOG = LoggerFactory.getLogger(StreetAddressParser.class);

    private final ApiUtils m_apiUtils;

    private final ParserUtils m_parserUtils;


    @Inject
    public StreetAddressParser(final ApiUtils apiUtils,
                               final ParserUtils parserUtils) {

        m_apiUtils = apiUtils;

        m_parserUtils = parserUtils;

    }


    /**
     * Parses the .DAT Street Address -file and returns the StreetAddresses as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The StreetAddress file input stream.
     * @return List of StreetAddress objects.
     */
    public List<StreetAddress> parseStreetAddressesFromInputStream(final String source,
                                                                   final InputStream inputStream) {

        final Map<String, StreetAddress> streetAddressesMap = new HashMap<>();

        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1)) {
            final BufferedReader in = new BufferedReader(inputStreamReader);
            FileUtils.skipBom(in);

            String line = null;

            final Map<String, StreetAddress> existingStreetAddressesMap = m_parserUtils.getStreetAddressesMap();

            final Map<String, Municipality> municipalitiesMap = m_parserUtils.getMunicipalitiesMap();

            final SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("yyyyMMdd");
            simpleDateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

            while ((line = in.readLine()) != null) {
                final String municipalityCode = Utils.ensureMunicipalityIdPadding(line.substring(213, 216).trim());
                Municipality municipality = null;
                if (municipalityCode != null && !municipalityCode.isEmpty()) {
                    municipality = municipalitiesMap.get(municipalityCode);
                }
                final String finnishName = line.substring(102, 132).trim();
                final String swedishName = line.substring(132, 162).trim();
                final Status status = Status.VALID;

                final StreetAddress existingStreetAddress = existingStreetAddressesMap.get(municipalityCode + finnishName);

                final Date timeStamp = new Date(System.currentTimeMillis());

                // Update
                if (existingStreetAddress != null) {

                    boolean hasChanges = false;
                    final String url = m_apiUtils.createResourceUrl(ApiConstants.API_PATH_STREETADDRESSES, existingStreetAddress.getId());

                    if (!Objects.equals(existingStreetAddress.getStatus(), status.toString())) {
                        existingStreetAddress.setStatus(status.toString());
                        hasChanges = true;
                    }
                    if (!Objects.equals(existingStreetAddress.getUri(), url)) {
                        existingStreetAddress.setUri(url);
                        hasChanges = true;
                    }
                    if (!Objects.equals(existingStreetAddress.getSource(), source)) {
                        existingStreetAddress.setSource(source);
                        hasChanges = true;
                    }
                    if (!Objects.equals(existingStreetAddress.getPrefLabelFi(), finnishName)) {
                        existingStreetAddress.setPrefLabelFi(finnishName);
                        hasChanges = true;
                    }
                    if (!Objects.equals(existingStreetAddress.getPrefLabelSe(), swedishName)) {
                        existingStreetAddress.setPrefLabelSe(swedishName);
                        hasChanges = true;
                    }
                    if (existingStreetAddress.getMunicipality() != municipality) {
                        existingStreetAddress.setMunicipality(municipality);
                        hasChanges = true;
                    }

                    if (hasChanges) {
                        existingStreetAddress.setModified(timeStamp);
                        streetAddressesMap.put(municipality.getCodeValue() + finnishName, existingStreetAddress);
                    }

                // Create
                } else {
                    if (!streetAddressesMap.containsKey(municipality.getCodeValue() + finnishName)) {
                        final StreetAddress streetAddress = new StreetAddress();
                        streetAddress.setId(UUID.randomUUID().toString());
                        final String url = m_apiUtils.createResourceUrl(ApiConstants.API_PATH_STREETADDRESSES, streetAddress.getId());
                        streetAddress.setUri(url);
                        streetAddress.setStatus(status.toString());
                        streetAddress.setSource(source);
                        streetAddress.setCreated(timeStamp);
                        streetAddress.setPrefLabelFi(finnishName);
                        streetAddress.setPrefLabelSe(swedishName);
                        streetAddress.setMunicipality(municipality);
                        streetAddressesMap.put(municipality.getCodeValue() + finnishName, streetAddress);
                    }
                }
            }

        } catch (IOException e) {
            LOG.error("Parsing streetaddresses failed: " + e.getMessage());
        }

        final List<StreetAddress> streetAddresses = new ArrayList<StreetAddress>(streetAddressesMap.values());
        return streetAddresses;

    }


    /**
     * Parses the .DAT Street Address -file and returns the StreetNumbers as an arrayList.
     *
     * @param source source identifier for the data.
     * @param inputStream The StreetNumber file input stream.
     * @return List of StreetNumber objects.
     */
    public List<StreetNumber> parseStreetNumbersFromInputStream(final String source,
                                                                final InputStream inputStream) {

        final List<StreetNumber> streetNumbersList = new ArrayList<>();

        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1)) {
            final BufferedReader in = new BufferedReader(inputStreamReader);
            FileUtils.skipBom(in);

            String line = null;

            final Map<String, StreetAddress> existingStreetAddressesMap = m_parserUtils.getStreetAddressesMap();

            final Map<String, PostalCode> postalCodeMap = m_parserUtils.getPostalCodesMap();

            final SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("yyyyMMdd");
            simpleDateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

            while ((line = in.readLine()) != null) {
                final String municipalityCode = Utils.ensureMunicipalityIdPadding(line.substring(213, 216).trim());
                final String finnishName = line.substring(102, 132).trim();
                final Status status = Status.VALID;

                final StreetAddress existingStreetAddress = existingStreetAddressesMap.get(municipalityCode+finnishName);

                final String postalCodeCode = Utils.ensurePostalCodeIdPadding(line.substring(13, 18).trim());
                PostalCode postalCode = null;
                if (postalCodeCode != null && !postalCodeCode.isEmpty()) {
                    postalCode = postalCodeMap.get(postalCodeCode);
                }
                final Date timeStamp = new Date(System.currentTimeMillis());

                Boolean isEven = null;
                final Integer type = Integer.parseInt(line.substring(186, 187));
                if (type == 1) {
                    isEven = false;
                } else if (type == 2) {
                    isEven = true;
                }
                final String startNumberString = line.substring(187, 192).trim();
                Integer startNumber = null;
                if (!startNumberString.isEmpty()) {
                    startNumber = Integer.parseInt(startNumberString);
                }
                final String startCharacter = line.substring(192, 193).trim();

                final String startNumberEndstring = line.substring(194, 199).trim();
                Integer startNumberEnd = null;
                if (!startNumberEndstring.isEmpty()) {
                    startNumberEnd = Integer.parseInt(startNumberEndstring);
                }
                final String startCharacterEnd = line.substring(199, 200).trim();

                final String endNumberString = line.substring(200, 205).trim();
                Integer endNumber = null;
                if (!endNumberString.isEmpty()) {
                    endNumber = Integer.parseInt(endNumberString);
                }
                final String endCharacter = line.substring(205, 206).trim();

                final String endNumberEndString = line.substring(207, 212).trim();
                Integer endNumberEnd = null;
                if (!endNumberEndString.isEmpty()) {
                    endNumberEnd = Integer.parseInt(endNumberEndString);
                }
                final String endCharacterEnd = line.substring(212, 213).trim();

                StreetNumber streetNumber = null;
                if (isEven != null) {
                    streetNumber = new StreetNumber();
                    streetNumber.setId(UUID.randomUUID().toString());
                    final String url = m_apiUtils.createResourceUrl(ApiConstants.API_PATH_STREETADDRESSES + "/streetnumber", streetNumber.getId());
                    streetNumber.setStatus(status.toString());
                    streetNumber.setUri(url);
                    streetNumber.setCreated(timeStamp);
                    streetNumber.setSource(source);
                    streetNumber.setPostalCode(postalCode);
                    streetNumber.setIsEven(isEven);
                    streetNumber.setStartNumber(startNumber);
                    streetNumber.setStartCharacter(startCharacter);
                    streetNumber.setStartNumberEnd(startNumberEnd);
                    streetNumber.setStartCharacterEnd(startCharacterEnd);
                    streetNumber.setEndNumber(endNumber);
                    streetNumber.setEndCharacter(endCharacter);
                    streetNumber.setEndNumberEnd(endNumberEnd);
                    streetNumber.setEndCharacterEnd(endCharacterEnd);
                }

                if (existingStreetAddress != null && streetNumber != null) {
                    streetNumber.setStreetAddress(existingStreetAddress);
                    streetNumbersList.add(streetNumber);

                } else if (streetNumber != null) {
                    LOG.error("No street address found for streetnumber: " + streetNumber);
                }
            }

        } catch (IOException e) {
            LOG.error("Parsing streetnumbers failed: " + e.getMessage());
        }

        return streetNumbersList;

    }

}
