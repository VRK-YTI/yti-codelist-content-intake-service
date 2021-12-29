package fi.vm.yti.codelist.intake.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.*;

@Entity
@Table(name = "organization")
public class Organization extends AbstractIdentifyableCode implements Serializable {

    private static final long serialVersionUID = 1L;

    private String url;
    private Boolean removed;
    private Map<String, String> prefLabel;
    private Map<String, String> description;
    private Set<CodeRegistry> codeRegistries;
    private Set<CodeScheme> codeSchemes;
    private Organization parent;

    @Column(name = "url")
    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    @Column(name = "removed")
    public Boolean getRemoved() {
        return removed;
    }

    public void setRemoved(final Boolean removed) {
        this.removed = removed;
    }

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "organization_preflabel", joinColumns = @JoinColumn(name = "organization_id", referencedColumnName = "id"))
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
        if (this.prefLabel != null && !this.prefLabel.isEmpty()) {
            return this.prefLabel.get(language);
        }
        return null;
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

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "organization_description", joinColumns = @JoinColumn(name = "organization_id", referencedColumnName = "id"))
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
    @JoinTable(name = "coderegistry_organization",
        joinColumns = {
            @JoinColumn(name = "organization_id", referencedColumnName = "id", nullable = false, updatable = false) },
        inverseJoinColumns = {
            @JoinColumn(name = "coderegistry_id", referencedColumnName = "id", nullable = false, updatable = false) })
    public Set<CodeRegistry> getCodeRegistries() {
        return codeRegistries;
    }

    public void setCodeRegistries(final Set<CodeRegistry> codeRegistries) {
        this.codeRegistries = codeRegistries;
    }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "codescheme_organization",
        joinColumns = {
            @JoinColumn(name = "organization_id", referencedColumnName = "id", nullable = false, updatable = false) },
        inverseJoinColumns = {
            @JoinColumn(name = "codescheme_id", referencedColumnName = "id", nullable = false, updatable = false) })
    public Set<CodeScheme> getCodeSchemes() {
        return codeSchemes;
    }

    public void setCodeSchemes(final Set<CodeScheme> codeSchemes) {
        this.codeSchemes = codeSchemes;
    }

    @OneToOne
    @JoinColumn(name = "parent_id")
    public Organization getParent() {
        return this.parent;
    }

    public void setParent(Organization parent) {
        this.parent = parent;
    }
}
