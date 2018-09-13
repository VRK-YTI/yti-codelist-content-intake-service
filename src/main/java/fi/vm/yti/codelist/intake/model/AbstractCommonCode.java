package fi.vm.yti.codelist.intake.model;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractCommonCode extends AbstractBaseCode {

    private String codeValue;

    @Column(name = "codevalue")
    public String getCodeValue() {
        return codeValue;
    }

    public void setCodeValue(final String codeValue) {
        this.codeValue = codeValue;
    }
}
