package fi.vm.yti.codelist.intake.api;

import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonView;

import fi.vm.yti.codelist.common.dto.Views;
import fi.vm.yti.codelist.intake.model.Meta;

@XmlRootElement
@XmlType(propOrder = {"meta", "results"})
public class ResponseWrapper<T> {

    @JsonView(Views.Normal.class)
    private Meta meta;

    @JsonView(Views.Normal.class)
    private Set<T> results;

    public ResponseWrapper() {
    }

    public ResponseWrapper(final Meta meta) {
        this.meta = meta;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(final Meta meta) {
        this.meta = meta;
    }

    public Set<T> getResults() {
        return results;
    }

    public void setResults(final Set<T> results) {
        this.results = results;
    }
}
