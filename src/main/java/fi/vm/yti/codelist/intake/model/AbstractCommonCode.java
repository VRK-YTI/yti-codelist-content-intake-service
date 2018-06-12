package fi.vm.yti.codelist.intake.model;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import com.fasterxml.jackson.annotation.JsonView;

import fi.vm.yti.codelist.common.dto.Views;

@MappedSuperclass
public abstract class AbstractCommonCode extends AbstractBaseCode {

    private String codeValue;

    @Column(name = "codevalue")
    @JsonView(Views.Normal.class)
    public String getCodeValue() {
        return codeValue;
    }

    public void setCodeValue(final String codeValue) {
        this.codeValue = codeValue;
    }
}
