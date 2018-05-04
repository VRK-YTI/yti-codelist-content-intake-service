package fi.vm.yti.codelist.intake.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("frontend")
@Component
@Validated
public class FrontendProperties {

    private String defaultStatus;

    private String codeSchemeSortMode;

    public String getDefaultStatus() {
        return defaultStatus;
    }

    public void setDefaultStatus(final String defaultStatus) {
        this.defaultStatus = defaultStatus;
    }

    public String getCodeSchemeSortMode() {
        return codeSchemeSortMode;
    }

    public void setCodeSchemeSortMode(final String codeSchemeSortMode) {
        this.codeSchemeSortMode = codeSchemeSortMode;
    }
}
