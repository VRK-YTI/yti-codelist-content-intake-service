package fi.vm.yti.codelist.intake.resource;

import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;
import com.google.inject.Inject;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.intake.dto.UserDTO;
import fi.vm.yti.codelist.intake.exception.NotFoundException;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CommitRepository;
import fi.vm.yti.codelist.intake.model.Commit;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_USER;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_401;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_406;

@Component
@Path("/v1/users")
@Api(value = "users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource implements AbstractBaseResource {

    private final AuthorizationManager authorizationManager;
    private final CommitRepository commitRepository;
    private final UserService userService;

    @Inject
    public UserResource(final AuthorizationManager authorizationManager,
                        final CommitRepository commitRepository,
                        final UserService userService) {
        this.authorizationManager = authorizationManager;
        this.commitRepository = commitRepository;
        this.userService = userService;
    }

    @GET
    @Path("/user")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Returns the user information for user that has done the latest modification for the resource.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Returns success.")
    })
    public Response getUser(@ApiParam(value = "CodeRegistry UUID") @QueryParam("codeRegistryId") final UUID codeRegistryId,
                            @ApiParam(value = "CodeScheme UUID") @QueryParam("codeSchemeId") final UUID codeSchemeId,
                            @ApiParam(value = "Code UUID") @QueryParam("codeId") final UUID codeId,
                            @ApiParam(value = "Extension UUID") @QueryParam("extensionId") final UUID extensionId,
                            @ApiParam(value = "Member UUID") @QueryParam("memberId") final UUID memberId,
                            @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
        if (authorizationManager.getUserId() != null) {
            final UUID id;
            ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_USER, "user"), pretty));
            if (codeRegistryId != null) {
                final Commit commit = commitRepository.findLatestCommitByCodeRegistryId(codeRegistryId);
                if (commit != null) {
                    id = commit.getUserId();
                } else {
                    id = null;
                }
            } else if (codeSchemeId != null) {
                final Commit commit = commitRepository.findLatestCommitByCodeSchemeId(codeSchemeId);
                if (commit != null) {
                    id = commit.getUserId();
                } else {
                    id = null;
                }
            } else if (codeId != null) {
                final Commit commit = commitRepository.findLatestCommitByCodeId(codeId);
                if (commit != null) {
                    id = commit.getUserId();
                } else {
                    id = null;
                }
            } else if (extensionId != null) {
                final Commit commit = commitRepository.findLatestCommitByExtensionId(extensionId);
                if (commit != null) {
                    id = commit.getUserId();
                } else {
                    id = null;
                }
            } else if (memberId != null) {
                final Commit commit = commitRepository.findLatestCommitByMemberId(memberId);
                if (commit != null) {
                    id = commit.getUserId();
                } else {
                    id = null;
                }
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
            }
            if (id != null) {
                final UserDTO userDto = userService.getUserById(id);
                if (userDto != null) {
                    return Response.ok(userDto).build();
                }
            }
            throw new NotFoundException();
        } else {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
    }

}
