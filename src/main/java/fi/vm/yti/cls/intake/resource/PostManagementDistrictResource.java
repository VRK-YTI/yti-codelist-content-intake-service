package fi.vm.yti.cls.intake.resource;

import fi.vm.yti.cls.common.model.Meta;
import fi.vm.yti.cls.common.model.PostManagementDistrict;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.intake.api.MetaResponseWrapper;
import fi.vm.yti.cls.intake.domain.Domain;
import fi.vm.yti.cls.intake.domain.DomainConstants;
import fi.vm.yti.cls.intake.jpa.PostManagementDistrictRepository;
import fi.vm.yti.cls.intake.parser.PostManagementDistrictParser;
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
@Path("/v1/postmanagementdistricts")
@Api(value = "postmanagementdistricts", description = "Operations for creating, deleting and updating postmanagementdistricts.")
@Produces("text/plain")
public class PostManagementDistrictResource {

    private static final Logger LOG = LoggerFactory.getLogger(PostManagementDistrictResource.class);

    private final Domain m_domain;

    private final PostManagementDistrictParser m_postManagementDistrictParser;

    private final PostManagementDistrictRepository m_postManagementDistrictRepository;


    @Inject
    public PostManagementDistrictResource(final Domain domain,
                                          final PostManagementDistrictParser postManagementDistrictParser,
                                          final PostManagementDistrictRepository postManagementDistrictRepository) {

        m_domain = domain;

        m_postManagementDistrictParser = postManagementDistrictParser;

        m_postManagementDistrictRepository = postManagementDistrictRepository;

    }


    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses postmanagementdistricts from CSV-source file with ',' delimiter.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdatePostManagementDistricts(@ApiParam(value = "Input-file") @FormDataParam("file") final InputStream inputStream) {

        LOG.info("/v1/postmanagementdistricts/ POST request.");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final List<PostManagementDistrict> postManagementDistricts = m_postManagementDistrictParser.parsePostManagementDistrictsFromClsInputStream(DomainConstants.SOURCE_INTERNAL, inputStream);

        if (!postManagementDistricts.isEmpty()) {
            m_domain.persistPostManagementDistricts(postManagementDistricts);
            m_domain.reIndexEverything();
        }

        meta.setMessage("PostManagementDistricts added or modified: " + postManagementDistricts.size());
        meta.setCode(200);

        return Response.ok(responseWrapper).build();

    }


    @DELETE
    @Path("{codeValue}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single postManagementDistrict. This means that the item status is set to Status.RETIRED.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response retirePostManagementDistrict(@ApiParam(value = "PostManagementDistrict code.") @PathParam("code") final String codeValue) {

        LOG.info("/v1/postmanagementdistricts/" + codeValue + " DELETE request.");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final PostManagementDistrict postManagementDistrict = m_postManagementDistrictRepository.findByCodeValue(codeValue);

        if (postManagementDistrict != null) {
            postManagementDistrict.setStatus(Status.RETIRED.toString());
            m_postManagementDistrictRepository.save(postManagementDistrict);
            m_domain.reIndexEverything();
        }

        meta.setMessage("PostManagementDistrict marked as RETIRED!");
        meta.setCode(200);

        return Response.ok(responseWrapper).build();

    }

}
