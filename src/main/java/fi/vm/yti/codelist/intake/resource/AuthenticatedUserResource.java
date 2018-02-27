package fi.vm.yti.codelist.intake.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import io.swagger.annotations.Api;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_PATH_AUTHENTICATED_USER;
import static fi.vm.yti.codelist.common.constants.ApiConstants.METHOD_GET;

@Component
@Path("/authenticated-user")
@Api(value = "authenticated-user")
@Produces(MediaType.APPLICATION_JSON)
public class AuthenticatedUserResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticatedUserResource.class);
    private final AuthenticatedUserProvider userProvider;

    @Autowired
    public AuthenticatedUserResource(AuthenticatedUserProvider userProvider) {
        this.userProvider = userProvider;
    }

    @GET
    public YtiUser getAuthenticatedUser() {
        logApiRequest(LOG, METHOD_GET, "", API_PATH_AUTHENTICATED_USER + "/");
        return this.userProvider.getUser();
    }
}
