package fi.vm.yti.codelist.intake.terminology;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Concept {

    private final String uri;
    private final Map prefLabel;
    private final Map description;
    private final String status;
    private final String containerUri; // this is the vocabulary
    private final Date modified;

    // Jackson constructor
    private Concept() {
        this("", Collections.emptyMap(), Collections.emptyMap(), "", "", null);
    }

    public Concept(final String uri,
                   final Map prefLabel,
                   final Map description,
                   final String status,
                   final String containerUri,
                   final Date modified) {
        this.uri = uri;
        this.prefLabel = prefLabel;
        this.description = description;
        this.status = status;
        this.containerUri = containerUri;
        this.modified = modified;
    }

    public Concept(final ConceptSuggestionResponse conceptSuggestionResponse) {
        this.uri = conceptSuggestionResponse.getUri();
        this.prefLabel = getPrefLabelFromSuggestion(conceptSuggestionResponse.getPrefLabel());
        this.description = getDefinitionFromSuggestion(conceptSuggestionResponse.getDefinition());
        this.status = "SUGGESTED";
        this.containerUri = conceptSuggestionResponse.getTerminologyUri();
        this.modified = conceptSuggestionResponse.getCreated();
    }

    private Map getPrefLabelFromSuggestion(final Attribute attribute) {
        final Map<String, String> thePrefLabel = new HashMap<>();
        thePrefLabel.put(attribute.getLang(), attribute.getValue());
        return thePrefLabel;
    }

    private Map getDefinitionFromSuggestion(final Attribute attribute) {
        final Map<String, String> theDescription = new HashMap<>();
        theDescription.put(attribute.getLang(), attribute.getValue());
        return theDescription;
    }

    public String getUri() {
        return uri;
    }

    public Map getPrefLabel() {
        return prefLabel;
    }

    public Map getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getContainerUri() {
        return containerUri;
    }

    public Date getModified() {
        return modified;
    }
}

