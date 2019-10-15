package fi.vm.yti.codelist.intake.terminology;

import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConceptSuggestionResponse {

    private Attribute prefLabel;
    private Attribute definition;
    private UUID creator;
    private String terminologyUri;
    private String uri;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date created;

    // Jackson constructor
    private ConceptSuggestionResponse() {
        this(null, null, null, "", "", null);
    }

    public ConceptSuggestionResponse(final Attribute prefLabel,
                                     final String terminologyUri) {
        this.prefLabel = prefLabel;
        this.definition = null;
        this.creator = null;
        this.terminologyUri = terminologyUri;
    }

    public ConceptSuggestionResponse(final Attribute prefLabel,
                                     final Attribute definition,
                                     final String terminologyUri) {
        this.prefLabel = prefLabel;
        this.definition = definition;
        this.creator = null;
        this.terminologyUri = terminologyUri;
    }

    public ConceptSuggestionResponse(final Attribute prefLabel,
                                     final Attribute definition,
                                     final UUID creator,
                                     final String terminologyUri,
                                     final String uri,
                                     final Date created) {
        this.prefLabel = prefLabel;
        this.definition = definition;
        this.creator = creator;
        this.terminologyUri = terminologyUri;
        this.uri = uri;
        this.created = created;
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

    public String getUri() {
        return uri;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(final Date created) {
        this.created = created;
    }
}
