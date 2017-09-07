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

    private final Domain domain;

    private final ElectoralDistrictParser electoralDistrictParser;

    private final ElectoralDistrictRepository electoralDistrictRepository;


    @Inject
    public ElectoralDistrictResource(final Domain domain,
                                     final ElectoralDistrictParser electoralDistrictParser,
                                     final ElectoralDistrictRepository electoralDistrictRepository) {

        this.domain = domain;

        this.electoralDistrictParser = electoralDistrictParser;

        this.electoralDistrictRepository = electoralDistrictRepository;

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

        final List<ElectoralDistrict> electoralDistricts = electoralDistrictParser.parseElectoralDistrictsFromClsInputStream(DomainConstants.SOURCE_INTERNAL, inputStream);

        for (final ElectoralDistrict electoralDistrict : electoralDistricts) {
            LOG.info("ElectoralDistrict parsed from input: " + electoralDistrict.getCodeValue());
        }

        if (!electoralDistricts.isEmpty()) {
            domain.persistElectoralDistricts(electoralDistricts);
            domain.reIndexEverything();
        }

        meta.setMessage("ElectoralDistricts added or modified: " + electoralDistricts.size());
        meta.setCode(200);

        return Response.ok(responseWrapper).build();

    }


    @DELETE
    @Path("{codeValue}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single electoralDistrict. This means that the item status is set to Status.RETIRED.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response retireElectoralDistrict(@ApiParam(value = "ElectoralDistrict code.") @PathParam("codeValue") final String codeValue) {

        LOG.info("/v1/electoraldistricts/" + codeValue + " DELETE request.");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final ElectoralDistrict electoralDistrict = electoralDistrictRepository.findByCodeValue(codeValue);

        if (electoralDistrict != null) {
            electoralDistrict.setStatus(Status.RETIRED.toString());
            electoralDistrictRepository.save(electoralDistrict);
            domain.reIndexEverything();
        }

        meta.setMessage("ElectoralDistrict marked as RETIRED!");
        meta.setCode(200);

        return Response.ok(responseWrapper).build();

    }

}
