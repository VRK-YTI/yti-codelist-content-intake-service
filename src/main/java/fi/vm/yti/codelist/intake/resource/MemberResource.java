package fi.vm.yti.codelist.intake.resource;

import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;

import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.common.dto.Meta;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.service.ExtensionService;
import fi.vm.yti.codelist.intake.service.MemberService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_MEMBER;

@Component
@Path("/v1/members")
@Api(value = "members")
@Produces(MediaType.APPLICATION_JSON)
public class MemberResource implements AbstractBaseResource {

    private final Indexing indexing;
    private final MemberService memberService;
    private final ExtensionService extensionService;

    @Inject
    public MemberResource(final Indexing indexing,
                          final MemberService memberService,
                          final ExtensionService extensionService) {
        this.indexing = indexing;
        this.memberService = memberService;
        this.extensionService = extensionService;
    }

    @POST
    @Path("{memberId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses and creates or updates single Member from JSON input.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Returns success.")
    })
    public Response addOrUpdateMemberFromJson(@ApiParam(value = "Member UUID", required = true) @PathParam("memberId") final UUID memberId,
                                              @ApiParam(value = "JSON playload for Member data.", required = true) final String jsonPayload) {
        return parseAndPersistMemberFromSource(jsonPayload);
    }

    @DELETE
    @Path("{memberId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single existing Member.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Member deleted."),
        @ApiResponse(code = 404, message = "Member not found.")
    })
    public Response deleteMember(@ApiParam(value = "Member UUID", required = true) @PathParam("memberId") final UUID memberId) {
        final MemberDTO existingMember = memberService.findById(memberId);
        if (existingMember != null) {
            final UUID extensionId = existingMember.getExtension().getId();
            memberService.deleteMember(existingMember.getId());
            indexing.deleteMember(existingMember);
            indexing.updateMembers(memberService.findByExtensionId(extensionId));
        } else {
            return Response.status(404).build();
        }
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    private Response parseAndPersistMemberFromSource(final String jsonPayload) {
        final Set<MemberDTO> members = memberService.parseAndPersistMemberFromJson(jsonPayload);
        indexing.updateMembers(members);
        if (!members.isEmpty()) {
            final ExtensionDTO extension = extensionService.findById(members.iterator().next().getExtension().getId());
            if (extension != null) {
                indexing.updateExtension(extension);
            }
        }
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_MEMBER, "member")));
        final ResponseWrapper<MemberDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("Member added or modified.");
        meta.setCode(200);
        return Response.ok(responseWrapper).build();
    }
}
