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

@Entity
@JsonFilter("externalReference")
@Table(name = "externalreference")
@XmlRootElement
@XmlType(propOrder = {"id", "uri", "referenceUrl", "global", "title", "description", "parentCodeScheme"})
@ApiModel(value = "ExternalReference", description = "ExternalReference model that represents data for either CodeScheme or Code related external link.")
public class ExternalReference extends AbstractBaseCode implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, String> title;
    private Map<String, String> description;
    private Set<CodeScheme> codeSchemes;
    private Set<Code> codes;
    private PropertyType propertyType;
    private CodeScheme parentCodeScheme;
    private Boolean global;
    private String referenceUrl;

    @Column(name = "global")
    @JsonView(Views.Normal.class)
    public Boolean getGlobal() {
        return global;
    }

    public void setGlobal(final Boolean global) {
        this.global = global;
    }

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "externalreference_title", joinColumns = @JoinColumn(name = "externalreference_id", referencedColumnName = "id"))
    @MapKeyColumn(name = "language")
    @Column(name = "title")
    @OrderColumn
    @JsonView(Views.Normal.class)
    public Map<String, String> getTitle() {
        if (title == null) {
            title = new HashMap<>();
        }
        return title;
    }

    public void setTitle(final Map<String, String> title) {
        this.title = title;
    }

    public String getTitle(final String language) {
        String value = this.title.get(language);
        if (value == null) {
            value = this.title.get(LANGUAGE_CODE_EN);
        }
        return value;
    }

    public void setTitle(final String language, final String value) {
        if (title == null) {
            title = new HashMap<>();
        }
        if (language != null && value != null && !value.isEmpty()) {
            title.put(language, value);
        } else if (language != null) {
            title.remove(language);
        }
        setTitle(title);
    }

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "externalreference_description", joinColumns = @JoinColumn(name = "externalreference_id", referencedColumnName = "id"))
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
        String value = this.description.get(language);
        if (value == null) {
            value = this.description.get(LANGUAGE_CODE_EN);
        }
        return value;
    }

    public void setDescription(final String language, final String value) {
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

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "externalReferences")
    @JsonView(Views.ExtendedExternalReference.class)
    public Set<CodeScheme> getCodeSchemes() {
        return this.codeSchemes;
    }

    public void setCodeSchemes(final Set<CodeScheme> codeSchemes) {
        this.codeSchemes = codeSchemes;
    }

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "externalReferences")
    @JsonView(Views.ExtendedExternalReference.class)
    public Set<Code> getCodes() {
        return this.codes;
    }

    public void setCodes(final Set<Code> codes) {
        this.codes = codes;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "propertytype_id", nullable = false, insertable = true, updatable = true)
    @JsonView(Views.Normal.class)
    public PropertyType getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(final PropertyType propertyType) {
        this.propertyType = propertyType;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentcodescheme_id", nullable = true, insertable = true, updatable = false)
    @JsonView(Views.ExtendedExternalReference.class)
    public CodeScheme getParentCodeScheme() {
        return parentCodeScheme;
    }

    public void setParentCodeScheme(final CodeScheme codeScheme) {
        this.parentCodeScheme = codeScheme;
    }

    @Column(name = "referenceurl")
    public String getReferenceUrl() {
        return referenceUrl;
    }

    public void setReferenceUrl(final String referenceUrl) {
        this.referenceUrl = referenceUrl;
    }
}
