package fi.vm.yti.codelist.intake.configuration;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("application")
@Component
@Validated
public class ContentIntakeServiceProperties {

    @NotNull
    private String host;

    @NotNull
    private String port;

    @NotNull
    private String scheme;

    @NotNull
    private String contextPath;

    private String env;

    private boolean initializeContent;

    private boolean initializeTestContent;

    public String getEnv() {
        return env;
    }

    public void setEnv(final String env) {
        this.env = env;
    }

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(final String port) {
        this.port = port;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(final String scheme) {
        this.scheme = scheme;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(final String contextPath) {
        this.contextPath = contextPath;
    }

    public boolean getInitializeContent() {
        return initializeContent;
    }

    public void setInitializeContent(final boolean initializeContent) {
        this.initializeContent = initializeContent;
    }

    public boolean getInitializeTestContent() {
        return initializeTestContent;
    }

    public void setInitializeTestContent(final boolean initializeTestContent) {
        this.initializeTestContent = initializeTestContent;
    }
}
