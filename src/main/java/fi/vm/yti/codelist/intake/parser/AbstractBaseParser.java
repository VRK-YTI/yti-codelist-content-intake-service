package fi.vm.yti.codelist.intake.parser;

public abstract class AbstractBaseParser {

    public String resolveLanguageFromHeader(final String prefix,
                                             final String header) {
        return header.substring(header.indexOf(prefix) + prefix.length()).toLowerCase();
    }

}
