package fi.vm.yti.codelist.intake.model;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "commit")
public class Commit {

    private UUID id;
    private String traceId;
    private UUID userId;
    private Date modified;
    private String description;

    public Commit() {
    }

    public Commit(final String traceId,
                  final UUID userId) {
        this.id = UUID.randomUUID();
        this.traceId = traceId;
        this.userId = userId;
        this.modified = new Date(System.currentTimeMillis());
    }

    @Id
    @Column(name = "id")
    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    @Column(name = "trace_id")
    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(final String traceId) {
        this.traceId = traceId;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "modified")
    public Date getModified() {
        if (modified != null) {
            return new Date(modified.getTime());
        }
        return null;
    }

    public void setModified(final Date modified) {
        if (modified != null) {
            this.modified = new Date(modified.getTime());
        } else {
            this.modified = null;
        }
    }

    @Column(name = "description")
    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Column(name = "user_id")
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(final UUID userId) {
        this.userId = userId;
    }
}
