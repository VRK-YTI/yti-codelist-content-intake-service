package fi.vm.yti.codelist.intake.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import static fi.vm.yti.codelist.common.constants.ApiConstants.LANGUAGE_CODE_EN;

@Entity
@Table(name = "code")
public class Code extends AbstractHistoricalCode implements Serializable {

    private static final long serialVersionUID = 1L;

    private CodeScheme codeSheme;
    private String shortName;
    private Integer hierarchyLevel;
    private Map<String, String> prefLabel;
    private Map<String, String> description;
    private Map<String, String> definition;
    private Set<ExternalReference> externalReferences;
    private Set<Member> members;
    private Code broaderCode;
    private Integer order;
    private String conceptUriInVocabularies;
    private CodeScheme subCodeScheme;
    private Date statusModified;

    @Column(name = "status_modified")
    @Temporal(TemporalType.TIMESTAMP)
    public Date getStatusModified() {
        if (statusModified != null) {
            return new Date(statusModified.getTime());
        }
        return null;
    }

    public void setStatusModified(final Date statusModified) {
        if (statusModified != null) {
            this.statusModified = new Date(statusModified.getTime());
        } else {
            this.statusModified = null;
        }
    }

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinColumn(name = "codescheme_id", nullable = false, updatable = false)
    public CodeScheme getCodeScheme() {
        return codeSheme;
    }

    public void setCodeScheme(final CodeScheme codeScheme) {
        this.codeSheme = codeScheme;
    }

    @Column(name = "hierarchylevel")
    public Integer getHierarchyLevel() {
        return hierarchyLevel;
    }

    public void setHierarchyLevel(final Integer hierarchyLevel) {
        this.hierarchyLevel = hierarchyLevel;
    }

    @Column(name = "shortname")
    public String getShortName() {
        return shortName;
    }

    public void setShortName(final String shortName) {
        this.shortName = shortName;
    }

    @ElementCollection(targetClass = String.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "code_preflabel", joinColumns = @JoinColumn(name = "code_id", referencedColumnName = "id"))
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

    @ElementCollection(targetClass = String.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "code_definition", joinColumns = @JoinColumn(name = "code_id", referencedColumnName = "id"))
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

    @ElementCollection(targetClass = String.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "code_description", joinColumns = @JoinColumn(name = "code_id", referencedColumnName = "id"))
    @MapKeyColumn(name = "language")
    @Column(name = "description")
    @OrderColumn
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
        String descriptionValue = null;
        if (this.description != null && !this.description.isEmpty()) {
            descriptionValue = this.description.get(language);
            if (descriptionValue == null) {
                descriptionValue = this.description.get(LANGUAGE_CODE_EN);
            }
        }
        return descriptionValue;
    }

    public void setDescription(final String language,
                               final String value) {
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
            @JoinColumn(name = "code_id", referencedColumnName = "id", nullable = false, updatable = false) },
        inverseJoinColumns = {
            @JoinColumn(name = "externalreference_id", referencedColumnName = "id", nullable = false, updatable = false) })
    public Set<ExternalReference> getExternalReferences() {
        return this.externalReferences;
    }

    public void setExternalReferences(final Set<ExternalReference> externalReferences) {
        this.externalReferences = externalReferences;
    }

    @Column(name = "flatorder")
    public Integer getOrder() {
        return order;
    }

    public void setOrder(final Integer order) {
        this.order = order;
    }

    @Column(name = "vocabularies_uri")
    public String getConceptUriInVocabularies() {
        return conceptUriInVocabularies;
    }

    public void setConceptUriInVocabularies(final String conceptUriInVocabularies) {
        this.conceptUriInVocabularies = conceptUriInVocabularies;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "code", cascade = CascadeType.ALL)
    public Set<Member> getMembers() {
        return members;
    }

    public void setMembers(final Set<Member> members) {
        this.members = members;
    }

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinColumn(name = "broadercode_id")
    public Code getBroaderCode() {
        return broaderCode;
    }

    public void setBroaderCode(final Code broaderCode) {
        this.broaderCode = broaderCode;
    }

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinColumn(name = "subcodescheme_id")
    public CodeScheme getSubCodeScheme() {
        return subCodeScheme;
    }

    public void setSubCodeScheme(final CodeScheme subCodeScheme) {
        this.subCodeScheme = subCodeScheme;
    }
}
