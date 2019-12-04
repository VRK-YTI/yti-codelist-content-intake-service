package fi.vm.yti.codelist.intake.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import static fi.vm.yti.codelist.common.constants.ApiConstants.LANGUAGE_CODE_EN;

@Entity
@Table(name = "propertytype")
public class PropertyType extends AbstractIdentifyableTimestampedCode implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uri;
    private String localName;
    private String context;
    private Map<String, String> prefLabel;
    private Map<String, String> definition;
    private Set<ValueType> valueTypes;

    @Column(name = "localname")
    public String getLocalName() {
        return localName;
    }

    public void setLocalName(final String localName) {
        this.localName = localName;
    }

    @Column(name = "context")
    public String getContext() {
        return context;
    }

    public void setContext(final String context) {
        this.context = context;
    }

    @Column(name = "uri")
    public String getUri() {
        return uri;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }

    @ElementCollection(targetClass = String.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "propertytype_preflabel", joinColumns = @JoinColumn(name = "propertytype_id", referencedColumnName = "id"))
    @MapKeyColumn(name = "language")
    @Column(name = "preflabel")
    @OrderColumn
    public Map<String, String> getPrefLabel() {
        if (prefLabel == null) {
            prefLabel = new HashMap<>();
        }
        return prefLabel;
    }

    public void setPrefLabel(final Map<String, String> prefLabel) {
        this.prefLabel = prefLabel;
    }

    public String getPrefLabel(final String language) {
        String prefLabelValue = null;
        if (this.prefLabel != null && !this.prefLabel.isEmpty()) {
            prefLabelValue = this.prefLabel.get(language);
            if (prefLabelValue == null) {
                prefLabelValue = this.prefLabel.get(LANGUAGE_CODE_EN);
            }
        }
        return prefLabelValue;
    }

    public void setPrefLabel(final String language,
                             final String value) {
        if (prefLabel == null) {
            prefLabel = new HashMap<>();
        }
        if (language != null && value != null && !value.isEmpty()) {
            prefLabel.put(language, value);
        } else if (language != null) {
            prefLabel.remove(language);
        }
        setPrefLabel(prefLabel);
    }

    @ElementCollection(targetClass = String.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "propertytype_definition", joinColumns = @JoinColumn(name = "propertytype_id", referencedColumnName = "id"))
    @MapKeyColumn(name = "language")
    @Column(name = "definition")
    @OrderColumn
    public Map<String, String> getDefinition() {
        if (definition == null) {
            definition = new HashMap<>();
        }
        return definition;
    }

    public void setDefinition(final Map<String, String> definition) {
        this.definition = definition;
    }

    public String getDefinition(final String language) {
        String definitionValue = null;
        if (this.definition != null && !this.definition.isEmpty()) {
            definitionValue = this.definition.get(language);
            if (definitionValue == null) {
                definitionValue = this.definition.get(LANGUAGE_CODE_EN);
            }
        }
        return definitionValue;
    }

    public void setDefinition(final String language,
                              final String value) {
        if (definition == null) {
            definition = new HashMap<>();
        }
        if (language != null && value != null && !value.isEmpty()) {
            definition.put(language, value);
        } else if (language != null) {
            definition.remove(language);
        }
        setDefinition(definition);
    }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "propertytype_valuetype",
        joinColumns = {
            @JoinColumn(name = "propertytype_id", referencedColumnName = "id") },
        inverseJoinColumns = {
            @JoinColumn(name = "valuetype_id", referencedColumnName = "id") })
    public Set<ValueType> getValueTypes() {
        return valueTypes;
    }

    public void setValueTypes(final Set<ValueType> valueTypes) {
        this.valueTypes = valueTypes;
    }

    public ValueType getValueTypeWithLocalName(final String localName) {
        if (valueTypes != null && !valueTypes.isEmpty()) {
            for (final ValueType valueType : valueTypes) {
                if (valueType.getLocalName().equalsIgnoreCase(localName)) {
                    return valueType;
                }
            }
        }
        return null;
    }
}
