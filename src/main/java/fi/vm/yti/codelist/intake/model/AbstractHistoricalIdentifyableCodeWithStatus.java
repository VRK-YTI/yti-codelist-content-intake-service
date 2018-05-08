package fi.vm.yti.codelist.intake.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;

import fi.vm.yti.codelist.common.dto.Views;
import io.swagger.annotations.ApiModelProperty;

@MappedSuperclass
public class AbstractHistoricalIdentifyableCodeWithStatus extends AbstractIdentifyableCode {

    private Date startDate;
    private Date endDate;
    private String status;

    @ApiModelProperty(dataType = "dateTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Temporal(TemporalType.DATE)
    @Column(name = "startdate")
    @JsonView(Views.Normal.class)
    public Date getStartDate() {
        if (startDate != null) {
            return new Date(startDate.getTime());
        }
        return null;
    }

    public void setStartDate(final Date startDate) {
        if (startDate != null) {
            this.startDate = new Date(startDate.getTime());
        } else {
            this.startDate = null;
        }
    }

    @ApiModelProperty(dataType = "dateTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Temporal(TemporalType.DATE)
    @Column(name = "enddate")
    @JsonView(Views.Normal.class)
    public Date getEndDate() {
        if (endDate != null) {
            return new Date(endDate.getTime());
        }
        return null;
    }

    public void setEndDate(final Date endDate) {
        if (endDate != null) {
            this.endDate = new Date(endDate.getTime());
        } else {
            this.endDate = null;
        }
    }

    @Column(name = "status")
    @JsonView(Views.Normal.class)
    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }
}
