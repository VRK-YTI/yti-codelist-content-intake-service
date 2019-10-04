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

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg.ObjectWriterInjector;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import fi.vm.yti.codelist.common.dto.Meta;
import fi.vm.yti.codelist.common.dto.OrganizationDTO;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_ORGANIZATION;

@Component
@Path("/v1/organizations")
@Produces(MediaType.APPLICATION_JSON)
public class OrganizationResource implements AbstractBaseResource {

    private final OrganizationService organizationService;

    @Inject
    public OrganizationResource(final OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Organizations API.")
    @ApiResponse(responseCode = "200", description = "Returns organizations.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrganizationDTO.class))))
    @Transactional
    public Response getOrganizations(@Parameter(description = "Filter string (csl) for expanding specific child resources.", in = ParameterIn.QUERY) @QueryParam("expand") final String expand,
                                     @Parameter(description = "A boolean value for only returning organizations with code lists.", in = ParameterIn.QUERY) @QueryParam("onlyOrganizationsWithCodeSchemes") final boolean onlyOrganizationsWithCodeSchemes,
                                     @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty) {
        ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(FILTER_NAME_ORGANIZATION, expand), pretty));
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
