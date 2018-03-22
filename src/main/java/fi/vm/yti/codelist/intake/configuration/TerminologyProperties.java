package fi.vm.yti.codelist.intake.configuration;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("terminology")
@Component
@Validated
public class TerminologyProperties {

    @NotNull
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
