package fi.vm.yti.cls.intake.resource;

import fi.vm.yti.cls.common.model.Magistrate;
import fi.vm.yti.cls.common.model.Meta;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.intake.api.MetaResponseWrapper;
import fi.vm.yti.cls.intake.domain.Domain;
import fi.vm.yti.cls.intake.domain.DomainConstants;
import fi.vm.yti.cls.intake.jpa.MagistrateRepository;
import fi.vm.yti.cls.intake.parser.MagistrateParser;
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
 * Content Intake Service: REST resources for magistrates.
 */
@Component
@Path("/v1/magistrates")
@Api(value = "magistrates", description = "Operations for creating, deleting and updating magistrates.")
@Produces("text/plain")
public class MagistrateResource {

    private static final Logger LOG = LoggerFactory.getLogger(MagistrateResource.class);

    private final Domain m_domain;

    private final MagistrateParser m_magistrateParser;

    private final MagistrateRepository m_magistrateRepository;


    @Inject
    public MagistrateResource(final Domain domain,
                              final MagistrateParser magistrateParser,
                              final MagistrateRepository magistrateRepository) {

        m_domain = domain;

        m_magistrateParser = magistrateParser;

        m_magistrateRepository = magistrateRepository;

    }


    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses magistrates from CSV-source file with ',' delimiter.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdateMagistrates(@ApiParam(value = "Input-file") @FormDataParam("file") final InputStream inputStream) {

        LOG.info("/v1/magistrates/ POST request.");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final List<Magistrate> magistrates = m_magistrateParser.parseMagistratesFromClsInputStream(DomainConstants.SOURCE_INTERNAL, inputStream);

        for (final Magistrate magistrate : magistrates) {
            LOG.info("Magistrate parsed from input: " + magistrate.getCodeValue());
        }

        if (!magistrates.isEmpty()) {
            m_domain.persistMagistrates(magistrates);
            m_domain.reIndexEverything();
        }

        meta.setMessage("Magistrates added or modified: " + magistrates.size());
        meta.setCode(200);

        return Response.ok(responseWrapper).build();

    }


    @DELETE
    @Path("{codeValue}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single magistrate. This means that the item status is set to Status.RETIRED.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response retireMagistrate(@ApiParam(value = "Magistrate code.") @PathParam("codeValue") final String codeValue) {

        LOG.info("/v1/magistrates/" + codeValue + " DELETE request.");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final Magistrate magistrate = m_magistrateRepository.findByCodeValue(codeValue);

        if (magistrate != null) {
            magistrate.setStatus(Status.RETIRED.toString());
            m_magistrateRepository.save(magistrate);
            m_domain.reIndexEverything();
        }

        meta.setMessage("Magistrate marked as RETIRED!");
        meta.setCode(200);

        return Response.ok(responseWrapper).build();

    }


}
