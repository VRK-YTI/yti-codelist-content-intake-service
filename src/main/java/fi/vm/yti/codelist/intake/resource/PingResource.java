package fi.vm.yti.codelist.intake.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Component
@Path("/ping")
@Produces("text/plain")
public class PingResource implements AbstractBaseResource {

    @GET
    @Operation(summary = "Ping pong health check API.")
    @ApiResponse(responseCode = "200", description = "Returns pong if service is this API is reachable.")
    @Produces("text/plain")
    public Response ping() {
        return Response.ok("pong").build();
    }
}
