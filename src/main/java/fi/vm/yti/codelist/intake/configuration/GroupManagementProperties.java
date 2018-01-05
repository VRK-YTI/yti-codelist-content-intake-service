package fi.vm.yti.codelist.intake.configuration;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("groupmanagement")
@Component
public class GroupManagementProperties {

    @NotNull
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    @NotNull
    private String publicUrl;

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(final String publicUrl) {
        this.publicUrl = publicUrl;
    }
}
