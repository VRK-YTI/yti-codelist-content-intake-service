package fi.vm.yti.codelist.intake.resource;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;

import fi.vm.yti.codelist.common.model.Code;

@JsonFilter("dataClassification")
@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DataClassification extends Code {

    private static final long serialVersionUID = 1L;
    private Integer count;

    public DataClassification() {
    }

    public DataClassification(final Code code, final Integer count) {
        super.setId(code.getId());
        super.setStatus(code.getStatus());
        super.setCodeScheme(code.getCodeScheme());
        super.setCodeValue(code.getCodeValue());
        super.setPrefLabel(code.getPrefLabel());
        setCount(count);
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(final Integer count) {
        this.count = count;
    }
}
