package fi.vm.yti.codelist.intake.resource;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg.ObjectWriterInjector;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.intake.dto.UserDTO;
import fi.vm.yti.codelist.intake.exception.NotFoundException;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CommitRepository;
import fi.vm.yti.codelist.intake.model.Commit;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_USER;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_401;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_406;

@Component
@Path("/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "System")
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
    @Operation(summary = "Returns the user information for user that has done the latest modification for the resource.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Returns success.")
    })
    public Response getUser(@Parameter(description = "CodeRegistry UUID", in = ParameterIn.QUERY) @QueryParam("codeRegistryId") final UUID codeRegistryId,
                            @Parameter(description = "CodeScheme UUID", in = ParameterIn.QUERY) @QueryParam("codeSchemeId") final UUID codeSchemeId,
                            @Parameter(description = "Code UUID", in = ParameterIn.QUERY) @QueryParam("codeId") final UUID codeId,
                            @Parameter(description = "Extension UUID", in = ParameterIn.QUERY) @QueryParam("extensionId") final UUID extensionId,
                            @Parameter(description = "Member UUID", in = ParameterIn.QUERY) @QueryParam("memberId") final UUID memberId,
                            @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty) {
        if (authorizationManager.getUserId() != null) {
            final UUID id;
            ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(FILTER_NAME_USER, "user"), pretty));
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
