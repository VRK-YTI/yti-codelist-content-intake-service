package fi.vm.yti.codelist.intake.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public abstract class Utils {

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

    public static String entityEncode(final String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
