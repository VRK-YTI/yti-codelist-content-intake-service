package fi.vm.yti.codelist.intake.terminology;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import static java.util.UUID.randomUUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Vocabulary {

    private final UUID id;
    private final Map prefLabel;
    private final String status;
    private final List<String> languages;

    // Jackson constructor
    private Vocabulary() {
        this(randomUUID(), Collections.emptyMap(), null, Collections.emptyList());
    }

    public Vocabulary(final UUID id,
                      final Map prefLabel,
                      final String status,
                      final List<String> languages) {
        this.id = id;
        this.prefLabel = prefLabel;
        this.status = status;
        this.languages = languages;
    }

    public UUID getId() {
        return id;
    }

    public Map getPrefLabel() {
        return prefLabel;
    }

    public String getStatus() {
        return status;
    }

    public List getLanguages() {
        return languages;
    }
}
