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
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonView;

import fi.vm.yti.codelist.common.dto.Views;
import io.swagger.annotations.ApiModel;
import static fi.vm.yti.codelist.common.constants.ApiConstants.LANGUAGE_CODE_EN;

@Entity
@JsonFilter("organization")
@Table(name = "organization")
@XmlType(propOrder = {"id", "url", "prefLabel", "description", "codeRegistries", "removed"})
@ApiModel(value = "Organization", description = "Organization model that represents data for one single organization.")
public class Organization extends AbstractIdentifyableCode implements Serializable {

    private static final long serialVersionUID = 1L;

    private String url;
    private Boolean removed;
    private Map<String, String> prefLabel;
    private Map<String, String> description;
    private Set<CodeRegistry> codeRegistries;

    @Column(name = "url")
    @JsonView(Views.Normal.class)
    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    @Column(name = "removed")
    @JsonView(Views.Normal.class)
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
    @CollectionTable(name = "organization_description", joinColumns = @JoinColumn(name = "organization_id", referencedColumnName = "id"))
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
    @JoinTable(name = "coderegistry_organization",
        joinColumns = {
            @JoinColumn(name = "organization_id", referencedColumnName = "id", nullable = false, updatable = false)},
        inverseJoinColumns = {
            @JoinColumn(name = "coderegistry_id", referencedColumnName = "id", nullable = false, updatable = false)})
    @JsonView(Views.ExtendedOrganization.class)
    public Set<CodeRegistry> getCodeRegistries() {
        return codeRegistries;
    }

    public void setCodeRegistries(final Set<CodeRegistry> codeRegistries) {
        this.codeRegistries = codeRegistries;
    }
}
