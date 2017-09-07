package fi.vm.yti.cls.intake.resource;

import fi.vm.yti.cls.common.model.MagistrateServiceUnit;
import fi.vm.yti.cls.common.model.Meta;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.intake.api.MetaResponseWrapper;
import fi.vm.yti.cls.intake.domain.Domain;
import fi.vm.yti.cls.intake.domain.DomainConstants;
import fi.vm.yti.cls.intake.jpa.MagistrateServiceUnitRepository;
import fi.vm.yti.cls.intake.parser.MagistrateServiceUnitParser;
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
 * Content Intake Service: REST resources for magistrateServiceUnits.
 */
@Component
@Path("/v1/magistrateserviceunits")
@Api(value = "magistrateserviceunits", description = "Operations for creating, deleting and updating magistrateserviceunits.")
@Produces("text/plain")
public class MagistrateServiceUnitResource {

    private static final Logger LOG = LoggerFactory.getLogger(MagistrateServiceUnitResource.class);

    private final Domain domain;

    private final MagistrateServiceUnitParser magistrateServiceUnitParser;

    private final MagistrateServiceUnitRepository magistrateServiceUnitRepository;


    @Inject
    public MagistrateServiceUnitResource(final Domain domain,
                                         final MagistrateServiceUnitParser magistrateServiceUnitParser,
                                         final MagistrateServiceUnitRepository magistrateServiceUnitRepository) {

        this.domain = domain;

        this.magistrateServiceUnitParser = magistrateServiceUnitParser;

        this.magistrateServiceUnitRepository = magistrateServiceUnitRepository;

    }


    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses magistrateserviceunits from CSV-source file with ',' delimiter.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdateMagistrateServiceUnits(@ApiParam(value = "Input-file") @FormDataParam("file") final InputStream inputStream) {

        LOG.info("/v1/magistrateserviceunits/ POST request.");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final List<MagistrateServiceUnit> magistrateServiceUnits = magistrateServiceUnitParser.parseMagistrateServiceUnitsFromClsInputStream(DomainConstants.SOURCE_INTERNAL, inputStream);

        for (final MagistrateServiceUnit magistrateServiceUnit : magistrateServiceUnits) {
            LOG.info("MagistrateServiceUnits parsed from input: " + magistrateServiceUnit.getCodeValue());
        }

        if (!magistrateServiceUnits.isEmpty()) {
            domain.persistMagistrateServiceUnits(magistrateServiceUnits);
            domain.reIndexEverything();
        }

        meta.setMessage("MagistrateServiceUnits added or modified: " + magistrateServiceUnits.size());
        meta.setCode(200);

        return Response.ok(responseWrapper).build();

    }


    @DELETE
    @Path("{codeValue}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single magistrateServiceUnit. This means that the item status is set to Status.RETIRED.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response retireMagistrateServiceUnit(@ApiParam(value = "MagistrateServiceUnit code.") @PathParam("codeValue") final String codeValue) {

        LOG.info("/v1/magistrateserviceunits/" + codeValue + " DELETE request.");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final MagistrateServiceUnit magistrateServiceUnit = magistrateServiceUnitRepository.findByCodeValue(codeValue);

        if (magistrateServiceUnit != null) {
            magistrateServiceUnit.setStatus(Status.RETIRED.toString());
            magistrateServiceUnitRepository.save(magistrateServiceUnit);
            domain.reIndexEverything();
        }

        meta.setMessage("MagistrateServiceUnit marked as RETIRED!");
        meta.setCode(200);

        return Response.ok(responseWrapper).build();

    }

}
