package fi.vm.yti.codelist.intake.model;

import java.io.Serializable;
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
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

@Entity
@Table(name = "coderegistry")
public class CodeRegistry extends AbstractCommonCode implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, String> prefLabel;
    private Map<String, String> description;
    private Set<CodeScheme> codeSchemes;
    private Set<Organization> organizations;

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "coderegistry_preflabel", joinColumns = @JoinColumn(name = "coderegistry_id", referencedColumnName = "id"))
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
        if (this.prefLabel != null && !this.prefLabel.isEmpty()) {
            return this.prefLabel.get(language);
        }
        return null;
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

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "coderegistry_description", joinColumns = @JoinColumn(name = "coderegistry_id", referencedColumnName = "id"))
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
        if (this.description != null && !this.description.isEmpty()) {
            return this.description.get(language);
        }
        return null;
    }

    public void setDescription(final String language,
                               final String value) {
        if (description == null) {
            description = new HashMap<>();
        }
        if (language != null && value != null && !value.isEmpty()) {
            description.put(language, value);
        } else if (language != null) {
            description.remove(language);
        }
        setDescription(description);
    }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "coderegistry_organization",
        joinColumns = {
            @JoinColumn(name = "coderegistry_id", referencedColumnName = "id", nullable = false, updatable = false) },
        inverseJoinColumns = {
            @JoinColumn(name = "organization_id", referencedColumnName = "id", nullable = false, updatable = false) })
    public Set<Organization> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(final Set<Organization> organizations) {
        this.organizations = organizations;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "codeRegistry", cascade = CascadeType.ALL)
    public Set<CodeScheme> getCodeSchemes() {
        return codeSchemes;
    }

    public void setCodeSchemes(final Set<CodeScheme> codeSchemes) {
        this.codeSchemes = codeSchemes;
    }
}
