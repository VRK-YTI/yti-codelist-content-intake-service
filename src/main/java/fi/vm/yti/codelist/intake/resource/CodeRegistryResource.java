package fi.vm.yti.codelist.intake.resource;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
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
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.Views;
import fi.vm.yti.codelist.common.model.CodeSchemeListItem;
import fi.vm.yti.codelist.common.util.YtiCollectionUtils;
import fi.vm.yti.codelist.intake.api.MetaResponseWrapper;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.model.Meta;
import fi.vm.yti.codelist.intake.parser.CodeSchemeParser;
import fi.vm.yti.codelist.intake.service.CloningService;
import fi.vm.yti.codelist.intake.service.CodeRegistryService;
import fi.vm.yti.codelist.intake.service.CodeSchemeService;
import fi.vm.yti.codelist.intake.service.CodeService;
import fi.vm.yti.codelist.intake.service.ExtensionSchemeService;
import fi.vm.yti.codelist.intake.service.ExtensionService;
import fi.vm.yti.codelist.intake.service.ExternalReferenceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_406;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_CODEREGISTRY_NOT_EMPTY;

@Component
@Path("/v1/coderegistries")
@Api(value = "coderegistries")
@Produces("text/plain")
public class CodeRegistryResource extends AbstractBaseResource {

    private final CodeService codeService;
    private final CodeSchemeService codeSchemeService;
    private final CodeRegistryService codeRegistryService;
    private final ExternalReferenceService externalReferenceService;
    private final ExtensionSchemeService extensionSchemeService;
    private final ExtensionService extensionService;
    private final Indexing indexing;
    private final CloningService cloningService;
    private final CodeSchemeParser codeSchemeParser;

    @Inject
    public CodeRegistryResource(final CodeService codeService,
                                final CodeSchemeService codeSchemeService,
                                final CodeRegistryService codeRegistryService,
                                final ExternalReferenceService externalReferenceService,
                                final ExtensionSchemeService extensionSchemeService,
                                final ExtensionService extensionService,
                                final Indexing indexing,
                                final CloningService cloningService,
                                final CodeSchemeParser codeSchemeParser) {
        this.codeService = codeService;
        this.codeSchemeService = codeSchemeService;
        this.codeRegistryService = codeRegistryService;
        this.externalReferenceService = externalReferenceService;
        this.extensionSchemeService = extensionSchemeService;
        this.extensionService = extensionService;
        this.indexing = indexing;
        this.cloningService = cloningService;
        this.codeSchemeParser = codeSchemeParser;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses and creates or updates CodeRegistries from JSON input.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "CodeRegistries added or modified successfully.")
    })
    public Response addOrUpdateCodeRegistriesFromJson(@ApiParam(value = "JSON playload for CodeRegistry data.", required = true) final String jsonPayload) {
        return parseAndPersistCodeRegistriesFromSource(FORMAT_JSON, null, jsonPayload);
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
                                                      @ApiParam(value = "Input-file for CSV or Excel import.", hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream) {
        return parseAndPersistCodeRegistriesFromSource(format, inputStream, null);
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
            final Set<ExtensionSchemeDTO> extensionSchemes = extensionSchemeService.findByCodeSchemeId(codeScheme.getId());
            if (extensionSchemes != null && !extensionSchemes.isEmpty()) {
                indexing.updateExtensionSchemes(extensionSchemes);
                for (final ExtensionSchemeDTO extensionScheme : extensionSchemes) {
                    indexing.updateExtensions(extensionService.findByExtensionSchemeId(extensionScheme.getId()));
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
    @Path("{codeRegistryCodeValue}/codeschemes/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses and creates or updates CodeSchemes from JSON input.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "CodeSchemes added or modified successfully.")
    })
    public Response addOrUpdateCodeSchemesFromJson(@ApiParam(value = "Format for input.") @QueryParam("format") @DefaultValue("json") final String format,
                                                   @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                   @ApiParam(value = "JSON playload for CodeScheme data.", required = true) final String jsonPayload) {
        return parseAndPersistCodeSchemesFromSource(codeRegistryCodeValue, FORMAT_JSON, null, jsonPayload);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses and creates or updates CodeSchemes from CSV or Excel input data.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "CodeSchemes added or modified successfully.")
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", dataType = "file", paramType = "formData")
    })
    public Response addOrUpdateCodeSchemesFromFile(@ApiParam(value = "Format for input.") @QueryParam("format") @DefaultValue("csv") final String format,
                                                   @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                   @ApiParam(value = "Input-file for CSV or Excel import.", hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream) {
        return parseAndPersistCodeSchemesFromSource(codeRegistryCodeValue, format, inputStream, null);
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
                                     @ApiParam(value = "JSON playload for CodeScheme data.") final String jsonPayload) {

        final CodeSchemeDTO codeScheme = codeSchemeService.parseAndPersistCodeSchemeFromJson(codeRegistryCodeValue, codeSchemeCodeValue, jsonPayload);
        if (codeScheme != null) {
            if (!codeScheme.getVariantMothersOfThisCodeScheme().isEmpty()) {
                for (CodeSchemeListItem mother : codeScheme.getVariantMothersOfThisCodeScheme()) {
                    CodeSchemeDTO motherCodeScheme = codeSchemeService.findById(mother.getId());
                    LinkedHashSet<CodeSchemeListItem> variantsOfTheMother = motherCodeScheme.getVariantsOfThisCodeScheme();
                    for (CodeSchemeListItem item : variantsOfTheMother) {
                        if (item.getId().equals(codeScheme.getId())) {
                            populateCodeSchemeListItem(codeScheme,
                                item);
                        }
                    }
                    indexing.updateCodeScheme(motherCodeScheme);
                }

            }
            if (codeScheme.getLastCodeschemeId() != null) {
                LinkedHashSet<CodeSchemeListItem> allVersions = codeScheme.getAllVersions();
                for (CodeSchemeListItem listItem : allVersions) {
                    if (listItem.getId().equals(codeScheme.getId())) {
                        populateCodeSchemeListItem(codeScheme,
                            listItem);
                    }
                }
                LinkedHashSet<CodeSchemeDTO> versionsToReIndex = new LinkedHashSet<>();
                for (CodeSchemeListItem listItem : allVersions) {
                    CodeSchemeDTO currentVersion = codeSchemeService.findById(listItem.getId());
                    currentVersion.setAllVersions(allVersions);
                    codeSchemeService.updateCodeSchemeFromDto(codeRegistryCodeValue, currentVersion);
                    versionsToReIndex.add(currentVersion);
                }
                indexing.updateCodeSchemes(versionsToReIndex);

            }
            indexing.updateCodeScheme(codeScheme);
            indexing.updateExternalReferences(codeScheme.getExternalReferences());
            indexing.updateCodes(codeService.findByCodeSchemeId(codeScheme.getId()));
            final Set<ExtensionSchemeDTO> extensionSchemes = extensionSchemeService.findByCodeSchemeId(codeScheme.getId());
            if (extensionSchemes != null) {
                indexing.updateExtensionSchemes(extensionSchemes);
                extensionSchemes.forEach(extensionScheme -> {
                    indexing.updateExtensions(extensionService.findByExtensionSchemeId(extensionScheme.getId()));
                });
            }
        }
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
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
    @Path("{codeRegistryCodeValue}/clone/codescheme/{originalCodeSchemeUuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response cloneCodeSchemeFromJson(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                            @PathParam("originalCodeSchemeUuid") final String originalCodeSchemeUuid,
                                            @ApiParam(value = "JSON playload for CodeScheme data.", required = true) final String jsonPayload) {
        CodeSchemeDTO codeSchemeWithUserChangesFromUi = codeSchemeParser.parseCodeSchemeFromJsonData(jsonPayload);
        codeSchemeWithUserChangesFromUi = cloningService.cloneCodeSchemeWithAllThePlumbing(codeSchemeWithUserChangesFromUi, codeRegistryCodeValue, originalCodeSchemeUuid);
        return indexCodeschemeAndCodesAfterCloning(codeSchemeWithUserChangesFromUi, codeRegistryCodeValue);
    }

    @POST
    @Path("{codeRegistryCodeValue}/attachvariant/{variantCodeSchemeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response attachAVariantToCodeScheme(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                               @PathParam("variantCodeSchemeId") final String variantCodeSchemeId,
                                               @ApiParam(value = "JSON playload for the mother CodeScheme data.", required = true) final String jsonPayload) {
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
                new CodeSchemeListItem(variantCodeScheme.getId(), variantCodeScheme.getPrefLabel(),
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
                new CodeSchemeListItem(motherCodeScheme.getId(), motherCodeScheme.getPrefLabel(),
                    motherCodeScheme.getUri(), motherCodeScheme.getStartDate(),
                    motherCodeScheme.getEndDate(), motherCodeScheme.getStatus()));
        }
        codeSchemeService.updateCodeSchemeFromDto(codeRegistryCodeValue, variantCodeScheme);

        return indexCodeschemesAfterVariantAttachmentOrDetachment(motherCodeScheme, variantCodeScheme);
    }

    @POST
    @Path("{codeRegistryCodeValue}/detachvariant/{idOfVariantToDetach}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response detachAVariantFromCodeScheme(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                 @PathParam("idOfVariantToDetach") final String idOfVariantToDetach,
                                                 @ApiParam(value = "JSON playload for the mother CodeScheme data.", required = true) final String jsonPayload) throws Exception {
        CodeSchemeDTO motherCodeScheme = codeSchemeParser.parseCodeSchemeFromJsonData(jsonPayload);
        CodeSchemeDTO variantCodeScheme = codeSchemeService.findById(UUID.fromString(idOfVariantToDetach));

        for (Iterator<CodeSchemeListItem> iterator = motherCodeScheme.getVariantsOfThisCodeScheme().iterator(); iterator.hasNext(); ) {
            CodeSchemeListItem item = iterator.next();
            if (item.getId().compareTo(variantCodeScheme.getId()) == 0) {
                iterator.remove();
            }
        }

        codeSchemeService.updateCodeSchemeFromDto(codeRegistryCodeValue, motherCodeScheme);

        for (Iterator<CodeSchemeListItem> iterator = variantCodeScheme.getVariantMothersOfThisCodeScheme().iterator(); iterator.hasNext(); ) {
            CodeSchemeListItem item = iterator.next();
            if (item.getId().compareTo(motherCodeScheme.getId()) == 0) {
                iterator.remove();
            }
        }

        codeSchemeService.updateCodeSchemeFromDto(codeRegistryCodeValue, variantCodeScheme);

        return indexCodeschemesAfterVariantAttachmentOrDetachment(motherCodeScheme, variantCodeScheme);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensionschemes/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses and creates or updates ExtensionSchemes from CSV or Excel input data.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "ExtensionSchemes added or modified successfully.")
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", dataType = "file", paramType = "formData")
    })
    public Response addOrUpdateExtensionSchemesFromFile(@ApiParam(value = "Format for input.") @QueryParam("format") @DefaultValue("csv") final String format,
                                                        @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                        @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                                        @ApiParam(value = "Input-file for CSV or Excel import.", hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream) {
        return parseAndPersistExtensionSchemesFromSource(codeRegistryCodeValue, codeSchemeCodeValue, format, inputStream, null, EXCEL_SHEET_EXTENSIONSCHEMES);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensionschemes/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses and creates or updates ExtensionSchemes from JSON input.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "ExtensionSchemes added or modified successfully.")
    })
    public Response addOrUpdateExtensionSchemesFromJson(@ApiParam(value = "Format for input.") @QueryParam("format") @DefaultValue("json") final String format,
                                                        @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                        @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                                        @ApiParam(value = "JSON playload for ExtensionScheme data.", required = true) final String jsonPayload) {
        return parseAndPersistExtensionSchemesFromSource(codeRegistryCodeValue, codeSchemeCodeValue, FORMAT_JSON, null, jsonPayload, null);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensionschemes/{extensionSchemeCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Modifies single existing ExtensionScheme.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "ExtensionScheme modified successfully.")
    })
    public Response updateExtensionScheme(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                          @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                          @ApiParam(value = "ExtensionScheme codeValue", required = true) @PathParam("extensionSchemeCodeValue") final String extensionSchemeCodeValue,
                                          @ApiParam(value = "JSON playload for ExtensionScheme data.") final String jsonPayload) {

        final ExtensionSchemeDTO extensionScheme = extensionSchemeService.parseAndPersistExtensionSchemeFromJson(codeRegistryCodeValue, codeSchemeCodeValue, extensionSchemeCodeValue, jsonPayload);
        indexing.updateExtensionScheme(extensionScheme);
        indexing.updateExtensions(extensionService.findByExtensionSchemeId(extensionScheme.getId()));
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensionschemes/{extensionSchemeCodeValue}/extensions/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses and creates or updates Extensions from CSV or Excel input data.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "ExtensionScheme modified successfully.")
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "file", value = "Input-file", dataType = "file", paramType = "formData")
    })
    public Response addOrUpdateExtensionsFromFile(@ApiParam(value = "Format for input.") @QueryParam("format") @DefaultValue("csv") final String format,
                                                  @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                  @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                                  @ApiParam(value = "ExtensionScheme codeValue", required = true) @PathParam("extensionSchemeCodeValue") final String extensionSchemeCodeValue,
                                                  @ApiParam(value = "Input-file for CSV or Excel import.", hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream) {
        return parseAndPersistExtensionsFromSource(codeRegistryCodeValue, codeSchemeCodeValue, extensionSchemeCodeValue, format, inputStream, null, EXCEL_SHEET_EXTENSIONS);
    }

    @POST
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensionschemes/{extensionSchemeCodeValue}/extensions/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses and creates or updates Extensions from JSON input.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Extensions modified successfully.")
    })
    public Response addOrUpdateExtensionsFromJson(@ApiParam(value = "Format for input.") @QueryParam("format") @DefaultValue("json") final String format,
                                                  @ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                  @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                                  @ApiParam(value = "ExtensionScheme codeValue", required = true) @PathParam("extensionSchemeCodeValue") final String extensionSchemeCodeValue,
                                                  @ApiParam(value = "JSON playload for ExtensionScheme data.", required = true) final String jsonPayload) {
        return parseAndPersistExtensionsFromSource(codeRegistryCodeValue, codeSchemeCodeValue, extensionSchemeCodeValue, FORMAT_JSON, null, jsonPayload, null);
    }

    @DELETE
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single existing CodeScheme.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "CodeScheme deleted."),
        @ApiResponse(code = 404, message = "CodeScheme not found.")
    })
    public Response deleteCodeScheme(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                     @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue) {

        final CodeSchemeDTO existingCodeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);

        if (existingCodeScheme != null) {
            final UUID codeSchemeId = existingCodeScheme.getId();
            final Set<CodeDTO> codes = codeService.findByCodeSchemeId(codeSchemeId);
            final Set<ExternalReferenceDTO> externalReferences = externalReferenceService.findByParentCodeSchemeId(codeSchemeId);
            final Set<ExtensionSchemeDTO> extensionSchemes = extensionSchemeService.findByCodeSchemeId(codeSchemeId);
            final Set<ExtensionDTO> extensions = extensionService.findByExtensionSchemeId(codeSchemeId);
            LinkedHashSet<CodeSchemeDTO> codeSchemeDTOsToIndex = new LinkedHashSet<>();
            final CodeSchemeDTO codeScheme = codeSchemeService.deleteCodeScheme(existingCodeScheme.getCodeRegistry().getCodeValue(), existingCodeScheme.getCodeValue(), codeSchemeDTOsToIndex);

            LinkedHashSet<CodeSchemeDTO> affectedCodeSchemes = new LinkedHashSet<>();
            for (CodeSchemeListItem item : codeScheme.getVariantsOfThisCodeScheme()) {
                affectedCodeSchemes.add(codeSchemeService.findById(item.getId()));
            }
            for (CodeSchemeListItem item : codeScheme.getVariantMothersOfThisCodeScheme()) {
                affectedCodeSchemes.add(codeSchemeService.findById(item.getId()));
            }

            for (CodeSchemeDTO dto : affectedCodeSchemes) {
                for (Iterator<CodeSchemeListItem> iterator = dto.getVariantsOfThisCodeScheme().iterator(); iterator.hasNext(); ) {
                    CodeSchemeListItem item = iterator.next();
                    if (item.getId().compareTo(codeScheme.getId()) == 0) {
                        iterator.remove();
                    }
                }
                for (Iterator<CodeSchemeListItem> iterator = dto.getVariantMothersOfThisCodeScheme().iterator(); iterator.hasNext(); ) {
                    CodeSchemeListItem item = iterator.next();
                    if (item.getId().compareTo(codeScheme.getId()) == 0) {
                        iterator.remove();
                    }
                }

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
            if (extensionSchemes != null) {
                indexing.deleteExtensionSchemes(extensionSchemes);
            }
            if (extensions != null) {
                indexing.deleteExtensions(extensions);
            }
        } else {
            return Response.status(404).build();
        }
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        return Response.ok(responseWrapper).build();
    }

    @DELETE
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensionschemes/{extensionSchemeCodeValue}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single existing ExtensionScheme.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "ExtensionScheme deleted."),
        @ApiResponse(code = 404, message = "ExtensionScheme not found.")
    })
    public Response deleteExtensionScheme(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                          @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                          @ApiParam(value = "ExtensionScheme codeValue", required = true) @PathParam("extensionSchemeCodeValue") final String extensionSchemeCodeValue) {

        final CodeSchemeDTO codeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme != null) {
            final ExtensionSchemeDTO existingExtensionScheme = extensionSchemeService.findByCodeSchemeIdAndCodeValue(codeScheme.getId(), extensionSchemeCodeValue);
            if (existingExtensionScheme != null) {
                final Set<ExtensionDTO> extensions = extensionService.findByExtensionSchemeId(existingExtensionScheme.getId());
                final ExtensionSchemeDTO extensionScheme = extensionSchemeService.deleteExtensionScheme(existingExtensionScheme.getId());
                indexing.deleteExtensions(extensions);
                indexing.deleteExtensionScheme(extensionScheme);
                indexing.updateCodeScheme(codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue));
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
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensionschemes/{extensionSchemeCodeValue}/extensions/{extensionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single existing Extension.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Extension deleted."),
        @ApiResponse(code = 404, message = "Extension not found.")
    })
    public Response deleteExtension(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                    @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                    @ApiParam(value = "ExtensionScheme codeValue", required = true) @PathParam("extensionSchemeCodeValue") final String extensionSchemeCodeValue,
                                    @ApiParam(value = "Extension UUID", required = true) @PathParam("extensionId") final UUID extensionId) {

        final ExtensionDTO existingExtension = extensionService.findById(extensionId);
        if (existingExtension != null) {
            final ExtensionDTO extensionScheme = extensionService.deleteExtension(existingExtension.getId());
            indexing.deleteExtension(extensionScheme);
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
                                             @ApiParam(value = "JSON playload for Code data.", required = true) final String jsonPayload) {

        return parseAndPersistCodesFromSource(codeRegistryCodeValue, codeSchemeCodeValue, format, null, jsonPayload);

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
                                             @ApiParam(value = "Input-file for CSV or Excel import.", hidden = true, type = "file") @FormDataParam("file") final InputStream inputStream) {

        return parseAndPersistCodesFromSource(codeRegistryCodeValue, codeSchemeCodeValue, format, inputStream, null);
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
        codes.forEach(code -> indexing.updateExternalReferences(code.getExternalReferences()));
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
        @ApiResponse(code = 404, message = "Code not found.")
    })
    public Response deleteCode(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                               @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                               @ApiParam(value = "Code codeValue.", required = true) @PathParam("codeCodeValue") final String codeCodeValue) {
        final CodeSchemeDTO codeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        final CodeDTO code = codeService.deleteCode(codeRegistryCodeValue, codeSchemeCodeValue, codeCodeValue);
        if (code != null && codeScheme != null) {
            final Set<CodeDTO> referencedCodes = codeService.removeBroaderCodeId(code.getId());
            if (referencedCodes != null && !referencedCodes.isEmpty()) {
                indexing.updateCodes(referencedCodes);
            }
            final Set<ExtensionDTO> extensions = code.getExtensions();
            if (extensions != null && !extensions.isEmpty()) {
                indexing.deleteExtensions(extensions);
            }
            indexing.deleteCode(code);
            indexing.updateCodeScheme(codeScheme);
            final Set<ExtensionSchemeDTO> extensionSchemes = extensionSchemeService.findByCodeSchemeId(codeScheme.getId());
            indexing.updateExtensionSchemes(extensionSchemes);
            final Meta meta = new Meta();
            final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
            return Response.ok(responseWrapper).build();
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
    @Path("{codeRegistryCodeValue}/codeschemes/{codeSchemeCodeValue}/extensionschemes/{extensionSchemeCodeValue}")
    @ApiOperation(value = "Check if an ExtensionScheme with the given codeValue exists.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Found"),
        @ApiResponse(code = 404, message = "Not found")
    })
    public Response checkForExistingExtensionScheme(@ApiParam(value = "CodeRegistry codeValue", required = true) @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                                    @ApiParam(value = "CodeScheme codeValue", required = true) @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                                    @ApiParam(value = "ExtensionScheme codeValue.", required = true) @PathParam("extensionSchemeCodeValue") final String extensionSchemeCodeValue) {
        final CodeSchemeDTO codeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme != null) {
            final ExtensionSchemeDTO extensionSchemeDTO = this.extensionSchemeService.findByCodeSchemeIdAndCodeValue(codeScheme.getId(), extensionSchemeCodeValue);
            if (extensionSchemeDTO == null) {
                return Response.status(404).build();
            }
            return Response.status(200).build();
        } else {
            return Response.status(404).build();
        }
    }

    private Response parseAndPersistCodeRegistriesFromSource(final String format,
                                                             final InputStream inputStream,
                                                             final String jsonPayload) {
        final Set<CodeRegistryDTO> codeRegistries = codeRegistryService.parseAndPersistCodeRegistriesFromSourceData(format, inputStream, jsonPayload);
        indexing.updateCodeRegistries(codeRegistries);
        codeRegistries.forEach(codeRegistry -> {
            final Set<CodeSchemeDTO> codeSchemes = codeSchemeService.findByCodeRegistryCodeValue(codeRegistry.getCodeValue());
            indexing.updateCodeSchemes(codeSchemes);
            codeSchemes.forEach(codeScheme -> {
                indexing.updateCodes(codeService.findByCodeSchemeId(codeScheme.getId()));
                indexing.updateExternalReferences(externalReferenceService.findByParentCodeSchemeId(codeScheme.getId()));
                final Set<ExtensionSchemeDTO> extensionSchemes = extensionSchemeService.findByCodeSchemeId(codeScheme.getId());
                if (extensionSchemes != null && !extensionSchemes.isEmpty()) {
                    indexing.updateExtensionSchemes(extensionSchemes);
                    for (final ExtensionSchemeDTO extensionScheme : extensionSchemes) {
                        indexing.updateExtensions(extensionService.findByExtensionSchemeId(extensionScheme.getId()));
                    }
                }
            });
        });
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODEREGISTRY, null)));
        final ResponseWrapper<CodeRegistryDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("CodeRegistries added or modified: " + codeRegistries.size());
        meta.setCode(200);
        responseWrapper.setResults(codeRegistries);
        return Response.ok(responseWrapper).build();
    }

    private Response indexCodeschemesAfterVariantAttachmentOrDetachment(final CodeSchemeDTO motherCodeScheme,
                                                                        final CodeSchemeDTO variantCodeScheme) {
        final HashSet<CodeSchemeDTO> codeSchemes = new HashSet<>();
        codeSchemeService.populateAllVersionsToCodeSchemeDTO(variantCodeScheme);
        codeSchemes.add(variantCodeScheme);
        codeSchemes.add(motherCodeScheme);
        indexing.updateCodeSchemes(codeSchemes);
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODESCHEME, "codeRegistry,code,extensionScheme,extension")));
        final ResponseWrapper<CodeSchemeDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("A Variant was attached to a CodeScheme.");
        meta.setCode(200);
        responseWrapper.setResults(codeSchemes);
        return Response.ok(responseWrapper).build();
    }

    private Response indexCodeschemeAndCodesAfterCloning(final CodeSchemeDTO codeScheme,
                                                         final String codeRegistryCodeValue) {
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
        final Set<ExtensionSchemeDTO> extensionSchemes = extensionSchemeService.findByCodeSchemeId(codeScheme.getId());
        if (extensionSchemes != null && !extensionSchemes.isEmpty()) {
            indexing.updateExtensionSchemes(extensionSchemes);
            for (final ExtensionSchemeDTO extensionScheme : extensionSchemes) {
                indexing.updateExtensions(extensionService.findByExtensionSchemeId(extensionScheme.getId()));
            }
        }
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODESCHEME, "codeRegistry,code,extensionScheme,extension")));
        final ResponseWrapper<CodeSchemeDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("A CodeScheme was cloned.");
        meta.setCode(200);
        responseWrapper.setResults(codeSchemes);
        return Response.ok(responseWrapper).build();
    }

    private Response parseAndPersistCodeSchemesFromSource(final String codeRegistryCodeValue,
                                                          final String format,
                                                          final InputStream inputStream,
                                                          final String jsonPayload) {
        final Set<CodeSchemeDTO> codeSchemes = codeSchemeService.parseAndPersistCodeSchemesFromSourceData(codeRegistryCodeValue, format, inputStream, jsonPayload);
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
            final Set<ExtensionSchemeDTO> extensionSchemes = extensionSchemeService.findByCodeSchemeId(codeScheme.getId());
            if (extensionSchemes != null && !extensionSchemes.isEmpty()) {
                indexing.updateExtensionSchemes(extensionSchemes);
                for (final ExtensionSchemeDTO extensionScheme : extensionSchemes) {
                    indexing.updateExtensions(extensionService.findByExtensionSchemeId(extensionScheme.getId()));
                }
            }
        }
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODESCHEME, "codeRegistry,code,extensionScheme,extension")));
        final ResponseWrapper<CodeSchemeDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("CodeSchemes added or modified: " + codeSchemes.size());
        meta.setCode(200);
        responseWrapper.setResults(codeSchemes);
        return Response.ok(responseWrapper).build();
    }

    private Response parseAndPersistExtensionSchemesFromSource(final String codeRegistryCodeValue,
                                                               final String codeSchemeCodeValue,
                                                               final String format,
                                                               final InputStream inputStream,
                                                               final String jsonPayload,
                                                               final String sheetName) {
        final Set<ExtensionSchemeDTO> extensionSchemes = extensionSchemeService.parseAndPersistExtensionSchemesFromSourceData(codeRegistryCodeValue, codeSchemeCodeValue, format, inputStream, jsonPayload, sheetName);
        indexing.updateExtensionSchemes(extensionSchemes);
        if (!extensionSchemes.isEmpty()) {
            final Set<CodeSchemeDTO> codeSchemes = new HashSet<>();
            extensionSchemes.forEach(extensionScheme -> {
                final CodeSchemeDTO codeScheme = codeSchemeService.findById(extensionScheme.getParentCodeScheme().getId());
                codeSchemes.add(codeScheme);
                final Set<ExtensionDTO> extensions = extensionService.findByExtensionSchemeId(extensionScheme.getId());
                indexing.updateExtensions(extensions);
            });
            indexing.updateCodeSchemes(codeSchemes);
        }
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_EXTENSIONSCHEME, "extension,codeScheme,code,codeRegistry")));
        final ResponseWrapper<ExtensionSchemeDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("ExtensionSchemes added or modified: " + extensionSchemes.size());
        meta.setCode(200);
        responseWrapper.setResults(extensionSchemes);
        return Response.ok(responseWrapper).build();
    }

    private Response parseAndPersistExtensionsFromSource(final String codeRegistryCodeValue,
                                                         final String codeSchemeCodeValue,
                                                         final String extensionSchemeCodeValue,
                                                         final String format,
                                                         final InputStream inputStream,
                                                         final String jsonPayload,
                                                         final String sheetName) {
        final CodeSchemeDTO codeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme != null) {
            final Set<ExtensionDTO> extensions = extensionService.parseAndPersistExtensionsFromSourceData(codeRegistryCodeValue, codeSchemeCodeValue, extensionSchemeCodeValue, format, inputStream, jsonPayload, sheetName);
            indexing.updateExtensions(extensions);
            final ExtensionSchemeDTO extensionScheme = extensionSchemeService.findByCodeSchemeIdAndCodeValue(codeScheme.getId(), extensionSchemeCodeValue);
            if (extensionScheme != null) {
                indexing.updateExtensionScheme(extensionScheme);
            }
            final Meta meta = new Meta();
            ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_EXTENSION, "extensionScheme,codeScheme,code,codeRegistry,propertyType")));
            final ResponseWrapper<ExtensionDTO> responseWrapper = new ResponseWrapper<>(meta);
            meta.setMessage("Extension added or modified: " + extensions.size());
            meta.setCode(200);
            responseWrapper.setResults(extensions);
            return Response.ok(responseWrapper).build();
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }

    private Response parseAndPersistCodesFromSource(final String codeRegistryCodeValue,
                                                    final String codeSchemeCodeValue,
                                                    final String format,
                                                    final InputStream inputStream,
                                                    final String jsonPayload) {
        final Set<CodeDTO> codes = codeService.parseAndPersistCodesFromSourceData(codeRegistryCodeValue, codeSchemeCodeValue, format, inputStream, jsonPayload);
        indexing.updateCodes(codes);
        final CodeSchemeDTO codeScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        indexing.updateCodeScheme(codeScheme);
        indexing.updateExternalReferences(externalReferenceService.findByParentCodeSchemeId(codeScheme.getId()));
        indexing.updateCodeRegistry(codeRegistryService.findByCodeValue(codeRegistryCodeValue));
        final Meta meta = new Meta();
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODE, "codeRegistry,codeScheme")));
        final ResponseWrapper<CodeDTO> responseWrapper = new ResponseWrapper<>(meta);
        meta.setMessage("Codes added or modified: " + codes.size());
        meta.setCode(200);
        responseWrapper.setResults(codes);
        return Response.ok(responseWrapper).build();
    }
}
