package fi.vm.yti.codelist.intake.util;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.lang.StringUtils;

public abstract class Utils {

    private static final int REGION_ID_LENGTH = 2;
    private static final int ELECTORALDISTRICT_ID_LENGTH = 2;
    private static final int MUNICIPALITY_ID_LENGTH = 3;
    private static final int MAGISTRATE_ID_LENGTH = 3;
    private static final int MAGISTRATESERVICEUNIT_ID_LENGTH = 3;
    private static final int HEALTHCAREDISTRICT_ID_LENGTH = 2;
    private static final int POSTALCODE_ID_LENGTH = 5;
    private static final int POSTMANAGEMENTDISTRICT_ID_LENGTH = 5;
    private static final int BUSINESSERVICESUBREGION_ID_LENGTH = 5;

    /**
     * String charset conversion from ISO-8859-1 to UTF-8.
     *
     * @param source String in ISO-8859-1 format.
     * @return String in UTF-8 encoding.
     */
    public static String convertStringToUtf8(final String source) {
        final String utf8String;
        final byte[] isoBytes = source.getBytes(StandardCharsets.ISO_8859_1);
        utf8String = new String(isoBytes, StandardCharsets.UTF_8);
        return utf8String;
    }

    /**
     * String charset conversion from UTF-8 to ISO-8859-1.
     *
     * @param source String in UTF-8 format.
     * @return String in ISO-8859-1 encoding.
     */
    public static String convertStringToIso88591(final String source) {
        final String isoString;
        final byte[] utf8Bytes = source.getBytes(StandardCharsets.UTF_8);
        isoString = new String(utf8Bytes, StandardCharsets.ISO_8859_1);
        return isoString;
    }

    /**
     * Trims leading zeroes from a String.
     *
     * @param source String that might contain leading zeroes.
     * @return String without leading zeroes.
     */
    public static String trimLeadingZeroes(final String source) {
        return source.replaceFirst("^0+(?!$)", "");
    }

    /**
     * Utility that returns yesterdays date in ISO 8601 yyyy-MM-dd format.
     *
     * @return String formatted date for yesterday in ISO 8601 yyyy-MM-dd format.
     */
    public static String yesterdayInIso8601() {
        final SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return simpleDateFormatter.format(cal.getTime());
    }

    /**
     * Utility that returns todays date in ISO 8601 yyyy-MM-dd format.
     *
     * @return String formatted date for yesterday in ISO 8601 yyyy-MM-dd format.
     */
    public static String todayInIso8601() {
        final SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        final Calendar cal = Calendar.getInstance();
        return simpleDateFormatter.format(cal.getTime());
    }

    public static String ensureRegionIdPadding(final String id) {
        return StringUtils.leftPad(id, REGION_ID_LENGTH, '0');
    }

    public static String ensureElectoralDistrictIdPadding(final String id) {
        return StringUtils.leftPad(id, ELECTORALDISTRICT_ID_LENGTH, '0');
    }

    public static String ensureBusinessServiceSubRegionIdPadding(final String id) {
        return StringUtils.leftPad(id, BUSINESSERVICESUBREGION_ID_LENGTH, '0');
    }

    public static String ensurePostalCodeIdPadding(final String id) {
        return StringUtils.leftPad(id, POSTALCODE_ID_LENGTH, '0');
    }

    public static String ensurePostManagementDistrictIdPadding(final String id) {
        return StringUtils.leftPad(id, POSTMANAGEMENTDISTRICT_ID_LENGTH, '0');
    }

    public static String ensureMagistrateServiceUnitIdPadding(final String id) {
        return StringUtils.leftPad(id, MAGISTRATESERVICEUNIT_ID_LENGTH, '0');
    }

    public static String ensureMunicipalityIdPadding(final String id) {
        return StringUtils.leftPad(id, MUNICIPALITY_ID_LENGTH, '0');
    }

    public static String ensureMagistrateIdPadding(final String id) {
        return StringUtils.leftPad(id, MAGISTRATE_ID_LENGTH, '0');
    }

    public static String ensureHealthCareDistrictIdPadding(final String id) {
        return StringUtils.leftPad(id, HEALTHCAREDISTRICT_ID_LENGTH, '0');
    }

}
