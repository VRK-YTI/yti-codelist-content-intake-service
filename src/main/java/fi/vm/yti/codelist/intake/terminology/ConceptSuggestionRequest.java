package fi.vm.yti.codelist.intake.terminology;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConceptSuggestionRequest {

    private Attribute prefLabel;
    private Attribute definition;
    private UUID creator;
    private String terminologyUri;

    // Jackson constructor
    private ConceptSuggestionRequest() {
        this(null, null, null, "");
    }

    public ConceptSuggestionRequest(final Attribute prefLabel,
                                    final Attribute definition,
                                    final UUID creator,
                                    final String terminologyUri) {
        this.prefLabel = prefLabel;
        this.definition = definition;
        this.creator = creator;
        this.terminologyUri = terminologyUri;
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

    public String getTerminologyUri() {
        return terminologyUri;
    }

    public void setTerminologyUri(final String terminologyUri) {
        this.terminologyUri = terminologyUri;
    }
}
