package fi.vm.yti.codelist.intake.dto;

import java.io.Serializable;
import java.util.Map;

public class MessagingResourceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uri;
    private String application;
    private String type;
    private Map<String, String> prefLabel;

    public String getUri() {
        return uri;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(final String application) {
        this.application = application;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public Map<String, String> getPrefLabel() {
        return prefLabel;
    }

    public void setPrefLabel(final Map<String, String> prefLabel) {
        this.prefLabel = prefLabel;
    }
}
