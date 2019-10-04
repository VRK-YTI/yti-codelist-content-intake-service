package fi.vm.yti.codelist.intake.resource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;

@Component
@Path("/authenticated-user")
@Produces(MediaType.APPLICATION_JSON)
public class AuthenticatedUserResource implements AbstractBaseResource {

    private final AuthenticatedUserProvider userProvider;

    @Inject
    public AuthenticatedUserResource(AuthenticatedUserProvider userProvider) {
        this.userProvider = userProvider;
    }

    @GET
    public YtiUser getAuthenticatedUser() {
        return this.userProvider.getUser();
    }
}
