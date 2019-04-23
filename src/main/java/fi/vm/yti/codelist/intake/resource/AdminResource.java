package fi.vm.yti.codelist.intake.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.common.dto.ValueTypeDTO;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.groupmanagement.OrganizationUpdater;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.jpa.ExtensionRepository;
import fi.vm.yti.codelist.intake.jpa.MemberRepository;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.Member;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.ExternalReferenceService;
import fi.vm.yti.codelist.intake.service.PropertyTypeService;
import fi.vm.yti.codelist.intake.service.ValueTypeService;
import fi.vm.yti.codelist.intake.util.FileUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.data.YtiDataAccess.*;

@Component
@Path("/admin")
@Api(value = "admin")
@Produces("text/plain")
public class AdminResource implements AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(AdminResource.class);
    private final AuthorizationManager authorizationManager;
    private final ExtensionRepository extensionRepository;
    private final MemberRepository memberRepository;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRepository codeRepository;
    private final PropertyTypeService propertyTypeService;
    private final ExternalReferenceService externalReferenceService;
    private final ApiUtils apiUtils;
    private final Indexing indexing;
    private final OrganizationUpdater organizationUpdater;
    private final ValueTypeService valueTypeService;

    @Inject
    public AdminResource(final AuthorizationManager authorizationManager,
                         final ExtensionRepository extensionRepository,
                         final MemberRepository memberRepository,
                         final CodeRegistryRepository codeRegistryRepository,
                         final CodeSchemeRepository codeSchemeRepository,
                         final CodeRepository codeRepository,
                         final PropertyTypeService propertyTypeService,
                         final ExternalReferenceService externalReferenceService,
                         final ApiUtils apiUtils,
                         final Indexing indexing,
                         final OrganizationUpdater organizationUpdater,
                         final ValueTypeService valueTypeService) {
        this.authorizationManager = authorizationManager;
        this.extensionRepository = extensionRepository;
        this.memberRepository = memberRepository;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
        this.propertyTypeService = propertyTypeService;
        this.externalReferenceService = externalReferenceService;
        this.apiUtils = apiUtils;
        this.indexing = indexing;
        this.organizationUpdater = organizationUpdater;
        this.valueTypeService = valueTypeService;
    }

    @Path("/updateorganizations")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Fetches and updates organization information from groupmanagement service.")
    @ApiResponse(code = 200, message = "Upon successful request.")
    @Transactional
    public Response updateOrganizations() {
        if (authorizationManager.isSuperUser()) {
            organizationUpdater.updateOrganizations();
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @GET
    @Path("/coderegistries/rewriteuris")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Rewrites all coderegistry resource uris.")
    @ApiResponse(code = 200, message = "Upon successful request.")
    @Transactional
    public Response rewriteCodeRegistryUris() {
        if (authorizationManager.isSuperUser()) {
            final Set<CodeRegistry> codeRegistries = codeRegistryRepository.findAll();
            for (final CodeRegistry codeRegistry : codeRegistries) {
                codeRegistry.setUri(apiUtils.createCodeRegistryUri(codeRegistry));
            }
            codeRegistryRepository.save(codeRegistries);
            indexing.reIndexEverything();
            LOG.info("CodeRegistry uris rewritten.");
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @GET
    @Path("/codeschemes/rewriteuris")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Rewrites all codescheme resource uris.")
    @ApiResponse(code = 200, message = "Upon successful request.")
    @Transactional
    public Response rewriteCodeSchemeUris() {
        if (authorizationManager.isSuperUser()) {
            final Set<CodeScheme> codeSchemes = codeSchemeRepository.findAll();
            for (final CodeScheme codeScheme : codeSchemes) {
                codeScheme.setUri(apiUtils.createCodeSchemeUri(codeScheme));
            }
            codeSchemeRepository.save(codeSchemes);
            indexing.reIndexEverything();
            LOG.info("CodeScheme uris rewritten.");
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @GET
    @Path("/codes/rewriteuris")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Rewrites all code resource uris.")
    @ApiResponse(code = 200, message = "Upon successful request.")
    @Transactional
    public Response rewriteCodeUris() {
        if (authorizationManager.isSuperUser()) {
            final Set<Code> codes = codeRepository.findAll();
            for (final Code code : codes) {
                code.setUri(apiUtils.createCodeUri(code));
            }
            codeRepository.save(codes);
            indexing.reIndexEverything();
            LOG.info("Code uris rewritten.");
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @GET
    @Path("/extensions/rewriteuris")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Rewrites all code resource uris.")
    @ApiResponse(code = 200, message = "Upon successful request.")
    @Transactional
    public Response rewriteExtensionUris() {
        if (authorizationManager.isSuperUser()) {
            final Set<Extension> extensions = extensionRepository.findAll();
            for (final Extension extension : extensions) {
                extension.setUri(apiUtils.createExtensionUrl(extension));
            }
            extensionRepository.save(extensions);
            indexing.reIndexEverything();
            LOG.info("Extension uris rewritten.");
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @GET
    @Path("/members/rewriteuris")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Rewrites all code resource uris.")
    @ApiResponse(code = 200, message = "Upon successful request.")
    @Transactional
    public Response rewriteMemberUris() {
        if (authorizationManager.isSuperUser()) {
            final Set<Member> members = memberRepository.findAll();
            for (final Member member : members) {
                member.setUri(apiUtils.createMemberUri(member));
            }
            memberRepository.save(members);
            indexing.reIndexEverything();
            LOG.info("Member uris rewritten.");
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @GET
    @Path("/externalreferences/reload")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Reloads global ExternalReferences from source data.")
    @ApiResponse(code = 200, message = "Upon successful request.")
    @Transactional
    public Response reloadGlobalExternalReferences() {
        if (authorizationManager.isSuperUser()) {
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_EXTERNALREFERENCES + "/" + DEFAULT_EXTERNALREFERENCE_FILENAME)) {
                final Set<ExternalReferenceDTO> externalReferenceDtos = externalReferenceService.parseAndPersistExternalReferencesFromSourceData(true, FORMAT_CSV, inputStream, null, null);
                LOG.info(String.format("Reloaded %d ExternalReferences from initial data!", externalReferenceDtos.size()));
                indexing.reIndexEverything();
                LOG.info("Reindexing finished.");
            } catch (final IOException e) {
                LOG.error("Issue with parsing ExternalReference file. ", e);
            }
            LOG.info("ExternalReferences reloaded.");
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @GET
    @Path("/propertytypes/reload")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Reloads PropertyTypes from source data.")
    @ApiResponse(code = 200, message = "Upon successful request.")
    @Transactional
    public Response reloadPropertyTypes() {
        if (authorizationManager.isSuperUser()) {
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_PROPERTYTYPES + "/" + DEFAULT_PROPERTYTYPE_FILENAME)) {
                final Set<PropertyTypeDTO> propertyTypeDtos = propertyTypeService.parseAndPersistPropertyTypesFromSourceData(true, FORMAT_CSV, inputStream, null);
                LOG.info(String.format("Reloaded %d PropertyTypes from initial data!", propertyTypeDtos.size()));
                indexing.reIndexEverything();
                LOG.info("Reindexing finished.");
            } catch (final IOException e) {
                LOG.error("Issue with parsing PropertyType file. ", e);
            }
            LOG.info("PropertyTypes reloaded.");
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @GET
    @Path("/valuetypes/reload")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Reloads ValueTypes from source data.")
    @ApiResponse(code = 200, message = "Upon successful request.")
    @Transactional
    public Response reloadValueTypes() {
        if (authorizationManager.isSuperUser()) {
            try (final InputStream inputStream = FileUtils.loadFileFromClassPath("/" + DATA_VALUETYPES + "/" + DEFAULT_VALUETYPE_FILENAME)) {
                final Set<ValueTypeDTO> valueTypeDtos = valueTypeService.parseAndPersistValueTypesFromSourceData(true, FORMAT_CSV, inputStream, null);
                LOG.info(String.format("Reloaded %d ValueTypes from initial data!", valueTypeDtos.size()));
                indexing.reIndexEverything();
                LOG.info("Reindexing finished.");
            } catch (final IOException e) {
                LOG.error("Issue with parsing ValueType file. ", e);
            }
            LOG.info("ValueTypes reloaded.");
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @GET
    @Path("/reindex")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Reindex ElasticSearch data.")
    @ApiResponse(code = 200, message = "Upon successful request.")
    @Transactional
    public Response reIndex() {
        if (authorizationManager.isSuperUser()) {
            indexing.reIndexEverything();
            LOG.info("Reindexing finished.");
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }
}
