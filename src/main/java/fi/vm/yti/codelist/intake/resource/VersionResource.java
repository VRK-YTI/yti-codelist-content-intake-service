package fi.vm.yti.codelist.intake.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.intake.configuration.VersionInformation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Component
@Path("/version")
@Produces("text/plain")
@Tag(name = "System")
public class VersionResource implements AbstractBaseResource {

    private final VersionInformation versionInformation;

    public VersionResource(final VersionInformation versionInformation) {
        this.versionInformation = versionInformation;
    }

    @GET
    @Operation(summary = "Get version information")
    @ApiResponse(responseCode = "200", description = "Returns the version of the running Content Intake Service application.")
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
