package fi.vm.yti.cls.intake.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource(value = "classpath:git.properties", ignoreResourceNotFound = true)
public class VersionInformation {

    private static final String APP_VERSION = VersionInformation.class.getPackage().getImplementationVersion();

    @Value(value = "${git.build.version:dev}")
    private String m_versionId;

    @Value(value = "${git.commit.id.abbrev:dev}")
    private String m_commitId;


    /**
     * Application version and build information construction.
     *
     * @return Returns the application version and build information.
     */
    public String getVersion() {

        String version = APP_VERSION;

        if (version == null) {
            version = m_versionId;
        }

        version = version + " build " + m_commitId;

        return version;
    }

}
