package fi.vm.yti.cls.intake.resource;

import fi.vm.yti.cls.common.model.ElectoralDistrict;
import fi.vm.yti.cls.common.model.Meta;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.intake.api.MetaResponseWrapper;
import fi.vm.yti.cls.intake.domain.Domain;
import fi.vm.yti.cls.intake.domain.DomainConstants;
import fi.vm.yti.cls.intake.jpa.ElectoralDistrictRepository;
import fi.vm.yti.cls.intake.parser.ElectoralDistrictParser;
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
 * Content Intake Service: REST resources for electoralDistricts.
 */
@Component
@Path("/v1/electoralDistricts")
@Api(value = "electoralDistricts", description = "Operations for creating, deleting and updating electoralDistricts.")
@Produces("text/plain")
public class ElectoralDistrictResource {

    private static final Logger LOG = LoggerFactory.getLogger(ElectoralDistrictResource.class);

    private final Domain m_domain;

    private final ElectoralDistrictParser m_electoralDistrictParser;

    private final ElectoralDistrictRepository m_electoralDistrictRepository;


    @Inject
    public ElectoralDistrictResource(final Domain domain,
                                     final ElectoralDistrictParser electoralDistrictParser,
                                     final ElectoralDistrictRepository electoralDistrictRepository) {

        m_domain = domain;

        m_electoralDistrictParser = electoralDistrictParser;

        m_electoralDistrictRepository = electoralDistrictRepository;

    }


    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses electoralDistricts from CSV-source file with ',' delimiter.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdateElectoralDistricts(@ApiParam(value = "Input-file") @FormDataParam("file") final InputStream inputStream) {

        LOG.info("/v1/electoraldistricts/ POST request.");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final List<ElectoralDistrict> electoralDistricts = m_electoralDistrictParser.parseElectoralDistrictsFromClsInputStream(DomainConstants.SOURCE_INTERNAL, inputStream);

        for (final ElectoralDistrict electoralDistrict : electoralDistricts) {
            LOG.info("Region parsed from input: " + electoralDistrict.getCode());
        }

        if (!electoralDistricts.isEmpty()) {
            m_domain.persistElectoralDistricts(electoralDistricts);
            m_domain.reIndexEverything();
        }

        meta.setMessage("ElectoralDistricts added or modified: " + electoralDistricts.size());
        meta.setCode(200);

        return Response.ok(responseWrapper).build();

    }


    @DELETE
    @Path("{code}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single electoralDistrict. This means that the item status is set to Status.RETIRED.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response retireElectoralDistrict(@ApiParam(value = "ElectoralDistrict code.") @PathParam("code") final String code) {

        LOG.info("/v1/electoraldistricts/" + code + " DELETE request.");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final ElectoralDistrict electoralDistrict = m_electoralDistrictRepository.findByCode(code);

        if (electoralDistrict != null) {
            electoralDistrict.setStatus(Status.RETIRED.toString());
            m_electoralDistrictRepository.save(electoralDistrict);
            m_domain.reIndexEverything();
        }

        meta.setMessage("ElectoralDistrict marked as RETIRED!");
        meta.setCode(200);

        return Response.ok(responseWrapper).build();

    }

}
