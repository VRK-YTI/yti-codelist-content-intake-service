package fi.vm.yti.cls.intake.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;


@ConfigurationProperties("public_api")
@Component
public class PublicApiServiceProperties {

    @NotNull
    private String host;

    @NotNull
    private String port;

    @NotNull
    private String scheme;

    @NotNull
    private String contextPath;


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

}
