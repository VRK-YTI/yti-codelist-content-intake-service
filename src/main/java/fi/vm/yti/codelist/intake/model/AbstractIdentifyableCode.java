package fi.vm.yti.codelist.intake.model;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import com.fasterxml.jackson.annotation.JsonView;

import fi.vm.yti.codelist.common.dto.Views;

@MappedSuperclass
public class AbstractIdentifyableCode {

    private UUID id;

    @Id
    @Column(name = "id", unique = true)
    @JsonView(Views.Normal.class)
    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }
}
