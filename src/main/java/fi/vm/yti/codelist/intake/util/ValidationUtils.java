package fi.vm.yti.codelist.intake.util;

import java.util.regex.Pattern;

public interface ValidationUtils {

    static boolean validateStringAgainstRegexp(final String input,
                                               final String regexp) {
        final Pattern pattern = Pattern.compile(regexp);
        return pattern.matcher(input).matches();
    }

}
