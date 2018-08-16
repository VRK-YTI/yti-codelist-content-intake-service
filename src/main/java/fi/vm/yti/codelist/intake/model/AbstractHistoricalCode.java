package fi.vm.yti.codelist.intake.model;

import java.util.Calendar;
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
public abstract class AbstractHistoricalCode extends AbstractCommonCode {

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
            return getTimeZoneProtectedDate(startDate);
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
            return getTimeZoneProtectedDate(endDate);
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

    /**
     * java.util.Date attaches time information to dates even though they are strictly date ie. 2018-12-31
     * in the db. This creates problems in ES indexing as the value will jump -2 or .-3 hours,
     * depending on is it currently summertime or not.
     * Because this backwards move happens across midnight, it  results in the wrong date in the
     * ES index (eg 2018-03-03 instead of 2018-03-04) and this hack ensures this will never happen.
     * There are plans to replace java.util.Date with the new java.time.LocalDate
     * throughout the codebase but this is to be done separately.
     */
    private Date getTimeZoneProtectedDate(final Date theDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(theDate);
        calendar.add(Calendar.HOUR_OF_DAY,
                4);
        return calendar.getTime();
    }
}
