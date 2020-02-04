package fi.vm.yti.codelist.intake.terminology;

public final class Attribute {

    private final String lang;
    private final String value;

    // Jackson constructor
    private Attribute() {
        this("", "");
    }

    public Attribute(String lang,
                     String value) {
        this.lang = lang;
        this.value = value;
    }

    public String getLang() {
        return lang;
    }

    public String getValue() {
        return value;
    }

    public Property asProperty() {
        return new Property(lang, value);
    }
}
