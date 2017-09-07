package fi.vm.yti.cls.intake.resource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.File;
import java.io.IOException;

import static fi.vm.yti.cls.intake.ServiceInitializer.LOCAL_SWAGGER_DATA_DIR;

@Component
@Path("/swagger.json")
@Api(value = "swagger.json", description = "Operation that outputs environment specific dynamic swagger.json.")
@Produces("text/plain")
public class SwaggerResource {

    private static final Logger LOG = LoggerFactory.getLogger(SwaggerResource.class);

    @GET
    @ApiOperation(value = "Get Swagger JSON", response = String.class)
    @ApiResponse(code = 200, message = "Returns the swagger.json description for this service.")
    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public String getSwaggerJson() throws IOException {
        LOG.info("/swagger.json called.");
        final File file = new File(LOCAL_SWAGGER_DATA_DIR + "swagger.json");
        final String swaggerJson = FileUtils.readFileToString(file, "UTF-8");
        return swaggerJson;
    }

}
