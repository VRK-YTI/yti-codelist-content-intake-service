package fi.vm.yti.codelist.intake.terminology;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import static java.util.UUID.randomUUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Vocabulary {

    private final UUID id;
    private final Map prefLabel;

    // Jackson constructor
    private Vocabulary() {
        this(randomUUID(), Collections.emptyMap());
    }

    public Vocabulary(final UUID id,
                      final Map prefLabel) {
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
