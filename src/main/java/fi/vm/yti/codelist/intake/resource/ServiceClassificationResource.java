package fi.vm.yti.codelist.intake.resource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.Meta;
import fi.vm.yti.codelist.common.model.ServiceClassification;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

/**
 * Content Intake Service: REST resources for ExternalReference entity management.
 */
@Component
@Path("/v1/serviceclassifications")
@Api(value = "serviceclassifications", description = "Operations for ServiceClassifications.")
@Produces(MediaType.APPLICATION_JSON)
public class ServiceClassificationResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalReferenceResource.class);
    private static final String YTI_REGISTRY_CODEVALUE = "yti";
    private static final String YTI_SERVICECLASSIFICATION_SCHEME_CODEVALUE = "serviceclassifications";
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRepository codeRepository;
    private final DataSource dataSource;

    @Inject
    public ServiceClassificationResource(final CodeRegistryRepository codeRegistryRepository,
                                         final CodeSchemeRepository codeSchemeRepository,
                                         final CodeRepository codeRepository,
                                         final DataSource dataSource) {
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
        this.dataSource = dataSource;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Service classification API.")
    @ApiResponse(code = 200, message = "Returns ServiceClassifications.")
    public Response getServiceClassifications(@ApiParam(value = "Filter string (csl) for expanding specific child resources.") @QueryParam("expand") final String expand) {
        logApiRequest(LOG, METHOD_GET, API_PATH_VERSION_V1, API_PATH_SERVICECLASSIFICATIONS + "/");
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_SERVICECLASSIFICATION, expand)));
        final Meta meta = new Meta();
        final ResponseWrapper<ServiceClassification> wrapper = new ResponseWrapper<>();
        wrapper.setMeta(meta);
        final ObjectMapper mapper = new ObjectMapper();
        final CodeRegistry ytiRegistry = codeRegistryRepository.findByCodeValue(YTI_REGISTRY_CODEVALUE);
        final CodeScheme serviceClassificationsScheme = codeSchemeRepository.findByCodeRegistryAndCodeValue(ytiRegistry, YTI_SERVICECLASSIFICATION_SCHEME_CODEVALUE);
        final Set<Code> codes = codeRepository.findByCodeScheme(serviceClassificationsScheme);
        final Set<ServiceClassification> serviceClassifications = new LinkedHashSet<>();
        final Map<String, Integer> statistics = new HashMap<>();
        try (final Connection connection = dataSource.getConnection()) {
            final ResultSet results = connection.prepareStatement("SELECT code_id, count(code_id) from service_codescheme_code group by code_id").executeQuery();
            while (results.next()) {
                statistics.put(results.getString(1), results.getInt(2));
            }
        } catch (SQLException e) {
            LOG.error("SQL query failed: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        codes.forEach(code -> {
            final Integer count = statistics.get(code.getId().toString());
            final ServiceClassification serviceClassification = new ServiceClassification(code, count != null ? count : 0);
            serviceClassifications.add(serviceClassification);
        });
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        wrapper.setResults(serviceClassifications);
        return Response.ok(wrapper).build();
    }
}
