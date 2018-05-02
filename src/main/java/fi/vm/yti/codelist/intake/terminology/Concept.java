package fi.vm.yti.codelist.intake.terminology;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.*;

import static java.util.UUID.randomUUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Concept {

    private final UUID id;
    private final UUID vocabularyId;
    private final Map<String, String> prefLabel;
    private final Map<String, String> definition;
    private final Map<String, String> vocabularyPrefLabel;
    private final String uri;

    // Jackson constructor
    private Concept() {
        this(randomUUID(), randomUUID(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), "");
    }

    public Concept(final UUID id, final UUID vocabularyId, final Map prefLabel, final Map definition, final Map vocabularyPrefLabel, final String uri) {
        this.id = id;
        this.vocabularyId = vocabularyId;
        this.prefLabel = prefLabel;
        this.definition = definition;
        this.vocabularyPrefLabel = vocabularyPrefLabel;
        this.uri = uri;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVocabularyId() {
        return vocabularyId;
    }

    public Map getPrefLabel() {
        return prefLabel;
    }

    public Map getDefinition() {
        return definition;
    }

    public Map<String, String> getVocabularyPrefLabel() {
        return vocabularyPrefLabel;
    }

    public String getUri() {
        return uri;
    }
}

