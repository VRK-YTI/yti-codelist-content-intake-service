package fi.vm.yti.codelist.intake.resource;

import static fi.vm.yti.codelist.common.constants.ApiConstants.API_PATH_IMPERSONABLE_USERS;
import static fi.vm.yti.codelist.common.constants.ApiConstants.METHOD_GET;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.intake.groupmanagement.GroupManagementUser;
import fi.vm.yti.codelist.intake.groupmanagement.ImpersonateUserService;
import io.swagger.annotations.Api;

@Component
@Path(API_PATH_IMPERSONABLE_USERS)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "fakeableUsers")
public class ImpersonateUserResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(ImpersonateUserResource.class);
    private ImpersonateUserService impersonateUserService;

    @Inject
    public ImpersonateUserResource(ImpersonateUserService impersonateUserService) {
        super();
        this.impersonateUserService = impersonateUserService;
    }

    @GET
    public List<GroupManagementUser> isLoginFakeable() {
        logApiRequest(LOG, METHOD_GET, "", API_PATH_IMPERSONABLE_USERS);
        return impersonateUserService.getUsers();
    }
}
