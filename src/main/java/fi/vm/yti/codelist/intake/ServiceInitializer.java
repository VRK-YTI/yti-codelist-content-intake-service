package fi.vm.yti.codelist.intake;

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

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.configuration.PublicApiServiceProperties;
import fi.vm.yti.codelist.intake.configuration.VersionInformation;
import fi.vm.yti.codelist.intake.data.YtiDataAccess;
import fi.vm.yti.codelist.intake.groupmanagement.OrganizationUpdater;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.util.FileUtils;

/**
 * Class that initializes and possibly refreshes domain data at application startup.
 */
@Component
public class ServiceInitializer {

    public static final String LOCAL_SWAGGER_DATA_DIR = "/data/yti/yti-codelist-intake/swagger/";
    private static final Logger LOG = LoggerFactory.getLogger(ServiceInitializer.class);
    private final YtiDataAccess ytiDataAccess;
    private final Indexing indexing;
    private final ApiUtils apiUtils;
    private final OrganizationUpdater organizationUpdater;
    private final PublicApiServiceProperties publicApiServiceProperties;
    private final VersionInformation versionInformation;

    @Inject
    public ServiceInitializer(final VersionInformation versionInformation,
                              final Indexing indexing,
                              final ApiUtils apiUtils,
                              final YtiDataAccess ytiDataAccess,
                              final OrganizationUpdater organizationUpdater,
                              final PublicApiServiceProperties publicApiServiceProperties) {
        this.versionInformation = versionInformation;
        this.indexing = indexing;
        this.apiUtils = apiUtils;
        this.ytiDataAccess = ytiDataAccess;
        this.organizationUpdater = organizationUpdater;
        this.publicApiServiceProperties = publicApiServiceProperties;
    }

    /**
     * Initialize the application, load data for services.
     */
    @Transactional
    public void initialize() {
        updateSwaggerHost();
        LOG.info("*** Initializing data. ***");
        LOG.info("*** Updating organizations. ***");
        organizationUpdater.updateOrganizations();
        final Stopwatch watch = Stopwatch.createStarted();
        ytiDataAccess.initializeOrRefresh();
        LOG.info("*** Database population took: " + watch + ". ***");
        final Stopwatch indexWatch = Stopwatch.createStarted();
        indexing.reIndexEverything();
        LOG.info("*** Elastic indexing took: " + indexWatch + ". ***");
        LOG.info("*** Data initialization complete, took " + watch + ". ***");
    }

    /**
     * Application logo printout to log.
     */
    public void printLogo() {
        LOG.info("");
        LOG.info("          __  .__          .__        __          __           ");
        LOG.info(" ___.__._/  |_|__|         |__| _____/  |______  |  | __ ____  ");
        LOG.info("<   |  |\\   __\\  |  ______ |  |/    \\   __\\__  \\ |  |/ // __ \\ ");
        LOG.info(" \\___  | |  | |  | /_____/ |  |   |  \\  |  / __ \\|    <\\  ___/ ");
        LOG.info(" / ____| |__| |__|         |__|___|  /__| (____  /__|_ \\\\___  >");
        LOG.info(" \\/                                \\/          \\/     \\/    \\/ ");
        LOG.info("                          .__              ");
        LOG.info("  ______ ______________  _|__| ____  ____  ");
        LOG.info(" /  ___// __ \\_  __ \\  \\/ /  |/ ___\\/ __ \\ ");
        LOG.info(" \\___ \\\\  ___/|  | \\/\\   /|  \\  \\__\\  ___/ ");
        LOG.info("/____  >\\___  >__|    \\_/ |__|\\___  >___  >");
        LOG.info("     \\/     \\/                    \\/    \\/ ");
        LOG.info("");
        LOG.info("                --- Version " + versionInformation.getVersion() + " starting up. --- ");
        LOG.info("");
    }

    /**
     * Updates the compile time generated swagger.json with the hostname of the current environment.
     * <p>
     * The file is stored in the {@value #LOCAL_SWAGGER_DATA_DIR} folder, where it will be served from the SwaggerResource.
     */
    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    private void updateSwaggerHost() {
        final ObjectMapper mapper = new ObjectMapper();
        try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/swagger/swagger.json")) {
            final ObjectNode jsonObject = (ObjectNode) mapper.readTree(new InputStreamReader(inputStream, "UTF-8"));
            final String hostname = apiUtils.getContentIntakeServiceHostname();
            jsonObject.put("host", hostname);
            final String scheme = publicApiServiceProperties.getScheme();
            final List<String> schemes = new ArrayList<>();
            schemes.add(scheme);
            final ArrayNode schemeArray = mapper.valueToTree(schemes);
            jsonObject.putArray("schemes").addAll(schemeArray);
            final File file = new File(LOCAL_SWAGGER_DATA_DIR + "swagger.json");
            Files.createDirectories(Paths.get(file.getParentFile().getPath()));
            final String fileLocation = file.toString();
            LOG.info("Storing modified swagger.json description with hostname: " + hostname + " to: " + fileLocation);
            try (FileOutputStream fos = new FileOutputStream(fileLocation, false)) {
                mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
                fos.write(mapper.writeValueAsString(jsonObject).getBytes(StandardCharsets.UTF_8));
            }
        } catch (final IOException e) {
            LOG.error("Swagger JSON parsing failed: ", e);
        }
    }

}
