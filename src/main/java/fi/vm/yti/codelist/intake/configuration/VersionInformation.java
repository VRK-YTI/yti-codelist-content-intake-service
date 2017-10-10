package fi.vm.yti.codelist.intake.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource(value = "classpath:git.properties", ignoreResourceNotFound = true)
public class VersionInformation {

    private static final String APP_VERSION = VersionInformation.class.getPackage().getImplementationVersion();

    @Value(value = "${git.build.version:dev}")
    private String versionId;

    @Value(value = "${git.commit.id.abbrev:dev}")
    private String commitId;

    /**
     * Application version and build information construction.
     *
     * @return Returns the application version and build information.
     */
    public String getVersion() {
        String version = APP_VERSION;
        if (version == null) {
            version = versionId;
        }
        version = version + " build " + commitId;
        return version;
    }
}
