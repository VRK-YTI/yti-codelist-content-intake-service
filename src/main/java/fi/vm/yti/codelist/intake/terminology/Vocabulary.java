package fi.vm.yti.codelist.intake.terminology;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.*;

import static java.util.UUID.randomUUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Vocabulary {

    private final UUID id;
    private final Map<String, String> prefLabel;

    // Jackson constructor
    private Vocabulary() {
        this(randomUUID(), Collections.emptyMap());
    }

    public Vocabulary(final UUID id, final Map prefLabel) {
        this.id = id;
        this.prefLabel = prefLabel;
    }

    public UUID getId() {
        return id;
    }

    public Map getPrefLabel() {
        return prefLabel;
    }
}
