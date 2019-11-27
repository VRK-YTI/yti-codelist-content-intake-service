package fi.vm.yti.codelist.intake.resource;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg.ObjectWriterInjector;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.common.dto.Meta;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.service.CodeSchemeService;
import fi.vm.yti.codelist.intake.service.CodeService;
import fi.vm.yti.codelist.intake.service.ExtensionService;
import fi.vm.yti.codelist.intake.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CODE_EXTENSION;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_MEMBER;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_EXTENSION_NOT_FOUND;

@Component
@Path("/v1/members")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Member")
public class MemberResource implements AbstractBaseResource {

    private final Indexing indexing;
    private final MemberService memberService;
    private final ExtensionService extensionService;
    private final CodeSchemeService codeSchemeService;
    private final CodeService codeService;

    @Inject
    public MemberResource(final Indexing indexing,
                          final MemberService memberService,
                          final ExtensionService extensionService,
                          final CodeSchemeService codeSchemeService,
                          final CodeService codeService) {
        this.indexing = indexing;
        this.memberService = memberService;
        this.extensionService = extensionService;
        this.codeSchemeService = codeSchemeService;
        this.codeService = codeService;
    }

    @POST
    @Path("{memberId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses and creates or updates single Member from JSON input.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Returns success.")
    })
    public Response addOrUpdateMemberFromJson(@Parameter(description = "Member UUID", required = true, in = ParameterIn.PATH) @PathParam("memberId") final UUID memberId,
                                              @Parameter(description = "Pretty format JSON output.") @QueryParam("pretty") final String pretty,
                                              @RequestBody(description = "JSON payload for Member data.", required = true) final String jsonPayload) {
        return parseAndPersistMemberFromSource(jsonPayload, pretty);
    }

    @DELETE
    @Path("{memberId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Deletes a single existing Member.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Member deleted."),
        @ApiResponse(responseCode = "404", description = "Member not found.")
    })
    public Response deleteMember(@Parameter(description = "Member UUID", required = true, in = ParameterIn.PATH) @PathParam("memberId") final UUID memberId) {
        final MemberDTO existingMember = memberService.findById(memberId);
        if (existingMember != null) {
            final Set<MemberDTO> affectedMembers = new HashSet<>();
            memberService.deleteMember(existingMember.getId(), affectedMembers);
            final CodeSchemeDTO updatedCodeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(existingMember.getExtension().getParentCodeScheme().getCodeRegistry().getCodeValue(), existingMember.getExtension().getParentCodeScheme().getCodeValue());
            codeSchemeService.populateAllVersionsToCodeSchemeDTO(updatedCodeScheme);
            indexing.updateCodeScheme(updatedCodeScheme);
            indexing.deleteMember(existingMember);
            indexing.updateMembers(affectedMembers);
        } else {
            return Response.status(404).build();
        }
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    private Response parseAndPersistMemberFromSource(final String jsonPayload,
                                                     final String pretty) {
        final Set<MemberDTO> members = memberService.parseAndPersistMemberFromJson(jsonPayload);
        indexing.updateMembers(members);
        if (!members.isEmpty()) {
            final ExtensionDTO extension = extensionService.findById(members.iterator().next().getExtension().getId());
            if (extension == null) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EXTENSION_NOT_FOUND));
            } else {
                indexing.updateExtension(extension);
                indexing.updateMembers(memberService.findByExtensionId(extension.getId()));
            }
            if (CODE_EXTENSION.equalsIgnoreCase(extension.getPropertyType().getContext())) {
                final Set<CodeDTO> codes = new HashSet<>();
                members.forEach(member -> codes.add(codeService.findById(member.getCode().getId())));
                indexing.updateCodes(codes);
            }
        }
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(FILTER_NAME_MEMBER, "member"), pretty));
        final ResponseWrapper<MemberDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("Member added or modified.");
        meta.setCode(200);
        return Response.ok(responseWrapper).build();
    }
}
