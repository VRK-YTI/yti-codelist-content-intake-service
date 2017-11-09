package fi.vm.yti.codelist.intake.parser;

import java.util.UUID;

public abstract class AbstractBaseParser {

    public static final String YTI_REGISTRY = "yti";
    public static final String YTI_SERVICE_CLASSIFICATION_CODESCHEME = "serviceclassifications";

    public String resolveLanguageFromHeader(final String prefix,
                                            final String header) {
        return header.substring(header.indexOf(prefix) + prefix.length()).toLowerCase();
    }

    public UUID parseUUIDFromString(final String uuidString) {
        final UUID uuid;
        if (uuidString == null || uuidString.isEmpty()) {
            uuid = null;
        } else {
            uuid = UUID.fromString(uuidString);
        }
        return uuid;
    }

}
