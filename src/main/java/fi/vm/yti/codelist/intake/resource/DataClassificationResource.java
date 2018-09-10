package fi.vm.yti.codelist.intake.resource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.dao.CodeDao;
import fi.vm.yti.codelist.intake.dao.CodeRegistryDao;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Meta;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_DATACLASSIFICATION;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_500;
import static fi.vm.yti.codelist.intake.parser.impl.AbstractBaseParser.JUPO_REGISTRY;
import static fi.vm.yti.codelist.intake.parser.impl.AbstractBaseParser.YTI_DATACLASSIFICATION_CODESCHEME;

@Component
@Path("/v1/dataclassifications")
@Api(value = "dataclassifications")
@Produces(MediaType.APPLICATION_JSON)
public class DataClassificationResource implements AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(DataClassificationResource.class);
    private final CodeRegistryDao codeRegistryDao;
    private final CodeSchemeDao codeSchemeDao;
    private final CodeDao codeDao;
    private final DataSource dataSource;

    @Inject
    public DataClassificationResource(final CodeRegistryDao codeRegistryDao,
                                      final CodeSchemeDao codeSchemeDao,
                                      final CodeDao codeDao,
                                      final DataSource dataSource) {
        this.codeRegistryDao = codeRegistryDao;
        this.codeSchemeDao = codeSchemeDao;
        this.codeDao = codeDao;
        this.dataSource = dataSource;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Data classification API for listing codes and counts.")
    @ApiResponse(code = 200, message = "Returns data classifications and counts.")
    @Transactional
    public Response getDataClassifications(@ApiParam(value = "Filter string (csl) for expanding specific child resources.") @QueryParam("expand") final String expand,
                                           @ApiParam(value = "Language code for sorting results.") @QueryParam("language") final String language) {
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_DATACLASSIFICATION, expand)));
        final Meta meta = new Meta();
        final ResponseWrapper<DataClassification> wrapper = new ResponseWrapper<>();
        wrapper.setMeta(meta);
        final ObjectMapper mapper = createObjectMapper();
        final CodeRegistry ytiRegistry = codeRegistryDao.findByCodeValue(JUPO_REGISTRY);
        final CodeScheme dataClassificationsScheme = codeSchemeDao.findByCodeRegistryAndCodeValue(ytiRegistry, YTI_DATACLASSIFICATION_CODESCHEME);
        final Set<Code> codes = codeDao.findByCodeSchemeIdAndBroaderCodeIdIsNull(dataClassificationsScheme.getId());
        final Set<DataClassification> dataClassifications = new LinkedHashSet<>();
        final Map<String, Integer> statistics = getClassificationCounts();
        codes.forEach(code -> {
            final Integer count = statistics.get(code.getId().toString());
            final DataClassification dataClassification = new DataClassification(code, count != null ? count : 0);
            dataClassifications.add(dataClassification);
        });
        if (language != null && !language.isEmpty()) {
            final List<DataClassification> sortedClassifications = new ArrayList<>(dataClassifications);
            sortedClassifications.sort(Comparator.comparing(dataClassification -> dataClassification.getPrefLabel(language)));
            final Set<DataClassification> sortedSet = new LinkedHashSet<>(sortedClassifications);
            wrapper.setResults(sortedSet);
        } else {
            wrapper.setResults(dataClassifications);
        }
        meta.setCode(200);
        meta.setResultCount(dataClassifications.size());
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        return Response.ok(wrapper).build();
    }

    private Map<String, Integer> getClassificationCounts() {
        final Map<String, Integer> statistics = new HashMap<>();
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement("SELECT code_id, count(code_id) FROM service_codescheme_code GROUP BY code_id");
             final ResultSet results = ps.executeQuery()) {
            while (results.next()) {
                statistics.put(results.getString(1), results.getInt(2));
            }
        } catch (final SQLException e) {
            LOG.error("SQL query failed: ", e);
            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
        }
        return statistics;
    }
}
