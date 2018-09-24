package fi.vm.yti.codelist.intake.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import static fi.vm.yti.codelist.common.constants.ApiConstants.LANGUAGE_CODE_EN;

@Entity
@Table(name = "valuetype")
public class ValueType extends AbstractIdentifyableCode implements Serializable {

    private static final long serialVersionUID = 1L;

    private String localName;
    private String valueTypeUri;
    private String typeUri;
    private Map<String, String> prefLabel;
    private String regexp;
    private boolean required;

    @Column(name = "localname")
    public String getLocalName() {
        return localName;
    }

    public void setLocalName(final String localName) {
        this.localName = localName;
    }

    @Column(name = "typeuri")
    public String getTypeUri() {
        return typeUri;
    }

    public void setTypeUri(final String typeUri) {
        this.typeUri = typeUri;
    }

    @Column(name = "valuetypeuri")
    public String getValueTypeUri() {
        return valueTypeUri;
    }

    public void setValueTypeUri(final String valueTypeUri) {
        this.valueTypeUri = valueTypeUri;
    }

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "valuetype_preflabel", joinColumns = @JoinColumn(name = "valuetype_id", referencedColumnName = "id"))
    @MapKeyColumn(name = "language")
    @Column(name = "preflabel")
    @OrderColumn
    public Map<String, String> getPrefLabel() {
        return prefLabel;
    }

    public void setPrefLabel(final Map<String, String> prefLabel) {
        this.prefLabel = prefLabel;
    }

    public String getPrefLabel(final String language) {
        String prefLabelValue = this.prefLabel.get(language);
        if (prefLabelValue == null) {
            prefLabelValue = this.prefLabel.get(LANGUAGE_CODE_EN);
        }
        return prefLabelValue;
    }

    public void setPrefLabel(final String language,
                             final String value) {
        if (this.prefLabel == null) {
            this.prefLabel = new HashMap<>();
        }
        if (language != null && value != null && !value.isEmpty()) {
            this.prefLabel.put(language, value);
        } else if (language != null) {
            this.prefLabel.remove(language);
        }
        setPrefLabel(this.prefLabel);
    }

    @Column(name = "regexp")
    public String getRegexp() {
        return regexp;
    }

    public void setRegexp(final String regexp) {
        this.regexp = regexp;
    }

    @Column(name = "required")
    public boolean getRequired() {
        return required;
    }

    public void setRequired(final boolean required) {
        this.required = required;
    }
}
