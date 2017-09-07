package fi.vm.yti.cls.intake.resource;

import fi.vm.yti.cls.common.model.CodeRegistry;
import fi.vm.yti.cls.common.model.Meta;
import fi.vm.yti.cls.common.model.CodeScheme;
import fi.vm.yti.cls.common.model.Code;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.intake.api.MetaResponseWrapper;
import fi.vm.yti.cls.intake.domain.Domain;
import fi.vm.yti.cls.intake.domain.DomainConstants;
import fi.vm.yti.cls.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.cls.intake.jpa.CodeRepository;
import fi.vm.yti.cls.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.cls.intake.parser.CodeParser;
import fi.vm.yti.cls.intake.parser.CodeRegistryParser;
import fi.vm.yti.cls.intake.parser.CodeSchemeParser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;


/**
 * Content Intake Service: REST resources for registryItems.
 */
@Component
@Path("/v1/coderegistries")
@Api(value = "coderegistries", description = "Operations for creating, deleting and updating coderegistries, codeschemes and codes.")
@Produces("text/plain")
public class CodeRegistryResource {

    private static final Logger LOG = LoggerFactory.getLogger(CodeRegistryResource.class);

    private final Domain m_domain;

    private final CodeRegistryParser m_codeRegistryParser;

    private final CodeRegistryRepository m_codeRegistryRepository;

    private final CodeSchemeParser m_codeSchemeParser;

    private final CodeSchemeRepository m_codeSchemeRepository;

    private final CodeParser m_codeParser;

    private final CodeRepository m_codeRepository;


    @Inject
    public CodeRegistryResource(final Domain domain,
                                final CodeRegistryParser codeRegistryParser,
                                final CodeRegistryRepository codeRegistryRepository,
                                final CodeSchemeParser registerParser,
                                final CodeSchemeRepository codeSchemeRepository,
                                final CodeParser codeParser,
                                final CodeRepository codeRepository) {

        m_domain = domain;

        m_codeRegistryParser = codeRegistryParser;

        m_codeRegistryRepository = codeRegistryRepository;

        m_codeSchemeParser = registerParser;

        m_codeSchemeRepository = codeSchemeRepository;

        m_codeParser = codeParser;

        m_codeRepository = codeRepository;

    }


    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses coderegistries from CSV-source file with ',' delimiter.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdateCodeRegistries(@ApiParam(value = "Input-file") @FormDataParam("file") final InputStream inputStream) {

        LOG.info("/v1/coderegistries/ POST request.");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final List<CodeRegistry> codeRegistries = m_codeRegistryParser.parseCodeRegistriesFromClsInputStream(DomainConstants.SOURCE_INTERNAL, inputStream);

        for (final CodeRegistry register : codeRegistries) {
            LOG.info("CodeRegistry parsed from input: " + register.getCodeValue());
        }

        if (!codeRegistries.isEmpty()) {
            m_domain.persistCodeRegistries(codeRegistries);
            m_domain.reIndexCodeRegistries();
        }

        meta.setMessage("CodeRegistries added or modified: " + codeRegistries.size());
        meta.setCode(200);

        return Response.ok(responseWrapper).build();

    }


    @POST
    @Path("{codeRegistryCodeValue}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses codes from CSV-source file with ',' delimiter.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdateCodeSchemes(@ApiParam(value = "CodeRegistry codeValue") @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                           @ApiParam(value = "Input-file") @FormDataParam("file") final InputStream inputStream) {

        LOG.info("/v1/coderegistries/" + codeRegistryCodeValue + "/ POST request!");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final CodeRegistry codeRegistry = m_codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);

        if (codeRegistry != null) {

            final List<CodeScheme> codeSchemes = m_codeSchemeParser.parseCodeSchemesFromClsInputStream(codeRegistry, DomainConstants.SOURCE_INTERNAL, inputStream);

            for (final CodeScheme codeScheme : codeSchemes) {
                LOG.info("CodeScheme parsed from input: " + codeScheme.getCodeValue());
            }

            if (!codeSchemes.isEmpty()) {
                m_domain.persistCodeSchemes(codeSchemes);
                m_domain.reIndexCodeSchemes();
            }

            meta.setMessage("CodeSchemes added or modified: " + codeSchemes.size());
            meta.setCode(200);
            return Response.ok(responseWrapper).build();
        }

        meta.setMessage("CodeScheme with code: " + codeRegistryCodeValue + " does not exist yet, please creater register first.");
        meta.setCode(404);

        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseWrapper).build();

    }


    @POST
    @Path("{codeRegistryCodeValue}/{codeSchemeCodeValue}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses codes from CSV-source file with ',' delimiter.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdateCodes(@ApiParam(value = "CodeRegistry codeValue") @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                     @ApiParam(value = "CodeScheme codeValue") @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                                     @ApiParam(value = "Input-file") @FormDataParam("file") final InputStream inputStream) {

        LOG.info("/v1/coderegistries/" + codeRegistryCodeValue + "/" + codeSchemeCodeValue + "/ POST request!");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final CodeRegistry codeRegistry = m_codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);

        if (codeRegistry != null) {

            final CodeScheme codeScheme = m_codeSchemeRepository.findByCodeRegistryAndCodeValue(codeRegistry, codeSchemeCodeValue);

            if (codeScheme != null) {

                final List<Code> codes = m_codeParser.parseCodesFromClsInputStream(codeScheme, DomainConstants.SOURCE_INTERNAL, inputStream);

                for (final Code code : codes) {
                    LOG.info("Code parsed from input: " + code.getCodeValue());
                }

                if (!codes.isEmpty()) {
                    m_domain.persistCodes(codes);
                    m_domain.reIndexCodes(codeRegistryCodeValue, codeSchemeCodeValue);
                }

                meta.setMessage("Codes added or modified: " + codes.size());
                meta.setCode(200);
                return Response.ok(responseWrapper).build();

            } else {
                meta.setMessage("CodeScheme with code: " + codeSchemeCodeValue + " does not exist yet, please create codeScheme first.");
                meta.setCode(406);
            }

        } else {
            meta.setMessage("CodeRegistry with code: " + codeRegistryCodeValue + " does not exist yet, please create registry first.");
            meta.setCode(406);
        }

        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseWrapper).build();

    }


    @DELETE
    @Path("{codeRegistryCodeValue}/{codeSchemeCodeValue}/{codeCodeValue}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single code. This means that the item status is set to Status.RETIRED.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response retireCode(@ApiParam(value = "CodeRegistry codeValue.") @PathParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                               @ApiParam(value = "CodeScheme codeValue.") @PathParam("codeSchemeCodeValue") final String codeSchemeCodeValue,
                               @ApiParam(value = "Code codeValue.") @PathParam("codeCodeValue") final String codeCodeValue) {

        LOG.info("/v1/coderegistries/" + codeRegistryCodeValue + "/" + codeSchemeCodeValue + "/" + codeCodeValue + " DELETE request.");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final CodeRegistry codeRegistry = m_codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);

        if (codeRegistry != null) {

            final CodeScheme codeScheme = m_codeSchemeRepository.findByCodeRegistryAndCodeValue(codeRegistry, codeSchemeCodeValue);

            if (codeScheme != null) {
                final Code code = m_codeRepository.findByCodeSchemeAndCodeValue(codeScheme, codeCodeValue);

                if (code != null) {
                    code.setStatus(Status.RETIRED.toString());
                    m_codeRepository.save(code);
                    m_domain.reIndexCodes(codeRegistryCodeValue, codeSchemeCodeValue);
                    meta.setMessage("Code marked as RETIRED!");
                    meta.setCode(200);
                    return Response.ok(responseWrapper).build();
                } else {
                    meta.setMessage("Code " + codeCodeValue + " not found!");
                    meta.setCode(404);
                }

            } else {
                meta.setMessage("CodeScheme " + codeSchemeCodeValue + " not found!");
                meta.setCode(404);
            }

        } else {
            meta.setMessage("CodeRegistry " + codeRegistryCodeValue + " not found!");
            meta.setCode(404);
        }

        return Response.status(Response.Status.NOT_FOUND).entity(responseWrapper).build();

    }

}
