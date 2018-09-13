package fi.vm.yti.codelist.intake.model;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractBaseCode extends AbstractIdentifyableTimestampedCode {

    private String uri;

    @Column(name = "uri")
    public String getUri() {
        return uri;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }
}
