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

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.Meta;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.dto.InfoDomainDTO;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.service.CodeSchemeService;
import fi.vm.yti.codelist.intake.service.CodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_INFODOMAIN;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_500;
import static fi.vm.yti.codelist.intake.parser.impl.AbstractBaseParser.JUPO_REGISTRY;
import static fi.vm.yti.codelist.intake.parser.impl.AbstractBaseParser.YTI_DATACLASSIFICATION_INFODOMAIN_CODESCHEME;

@Component
@Path("/v1/infodomains")
@Api(value = "infodomains")
@Produces(MediaType.APPLICATION_JSON)
public class InfoDomainResource implements AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(InfoDomainResource.class);
    private final CodeSchemeService codeSchemeService;
    private final CodeService codeService;
    private final DataSource dataSource;

    @Inject
    public InfoDomainResource(final CodeSchemeService codeSchemeService,
                              final CodeService codeService,
                              final DataSource dataSource) {
        this.codeSchemeService = codeSchemeService;
        this.codeService = codeService;
        this.dataSource = dataSource;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Data classification (information domain) API for listing codes and counts.")
    @ApiResponse(code = 200, message = "Returns data classifications (information domains) and counts.")
    @Transactional
    public Response getInfoDomains(@ApiParam(value = "Filter string (csl) for expanding specific child resources.") @QueryParam("expand") final String expand,
                                   @ApiParam(value = "Language code for sorting results.") @QueryParam("language") final String language,
                                   @ApiParam(value = "Pretty format JSON output.") @QueryParam("pretty") final String pretty) {
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_INFODOMAIN, expand), pretty));
        final Meta meta = new Meta();
        final ResponseWrapper<InfoDomainDTO> wrapper = new ResponseWrapper<>();
        wrapper.setMeta(meta);
        final ObjectMapper mapper = createObjectMapper();
        final CodeSchemeDTO infoDomainScheme = codeSchemeService.findByCodeRegistryCodeValueAndCodeValue(JUPO_REGISTRY, YTI_DATACLASSIFICATION_INFODOMAIN_CODESCHEME);
        final Set<CodeDTO> codes = codeService.findByCodeSchemeId(infoDomainScheme.getId());
        final Set<InfoDomainDTO> infoDomains = new LinkedHashSet<>();
        final Map<String, Integer> statistics = getInfoDomainCounts();
        codes.forEach(code -> {
            final Integer count = statistics.get(code.getId().toString());
            final InfoDomainDTO infoDomainDTO = new InfoDomainDTO(code, count != null ? count : 0);
            infoDomains.add(infoDomainDTO);
        });
        if (language != null && !language.isEmpty()) {
            final List<InfoDomainDTO> sortedInfoDomains = new ArrayList<>(infoDomains);
            sortedInfoDomains.sort(Comparator.comparing(infoDomain -> infoDomain.getPrefLabel(language), Comparator.nullsLast(Comparator.reverseOrder())));
            final Set<InfoDomainDTO> sortedSet = new LinkedHashSet<>(sortedInfoDomains);
            wrapper.setResults(sortedSet);
        } else {
            wrapper.setResults(infoDomains);
        }
        meta.setCode(200);
        meta.setResultCount(infoDomains.size());
        mapper.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
        return Response.ok(wrapper).build();
    }

    private Map<String, Integer> getInfoDomainCounts() {
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
