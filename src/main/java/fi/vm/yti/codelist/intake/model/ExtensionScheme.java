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
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
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
@JsonFilter("extensionScheme")
@Table(name = "extensionscheme")
@XmlRootElement
@XmlType(propOrder = { "id", "codeValue", "status", "startDate", "endDate", "prefLabel", "propertyType", "parentCodeScheme", "codeSchemes", "extensions" })
@ApiModel(value = "ExtensionScheme", description = "ExtensionScheme model that represents data for one extension scheme element.")
public class ExtensionScheme extends AbstractHistoricalIdentifyableCodeWithStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, String> prefLabel;
    private PropertyType propertyType;
    private CodeScheme parentCodeScheme;
    private Set<CodeScheme> codeSchemes;
    private Set<Extension> extensions;
    private String codeValue;

    @Column(name = "codevalue")
    @JsonView(Views.Normal.class)
    public String getCodeValue() {
        return codeValue;
    }

    public void setCodeValue(final String codeValue) {
        this.codeValue = codeValue;
    }

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "extensionscheme_preflabel", joinColumns = @JoinColumn(name = "extensionscheme_id", referencedColumnName = "id"))
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

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "extensionscheme_codescheme",
        joinColumns = {
            @JoinColumn(name = "extensionscheme_id", referencedColumnName = "id", nullable = false, updatable = false) },
        inverseJoinColumns = {
            @JoinColumn(name = "codescheme_id", referencedColumnName = "id", nullable = false, updatable = false) })
    @JsonView(Views.ExtendedExtensionScheme.class)
    public Set<CodeScheme> getCodeSchemes() {
        return codeSchemes;
    }

    public void setCodeSchemes(final Set<CodeScheme> codeSchemes) {
        this.codeSchemes = codeSchemes;
    }

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinColumn(name = "propertytype_id", nullable = false, insertable = true, updatable = false)
    @JsonView(Views.Normal.class)
    public PropertyType getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(final PropertyType propertyType) {
        this.propertyType = propertyType;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "extensionScheme", cascade = CascadeType.ALL)
    @JsonView(Views.ExtendedExtensionScheme.class)
    public Set<Extension> getExtensions() {
        return extensions;
    }

    public void setExtensions(final Set<Extension> extensions) {
        this.extensions = extensions;
    }

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "parentcodescheme_id", nullable = false, insertable = true, updatable = false)
    @JsonView(Views.ExtendedExtensionScheme.class)
    public CodeScheme getParentCodeScheme() {
        return parentCodeScheme;
    }

    public void setParentCodeScheme(final CodeScheme parentCodeScheme) {
        this.parentCodeScheme = parentCodeScheme;
    }
}
