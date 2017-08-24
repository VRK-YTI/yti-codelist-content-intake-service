package fi.vm.yti.cls.intake.resource;

import fi.vm.yti.cls.common.model.Meta;
import fi.vm.yti.cls.common.model.Region;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.intake.api.MetaResponseWrapper;
import fi.vm.yti.cls.intake.domain.Domain;
import fi.vm.yti.cls.intake.domain.DomainConstants;
import fi.vm.yti.cls.intake.jpa.RegionRepository;
import fi.vm.yti.cls.intake.parser.RegionItemParser;
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
 * Content Intake Service: REST resources for regions.
 */
@Component
@Path("/v1/regions")
@Api(value = "regions", description = "Operations for creating, deleting and updating regions.")
@Produces("text/plain")
public class RegionResource {

    private static final Logger LOG = LoggerFactory.getLogger(RegionResource.class);

    private final Domain m_domain;

    private final RegionItemParser m_regionParser;

    private final RegionRepository m_regionRepository;


    @Inject
    public RegionResource(final Domain domain,
                          final RegionItemParser regionParser,
                          final RegionRepository regionRepository) {

        m_domain = domain;

        m_regionParser = regionParser;

        m_regionRepository = regionRepository;

    }


    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses regions from CSV-source file with ',' delimiter.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdateRegions(@ApiParam(value = "Input-file") @FormDataParam("file") final InputStream inputStream) {

        LOG.info("/v1/regions/ POST request.");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final List<Region> regions = m_regionParser.parseRegionsFromClsInputStream(DomainConstants.SOURCE_INTERNAL, inputStream);

        for (final Region region : regions) {
            LOG.info("Region parsed from input: " + region.getCode());
        }

        if (!regions.isEmpty()) {
            m_domain.persistRegions(regions);
            m_domain.reIndexEverything();
        }

        meta.setMessage("Regions added or modified: " + regions.size());
        meta.setCode(200);

        return Response.ok(responseWrapper).build();

    }


    @DELETE
    @Path("{code}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single region. This means that the item status is set to Status.RETIRED.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response retireRegion(@ApiParam(value = "Region code.") @PathParam("code") final String code) {

        LOG.info("/v1/regions/" + code + " DELETE request.");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final Region region = m_regionRepository.findByCode(code);

        if (region != null) {
            region.setStatus(Status.RETIRED.toString());
            m_regionRepository.save(region);
            m_domain.reIndexEverything();
        }

        meta.setMessage("Region marked as RETIRED!");
        meta.setCode(200);

        return Response.ok(responseWrapper).build();

    }

}
