package fi.vm.yti.cls.intake.resource;

import fi.vm.yti.cls.common.model.Meta;
import fi.vm.yti.cls.common.model.Municipality;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.intake.api.MetaResponseWrapper;
import fi.vm.yti.cls.intake.domain.Domain;
import fi.vm.yti.cls.intake.domain.DomainConstants;
import fi.vm.yti.cls.intake.jpa.MunicipalityRepository;
import fi.vm.yti.cls.intake.parser.MunicipalityParser;
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
 * Content Intake Service: REST resources for municipalities.
 */
@Component
@Path("/v1/municipalities")
@Api(value = "municipalities", description = "Operations for creating, deleting and updating municipalities.")
@Produces("text/plain")
public class MunicipalityResource {

    private static final Logger LOG = LoggerFactory.getLogger(MunicipalityResource.class);

    private final Domain domain;

    private final MunicipalityParser municipalityParser;

    private final MunicipalityRepository municipalityRepository;


    @Inject
    public MunicipalityResource(final Domain domain,
                                final MunicipalityParser municipalityParser,
                                final MunicipalityRepository municipalityRepository) {

        this.domain = domain;

        this.municipalityParser = municipalityParser;

        this.municipalityRepository = municipalityRepository;

    }


    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses municipalities from CSV-source file with ',' delimiter.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdateMunicipalities(@ApiParam(value = "Input-file") @FormDataParam("file") final InputStream inputStream) {

        LOG.info("/v1/municipalities/ POST request.");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final List<Municipality> municipalities = municipalityParser.parseMunicipalitiesFromClsInputStream(DomainConstants.SOURCE_INTERNAL, inputStream);

        for (final Municipality municipality : municipalities) {
            LOG.info("Municipality parsed from input: " + municipality.getCodeValue());
        }

        if (!municipalities.isEmpty()) {
            domain.persistMunicipalities(municipalities);
            domain.reIndexEverything();
        }

        meta.setMessage("Municipalities added or modified: " + municipalities.size());
        meta.setCode(200);

        return Response.ok(responseWrapper).build();

    }


    @DELETE
    @Path("{codeValue}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single municipality. This means that the item status is set to Status.RETIRED.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response retireMunicipality(@ApiParam(value = "Municipality code.") @PathParam("codeValue") final String codeValue) {

        LOG.info("/v1/municipalities/" + codeValue + " DELETE request.");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final Municipality municipality = municipalityRepository.findByCodeValue(codeValue);

        if (municipality != null) {
            municipality.setStatus(Status.RETIRED.toString());
            municipalityRepository.save(municipality);
            domain.reIndexEverything();
        }

        meta.setMessage("Municipality marked as RETIRED!");
        meta.setCode(200);

        return Response.ok(responseWrapper).build();

    }

}
