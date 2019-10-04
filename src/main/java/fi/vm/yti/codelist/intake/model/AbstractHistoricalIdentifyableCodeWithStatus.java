package fi.vm.yti.codelist.intake.model;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractHistoricalIdentifyableCodeWithStatus extends AbstractIdentifyableTimestampedCode {

    @Convert(converter = LocalDateAttributeConverter.class)
    private LocalDate startDate;
    @Convert(converter = LocalDateAttributeConverter.class)
    private LocalDate endDate;
    private String status;

    @Column(name = "startdate")
    public LocalDate getStartDate() {
        if (startDate != null) {
            return startDate;
        }
        return null;
    }

    public void setStartDate(final LocalDate startDate) {
        if (startDate != null) {
            this.startDate = startDate;
        } else {
            this.startDate = null;
        }
    }

    @Column(name = "enddate")
    public LocalDate getEndDate() {
        if (endDate != null) {
            return endDate;
        }
        return null;
    }

    public void setEndDate(final LocalDate endDate) {
        if (endDate != null) {
            this.endDate = endDate;
        } else {
            this.endDate = null;
        }
    }

    @Column(name = "status")
    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }
}
