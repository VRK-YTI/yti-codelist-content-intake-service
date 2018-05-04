package fi.vm.yti.codelist.intake.model;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.annotations.ApiModelProperty;

@Entity
@XmlRootElement
@XmlType(propOrder = {"id", "userId", "modified", "description"})
@Table(name = "commit")
public class Commit {

    private String id;
    private UUID userId;
    private Date modified;
    private String description;
    private Set<EditedEntity> editedEntities;

    public Commit() {
    }

    public Commit(final String traceId,
                  final UUID userId) {
        this.id = traceId;
        this.userId = userId;
        this.modified = new Date(System.currentTimeMillis());
    }

    @Id
    @Column(name = "id")
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    @ApiModelProperty(dataType = "dateTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
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

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "commit", cascade = CascadeType.ALL)
    public Set<EditedEntity> getEditedEntities() {
        return editedEntities;
    }

    public void setEditedEntities(final Set<EditedEntity> editedEntities) {
        this.editedEntities = editedEntities;
    }
}
