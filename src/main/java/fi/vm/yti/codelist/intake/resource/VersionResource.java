package fi.vm.yti.codelist.intake.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.intake.configuration.VersionInformation;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;

@Component
@Path("/version")
@Api(value = "version")
@Produces("text/plain")
public class VersionResource extends AbstractBaseResource {

    private VersionInformation versionInformation;

    public VersionResource(final VersionInformation versionInformation) {
        this.versionInformation = versionInformation;
    }

    @GET
    @ApiOperation(value = "Get version information", response = String.class)
    @ApiResponse(code = 200, message = "Returns the version of the running Content Intake Service application.")
    public String getVersionInformation() {
        return "\n" +
            "          __  .__          .__        __          __           \n" +
            " ___.__._/  |_|__|         |__| _____/  |______  |  | __ ____  \n" +
            "<   |  |\\   __\\  |  ______ |  |/    \\   __\\__  \\ |  |/ // __ \\ \n" +
            " \\___  | |  | |  | /_____/ |  |   |  \\  |  / __ \\|    <\\  ___/ \n" +
            " / ____| |__| |__|         |__|___|  /__| (____  /__|_ \\\\___  >\n" +
            " \\/                                \\/          \\/     \\/    \\/ \n" +
            "                          .__              \n" +
            "  ______ ______________  _|__| ____  ____  \n" +
            " /  ___// __ \\_  __ \\  \\/ /  |/ ___\\/ __ \\ \n" +
            " \\___ \\\\  ___/|  | \\/\\   /|  \\  \\__\\  ___/ \n" +
            "/____  >\\___  >__|    \\_/ |__|\\___  >___  >\n" +
            "     \\/     \\/                    \\/    \\/ \n" +
            "\n" +
            "                --- Version " + versionInformation.getVersion() + " running. --- \n";
    }
}
