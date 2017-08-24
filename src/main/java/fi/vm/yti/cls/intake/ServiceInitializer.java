package fi.vm.yti.cls.intake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.cls.intake.api.ApiUtils;
import fi.vm.yti.cls.intake.configuration.PublicApiServiceProperties;
import fi.vm.yti.cls.intake.data.GenericDataAccess;
import fi.vm.yti.cls.intake.data.PostiDataAccess;
import fi.vm.yti.cls.intake.data.YtjDataAccess;
import fi.vm.yti.cls.intake.domain.Domain;
import fi.vm.yti.cls.intake.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


/**
 * Class that initializes and possibly refreshes domain data at application startup.
 */
@Component
public class ServiceInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceInitializer.class);

    public static final String LOCAL_SWAGGER_DATA_DIR = "/data/cls/cls-intake/swagger/";

    private final GenericDataAccess m_genericDataAccess;

    private final YtjDataAccess m_ytjDataAccess;

    private final PostiDataAccess m_postiDataAccess;

    private final Domain m_domain;

    private final ApiUtils m_apiUtils;

    private final PublicApiServiceProperties m_publicApiServiceProperties;


    @Inject
    public ServiceInitializer(final Domain domain,
                              final ApiUtils apiUtils,
                              final GenericDataAccess genericDataAccess,
                              final YtjDataAccess ytjDataAccess,
                              final PostiDataAccess postiDataAccess,
                              final PublicApiServiceProperties publicApiServiceProperties) {

        m_domain = domain;

        m_genericDataAccess = genericDataAccess;

        m_apiUtils = apiUtils;

        m_ytjDataAccess = ytjDataAccess;

        m_postiDataAccess = postiDataAccess;

        m_publicApiServiceProperties = publicApiServiceProperties;

    }


    /**
     * Initialize the application, load data for services.
     */
    public void initialize(final boolean indexOnly) {

        updateSwaggerHost();

        LOG.info("*** Initializing data. ***");

        final Stopwatch watch = Stopwatch.createStarted();

        if (!indexOnly) {

            // PostgreSQL persistance

            m_ytjDataAccess.initializeOrRefresh();

            m_genericDataAccess.initializeOrRefresh();

            m_postiDataAccess.initializeOrRefresh();

            LOG.info("*** Database population took: " + watch + ". ***");

        } else {
            LOG.info("*** Only indexing selected, not populating database. ***");
        }

        final Stopwatch indexWatch = Stopwatch.createStarted();

        // ElasticSearch indexing

        m_domain.reIndexEverything();

        LOG.info("*** Elastic indexing took: " + indexWatch + ". ***");

        // Timing of initialization.

        LOG.info("*** Data initialization complete, took " + watch + ". ***");

    }


    /**
     * Updates the compile time generated swagger.json with the hostname of the current environment.
     *
     * The file is stored in the {@value #LOCAL_SWAGGER_DATA_DIR} folder, where it will be served from the SwaggerResource.
     */
    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    private void updateSwaggerHost() {

        final ObjectMapper mapper = new ObjectMapper();

        FileOutputStream fos = null;

        try {

            final InputStream inputStream = FileUtils.loadFileFromClassPath("/swagger/swagger.json");
            final ObjectNode jsonObject = (ObjectNode) mapper.readTree(new InputStreamReader(inputStream, "UTF-8"));

            final String hostname = m_apiUtils.getPublicApiServiceHostname();
            jsonObject.put("host", hostname);

            final String scheme = m_publicApiServiceProperties.getScheme();
            final List<String> schemes = new ArrayList<>();
            schemes.add(scheme);
            final ArrayNode schemeArray = mapper.valueToTree(schemes);
            jsonObject.putArray("schemes").addAll(schemeArray);

            final File file = new File(LOCAL_SWAGGER_DATA_DIR + "swagger.json");
            Files.createDirectories(Paths.get(file.getParentFile().getPath()));

            final String fileLocation = file.toString();
            LOG.info("Storing modified swagger.json description with hostname: " + hostname + " to: " + fileLocation);

            fos = new FileOutputStream(fileLocation, false);

            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            fos.write(mapper.writeValueAsString(jsonObject).getBytes(StandardCharsets.UTF_8));
            fos.close();

        } catch (IOException e) {
            LOG.error("Swagger JSON parsing failed: " + e.getMessage());

        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    LOG.error("Closing output stream failed: " + e.getMessage());
                }
            }
        }

    }

}
