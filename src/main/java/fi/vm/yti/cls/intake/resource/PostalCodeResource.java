package fi.vm.yti.cls.intake.resource;

import fi.vm.yti.cls.common.model.Meta;
import fi.vm.yti.cls.common.model.PostalCode;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.intake.api.MetaResponseWrapper;
import fi.vm.yti.cls.intake.domain.Domain;
import fi.vm.yti.cls.intake.domain.DomainConstants;
import fi.vm.yti.cls.intake.jpa.PostalCodeRepository;
import fi.vm.yti.cls.intake.parser.PostalCodeParser;
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
 * Content Intake Service: REST resources for postal codes.
 */
@Component
@Path("/v1/postalcodes")
@Api(value = "postalcodes", description = "Operations for creating, deleting and updating postal codes.")
@Produces("text/plain")
public class PostalCodeResource {

    private static final Logger LOG = LoggerFactory.getLogger(PostalCodeResource.class);
    private final Domain domain;
    private final PostalCodeParser postalCodeParser;
    private final PostalCodeRepository postalCodeRepository;

    @Inject
    public PostalCodeResource(final Domain domain,
                              final PostalCodeParser postalCodeParser,
                              final PostalCodeRepository postalCodeRepository) {
        this.domain = domain;
        this.postalCodeParser = postalCodeParser;
        this.postalCodeRepository = postalCodeRepository;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses postal codes from CSV-source file with ',' delimiter.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdatePostalCodes(@ApiParam(value = "Input-file") @FormDataParam("file") final InputStream inputStream) {
        LOG.info("/v1/postalcodes/ POST request.");
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        final List<PostalCode> postalCodes = postalCodeParser.parsePostalCodesFromClsInputStream(DomainConstants.SOURCE_INTERNAL, inputStream);
        for (final PostalCode postalCode : postalCodes) {
            LOG.info("Postal code parsed from input: " + postalCode.getCodeValue());
        }
        if (!postalCodes.isEmpty()) {
            domain.persistPostalCodes(postalCodes);
            domain.reIndexEverything();
        }
        meta.setMessage("Postal codes added or modified: " + postalCodes.size());
        meta.setCode(200);
        return Response.ok(responseWrapper).build();
    }

    @DELETE
    @Path("{codeValue}")
    @ApiResponse(code = 200, message = "Returns success.")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single postalCode. This means that the item status is set to Status.RETIRED.")
    public Response retirePostalCode(@ApiParam(value = "PostalCode code.") @PathParam("codeValue") final String codeValue) {
        LOG.info("/v1/postalcodes/" + codeValue + " DELETE request.");
        final Meta meta = new Meta();
        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);
        final PostalCode postalCode = postalCodeRepository.findByCodeValue(codeValue);
        if (postalCode != null) {
            postalCode.setStatus(Status.RETIRED.toString());
            postalCodeRepository.save(postalCode);
            domain.reIndexEverything();
        }
        meta.setMessage("PostalCode marked as RETIRED!");
        meta.setCode(200);
        return Response.ok(responseWrapper).build();
    }

}
