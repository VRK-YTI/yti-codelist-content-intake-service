package fi.vm.yti.codelist.intake.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import io.swagger.annotations.Api;

@Component
@Path("/authenticated-user")
@Api(value = "authenticateduser", description = "Operation for fetching authenticated user to the frontend.")
@Produces(MediaType.APPLICATION_JSON)
public class AuthenticatedUserResource {

    private final AuthenticatedUserProvider userProvider;

    @Autowired
    public AuthenticatedUserResource(AuthenticatedUserProvider userProvider) {
        this.userProvider = userProvider;
    }

    @GET
    public YtiUser getAuthenticatedUser() {
        return this.userProvider.getUser();
    }
}
