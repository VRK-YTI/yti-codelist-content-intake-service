package fi.vm.yti.codelist.intake.dto;

import java.util.Set;

import fi.vm.yti.codelist.common.dto.Meta;
import fi.vm.yti.codelist.intake.terminology.Concept;

public class ConceptsResponseDTO {

    private Meta meta;
    private Set<Concept> results;

    public ConceptsResponseDTO() {
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(final Meta meta) {
        this.meta = meta;
    }

    public Set<Concept> getResults() {
        return results;
    }

    public void setResults(final Set<Concept> results) {
        this.results = results;
    }
}
