package fi.vm.yti.codelist.intake.terminology;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import static java.util.UUID.randomUUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Concept {

    private final UUID id;
    private final UUID vocabularyId;
    private final Map prefLabel;
    private final Map definition;
    private final Map vocabularyPrefLabel;
    private final String uri;
    private final String status;

    // Jackson constructor
    private Concept() {
        this(randomUUID(), randomUUID(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), "", "");
    }

    public Concept(final UUID id,
                   final UUID vocabularyId,
                   final Map prefLabel,
                   final Map definition,
                   final Map vocabularyPrefLabel,
                   final String uri,
                   final String status) {
        this.id = id;
        this.vocabularyId = vocabularyId;
        this.prefLabel = prefLabel;
        this.definition = definition;
        this.vocabularyPrefLabel = vocabularyPrefLabel;
        this.uri = uri;
        this.status = status;
    }

    public Concept(final ConceptSuggestion conceptSuggestion) {
        this.id = null;
        this.vocabularyId = conceptSuggestion.getVocabulary();
        this.prefLabel = getPrefLabelFromSuggestion(conceptSuggestion.getPrefLabel());
        this.definition = getDefinitionFromSuggestion(conceptSuggestion.getDefinition());
        this.vocabularyPrefLabel = null;
        this.uri = conceptSuggestion.getUri();
        this.status = "SUGGESTED";
    }

    private Map getPrefLabelFromSuggestion(final Attribute attribute) {
        final Map<String, String> prefLabel = new HashMap<>();
        prefLabel.put(attribute.getLang(), attribute.getValue());
        return prefLabel;
    }

    private Map getDefinitionFromSuggestion(final Attribute attribute) {
        final Map<String, String> definition = new HashMap<>();
        definition.put(attribute.getLang(), attribute.getValue());
        return definition;
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

    public Map getVocabularyPrefLabel() {
        return vocabularyPrefLabel;
    }

    public String getUri() {
        return uri;
    }

    public String getStatus() {
        return status;
    }
}

