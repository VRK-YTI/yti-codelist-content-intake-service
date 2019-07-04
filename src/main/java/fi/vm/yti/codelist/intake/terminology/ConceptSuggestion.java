package fi.vm.yti.codelist.intake.terminology;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ConceptSuggestion {

    private Attribute prefLabel;
    private Attribute definition;
    private UUID creator;
    private UUID vocabulary;
    private String uri;

    // Jackson constructor
    private ConceptSuggestion() {
        this(null, null, null, null, "");
    }

    public ConceptSuggestion(final Attribute prefLabel,
                             final UUID vocabulary) {
        this.prefLabel = prefLabel;
        this.definition = null;
        this.creator = null;
        this.vocabulary = vocabulary;
        this.uri = null;
    }

    public ConceptSuggestion(final Attribute prefLabel,
                             final Attribute definition,
                             final UUID vocabulary) {
        this.prefLabel = prefLabel;
        this.definition = definition;
        this.creator = null;
        this.vocabulary = vocabulary;
        this.uri = null;
    }

    public ConceptSuggestion(final Attribute prefLabel,
                             final Attribute definition,
                             final UUID creator,
                             final UUID vocabulary,
                             final String uri) {
        this.prefLabel = prefLabel;
        this.definition = definition;
        this.creator = creator;
        this.vocabulary = vocabulary;
        this.uri = uri;
    }

    public Attribute getPrefLabel() {
        return prefLabel;
    }

    public void setPrefLabel(final Attribute prefLabel) {
        this.prefLabel = prefLabel;
    }

    public Attribute getDefinition() {
        return definition;
    }

    public void setDefinition(final Attribute definition) {
        this.definition = definition;
    }

    public UUID getCreator() {
        return creator;
    }

    public void setCreator(final UUID creator) {
        this.creator = creator;
    }

    public UUID getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(final UUID vocabulary) {
        this.vocabulary = vocabulary;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }
}