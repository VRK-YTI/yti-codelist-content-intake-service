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
    private final String status;

    // Jackson constructor
    private Vocabulary() {
        this(randomUUID(), Collections.emptyMap(), null);
    }

    public Vocabulary(final UUID id,
                      final Map prefLabel,
                      final String status) {
        this.id = id;
        this.prefLabel = prefLabel;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public Map getPrefLabel() {
        return prefLabel;
    }

    public String getStatus() { return status; }
}
