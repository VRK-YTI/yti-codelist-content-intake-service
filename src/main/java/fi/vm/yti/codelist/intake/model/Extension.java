package fi.vm.yti.codelist.intake.model;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonView;

import fi.vm.yti.codelist.common.dto.Views;
import io.swagger.annotations.ApiModel;

@Entity
@JsonFilter("extension")
@Table(name = "extension")
@XmlRootElement
@XmlType(propOrder = { "id", "code", "extensionValue", "extensionOrder", "extensionScheme", "extension" })
@ApiModel(value = "Extension", description = "Extension model that represents data for one extension element.")
public class Extension extends AbstractIdentifyableCode implements Serializable {

    private static final long serialVersionUID = 1L;

    private String extensionValue;
    private Integer extensionOrder;
    private Code code;
    private ExtensionScheme extensionScheme;
    private Extension extension;

    @Column(name = "extensionvalue")
    @JsonView(Views.Normal.class)
    public String getExtensionValue() {
        return extensionValue;
    }

    public void setExtensionValue(final String extensionValue) {
        this.extensionValue = extensionValue;
    }

    @Column(name = "extensionorder")
    @JsonView(Views.Normal.class)
    public Integer getExtensionOrder() {
        return extensionOrder;
    }

    public void setExtensionOrder(final Integer extensionOrder) {
        this.extensionOrder = extensionOrder;
    }

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JsonView(Views.ExtendedExtension.class)
    @JoinColumn(name = "code_id", nullable = false, insertable = true, updatable = false)
    public Code getCode() {
        return code;
    }

    public void setCode(final Code code) {
        this.code = code;
    }

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "extensionscheme_id", nullable = false, insertable = true, updatable = false)
    @JsonView(Views.ExtendedExtension.class)
    public ExtensionScheme getExtensionScheme() {
        return extensionScheme;
    }

    public void setExtensionScheme(final ExtensionScheme extensionScheme) {
        this.extensionScheme = extensionScheme;
    }

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "extension_id", nullable = false, insertable = true, updatable = false)
    @JsonView(Views.ExtendedExtension.class)
    public Extension getExtension() {
        return extension;
    }

    public void setExtension(final Extension extension) {
        this.extension = extension;
    }
}