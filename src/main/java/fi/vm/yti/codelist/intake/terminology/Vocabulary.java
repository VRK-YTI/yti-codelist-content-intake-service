package fi.vm.yti.codelist.intake.terminology;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Vocabulary {

    private final String uri;
    private final Map prefLabel;
    private final String status;
    private final Map description;
    private final List<String> languages;

    // Jackson constructor
    private Vocabulary() {
        this(null, Collections.emptyMap(), null, Collections.emptyMap(), Collections.emptyList());
    }

    public Vocabulary(final String uri,
                      final Map prefLabel,
                      final String status,
                      final Map description,
                      final List<String> languages) {
        this.uri = uri;
        this.prefLabel = prefLabel;
        this.status = status;
        this.description = description;
        this.languages = languages;
    }

    public String getUri() {
        return uri;
    }

    public Map getPrefLabel() {
        return prefLabel;
    }

    public String getStatus() {
        return status;
    }

    public Map getDescription() {
        return description;
    }

    public List<String> getLanguages() {
        return languages;
    }
}
