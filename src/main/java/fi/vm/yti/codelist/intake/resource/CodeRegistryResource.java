package fi.vm.yti.codelist.intake.resource;

import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg.ObjectWriterInjector;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.common.dto.Meta;
import fi.vm.yti.codelist.common.dto.Views;
import fi.vm.yti.codelist.common.model.CodeSchemeListItem;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.common.util.YtiCollectionUtils;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.exception.TooManyCodeSchemesException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.parser.CodeSchemeParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.CloningService;
import fi.vm.yti.codelist.intake.service.CodeRegistryService;
import fi.vm.yti.codelist.intake.service.CodeSchemeService;
import fi.vm.yti.codelist.intake.service.CodeService;
import fi.vm.yti.codelist.intake.service.ExtensionService;
import fi.vm.yti.codelist.intake.service.ExternalReferenceService;
import fi.vm.yti.codelist.intake.service.MemberService;
import fi.vm.yti.codelist.intake.util.ValidationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;
import static fi.vm.yti.codelist.intake.util.EncodingUtils.urlDecodeCodeValue;
import static fi.vm.yti.codelist.intake.util.EncodingUtils.urlDecodeString;

@Component
@Path("/v1/coderegistries")
@Produces("text/plain")
public class CodeRegistryResource implements AbstractBaseResource {

    private final CodeService codeService;
    private final CodeSchemeService codeSchemeService;
    private final CodeRegistryService codeRegistryService;
    private final ExternalReferenceService externalReferenceService;
    private final ExtensionService extensionService;
    private final MemberService memberService;
    private final Indexing indexing;
    private final CloningService cloningService;
    private final CodeSchemeParser codeSchemeParser;
    private final AuthorizationManager authorizationManager;

    @Inject
    public CodeRegistryResource(final CodeService codeService,
                                final CodeSchemeService codeSchemeService,
                                final CodeRegistryService codeRegistryService,
                                final ExternalReferenceService externalReferenceService,
                                final ExtensionService extensionService,
                                final MemberService memberService,
                                final Indexing indexing,
                                final CloningService cloningService,
                                final CodeSchemeParser codeSchemeParser,
                                final AuthorizationManager authorizationManager) {
        this.codeService = codeService;
        this.codeSchemeService = codeSchemeService;
        this.codeRegistryService = codeRegistryService;
        this.externalReferenceService = externalReferenceService;
        this.extensionService = extensionService;
        this.memberService = memberService;
        this.indexing = indexing;
        this.cloningService = cloningService;
        this.codeSchemeParser = codeSchemeParser;
        this.authorizationManager = authorizationManager;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses and creates or updates CodeRegistries from JSON or file input.")
    @ApiResponse(responseCode = "200", description = "CodeRegistries added or modified successfully.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CodeRegistryDTO.class))))
    @Tag(name = "CodeRegistry")
    public Response addOrUpdateCodeRegistriesFromJson(@Parameter(description = "Format for input.", required = true, in = ParameterIn.QUERY) @QueryParam("format") @DefaultValue("json") final String format,
                                                      @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty,
                                                      @RequestBody(description = "JSON payload for CodeRegistry data.", required = true) final String jsonPayload) {
        return parseAndPersistCodeRegistriesFromSource(FORMAT_JSON, null, jsonPayload, pretty);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses and creates or updates CodeRegistries from CSV or Excel input data.")
    @ApiResponse(responseCode = "200", description = "CodeRegistries added or modified successfully.")
    @Tag(name = "CodeRegistry")
    public Response addOrUpdateCodeRegistriesFromFile(@Parameter(description = "Format for input.", required = true) @QueryParam("format") @DefaultValue("json") final String format,
                                                      @Parameter(description = "Pretty format JSON output.") @QueryParam("pretty") final String pretty,
                                                      @Parameter(description = "Input-file for CSV or Excel import.", required = true, style = ParameterStyle.FORM, schema = @Schema(type = "string", format = "binary", description = "Incoming file.")) @FormDataParam("file") final InputStream inputStream) {
        return parseAndPersistCodeRegistriesFromSource(format, inputStream, null, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Modifies a single existing CodeRegistry.")
    @ApiResponse(responseCode = "200", description = "CodeRegistry modified successfully.", content = @Content(schema = @Schema(implementation = CodeRegistryDTO.class)))
    @Tag(name = "CodeRegistry")
    public Response updateCodeRegistry(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                       @RequestBody(description = "JSON payload for Code data.", required = true) final String jsonPayload) {

        final CodeRegistryDTO codeRegistry = codeRegistryService.parseAndPersistCodeRegistryFromJson(codeRegistryCodeValue, jsonPayload);
        indexing.updateCodeRegistry(codeRegistry);
        final Set<CodeSchemeDTO> codeSchemes = codeSchemeService.findByCodeRegistryCodeValue(codeRegistry.getCodeValue());
        indexing.updateCodeSchemes(codeSchemes);
        codeSchemes.forEach(codeScheme -> {
            indexing.updateCodes(codeService.findByCodeSchemeId(codeScheme.getId()));
            indexing.updateExternalReferences(externalReferenceService.findByParentCodeSchemeId(codeScheme.getId()));
            final Set<ExtensionDTO> extensions = extensionService.findByParentCodeSchemeId(codeScheme.getId());
            if (extensions != null && !extensions.isEmpty()) {
                indexing.updateExtensions(extensions);
                for (final ExtensionDTO extension : extensions) {
                    indexing.updateMembers(memberService.findByExtensionId(extension.getId()));
                }
            }
        });
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    @DELETE
    @Path("{codeRegistryCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Deletes a single existing CodeRegistry.")
    @ApiResponse(responseCode = "200", description = "CodeRegistry deleted.")
    @ApiResponse(responseCode = "404", description = "CodeRegistry not found.")
    @ApiResponse(responseCode = "406", description = "CodeRegistry has code lists, cannot delete.")
    @Tag(name = "CodeRegistry")
    public Response deleteCodeRegistry(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue) {

        final CodeRegistryDTO existingCodeRegistry = codeRegistryService.findByCodeValue(codeRegistryCodeValue);
        if (existingCodeRegistry != null) {
            final Set<CodeSchemeDTO> codeSchemes = codeSchemeService.findByCodeRegistryCodeValue(codeRegistryCodeValue);
            if (codeSchemes != null && !codeSchemes.isEmpty()) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODEREGISTRY_NOT_EMPTY));
            }
            final CodeRegistryDTO codeRegistry = codeRegistryService.deleteCodeRegistry(codeRegistryCodeValue);
            indexing.deleteCodeRegistry(codeRegistry);
        } else {
            return Response.status(404).build();
        }
        final Meta meta = new Meta();
        meta.setCode(200);
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses and creates or updates CodeSchemes from JSON input.", operationId = "addOrUpdateCodeSchemesFromJson")
    @ApiResponse(responseCode = "200", description = "CodeSchemes added or modified successfully.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CodeSchemeDTO.class))))
    @Tag(name = "CodeScheme")
    public Response addOrUpdateCodeSchemesFromJson(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                   @Parameter(description = "Format for input.", required = true, in = ParameterIn.QUERY) @QueryParam("format") @DefaultValue("json") final String format,
                                                   @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty,
                                                   @RequestBody(description = "JSON payload for CodeScheme data.", content = @Content(mediaType = MediaType.APPLICATION_JSON)) final String jsonPayload) {
        return parseAndPersistCodeSchemesFromSource(codeRegistryCodeValue, FORMAT_JSON, null, jsonPayload, false, "", false, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses and creates or updates CodeSchemes from Excel or CSV file.", operationId = "addOrUpdateCodeSchemesFromFile")
    @ApiResponse(responseCode = "200", description = "CodeSchemes added or modified successfully.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CodeSchemeDTO.class))))
    @Tag(name = "CodeScheme")
    public Response addOrUpdateCodeSchemesFromFile(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                   @Parameter(description = "Format for input.", in = ParameterIn.QUERY) @QueryParam("format") @DefaultValue("csv") final String format,
                                                   @Parameter(description = "New Codelist version", in = ParameterIn.QUERY) @QueryParam("newVersionOfCodeScheme") @DefaultValue("false") final boolean userIsCreatingANewVersionOfACodeScheme,
                                                   @Parameter(description = "True if user is updating a particular code list with a file from the code list page menu", in = ParameterIn.QUERY) @QueryParam("updatingExistingCodeScheme") @DefaultValue("false") final boolean updatingExistingCodeScheme,
                                                   @Parameter(description = "If creating new version, id of previous code list version", in = ParameterIn.QUERY) @QueryParam("originalCodeSchemeId") final String originalCodeSchemeId,
                                                   @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty,
                                                   @Parameter(description = "Input-file for CSV or Excel import.", in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "binary", description = "Incoming file.")) @FormDataParam("file") final InputStream inputStream) {
        return parseAndPersistCodeSchemesFromSource(codeRegistryCodeValue, format, inputStream, null, userIsCreatingANewVersionOfACodeScheme, originalCodeSchemeId, updatingExistingCodeScheme, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/validate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Validates CSV or Excel input data in case of new Code list version creation directly from file.")
    @ApiResponse(responseCode = "200", description = "The file is valid or not.")
    @Tag(name = "CodeScheme")
    public Response canANewVersionOfACodeSchemeBeCreatedFromTheIncomingFileDirectly(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                                                    @Parameter(description = "Format for input.", in = ParameterIn.QUERY) @QueryParam("format") @DefaultValue("csv") final String format,
                                                                                    @Parameter(description = "Input-file for CSV or Excel import.", in = ParameterIn.QUERY, style = ParameterStyle.FORM, schema = @Schema(type = "string", format = "binary", description = "Incoming file."), content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA)) @FormDataParam("file") final InputStream inputStream) {
        boolean okToCreateANewVersion = codeSchemeService.canANewVersionOfACodeSchemeBeCreatedFromTheIncomingFileDirectly(codeRegistryCodeValue, format, inputStream);
        final ObjectMapper mapper = new ObjectMapper();
        Response response;
        try {
            if (okToCreateANewVersion) {
                response = Response.status(Response.Status.OK).entity(mapper.writeValueAsString(okToCreateANewVersion)).build();
            } else {
                throw new TooManyCodeSchemesException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_TOO_MANY_CODESCHEMES_IN_FILE));
            }

        } catch (JsonProcessingException e) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "JSON processing exceptionm"));
        }
        return response;
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Modifies single existing CodeScheme.")
    @ApiResponse(responseCode = "200", description = "CodeScheme modified successfully.", content = @Content(schema = @Schema(implementation = CodeSchemeDTO.class)))
    @Tag(name = "CodeScheme")
    public Response updateCodeScheme(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                     @Parameter(description = "CodeScheme codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                     @Parameter(description = "Control query parameter that changes code status according to codeScheme status change.", in = ParameterIn.QUERY) @QueryParam("changeCodeStatuses") @DefaultValue("false") final boolean changeCodeStatuses,
                                     @RequestBody(description = "JSON payload for CodeScheme data.", required = true) final String jsonPayload) {

        final CodeSchemeDTO originalCodeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        final CodeSchemeDTO newCodeScheme = codeSchemeParser.parseCodeSchemeFromJsonData(jsonPayload);
        String initialCodeStatus = originalCodeScheme.getStatus();
        String endCodeStatus = newCodeScheme.getStatus();
        Set<CodeDTO> codesWhereStatusChanged = null;

        if (changeCodeStatuses && !authorizationManager.isSuperUser()) {
            ValidationUtils.validateStatusTransitions(initialCodeStatus, endCodeStatus);
        }

        final CodeSchemeDTO codeScheme = codeSchemeService.parseAndPersistCodeSchemeFromJson(codeRegistryCodeValue, codeSchemeCodeValue, jsonPayload);

        final Set<CodeSchemeDTO> versionsToReIndex = new LinkedHashSet<>();
        UUID codeSchemeWhoseStatusMustBeSetToSuperseded = null;

        if (codeScheme != null) {
            if (codeScheme.getLastCodeschemeId() != null) {
                if (currentCodeSchemeIsTheLatestVersion(codeScheme)) {
                    if (codeScheme.getStatus().equals(Status.VALID.toString())) { //When the latest version goes VALID, the prev version goes SUPERSEDED. Update all listings too.
                        final CodeSchemeDTO previousCodeScheme = codeSchemeService.findById(codeScheme.getPrevCodeschemeId());
                        if (previousCodeScheme != null && previousCodeScheme.getStatus().equals(Status.VALID.toString())) {
                            previousCodeScheme.setStatus(Status.SUPERSEDED.toString());
                            codeSchemeService.updateCodeSchemeFromDto(previousCodeScheme.getCodeRegistry().getCodeValue(), previousCodeScheme);
                            versionsToReIndex.add(previousCodeScheme);
                            codeSchemeWhoseStatusMustBeSetToSuperseded = previousCodeScheme.getId();
                        }

                        if (previousCodeScheme != null && !previousCodeScheme.getVariantMothersOfThisCodeScheme().isEmpty()) {
                            for (final CodeSchemeListItem mother : previousCodeScheme.getVariantMothersOfThisCodeScheme()) {
                                final CodeSchemeDTO motherCodeScheme = codeSchemeService.findById(mother.getId());
                                final LinkedHashSet<CodeSchemeListItem> variantsOfTheMother = motherCodeScheme.getVariantsOfThisCodeScheme();
                                for (CodeSchemeListItem item : variantsOfTheMother) {
                                    if (item.getId().equals(previousCodeScheme.getId())) {
                                        populateCodeSchemeListItem(previousCodeScheme, item);
                                    }
                                    if (codeSchemeWhoseStatusMustBeSetToSuperseded != null && item.getId().equals(codeSchemeWhoseStatusMustBeSetToSuperseded)) {
                                        item.setStatus(Status.SUPERSEDED.toString());
                                    }
                                }
                                codeSchemeService.updateCodeSchemeFromDto(motherCodeScheme.getCodeRegistry().getCodeValue(), motherCodeScheme);
                                indexing.updateCodeScheme(motherCodeScheme);
                            }
                        }

                        if (previousCodeScheme != null && !previousCodeScheme.getVariantsOfThisCodeScheme().isEmpty()) {
                            for (final CodeSchemeListItem variant : previousCodeScheme.getVariantsOfThisCodeScheme()) {
                                final CodeSchemeDTO variantCodeScheme = codeSchemeService.findById(variant.getId());
                                final LinkedHashSet<CodeSchemeListItem> mothersOfTheVariant = variantCodeScheme.getVariantMothersOfThisCodeScheme();
                                for (CodeSchemeListItem item : mothersOfTheVariant) {
                                    if (item.getId().equals(previousCodeScheme.getId())) {
                                        populateCodeSchemeListItem(previousCodeScheme, item);
                                    }
                                    if (codeSchemeWhoseStatusMustBeSetToSuperseded != null && item.getId().equals(codeSchemeWhoseStatusMustBeSetToSuperseded)) {
                                        item.setStatus(Status.SUPERSEDED.toString());
                                    }
                                }
                                codeSchemeService.updateCodeSchemeFromDto(variantCodeScheme.getCodeRegistry().getCodeValue(), variantCodeScheme);
                                indexing.updateCodeScheme(variantCodeScheme);
                            }
                        }
                    }
                }

                final LinkedHashSet<CodeSchemeListItem> allVersions = codeScheme.getAllVersions();

                for (final CodeSchemeListItem listItem : allVersions) {
                    if (listItem.getId().equals(codeScheme.getId())) {
                        populateCodeSchemeListItem(codeScheme, listItem);
                    }
                    if (codeSchemeWhoseStatusMustBeSetToSuperseded != null && listItem.getId().equals(codeSchemeWhoseStatusMustBeSetToSuperseded)) {
                        listItem.setStatus(Status.SUPERSEDED.toString());
                    }
                }

                for (final CodeSchemeListItem listItem : allVersions) {
                    CodeSchemeDTO currentVersion = codeSchemeService.findById(listItem.getId());
                    currentVersion.setAllVersions(allVersions);
                    codeSchemeService.updateCodeSchemeFromDto(codeRegistryCodeValue, currentVersion);
                    versionsToReIndex.add(currentVersion);
                }

                indexing.updateCodeSchemes(versionsToReIndex);
            }

            if (!codeScheme.getVariantMothersOfThisCodeScheme().isEmpty()) {
                for (final CodeSchemeListItem mother : codeScheme.getVariantMothersOfThisCodeScheme()) {
                    CodeSchemeDTO motherCodeScheme = codeSchemeService.findById(mother.getId());
                    final LinkedHashSet<CodeSchemeListItem> variantsOfTheMother = motherCodeScheme.getVariantsOfThisCodeScheme();
                    for (CodeSchemeListItem item : variantsOfTheMother) {
                        if (item.getId().equals(codeScheme.getId())) {
                            populateCodeSchemeListItem(codeScheme, item);
                        }
                    }
                    codeSchemeService.updateCodeSchemeFromDto(motherCodeScheme.getCodeRegistry().getCodeValue(), motherCodeScheme);
                    indexing.updateCodeScheme(motherCodeScheme);
                }
            }

            if (!codeScheme.getVariantsOfThisCodeScheme().isEmpty()) {
                for (final CodeSchemeListItem variant : codeScheme.getVariantsOfThisCodeScheme()) {
                    CodeSchemeDTO variantCodeScheme = codeSchemeService.findById(variant.getId());
                    final LinkedHashSet<CodeSchemeListItem> mothersOfTheVariant = variantCodeScheme.getVariantMothersOfThisCodeScheme();
                    for (CodeSchemeListItem item : mothersOfTheVariant) {
                        if (item.getId().equals(codeScheme.getId())) {
                            populateCodeSchemeListItem(codeScheme, item);
                        }
                    }
                    codeSchemeService.updateCodeSchemeFromDto(variantCodeScheme.getCodeRegistry().getCodeValue(), variantCodeScheme);
                    indexing.updateCodeScheme(variantCodeScheme);
                }
            }

            if (changeCodeStatuses) {
                codesWhereStatusChanged = codeService.massChangeCodeStatuses(codeRegistryCodeValue, codeSchemeCodeValue, initialCodeStatus, endCodeStatus, true);
            }

            indexing.updateCodeScheme(codeScheme);
            indexing.updateExternalReferences(codeScheme.getExternalReferences());
            indexing.updateCodes(codeService.findByCodeSchemeId(codeScheme.getId()));
            final Set<ExtensionDTO> extensions = extensionService.findByParentCodeSchemeId(codeScheme.getId());
            if (extensions != null) {
                indexing.updateExtensions(extensions);
                extensions.forEach(extension -> indexing.updateMembers(memberService.findByExtensionId(extension.getId())));
            }
            final Set<ExtensionDTO> relatedExtensions = extensionService.findByCodeSchemeId(codeScheme.getId());
            if (relatedExtensions != null && !relatedExtensions.isEmpty()) {
                indexing.updateExtensions(relatedExtensions);
                for (final ExtensionDTO extension : relatedExtensions) {
                    indexing.updateMembers(memberService.findByExtensionId(extension.getId()));
                }
            }
        }
        final Meta meta = new Meta();
        if (codesWhereStatusChanged != null) {
            meta.setNonTranslatableMessage(Integer.toString(codesWhereStatusChanged.size()));
        }
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    private boolean currentCodeSchemeIsTheLatestVersion(final CodeSchemeDTO codeScheme) {
        return codeScheme.getLastCodeschemeId() != null && codeScheme.getLastCodeschemeId().compareTo(codeScheme.getId()) == 0;
    }

    private void populateCodeSchemeListItem(final CodeSchemeDTO codeScheme,
                                            final CodeSchemeListItem item) {
        item.setEndDate(codeScheme.getEndDate());
        item.setPrefLabel(codeScheme.getPrefLabel());
        item.setStartDate(codeScheme.getStartDate());
        item.setStatus(codeScheme.getStatus());
        item.setUri(codeScheme.getUri());
    }

    @POST
    @Path("{codeRegistryCodeValue}/clone/codescheme/{originalCodeSchemeUuid}/newversionempty/{newversionempty}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Tag(name = "CodeScheme")
    public Response cloneCodeSchemeFromJson(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                            @Parameter(description = "CodeScheme UUID identifier", required = true, in = ParameterIn.PATH) @PathParam("originalCodeSchemeUuid") final String originalCodeSchemeUuid,
                                            @Parameter(description = "Is new version empty control boolean.", required = true, in = ParameterIn.PATH) @PathParam("newversionempty") final boolean createNewVersionAsEmpty,
                                            @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty,
                                            @RequestBody(description = "JSON payload for CodeScheme data.", required = true) final String jsonPayload) {
        CodeSchemeDTO codeSchemeWithUserChangesFromUi = codeSchemeParser.parseCodeSchemeFromJsonData(jsonPayload);
        if (createNewVersionAsEmpty) {
            codeSchemeWithUserChangesFromUi = cloningService.cloneCodeSchemeAsEmpty(codeSchemeWithUserChangesFromUi, codeRegistryCodeValue, originalCodeSchemeUuid);
        } else {
            codeSchemeWithUserChangesFromUi = cloningService.cloneCodeSchemeWithAllThePlumbing(codeSchemeWithUserChangesFromUi, codeRegistryCodeValue, originalCodeSchemeUuid);
        }
        return indexCodeschemeAndCodesAfterCloning(codeSchemeWithUserChangesFromUi, codeRegistryCodeValue, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/attachvariant/{variantCodeSchemeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Tag(name = "CodeScheme")
    public Response attachAVariantToCodeScheme(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                               @Parameter(description = "Variant CodeScheme UUID identifier", in = ParameterIn.PATH) @PathParam("variantCodeSchemeId") final String variantCodeSchemeId,
                                               @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty,
                                               @RequestBody(description = "JSON payload for the mother CodeScheme data.", required = true) final String jsonPayload) {
        CodeSchemeDTO motherCodeScheme = codeSchemeParser.parseCodeSchemeFromJsonData(jsonPayload);
        CodeSchemeDTO variantCodeScheme = codeSchemeService.findById(UUID.fromString(variantCodeSchemeId));

        boolean found = false;
        for (CodeSchemeListItem variant : motherCodeScheme.getVariantsOfThisCodeScheme()) {
            if (variant.getId().compareTo(variantCodeScheme.getId()) == 0) {
                found = true;
            }
        }
        if (!found) {
            motherCodeScheme.getVariantsOfThisCodeScheme().add(
                new CodeSchemeListItem(variantCodeScheme.getId(), variantCodeScheme.getPrefLabel(), variantCodeScheme.getCodeValue(),
                    variantCodeScheme.getUri(), variantCodeScheme.getStartDate(),
                    variantCodeScheme.getEndDate(), variantCodeScheme.getStatus()));
            codeSchemeService.updateCodeSchemeFromDto(codeRegistryCodeValue, motherCodeScheme);
        }

        found = false;
        for (CodeSchemeListItem variantMotha : variantCodeScheme.getVariantMothersOfThisCodeScheme()) {
            if (variantMotha.getId().compareTo(motherCodeScheme.getId()) == 0) {
                found = true;
            }
        }
        if (!found) {
            variantCodeScheme.getVariantMothersOfThisCodeScheme().add(
                new CodeSchemeListItem(motherCodeScheme.getId(), motherCodeScheme.getPrefLabel(), motherCodeScheme.getCodeValue(),
                    motherCodeScheme.getUri(), motherCodeScheme.getStartDate(),
                    motherCodeScheme.getEndDate(), motherCodeScheme.getStatus()));
        }
        codeSchemeService.updateCodeSchemeFromDto(variantCodeScheme.getCodeRegistry().getCodeValue(), variantCodeScheme);

        return indexCodeSchemesAfterVariantAttachmentOrDetachment(motherCodeScheme, variantCodeScheme, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/detachvariant/{idOfVariantToDetach}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Tag(name = "CodeScheme")
    public Response detachAVariantFromCodeScheme(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                 @Parameter(description = "ID of variant to detach", in = ParameterIn.PATH) @PathParam("idOfVariantToDetach") final String idOfVariantToDetach,
                                                 @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty,
                                                 @RequestBody(description = "JSON payload for the mother CodeScheme data.", required = true) final String jsonPayload) {
        CodeSchemeDTO motherCodeScheme = codeSchemeParser.parseCodeSchemeFromJsonData(jsonPayload);
        CodeSchemeDTO variantCodeScheme = codeSchemeService.findById(UUID.fromString(idOfVariantToDetach));
        motherCodeScheme.getVariantsOfThisCodeScheme().removeIf(item -> item.getId().compareTo(variantCodeScheme.getId()) == 0);
        codeSchemeService.updateCodeSchemeFromDto(codeRegistryCodeValue, motherCodeScheme);
        variantCodeScheme.getVariantMothersOfThisCodeScheme().removeIf(item -> item.getId().compareTo(motherCodeScheme.getId()) == 0);
        codeSchemeService.updateCodeSchemeFromDto(variantCodeScheme.getCodeRegistry().getCodeValue(), variantCodeScheme);
        return indexCodeSchemesAfterVariantAttachmentOrDetachment(motherCodeScheme, variantCodeScheme, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensions/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses and creates or updates extensions from JSON input.")
    @ApiResponse(responseCode = "200", description = "Extensions added or modified successfully.")
    @Tag(name = "Extension")
    public Response addOrUpdateExtensionsFromJson(@Parameter(description = "Format for input.") @QueryParam("format") @DefaultValue("json") final String format,
                                                  @Parameter(description = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                  @Parameter(description = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                                  @Parameter(description = "Auto-create members for all codes in the extensions codeschemes") @QueryParam("autoCreateMembers") @DefaultValue("false") final boolean autoCreateMembers,
                                                  @Parameter(description = "Pretty format JSON output.") @QueryParam("pretty") final String pretty,
                                                  @RequestBody(description = "JSON payload for Extension data.", required = true) final String jsonPayload) {
        return parseAndPersistExtensionsFromSource(codeRegistryCodeValue, codeSchemeCodeValue, FORMAT_JSON, null, jsonPayload, null, autoCreateMembers, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensions/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses and creates or updates extensions from CSV or Excel input data.")
    @ApiResponse(responseCode = "200", description = "Extensions added or modified successfully.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ExtensionDTO.class))))
    @Tag(name = "Extension")
    public Response addOrUpdateExtensionsFromFile(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                  @Parameter(description = "CodeScheme codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                                  @Parameter(description = "Format for input.", in = ParameterIn.QUERY) @QueryParam("format") @DefaultValue("csv") final String format,
                                                  @Parameter(description = "Auto-create members for all codes in the extensions codeschemes", in = ParameterIn.QUERY) @QueryParam("autoCreateMembers") @DefaultValue("false") final boolean autoCreateMembers,
                                                  @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty,
                                                  @Parameter(description = "Input-file for CSV or Excel import.", required = true, in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "binary", description = "Incoming file.")) @FormDataParam("file") final InputStream inputStream) {
        return parseAndPersistExtensionsFromSource(codeRegistryCodeValue, codeSchemeCodeValue, format, inputStream, null, EXCEL_SHEET_EXTENSIONS, false, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensions/{extensionCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Modifies single existing Extension.")
    @ApiResponse(responseCode = "200", description = "Extension modified successfully.")
    @Tag(name = "Extension")
    public Response updateExtension(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                    @Parameter(description = "CodeScheme codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                    @Parameter(description = "Extension codeValue", required = true, in = ParameterIn.PATH) @PathParam("extensionCodeValue") final String extensionCodeValue,
                                    @RequestBody(description = "JSON payload for Extension data.", required = true) final String jsonPayload) {
        final ExtensionDTO extension = extensionService.parseAndPersistExtensionFromJson(codeRegistryCodeValue, codeSchemeCodeValue, extensionCodeValue, jsonPayload, false);
        indexing.updateExtension(extension);
        indexing.updateMembers(memberService.findByExtensionId(extension.getId()));
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensions/{extensionCodeValue}/members/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses and creates or updates Members from JSON input.")
    @ApiResponse(responseCode = "200", description = "Members added or updated successfully.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MemberDTO.class))))
    @Tag(name = "Member")
    public Response addOrUpdateMembersFromJson(@Parameter(description = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                               @Parameter(description = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                               @Parameter(description = "Extension codeValue", required = true) @PathParam("extensionCodeValue") final String extensionCodeValue,
                                               @Parameter(description = "Format for input.", in = ParameterIn.QUERY) @QueryParam("format") @DefaultValue("json") final String format,
                                               @Parameter(description = "Pretty format JSON output.") @QueryParam("pretty") final String pretty,
                                               @RequestBody(description = "JSON payload for Member data.", required = true) final String jsonPayload) {
        return parseAndPersistMembersFromSource(codeRegistryCodeValue, codeSchemeCodeValue, extensionCodeValue, FORMAT_JSON, null, jsonPayload, null, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensions/{extensionCodeValue}/members/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses and creates or updates Members from CSV or Excel input data.")
    @ApiResponse(responseCode = "200", description = "Members modified successfully.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MemberDTO.class))))
    @Tag(name = "Member")
    public Response addOrUpdateMembersFromFile(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                               @Parameter(description = "CodeScheme codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                               @Parameter(description = "Extension codeValue", required = true, in = ParameterIn.PATH) @PathParam("extensionCodeValue") final String extensionCodeValue,
                                               @Parameter(description = "Format for input.", in = ParameterIn.QUERY) @QueryParam("format") @DefaultValue("csv") final String format,
                                               @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty,
                                               @Parameter(description = "Input-file for CSV or Excel import.", required = true, in = ParameterIn.QUERY, schema = @Schema(type = "string", format = "binary", description = "Incoming file.")) @FormDataParam("file") final InputStream inputStream) {
        return parseAndPersistMembersFromSource(codeRegistryCodeValue, codeSchemeCodeValue, extensionCodeValue, format, inputStream, null, EXCEL_SHEET_MEMBERS, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeId}/extensions/{extensionCodeValue}/members/createmissing/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Creates a Member for all Codes missing it, in all Codelists of a given Extension, from JSON input.")
    @ApiResponse(responseCode = "200", description = "Missing Members created successfully.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MemberDTO.class))))
    @Tag(name = "Member")
    public Response createMissingMembersFromJson(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                 @Parameter(description = "CodeScheme UUID", required = true, in = ParameterIn.PATH) @PathParam("codeSchemeId") final String codeSchemeId,
                                                 @Parameter(description = "Extension codeValue", required = true, in = ParameterIn.PATH) @PathParam("extensionCodeValue") final String extensionCodeValue,
                                                 @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty) {
        return createMissingMembersForExtension(UUID.fromString(codeSchemeId), extensionCodeValue, pretty);
    }

    @DELETE
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Deletes a single existing CodeScheme.")
    @ApiResponse(responseCode = "200", description = "CodeScheme deleted.")
    @ApiResponse(responseCode = "404", description = "CodeScheme not found.")
    @ApiResponse(responseCode = "406", description = "CodeScheme delete failed.")
    @Tag(name = "CodeScheme")
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    public Response deleteCodeScheme(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                     @Parameter(description = "CodeScheme codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue) {

        final CodeSchemeDTO existingCodeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);

        if (existingCodeScheme != null) {
            final UUID codeSchemeId = existingCodeScheme.getId();
            final Set<CodeDTO> codes = codeService.findByCodeSchemeId(codeSchemeId);
            final Set<ExternalReferenceDTO> externalReferences = externalReferenceService.findByParentCodeSchemeId(codeSchemeId);
            final Set<ExtensionDTO> extensions = extensionService.findByParentCodeSchemeId(codeSchemeId);
            final Set<MemberDTO> members = new HashSet<>();
            extensions.forEach(extensionDTO -> members.addAll(memberService.findByExtensionId(extensionDTO.getId())));
            final LinkedHashSet<CodeSchemeDTO> codeSchemeDTOsToIndex = new LinkedHashSet<>();
            final CodeSchemeDTO codeScheme = codeSchemeService.deleteCodeScheme(existingCodeScheme.getCodeRegistry().getCodeValue(), existingCodeScheme.getCodeValue(), codeSchemeDTOsToIndex);

            final Set<CodeSchemeDTO> affectedCodeSchemes = new LinkedHashSet<>();
            for (final CodeSchemeListItem item : codeScheme.getVariantsOfThisCodeScheme()) {
                affectedCodeSchemes.add(codeSchemeService.findById(item.getId()));
            }
            for (final CodeSchemeListItem item : codeScheme.getVariantMothersOfThisCodeScheme()) {
                affectedCodeSchemes.add(codeSchemeService.findById(item.getId()));
            }

            for (final CodeSchemeDTO dto : affectedCodeSchemes) {
                dto.getVariantsOfThisCodeScheme().removeIf(item -> item.getId().compareTo(codeScheme.getId()) == 0);
                dto.getVariantMothersOfThisCodeScheme().removeIf(item -> item.getId().compareTo(codeScheme.getId()) == 0);

                if (!YtiCollectionUtils.containsItemWithSameId(codeSchemeDTOsToIndex, dto)) {
                    codeSchemeDTOsToIndex.add(dto);
                }
            }
            indexing.updateCodeSchemes(codeSchemeDTOsToIndex);
            indexing.deleteCodeScheme(codeScheme);
            if (codes != null) {
                indexing.deleteCodes(codes);
            }
            if (externalReferences != null) {
                indexing.deleteExternalReferences(externalReferences);
            }
            indexing.deleteExtensions(extensions);
            indexing.deleteMembers(members);
        } else {
            return Response.status(404).build();
        }
        final Meta meta = new Meta();
        meta.setCode(200);
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    @DELETE
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensions/{extensionCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Deletes a single existing Extension.")
    @ApiResponse(responseCode = "200", description = "Extension deleted.")
    @ApiResponse(responseCode = "404", description = "Extension not found.")
    @Tag(name = "Extension")
    public Response deleteExtension(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                    @Parameter(description = "CodeScheme codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                    @Parameter(description = "Extension codeValue", required = true, in = ParameterIn.PATH) @PathParam("extensionCodeValue") final String extensionCodeValue) {

        final CodeSchemeDTO codeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme != null) {
            final ExtensionDTO existingExtension = extensionService.findByCodeSchemeIdAndCodeValue(codeScheme.getId(), extensionCodeValue);
            if (existingExtension != null) {
                final UUID extensionId = existingExtension.getId();
                final Set<MemberDTO> members = memberService.findByExtensionId(extensionId);
                final ExtensionDTO extension = extensionService.deleteExtension(extensionId);
                final CodeSchemeDTO updatedCodeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);

                codeSchemeService.populateAllVersionsToCodeSchemeDTO(updatedCodeScheme);
                indexing.updateCodeScheme(updatedCodeScheme);
                indexing.updateCodes(codeService.findByCodeSchemeId(updatedCodeScheme.getId()));

                indexing.deleteMembers(members);
                indexing.deleteExtension(extension);
                ExtensionDTO extensionToBeRemoved = null;
                for (final ExtensionDTO extensionDto : codeScheme.getExtensions()) {
                    if (extensionDto.getId().equals(extensionId)) {
                        extensionToBeRemoved = extensionDto;
                    }
                }
                if (extensionToBeRemoved != null) {
                    codeScheme.getExtensions().remove(extensionToBeRemoved);
                }
            } else {
                return Response.status(404).build();
            }
            final Meta meta = new Meta();
            final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
            return Response.ok(responseWrapper).build();
        } else {
            return Response.status(404).build();
        }
    }

    @DELETE
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensions/{extensionCodeValue}/members/{memberId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Deletes a single existing Member.")
    @ApiResponse(responseCode = "200", description = "Member deleted.")
    @ApiResponse(responseCode = "404", description = "Member not found.")
    @Tag(name = "Member")
    public Response deleteMember(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                 @Parameter(description = "CodeScheme codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                 @Parameter(description = "Extension codeValue", required = true, in = ParameterIn.PATH) @PathParam("extensionCodeValue") final String extensionCodeValue,
                                 @Parameter(description = "Member UUID", required = true, in = ParameterIn.PATH) @PathParam("memberId") final UUID memberId) {
        final MemberDTO existingMember = memberService.findById(memberId);
        if (existingMember != null) {
            final Set<MemberDTO> affectedMembers = new HashSet<>();
            final MemberDTO memberToBeDeleted = memberService.deleteMember(existingMember.getId(), affectedMembers);
            final CodeSchemeDTO updatedCodeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
            indexing.updateMembers(affectedMembers);
            indexing.updateCode(codeService.findById(existingMember.getCode().getId()));
            codeSchemeService.populateAllVersionsToCodeSchemeDTO(updatedCodeScheme);
            indexing.updateCodeScheme(updatedCodeScheme);
            indexing.updateCodes(codeService.findByCodeSchemeId(updatedCodeScheme.getId()));
            indexing.deleteMember(memberToBeDeleted);
        } else {
            return Response.status(404).build();
        }
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/codes/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses and creates or updates Codes from JSON input.")
    @ApiResponse(responseCode = "200", description = "Codes added or modified successfully.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CodeDTO.class))))
    @Tag(name = "Code")
    @JsonView({ Views.ExtendedCode.class, Views.Normal.class })
    public Response addOrUpdateCodesFromJson(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                             @Parameter(description = "CodeScheme codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                             @Parameter(description = "Format for input.", required = true) @QueryParam("format") @DefaultValue("json") final String format,
                                             @Parameter(description = "Code status before change.", in = ParameterIn.QUERY) @QueryParam("initialCodeStatus") final String initialCodeStatus,
                                             @Parameter(description = "Code status after change.", in = ParameterIn.QUERY) @QueryParam("endCodeStatus") final String endCodeStatus,
                                             @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty,
                                             @RequestBody(description = "JSON payload for Code data.") final String jsonPayload) {
        if (initialCodeStatus != null && !initialCodeStatus.isEmpty() && endCodeStatus != null && !endCodeStatus.isEmpty()) {
            return massChangeCodeStatuses(codeRegistryCodeValue, codeSchemeCodeValue, parseStatusFromString(initialCodeStatus), parseStatusFromString(endCodeStatus), pretty);
        }
        return parseAndPersistCodesFromSource(codeRegistryCodeValue, codeSchemeCodeValue, format, null, jsonPayload, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/codes/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Parses and creates or updates Codes from CSV or Excel input data.")
    @ApiResponse(responseCode = "200", description = "Codes added or modified successfully.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CodeDTO.class))))
    @Tag(name = "Code")
    @JsonView({ Views.ExtendedCode.class, Views.Normal.class })
    public Response addOrUpdateCodesFromFile(@Parameter(description = "Format for input.", required = true) @QueryParam("format") @DefaultValue("csv") final String format,
                                             @Parameter(description = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                             @Parameter(description = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                             @Parameter(description = "Pretty format JSON output.") @QueryParam("pretty") final String pretty,
                                             @Parameter(description = "Input-file for CSV or Excel import.", required = true, schema = @Schema(type = "string", format = "binary", description = "Incoming file.")) @FormDataParam("file") final InputStream inputStream) {

        return parseAndPersistCodesFromSource(codeRegistryCodeValue, codeSchemeCodeValue, format, inputStream, null, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/codes/{codeCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Modifies a single existing Code.")
    @ApiResponse(responseCode = "200", description = "Code modified successfully.")
    @Tag(name = "Code")
    public Response updateCode(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                               @Parameter(description = "CodeScheme codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                               @Parameter(description = "Code codeValue.", required = true, in = ParameterIn.PATH) @Encoded @PathParam("codeCodeValue") final String codeCodeValue,
                               @RequestBody(description = "JSON payload for Code data.", required = true) final String jsonPayload) {

        final Set<CodeDTO> codes = codeService.parseAndPersistCodeFromJson(codeRegistryCodeValue, codeSchemeCodeValue, urlDecodeCodeValue(codeCodeValue), jsonPayload);
        indexing.updateCodes(codes);
        codes.forEach(code -> {
            indexing.updateExternalReferences(code.getExternalReferences());
            indexing.updateMembers(memberService.findByCodeId(code.getId()));
            indexing.updateMembers(memberService.findByRelatedMemberCode(code));
        });
        CodeSchemeDTO parentCodeSchemeDTO = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        codeSchemeService.populateAllVersionsToCodeSchemeDTO(parentCodeSchemeDTO);
        indexing.updateCodeScheme(parentCodeSchemeDTO);
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    @DELETE
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/codes/{codeCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Deletes a single existing Code.")
    @ApiResponse(responseCode = "200", description = "Code deleted.")
    @ApiResponse(responseCode = "404", description = "Code not found.")
    @ApiResponse(responseCode = "406", description = "Code delete failed.")
    @Tag(name = "Code")
    public Response deleteCode(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                               @Parameter(description = "CodeScheme codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                               @Parameter(description = "Code codeValue.", required = true, in = ParameterIn.PATH) @Encoded @PathParam("codeCodeValue") final String codeCodeValue) {
        final CodeSchemeDTO codeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme != null) {
            final CodeDTO codeToBeDeleted = codeService.findByCodeRegistryCodeValueAndCodeSchemeCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue, urlDecodeCodeValue(codeCodeValue));
            if (codeToBeDeleted != null) {
                final Set<CodeDTO> affectedCodes = new HashSet<>();
                final CodeDTO code = codeService.deleteCode(codeRegistryCodeValue, codeSchemeCodeValue, urlDecodeCodeValue(codeCodeValue), affectedCodes);
                final CodeSchemeDTO updatedCodeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
                if (!affectedCodes.isEmpty()) {
                    indexing.updateCodes(affectedCodes);
                }
                final Set<MemberDTO> members = code.getMembers();
                if (members != null && !members.isEmpty()) {
                    indexing.deleteMembers(members);
                }
                indexing.deleteCode(code);
                codeSchemeService.populateAllVersionsToCodeSchemeDTO(updatedCodeScheme);
                indexing.updateCodeScheme(updatedCodeScheme);
                final Set<ExtensionDTO> extensions = extensionService.findByParentCodeSchemeId(updatedCodeScheme.getId());
                indexing.updateExtensions(extensions);
                final Meta meta = new Meta();
                meta.setCode(200);
                final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
                return Response.ok(responseWrapper).build();
            } else {
                return Response.status(404).build();
            }
        } else {
            return Response.status(404).build();
        }
    }

    @HEAD
    @Path("{codeRegistryCodeValue}")
    @Operation(summary = "Check if a CodeRegistry with a given codeValue exists.")
    @ApiResponse(responseCode = "200", description = "Found")
    @ApiResponse(responseCode = "404", description = "Not found")
    @Tag(name = "CodeRegistry")
    public Response checkForExistingCodeRegistry(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue) {
        final CodeRegistryDTO registry = this.codeRegistryService.findByCodeValue(codeRegistryCodeValue);
        if (registry == null) {
            return Response.status(404).build();
        }
        return Response.status(200).build();
    }

    @HEAD
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}")
    @Operation(summary = "Check if a CodeScheme with a given codeValue exists.")
    @ApiResponse(responseCode = "200", description = "Found")
    @ApiResponse(responseCode = "404", description = "Not found")
    @Tag(name = "CodeScheme")
    public Response checkForExistingCodeScheme(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                               @Parameter(description = "CodeScheme codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue) {
        final CodeSchemeDTO scheme = this.codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (scheme == null) {
            return Response.status(404).build();
        }
        return Response.status(200).build();
    }

    @HEAD
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/codes/{codeCodeValue}")
    @Operation(summary = "Check if a Code with the given codeValue exists.")
    @ApiResponse(responseCode = "200", description = "Found")
    @ApiResponse(responseCode = "404", description = "Not found")
    @Tag(name = "Code")
    public Response checkForExistingCode(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                         @Parameter(description = "CodeScheme codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                         @Parameter(description = "Code codeValue.", required = true, in = ParameterIn.PATH) @Encoded @PathParam("codeCodeValue") final String codeCodeValue) {
        final CodeDTO code = this.codeService.findByCodeRegistryCodeValueAndCodeSchemeCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue, urlDecodeCodeValue(codeCodeValue));
        if (code == null) {
            return Response.status(404).build();
        }
        return Response.status(200).build();
    }

    @HEAD
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensions/{extensionCodeValue}")
    @Operation(summary = "Check if an Extension with the given codeValue exists.")
    @ApiResponse(responseCode = "200", description = "Found")
    @ApiResponse(responseCode = "404", description = "Not found")
    @Tag(name = "Extension")
    public Response checkForExistingExtension(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                              @Parameter(description = "CodeScheme codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                              @Parameter(description = "Extension codeValue.", required = true, in = ParameterIn.PATH) @PathParam("extensionCodeValue") final String extensionCodeValue) {
        final CodeSchemeDTO codeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme != null) {
            final ExtensionDTO extension = this.extensionService.findByCodeSchemeIdAndCodeValue(codeScheme.getId(), extensionCodeValue);
            if (extension == null) {
                return Response.status(404).build();
            }
            return Response.status(200).build();
        } else {
            return Response.status(404).build();
        }
    }

    @HEAD
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/externalreferences/")
    @Operation(summary = "Check if an external reference with the given href exists.")
    @ApiResponse(responseCode = "200", description = "Found")
    @ApiResponse(responseCode = "404", description = "Not found")
    @Tag(name = "ExternalReference")
    public Response checkForExistingExternalReference(@Parameter(description = "CodeRegistry codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                      @Parameter(description = "CodeScheme codeValue", required = true, in = ParameterIn.PATH) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                                      @Parameter(description = "Extension codeValue.", required = true, in = ParameterIn.PATH) @QueryParam("href") final String href) {
        final CodeSchemeDTO codeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme != null) {
            final String decodedHref = urlDecodeString(href);
            final ExternalReferenceDTO externalReference = this.externalReferenceService.findByParentCodeSchemeIdAndHref(codeScheme.getId(), decodedHref);
            if (externalReference == null) {
                return Response.status(404).build();
            }
            return Response.status(200).build();
        } else {
            return Response.status(404).build();
        }
    }

    private Response parseAndPersistCodeRegistriesFromSource(final String format,
                                                             final InputStream inputStream,
                                                             final String jsonPayload,
                                                             final String pretty) {
        final Set<CodeRegistryDTO> codeRegistries = codeRegistryService.parseAndPersistCodeRegistriesFromSourceData(format, inputStream, jsonPayload);
        indexing.updateCodeRegistries(codeRegistries);
        codeRegistries.forEach(codeRegistry -> {
            final Set<CodeSchemeDTO> codeSchemes = codeSchemeService.findByCodeRegistryCodeValue(codeRegistry.getCodeValue());
            indexing.updateCodeSchemes(codeSchemes);
            codeSchemes.forEach(codeScheme -> {
                indexing.updateCodes(codeService.findByCodeSchemeId(codeScheme.getId()));
                indexing.updateExternalReferences(externalReferenceService.findByParentCodeSchemeId(codeScheme.getId()));
                final Set<ExtensionDTO> extensions = extensionService.findByParentCodeSchemeId(codeScheme.getId());
                if (extensions != null && !extensions.isEmpty()) {
                    indexing.updateExtensions(extensions);
                    for (final ExtensionDTO extension : extensions) {
                        indexing.updateMembers(memberService.findByExtensionId(extension.getId()));
                    }
                }
            });
        });
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODEREGISTRY, null), pretty));
        final ResponseWrapper<CodeRegistryDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("CodeRegistries added or modified: " + codeRegistries.size());
        meta.setCode(200);
        responseWrapper.setResults(codeRegistries);
        return Response.ok(responseWrapper).build();
    }

    private Response indexCodeSchemesAfterVariantAttachmentOrDetachment(final CodeSchemeDTO motherCodeScheme,
                                                                        final CodeSchemeDTO variantCodeScheme,
                                                                        final String pretty) {
        final HashSet<CodeSchemeDTO> codeSchemes = new HashSet<>();
        codeSchemeService.populateAllVersionsToCodeSchemeDTO(variantCodeScheme);
        codeSchemes.add(variantCodeScheme);
        codeSchemes.add(motherCodeScheme);
        indexing.updateCodeSchemes(codeSchemes);
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODESCHEME, "codeRegistry,code,extension,valueType,member,memberValue"), pretty));
        final ResponseWrapper<CodeSchemeDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("A Variant was attached to a CodeScheme.");
        meta.setCode(200);
        responseWrapper.setResults(codeSchemes);
        return Response.ok(responseWrapper).build();
    }

    private Response indexCodeschemeAndCodesAfterCloning(final CodeSchemeDTO codeScheme,
                                                         final String codeRegistryCodeValue,
                                                         final String pretty) {
        final HashSet<CodeSchemeDTO> codeSchemes = new HashSet<>();
        codeSchemeService.populateAllVersionsToCodeSchemeDTO(codeScheme);
        codeSchemes.add(codeScheme);
        LinkedHashSet<CodeSchemeDTO> previousVersions = new LinkedHashSet<>();
        previousVersions = codeSchemeService.getPreviousVersions(codeScheme.getPrevCodeschemeId(), previousVersions);
        for (CodeSchemeDTO prevVersion : previousVersions) {
            codeSchemeService.populateAllVersionsToCodeSchemeDTO(prevVersion);
        }
        codeSchemes.addAll(previousVersions);
        indexing.updateCodeSchemes(codeSchemes);
        indexing.updateCodeRegistry(codeRegistryService.findByCodeValue(codeRegistryCodeValue));
        indexing.updateCodes(codeService.findByCodeSchemeId(codeScheme.getId()));
        indexing.updateExternalReferences(externalReferenceService.findByParentCodeSchemeId(codeScheme.getId()));
        final Set<ExtensionDTO> extensions = extensionService.findByParentCodeSchemeId(codeScheme.getId());
        if (extensions != null && !extensions.isEmpty()) {
            indexing.updateExtensions(extensions);
            for (final ExtensionDTO extension : extensions) {
                indexing.updateMembers(memberService.findByExtensionId(extension.getId()));
            }
        }
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODESCHEME, "codeRegistry,code,extension,valueType,member,memberValue"), pretty));
        final ResponseWrapper<CodeSchemeDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("A CodeScheme was cloned.");
        meta.setCode(200);
        responseWrapper.setResults(codeSchemes);
        return Response.ok(responseWrapper).build();
    }

    private Response parseAndPersistCodeSchemesFromSource(final String codeRegistryCodeValue,
                                                          final String format,
                                                          final InputStream inputStream,
                                                          final String jsonPayload,
                                                          final boolean userIsCreatingANewVersionOfACodeScheme,
                                                          final String originalCodeSchemeId,
                                                          final boolean updatingExistingCodeScheme,
                                                          final String pretty) {
        final Set<CodeSchemeDTO> codeSchemes = codeSchemeService.parseAndPersistCodeSchemesFromSourceData(codeRegistryCodeValue, format, inputStream, jsonPayload, userIsCreatingANewVersionOfACodeScheme, originalCodeSchemeId, updatingExistingCodeScheme);
        for (CodeSchemeDTO codeScheme : codeSchemes) {
            if (codeScheme.getLastCodeschemeId() != null) {
                codeSchemeService.populateAllVersionsToCodeSchemeDTO(codeScheme);
            }
        }
        indexing.updateCodeSchemes(codeSchemes);
        indexing.updateCodeRegistry(codeRegistryService.findByCodeValue(codeRegistryCodeValue));
        for (final CodeSchemeDTO codeScheme : codeSchemes) {
            indexing.updateCodes(codeService.findByCodeSchemeId(codeScheme.getId()));
            indexing.updateExternalReferences(externalReferenceService.findByParentCodeSchemeId(codeScheme.getId()));
            final Set<ExtensionDTO> extensions = extensionService.findByParentCodeSchemeId(codeScheme.getId());
            if (extensions != null && !extensions.isEmpty()) {
                indexing.updateExtensions(extensions);
                for (final ExtensionDTO extension : extensions) {
                    indexing.updateMembers(memberService.findByExtensionId(extension.getId()));
                }
            }
            final Set<ExtensionDTO> relatedExtensions = extensionService.findByCodeSchemeId(codeScheme.getId());
            if (relatedExtensions != null && !relatedExtensions.isEmpty()) {
                indexing.updateExtensions(relatedExtensions);
                for (final ExtensionDTO extension : relatedExtensions) {
                    indexing.updateMembers(memberService.findByExtensionId(extension.getId()));
                }
            }
        }
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODESCHEME, "codeRegistry,code,extension,valueType,member,memberValue"), pretty));
        final ResponseWrapper<CodeSchemeDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("CodeSchemes added or modified: " + codeSchemes.size());
        meta.setCode(200);
        responseWrapper.setResults(codeSchemes);
        return Response.ok(responseWrapper).build();
    }

    private Response parseAndPersistExtensionsFromSource(final String codeRegistryCodeValue,
                                                         final String codeSchemeCodeValue,
                                                         final String format,
                                                         final InputStream inputStream,
                                                         final String jsonPayload,
                                                         final String sheetName,
                                                         final boolean autoCreateMembers,
                                                         final String pretty) {
        final Set<ExtensionDTO> extensions = extensionService.parseAndPersistExtensionsFromSourceData(codeRegistryCodeValue, codeSchemeCodeValue, format, inputStream, jsonPayload, sheetName, autoCreateMembers);
        indexing.updateExtensions(extensions);
        if (!extensions.isEmpty()) {
            final Set<CodeSchemeDTO> codeSchemes = new HashSet<>();
            extensions.forEach(extension -> {
                final UUID parentCodeSchemeId = extension.getParentCodeScheme().getId();
                final CodeSchemeDTO codeScheme = codeSchemeService.findById(parentCodeSchemeId);
                codeSchemeService.populateAllVersionsToCodeSchemeDTO(codeScheme);
                codeSchemes.add(codeScheme);
                final Set<MemberDTO> members = memberService.findByExtensionId(extension.getId());
                indexing.updateMembers(members);
                indexing.updateCodes(codeService.findByCodeSchemeId(parentCodeSchemeId));
            });
            indexing.updateCodeSchemes(codeSchemes);
        }
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(FILTER_NAME_EXTENSION, "member,memberValue,valueType,propertyType,codeScheme,code,codeRegistry"), pretty));
        final ResponseWrapper<ExtensionDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("Extensions added or modified: " + extensions.size());
        meta.setCode(200);
        responseWrapper.setResults(extensions);
        return Response.ok(responseWrapper).build();
    }

    private Response createMissingMembersForExtension(final UUID codeSchemeId,
                                                      final String extensionCodeValue,
                                                      final String pretty) {
        final ExtensionDTO extension = extensionService.findByCodeSchemeIdAndCodeValue(codeSchemeId, extensionCodeValue);
        final Set<MemberDTO> createdMembers = memberService.createMissingMembersForAllCodesOfAllCodelistsOfAnExtension(extension);
        final CodeSchemeDTO codeScheme = codeSchemeService.findById(codeSchemeId);
        final Set<CodeDTO> codes = codeService.findByCodeSchemeId(codeScheme.getId());
        codeSchemeService.populateAllVersionsToCodeSchemeDTO(codeScheme);
        indexing.updateCodeScheme(codeScheme);
        indexing.updateExtension(extension);
        indexing.updateCodes(codes);
        indexing.updateMembers(createdMembers);
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(FILTER_NAME_MEMBER, "extension,codeScheme,code,codeRegistry,propertyType,valueType,memberValue"), pretty));
        final ResponseWrapper<MemberDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("Members created: " + createdMembers.size());
        meta.setCode(200);
        responseWrapper.setResults(createdMembers);
        return Response.ok(responseWrapper).build();
    }

    private Response parseAndPersistMembersFromSource(final String codeRegistryCodeValue,
                                                      final String codeSchemeCodeValue,
                                                      final String extensionCodeValue,
                                                      final String format,
                                                      final InputStream inputStream,
                                                      final String jsonPayload,
                                                      final String sheetName,
                                                      final String pretty) {
        final CodeSchemeDTO codeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme != null) {
            final Set<MemberDTO> members = memberService.parseAndPersistMembersFromSourceData(codeRegistryCodeValue, codeSchemeCodeValue, extensionCodeValue, format, inputStream, jsonPayload, sheetName);
            indexing.updateMembers(members);
            final ExtensionDTO extension = extensionService.findByCodeSchemeIdAndCodeValue(codeScheme.getId(), extensionCodeValue);
            if (extension == null) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EXTENSION_NOT_FOUND));
            } else {
                indexing.updateExtension(extension);
            }
            if (CODE_EXTENSION.equalsIgnoreCase(extension.getPropertyType().getContext())) {
                final Set<CodeDTO> codes = new HashSet<>();
                members.forEach(member -> codes.add(codeService.findById(member.getCode().getId())));
                indexing.updateCodes(codes);
            }
            indexing.updateCodeScheme(codeSchemeService.findById(codeScheme.getId()));
            final Meta meta = new Meta();
            ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(FILTER_NAME_MEMBER, "extension,codeScheme,code,codeRegistry,propertyType,valueType,memberValue"), pretty));
            final ResponseWrapper<MemberDTO> responseWrapper = new ResponseWrapper<>(meta);
            meta.setMessage("Member added or modified: " + members.size());
            meta.setCode(200);
            responseWrapper.setResults(members);
            return Response.ok(responseWrapper).build();
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODESCHEME_NOT_FOUND));
        }
    }

    private Response massChangeCodeStatuses(final String codeRegistryCodeValue,
                                            final String codeSchemeCodeValue,
                                            final String initialCodeStatus,
                                            final String endCodeStatus,
                                            final String pretty) {
        final Set<CodeDTO> codes = codeService.massChangeCodeStatuses(codeRegistryCodeValue, codeSchemeCodeValue, initialCodeStatus, endCodeStatus, false);
        return constructCodeResponse(codeRegistryCodeValue, codeSchemeCodeValue, codes, pretty);
    }

    private Response parseAndPersistCodesFromSource(final String codeRegistryCodeValue,
                                                    final String codeSchemeCodeValue,
                                                    final String format,
                                                    final InputStream inputStream,
                                                    final String jsonPayload,
                                                    final String pretty) {
        final Set<CodeDTO> codes = codeService.parseAndPersistCodesFromSourceData(codeRegistryCodeValue, codeSchemeCodeValue, format, inputStream, jsonPayload);
        return constructCodeResponse(codeRegistryCodeValue, codeSchemeCodeValue, codes, pretty);
    }

    private Response constructCodeResponse(final String codeRegistryCodeValue,
                                           final String codeSchemeCodeValue,
                                           final Set<CodeDTO> codes,
                                           final String pretty) {
        final CodeSchemeDTO codeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        indexing.updateCodes(codes);
        codes.forEach(code -> indexing.updateMembers(memberService.findByCodeId(code.getId())));
        codeSchemeService.populateAllVersionsToCodeSchemeDTO(codeScheme);
        indexing.updateCodeScheme(codeScheme);
        indexing.updateExternalReferences(externalReferenceService.findByParentCodeSchemeId(codeScheme.getId()));
        indexing.updateCodeRegistry(codeRegistryService.findByCodeValue(codeRegistryCodeValue));
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODE, "codeRegistry,codeScheme,extension,valueType,member,memberValue"), pretty));
        final ResponseWrapper<CodeDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("Codes added or modified: " + codes.size());
        meta.setCode(200);
        responseWrapper.setResults(codes);
        return Response.ok(responseWrapper).build();

    }

    private String parseStatusFromString(final String status) {
        try {
            return Status.valueOf(status.replaceAll(" ", "").trim().toUpperCase()).toString();
        } catch (final Exception e) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_STATUS_NOT_VALID, status));
        }
    }
}
