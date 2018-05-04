package fi.vm.yti.codelist.intake.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonView;

import fi.vm.yti.codelist.common.dto.Views;
import io.swagger.annotations.ApiModel;
import static fi.vm.yti.codelist.common.constants.ApiConstants.LANGUAGE_CODE_EN;

/**
 * Object model that represents a single code.
 */
@Entity
@JsonFilter("code")
@Table(name = "code")
@XmlRootElement
@XmlType(propOrder = {"id", "codeValue", "uri", "url", "status", "hierarchyLevel", "startDate", "endDate", "prefLabel", "description", "definition", "codeScheme", "shortName", "externalReferences", "broaderCodeId",  "order", "conceptUriInVocabularies"})
@ApiModel(value = "Code", description = "Code model that represents data for one single generic registeritem.")
public class Code extends AbstractHistoricalCode implements Serializable {

    private static final long serialVersionUID = 1L;

    private CodeScheme codeSheme;
    private String shortName;
    private Integer hierarchyLevel;
    private Map<String, String> prefLabel;
    private Map<String, String> description;
    private Map<String, String> definition;
    private Set<ExternalReference> externalReferences;
    private UUID broaderCodeId;
    private Integer order;
    private String conceptUriInVocabularies;

    @Column(name = "broadercode_id")
    @JsonView(Views.Normal.class)
    public UUID getBroaderCodeId() {
        return broaderCodeId;
    }

    public void setBroaderCodeId(final UUID broaderCodeId) {
        this.broaderCodeId = broaderCodeId;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codescheme_id", nullable = false, insertable = true, updatable = false)
    @JsonView(Views.ExtendedCode.class)
    public CodeScheme getCodeScheme() {
        return codeSheme;
    }

    public void setCodeScheme(final CodeScheme codeScheme) {
        this.codeSheme = codeScheme;
    }

    @Column(name = "hierarchylevel")
    @JsonView(Views.Normal.class)
    public Integer getHierarchyLevel() {
        return hierarchyLevel;
    }

    public void setHierarchyLevel(final Integer hierarchyLevel) {
        this.hierarchyLevel = hierarchyLevel;
    }

    @Column(name = "shortname")
    @JsonView(Views.Normal.class)
    public String getShortName() {
        return shortName;
    }

    public void setShortName(final String shortName) {
        this.shortName = shortName;
    }

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "code_preflabel", joinColumns = @JoinColumn(name = "code_id", referencedColumnName = "id"))
    @MapKeyColumn(name = "language")
    @Column(name = "preflabel")
    @OrderColumn
    @JsonView(Views.Normal.class)
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

    public void setPrefLabel(final String language, final String value) {
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

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "code_definition", joinColumns = @JoinColumn(name = "code_id", referencedColumnName = "id"))
    @MapKeyColumn(name = "language")
    @Column(name = "definition")
    @OrderColumn
    @JsonView(Views.Normal.class)
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
        String definitionValue = this.definition.get(language);
        if (definitionValue == null) {
            definitionValue = this.definition.get(LANGUAGE_CODE_EN);
        }
        return definitionValue;
    }

    public void setDefinition(final String language, final String value) {
        if (this.definition == null) {
            this.definition = new HashMap<>();
        }
        if (language != null && value != null && !value.isEmpty()) {
            this.definition.put(language, value);
        } else if (language != null) {
            this.definition.remove(language);
        }
        setDefinition(this.definition);
    }

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "code_description", joinColumns = @JoinColumn(name = "code_id", referencedColumnName = "id"))
    @MapKeyColumn(name = "language")
    @Column(name = "description")
    @OrderColumn
    @JsonView(Views.Normal.class)
    public Map<String, String> getDescription() {
        if (description == null) {
            description = new HashMap<>();
        }
        return description;
    }

    public void setDescription(final Map<String, String> description) {
        this.description = description;
    }

    public String getDescription(final String language) {
        String descriptionValue = this.description.get(language);
        if (descriptionValue == null) {
            descriptionValue = this.description.get(LANGUAGE_CODE_EN);
        }
        return descriptionValue;
    }

    public void setDescription(final String language, final String value) {
        if (this.description == null) {
            this.description = new HashMap<>();
        }
        if (language != null && value != null && !value.isEmpty()) {
            this.description.put(language, value);
        } else if (language != null) {
            this.description.remove(language);
        }
        setDescription(this.description);
    }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "code_externalreference",
        joinColumns = {
            @JoinColumn(name = "code_id", referencedColumnName = "id", nullable = false, updatable = false)},
        inverseJoinColumns = {
            @JoinColumn(name = "externalreference_id", referencedColumnName = "id", nullable = false, updatable = false)})
    @JsonView(Views.ExtendedCode.class)
    public Set<ExternalReference> getExternalReferences() {
        return this.externalReferences;
    }

    public void setExternalReferences(final Set<ExternalReference> externalReferences) {
        this.externalReferences = externalReferences;
    }

    @Column(name = "flatorder")
    @JsonView(Views.Normal.class)
    public Integer getOrder() {
        return order;
    }

    public void setOrder(final Integer order) {
        this.order = order;
    }

    @Column(name = "vocabularies_uri")
    @JsonView(Views.Normal.class)
    public String getConceptUriInVocabularies() {
        return conceptUriInVocabularies;
    }

    public void setConceptUriInVocabularies(final String conceptUriInVocabularies) {
        this.conceptUriInVocabularies = conceptUriInVocabularies;
    }
}
