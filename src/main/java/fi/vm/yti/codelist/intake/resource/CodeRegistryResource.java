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
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;

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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Component
@Path("/v1/coderegistries")
@Api(value = "coderegistries")
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
    @ApiOperation(value = "Parses and creates or updates CodeRegistries from JSON input.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "CodeRegistries added or modified successfully.")
    })
    public Response addOrUpdateCodeRegistriesFromJson(@ApiParam(value = "JSON playload for CodeRegistry data.", required = true) final String jsonPayload,
                                                      @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
        return parseAndPersistCodeRegistriesFromSource(FORMAT_JSON, null, jsonPayload, pretty);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses and creates or updates CodeRegistries from CSV or Excel input data.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "CodeRegistries added or modified successfully.")
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", dataType = "file", paramType = "formData")
    })
    public Response addOrUpdateCodeRegistriesFromFile(@ApiParam(value = "Format for input.", required = true) @QueryParam("format") @DefaultValue("json") final String format,
                                                      @ApiParam(value = "Input-file for CSV or Excel import.", hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream,
                                                      @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
        return parseAndPersistCodeRegistriesFromSource(format, inputStream, null, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Modifies a single existing CodeRegistry.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "CodeRegistry modified successfully.")
    })
    public Response updateCodeRegistry(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                       @ApiParam(value = "JSON playload for Code data.") final String jsonPayload) {

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
    @ApiOperation(value = "Deletes a single existing CodeRegistry.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "CodeRegistry deleted."),
        @ApiResponse(code = 404, message = "CodeRegistry not found."),
        @ApiResponse(code = 406, message = "CodeRegistry has code lists, cannot delete.")
    })
    public Response deleteCodeRegistry(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue) {

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
    @ApiOperation(value = "Parses and creates or updates CodeSchemes from JSON input.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "CodeSchemes added or modified successfully.")
    })
    public Response addOrUpdateCodeSchemesFromJson(@ApiParam(value = "Format for input.") @QueryParam("format") @DefaultValue("json") final String format,
                                                   @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                   @ApiParam(value = "JSON playload for CodeScheme data.", required = true) final String jsonPayload,
                                                   @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
        return parseAndPersistCodeSchemesFromSource(codeRegistryCodeValue, FORMAT_JSON, null, jsonPayload, false, "", false, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses and creates or updates CodeSchemes from Excel or CSV file.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "CodeSchemes added or modified successfully.")
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", dataType = "file", paramType = "formData")
    })
    public Response addOrUpdateCodeSchemesFromFile(@ApiParam(value = "Format for input.") @QueryParam("format") @DefaultValue("csv") final String format,
                                                   @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                   @ApiParam(value = "New Codelist version") @QueryParam("newVersionOfCodeScheme") @DefaultValue("false") final boolean userIsCreatingANewVersionOfACodeScheme,
                                                   @ApiParam(value = "True if user is updating a particular code list with a file from the code list page menu") @QueryParam("updatingExistingCodeScheme") @DefaultValue("false") final boolean updatingExistingCodeScheme,
                                                   @ApiParam(value = "If creating new version, id of previous code list version") @QueryParam("originalCodeSchemeId") final String originalCodeSchemeId,
                                                   @ApiParam(value = "Input-file for CSV or Excel import.", hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream,
                                                   @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
        return parseAndPersistCodeSchemesFromSource(codeRegistryCodeValue, format, inputStream, null, userIsCreatingANewVersionOfACodeScheme, originalCodeSchemeId, updatingExistingCodeScheme, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/validate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Validates CSV or Excel input data in case of new Code list version creation directly from file.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "The file is valid or not.")
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", dataType = "file", paramType = "formData")
    })
    public Response canANewVersionOfACodeSchemeBeCreatedFromTheIncomingFileDirectly(@ApiParam(value = "Format for input.") @QueryParam("format") @DefaultValue("csv") final String format,
                                                                                    @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                                                    @ApiParam(value = "Input-file for CSV or Excel import.", hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream) {
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
    @ApiOperation(value = "Modifies single existing CodeScheme.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "CodeScheme modified successfully.")
    })
    public Response updateCodeScheme(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                     @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                     @ApiParam(value = "Control query parameter that changes code status according to codeScheme status change.") @QueryParam("changeCodeStatuses") final String changeCodeStatuses,
                                     @ApiParam(value = "JSON playload for CodeScheme data.") final String jsonPayload) {

        final CodeSchemeDTO originalCodeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        final CodeSchemeDTO newCodeScheme = codeSchemeParser.parseCodeSchemeFromJsonData(jsonPayload);
        String initialCodeStatus = originalCodeScheme.getStatus();
        String endCodeStatus = newCodeScheme.getStatus();
        Set<CodeDTO> codesWhereStatusChanged = null;

        if (changeCodeStatuses != null && changeCodeStatuses.equals("true") && !authorizationManager.isSuperUser()) {
            ValidationUtils.validateCodeStatusTransitions(initialCodeStatus, endCodeStatus);
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

            if (changeCodeStatuses != null && changeCodeStatuses.equals("true")) {
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
    public Response cloneCodeSchemeFromJson(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                            @PathParam("originalCodeSchemeUuid") final String originalCodeSchemeUuid,
                                            @PathParam("newversionempty") final boolean createNewVersionAsEmpty,
                                            @ApiParam(value = "JSON playload for CodeScheme data.", required = true) final String jsonPayload,
                                            @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
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
    public Response attachAVariantToCodeScheme(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                               @PathParam("variantCodeSchemeId") final String variantCodeSchemeId,
                                               @ApiParam(value = "JSON playload for the mother CodeScheme data.", required = true) final String jsonPayload,
                                               @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
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
    public Response detachAVariantFromCodeScheme(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                 @PathParam("idOfVariantToDetach") final String idOfVariantToDetach,
                                                 @ApiParam(value = "JSON playload for the mother CodeScheme data.", required = true) final String jsonPayload,
                                                 @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
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
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses and creates or updates extensions from CSV or Excel input data.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Extensions added or modified successfully.")
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", dataType = "file", paramType = "formData")
    })
    public Response addOrUpdateExtensionsFromFile(@ApiParam(value = "Format for input.") @QueryParam("format") @DefaultValue("csv") final String format,
                                                  @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                  @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                                  @ApiParam(value = "Input-file for CSV or Excel import.", hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream,
                                                  @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
        return parseAndPersistExtensionsFromSource(codeRegistryCodeValue, codeSchemeCodeValue, format, inputStream, null, EXCEL_SHEET_EXTENSIONS, false, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensions/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses and creates or updates extensions from JSON input.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Extensions added or modified successfully.")
    })
    public Response addOrUpdateExtensionsFromJson(@ApiParam(value = "Format for input.") @QueryParam("format") @DefaultValue("json") final String format,
                                                  @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                  @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                                  @ApiParam(value = "Auto-create members for all codes in the extensions codeschemes") @QueryParam("autoCreateMembers") @DefaultValue("false") final boolean autoCreateMembers,
                                                  @ApiParam(value = "JSON playload for Extension data.", required = true) final String jsonPayload,
                                                  @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
        return parseAndPersistExtensionsFromSource(codeRegistryCodeValue, codeSchemeCodeValue, FORMAT_JSON, null, jsonPayload, null, autoCreateMembers, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensions/{extensionCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Modifies single existing Extension.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Extension modified successfully.")
    })
    public Response updateExtension(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                    @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                    @ApiParam(value = "Extension codeValue", required = true) @PathParam("extensionCodeValue") final String extensionCodeValue,
                                    @ApiParam(value = "JSON playload for Extension data.") final String jsonPayload) {

        final ExtensionDTO extension = extensionService.parseAndPersistExtensionFromJson(codeRegistryCodeValue, codeSchemeCodeValue, extensionCodeValue, jsonPayload, false);
        indexing.updateExtension(extension);
        indexing.updateMembers(memberService.findByExtensionId(extension.getId()));
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensions/{extensionCodeValue}/members/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses and creates or updates Members from CSV or Excel input data.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Members modified successfully.")
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", dataType = "file", paramType = "formData")
    })
    public Response addOrUpdateMembersFromFile(@ApiParam(value = "Format for input.") @QueryParam("format") @DefaultValue("csv") final String format,
                                               @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                               @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                               @ApiParam(value = "Extension codeValue", required = true) @PathParam("extensionCodeValue") final String extensionCodeValue,
                                               @ApiParam(value = "Input-file for CSV or Excel import.", hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream,
                                               @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
        return parseAndPersistMembersFromSource(codeRegistryCodeValue, codeSchemeCodeValue, extensionCodeValue, format, inputStream, null, EXCEL_SHEET_MEMBERS, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensions/{extensionCodeValue}/members/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses and creates or updates Members from JSON input.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Members modified successfully.")
    })
    public Response addOrUpdateMembersFromJson(@ApiParam(value = "Format for input.") @QueryParam("format") @DefaultValue("json") final String format,
                                               @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                               @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                               @ApiParam(value = "Extension codeValue", required = true) @PathParam("extensionCodeValue") final String extensionCodeValue,
                                               @ApiParam(value = "JSON playload for Member data.", required = true) final String jsonPayload,
                                               @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
        return parseAndPersistMembersFromSource(codeRegistryCodeValue, codeSchemeCodeValue, extensionCodeValue, FORMAT_JSON, null, jsonPayload, null, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeId}/extensions/{extensionCodeValue}/members/createmissing/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Creates a Member for all Codes missing it, in all Codelists of a given Extension, from JSON input.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Missing Members created successfully.")
    })
    public Response createMissingMembersFromJson(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                 @ApiParam(value = "CodeScheme UUID", required = true) @PathParam("codeSchemeId") final String codeSchemeId,
                                                 @ApiParam(value = "Extension codeValue", required = true) @PathParam("extensionCodeValue") final String extensionCodeValue,
                                                 @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
        return createMissingMembersForExtension(UUID.fromString(codeSchemeId), extensionCodeValue, pretty);
    }

    @DELETE
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single existing CodeScheme.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "CodeScheme deleted."),
        @ApiResponse(code = 404, message = "CodeScheme not found."),
        @ApiResponse(code = 406, message = "CodeScheme delete failed.")
    })
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    public Response deleteCodeScheme(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                     @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue) {

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
    @ApiOperation(value = "Deletes a single existing Extension.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Extension deleted."),
        @ApiResponse(code = 404, message = "Extension not found.")
    })
    public Response deleteExtension(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                    @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                    @ApiParam(value = "Extension codeValue", required = true) @PathParam("extensionCodeValue") final String extensionCodeValue) {

        final CodeSchemeDTO codeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme != null) {
            final ExtensionDTO existingExtension = extensionService.findByCodeSchemeIdAndCodeValue(codeScheme.getId(), extensionCodeValue);
            if (existingExtension != null) {
                final UUID extensionId = existingExtension.getId();
                final Set<MemberDTO> members = memberService.findByExtensionId(extensionId);
                final ExtensionDTO extension = extensionService.deleteExtension(extensionId);
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
                codeSchemeService.populateAllVersionsToCodeSchemeDTO(codeScheme);
                indexing.updateCodeScheme(codeScheme);
                indexing.updateCodes(codeService.findByCodeSchemeId(codeScheme.getId()));
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
    @ApiOperation(value = "Deletes a single existing Member.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Member deleted."),
        @ApiResponse(code = 404, message = "Member not found.")
    })
    public Response deleteMember(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                 @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                 @ApiParam(value = "Extension codeValue", required = true) @PathParam("extensionCodeValue") final String extensionCodeValue,
                                 @ApiParam(value = "Member UUID", required = true) @PathParam("memberId") final UUID memberId) {
        final MemberDTO existingMember = memberService.findById(memberId);
        if (existingMember != null) {
            final Set<MemberDTO> affectedMembers = new HashSet<>();
            final MemberDTO memberToBeDeleted = memberService.deleteMember(existingMember.getId(), affectedMembers);
            indexing.updateMembers(affectedMembers);
            indexing.updateCode(codeService.findById(existingMember.getCode().getId()));
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
    @ApiOperation(value = "Parses and creates or updates Codes from JSON input.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Codes added or modified successfully.")
    })
    @JsonView({ Views.ExtendedCode.class, Views.Normal.class })
    public Response addOrUpdateCodesFromJson(@ApiParam(value = "Format for input.", required = true) @QueryParam("format") @DefaultValue("json") final String format,
                                             @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                             @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                             @ApiParam(value = "JSON playload for Code data.", required = true) final String jsonPayload,
                                             @ApiParam(value = "Code status before change.") @QueryParam("initialCodeStatus") final String initialCodeStatus,
                                             @ApiParam(value = "Code status after change.") @QueryParam("endCodeStatus") final String endCodeStatus,
                                             @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
        if (initialCodeStatus != null && !initialCodeStatus.isEmpty() && endCodeStatus != null && !endCodeStatus.isEmpty()) {
            return massChangeCodeStatuses(codeRegistryCodeValue, codeSchemeCodeValue, parseStatusFromString(initialCodeStatus), parseStatusFromString(endCodeStatus), pretty);
        }
        return parseAndPersistCodesFromSource(codeRegistryCodeValue, codeSchemeCodeValue, format, null, jsonPayload, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/codes/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses and creates or updates Codes from CSV or Excel input data.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Codes added or modified successfully.")
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", dataType = "file", paramType = "formData")
    })
    @JsonView({ Views.ExtendedCode.class, Views.Normal.class })
    public Response addOrUpdateCodesFromFile(@ApiParam(value = "Format for input.", required = true) @QueryParam("format") @DefaultValue("csv") final String format,
                                             @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                             @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                             @ApiParam(value = "Input-file for CSV or Excel import.", hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream,
                                             @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {

        return parseAndPersistCodesFromSource(codeRegistryCodeValue, codeSchemeCodeValue, format, inputStream, null, pretty);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/codes/{codeCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Modifies a single existing Code.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Code modified successfully.")
    })
    public Response updateCode(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                               @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                               @ApiParam(value = "Code codeValue.", required = true) @PathParam("codeCodeValue") final String codeCodeValue,
                               @ApiParam(value = "JSON playload for Code data.") final String jsonPayload) {

        final Set<CodeDTO> codes = codeService.parseAndPersistCodeFromJson(codeRegistryCodeValue, codeSchemeCodeValue, codeCodeValue, jsonPayload);
        indexing.updateCodes(codes);
        codes.forEach(code -> {
            indexing.updateExternalReferences(code.getExternalReferences());
            indexing.updateMembers(memberService.findByCodeId(code.getId()));
            indexing.updateMembers(memberService.findByRelatedMemberCode(code));
        });
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    @DELETE
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/codes/{codeCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single existing Code.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Code deleted."),
        @ApiResponse(code = 404, message = "Code not found."),
        @ApiResponse(code = 406, message = "Code delete failed.")
    })
    public Response deleteCode(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                               @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                               @ApiParam(value = "Code codeValue.", required = true) @PathParam("codeCodeValue") final String codeCodeValue) {
        final CodeSchemeDTO codeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme != null) {
            final CodeDTO codeToBeDeleted = codeService.findByCodeRegistryCodeValueAndCodeSchemeCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue, codeCodeValue);
            if (codeToBeDeleted != null) {
                final Set<CodeDTO> affectedCodes = new HashSet<>();
                final CodeDTO code = codeService.deleteCode(codeRegistryCodeValue, codeSchemeCodeValue, codeCodeValue, affectedCodes);
                if (!affectedCodes.isEmpty()) {
                    indexing.updateCodes(affectedCodes);
                }
                final Set<MemberDTO> members = code.getMembers();
                if (members != null && !members.isEmpty()) {
                    indexing.deleteMembers(members);
                }
                indexing.deleteCode(code);
                codeSchemeService.populateAllVersionsToCodeSchemeDTO(codeScheme);
                indexing.updateCodeScheme(codeScheme);
                final Set<ExtensionDTO> extensions = extensionService.findByParentCodeSchemeId(codeScheme.getId());
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
    @ApiOperation(value = "Check if a CodeRegistry with a given codeValue exists.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Found"),
        @ApiResponse(code = 404, message = "Not found")
    })
    public Response checkForExistingCodeRegistry(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue) {
        final CodeRegistryDTO registry = this.codeRegistryService.findByCodeValue(codeRegistryCodeValue);
        if (registry == null) {
            return Response.status(404).build();
        }
        return Response.status(200).build();
    }

    @HEAD
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}")
    @ApiOperation(value = "Check if a CodeScheme with a given codeValue exists.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Found"),
        @ApiResponse(code = 404, message = "Not found")
    })
    public Response checkForExistingCodeScheme(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                               @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue) {
        final CodeSchemeDTO scheme = this.codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (scheme == null) {
            return Response.status(404).build();
        }
        return Response.status(200).build();
    }

    @HEAD
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/codes/{codeCodeValue}")
    @ApiOperation(value = "Check if a Code with the given codeValue exists.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Found"),
        @ApiResponse(code = 404, message = "Not found")
    })
    public Response checkForExistingCode(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                         @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                         @ApiParam(value = "Code codeValue.", required = true) @PathParam("codeCodeValue") final String codeCodeValue) {
        final CodeDTO code = this.codeService.findByCodeRegistryCodeValueAndCodeSchemeCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue, codeCodeValue);
        if (code == null) {
            return Response.status(404).build();
        }
        return Response.status(200).build();
    }

    @HEAD
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensions/{extensionCodeValue}")
    @ApiOperation(value = "Check if an Extension with the given codeValue exists.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Found"),
        @ApiResponse(code = 404, message = "Not found")
    })
    public Response checkForExistingExtension(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                              @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                              @ApiParam(value = "Extension codeValue.", required = true) @PathParam("extensionCodeValue") final String extensionCodeValue) {
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
    @ApiOperation(value = "Check if an external reference with the given href exists.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Found"),
        @ApiResponse(code = 404, message = "Not found")
    })
    public Response checkForExistingExternalReference(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                      @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                                      @ApiParam(value = "Extension codeValue.", required = true) @QueryParam("href") final String href) {
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
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODEREGISTRY, null), pretty));
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
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODESCHEME, "codeRegistry,code,extension,valueType,member,memberValue"), pretty));
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
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODESCHEME, "codeRegistry,code,extension,valueType,member,memberValue"), pretty));
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
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODESCHEME, "codeRegistry,code,extension,valueType,member,memberValue"), pretty));
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
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_EXTENSION, "member,memberValue,valueType,propertyType,codeScheme,code,codeRegistry"), pretty));
        final ResponseWrapper<ExtensionDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("Extensions added or modified: " + extensions.size());
        meta.setCode(200);
        responseWrapper.setResults(extensions);
        return Response.ok(responseWrapper).build();
    }

    private Response createMissingMembersForExtension(final UUID codeSchemeId,
                                                      final String extensionCodeValue,
                                                      final String pretty) {
        ExtensionDTO extension = extensionService.findByCodeSchemeIdAndCodeValue(codeSchemeId, extensionCodeValue);
        Set<MemberDTO> createdMembers = memberService.createMissingMembersForAllCodesOfAllCodelistsOfAnExtension(extension);
        indexing.updateExtension(extension);
        indexing.updateMembers(createdMembers);
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_MEMBER, "extension,codeScheme,code,codeRegistry,propertyType,valueType,memberValue"), pretty));
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
            ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_MEMBER, "extension,codeScheme,code,codeRegistry,propertyType,valueType,memberValue"), pretty));
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
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODE, "codeRegistry,codeScheme,extension,valueType,member,memberValue"), pretty));
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
