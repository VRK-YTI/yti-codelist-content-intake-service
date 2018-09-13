package fi.vm.yti.codelist.intake.dto;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Id;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.Views;

@JsonFilter("dataClassification")
@XmlRootElement
@XmlType(propOrder = { "id", "codeValue", "status", "prefLabel", "count" })
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DataClassificationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String codeValue;
    private String status;
    private Integer count;
    private Map<String, String> prefLabel;

    public DataClassificationDTO() {
    }

    public DataClassificationDTO(final CodeDTO code,
                                 final Integer count) {
        setId(code.getId());
        setStatus(code.getStatus());
        setCodeValue(code.getCodeValue());
        setPrefLabel(code.getPrefLabel());
        setCount(count);
    }


    @Id
    @JsonView(Views.Normal.class)
    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    @JsonView(Views.Normal.class)
    public String getCodeValue() {
        return codeValue;
    }

    public void setCodeValue(final String codeValue) {
        this.codeValue = codeValue;
    }

    @JsonView(Views.Normal.class)
    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @JsonView(Views.Normal.class)
    public Map<String, String> getPrefLabel() {
        return prefLabel;
    }

    public void setPrefLabel(final Map<String, String> prefLabel) {
        this.prefLabel = prefLabel;
    }


    public Integer getCount() {
        return count;
    }

    public void setCount(final Integer count) {
        this.count = count;
    }
}
