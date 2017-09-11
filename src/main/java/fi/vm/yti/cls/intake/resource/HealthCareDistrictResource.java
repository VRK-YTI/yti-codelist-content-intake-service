package fi.vm.yti.cls.intake.resource;

import fi.vm.yti.cls.common.model.HealthCareDistrict;
import fi.vm.yti.cls.common.model.Meta;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.intake.api.MetaResponseWrapper;
import fi.vm.yti.cls.intake.domain.Domain;
import fi.vm.yti.cls.intake.domain.DomainConstants;
import fi.vm.yti.cls.intake.jpa.HealthCareDistrictRepository;
import fi.vm.yti.cls.intake.parser.HealthCareDistrictParser;
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
 * Content Intake Service: REST resources for healthCareDistricts.
 */
@Component
@Path("/v1/healthCareDistricts")
@Api(value = "healthCareDistricts", description = "Operations for creating, deleting and updating healthCareDistricts.")
@Produces("text/plain")
public class HealthCareDistrictResource {

    private static final Logger LOG = LoggerFactory.getLogger(HealthCareDistrictResource.class);
    private final Domain domain;
    private final HealthCareDistrictParser healthCareDistrictParser;
    private final HealthCareDistrictRepository healthCareDistrictRepository;

    @Inject
    public HealthCareDistrictResource(final Domain domain,
                                      final HealthCareDistrictParser healthCareDistrictParser,
                                      final HealthCareDistrictRepository healthCareDistrictRepository) {
        this.domain = domain;
        this.healthCareDistrictParser = healthCareDistrictParser;
        this.healthCareDistrictRepository = healthCareDistrictRepository;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses healthCareDistricts from CSV-source file with ',' delimiter.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdateHealthCareDistricts(@ApiParam(value = "Input-file") @FormDataParam("file") final InputStream inputStream) {
        LOG.info("/v1/healthcaredistricts/ POST request.");
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        final List<HealthCareDistrict> healthCareDistricts = healthCareDistrictParser.parseHealthCareDistrictsFromClsInputStream(DomainConstants.SOURCE_INTERNAL, inputStream);
        for (final HealthCareDistrict healthCareDistrict : healthCareDistricts) {
            LOG.info("Region parsed from input: " + healthCareDistrict.getCodeValue());
        }
        if (!healthCareDistricts.isEmpty()) {
            domain.persistHealthCareDistricts(healthCareDistricts);
            domain.reIndexEverything();
        }
        meta.setMessage("HealthCareDistricts added or modified: " + healthCareDistricts.size());
        meta.setCode(200);
        return Response.ok(responseWrapper).build();
    }

    @DELETE
    @Path("{codeValue}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single healthCareDistrict. This means that the item status is set to Status.RETIRED.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response retireHealthCareDistrict(@ApiParam(value = "HealthCareDistricts code.") @PathParam("codeValue") final String codeValue) {
        LOG.info("/v1/healthcaredistricts/" + codeValue + " DELETE request.");
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        final HealthCareDistrict healthCareDistrict = healthCareDistrictRepository.findByCodeValue(codeValue);
        if (healthCareDistrict != null) {
            healthCareDistrict.setStatus(Status.RETIRED.toString());
            healthCareDistrictRepository.save(healthCareDistrict);
            domain.reIndexEverything();
        }
        meta.setMessage("HealthCareDistrict marked as RETIRED.");
        meta.setCode(200);
        return Response.ok(responseWrapper).build();
    }

}
