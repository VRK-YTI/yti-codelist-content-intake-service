package fi.vm.yti.cls.intake.resource;

import fi.vm.yti.cls.intake.configuration.VersionInformation;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;


@Component
@Path("/version")
@Api(value = "version", description = "Returns version information of the running application.")
@Produces("text/plain")
public class VersionResource {

    private static final Logger LOG = LoggerFactory.getLogger(VersionResource.class);

    private VersionInformation m_versionInformation;


    public VersionResource(final VersionInformation versionInformation) {

        m_versionInformation = versionInformation;

    }


    @GET
    @ApiOperation(value = "Get version information", response = String.class)
    @ApiResponse(code = 200, message = "Returns the version of the running Content Intake Service application.")
    public String getVersionInformation() {

        LOG.info("/version called");

        return "\n" +
               "       .__                     .__        __          __           \n" +
               "  ____ |  |   ______           |__| _____/  |______  |  | __ ____  \n" +
               "_/ ___\\|  |  /  ___/   ______  |  |/    \\   __\\__  \\ |  |/ // __ \\ \n" +
               "\\  \\___|  |__\\___ \\   /_____/  |  |   |  \\  |  / __ \\|    <\\  ___/ \n" +
               " \\___  >____/____  >           |__|___|  /__| (____  /__|_ \\\\___  >\n" +
               "     \\/          \\/                    \\/          \\/     \\/    \\/ \n" +
               "                          .__              \n" +
               "  ______ ______________  _|__| ____  ____  \n" +
               " /  ___// __ \\_  __ \\  \\/ /  |/ ___\\/ __ \\ \n" +
               " \\___ \\\\  ___/|  | \\/\\   /|  \\  \\__\\  ___/ \n" +
               "/____  >\\___  >__|    \\_/ |__|\\___  >___  >\n" +
               "     \\/     \\/                    \\/    \\/ \n" +
               "\n" +
               "                --- Version " + m_versionInformation.getVersion() + " running. --- \n";

    }

}
