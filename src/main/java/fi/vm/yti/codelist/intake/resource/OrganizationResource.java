package fi.vm.yti.codelist.intake.resource;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;

import fi.vm.yti.codelist.common.dto.Meta;
import fi.vm.yti.codelist.common.dto.OrganizationDTO;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.service.OrganizationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_ORGANIZATION;

@Component
@Path("/v1/organizations")
@Api(value = "organizations")
@Produces(MediaType.APPLICATION_JSON)
public class OrganizationResource implements AbstractBaseResource {

    private final OrganizationService organizationService;

    @Inject
    public OrganizationResource(final OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Organizations API.")
    @ApiResponse(code = 200, message = "Returns organizations.")
    @Transactional
    public Response getOrganizations(@ApiParam(value = "Filter string (csl) for expanding specific child resources.") @QueryParam("expand") final String expand,
                                     @ApiParam(value = "A boolean value for only returning organizations with code lists.") @QueryParam("onlyOrganizationsWithCodeSchemes") final boolean onlyOrganizationsWithCodeSchemes) {
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_ORGANIZATION, expand)));
        final Meta meta = new Meta();
        final ResponseWrapper<OrganizationDTO> wrapper = new ResponseWrapper<>();
        wrapper.setMeta(meta);
        final ObjectMapper mapper = createObjectMapper();
        final Set<OrganizationDTO> organizations = organizationService.findByRemovedIsFalse(onlyOrganizationsWithCodeSchemes);
        meta.setCode(200);
        meta.setResultCount(organizations.size());
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        wrapper.setResults(organizations);
        return Response.ok(wrapper).build();
    }
}
