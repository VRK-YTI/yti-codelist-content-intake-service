package fi.vm.yti.codelist.intake.resource;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.DefaultValue;
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
import com.fasterxml.jackson.databind.node.ObjectNode;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.dto.SystemMetaCountDTO;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.jpa.ExtensionRepository;
import fi.vm.yti.codelist.intake.jpa.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Component
@Path("/v1/system")
@Produces({ "text/plain;charset=utf-8", MediaType.APPLICATION_JSON + ";charset=UTF-8" })
@Tag(name = "System")
public class SystemResource implements AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(SystemResource.class);

    private final ApiUtils apiUtils;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRepository codeRepository;
    private final ExtensionRepository extensionRepository;
    private final MemberRepository memberRepository;

    public SystemResource(final ApiUtils apiUtils,
                          final CodeRegistryRepository codeRegistryRepository,
                          final CodeSchemeRepository codeSchemeRepository,
                          final CodeRepository codeRepository,
                          final ExtensionRepository extensionRepository,
                          final MemberRepository memberRepository) {
        this.apiUtils = apiUtils;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
        this.extensionRepository = extensionRepository;
        this.memberRepository = memberRepository;
    }

    @GET
    @Path("counts")
    @Operation(summary = "Get entity count meta information from the system")
    @ApiResponse(responseCode = "200", description = "Returns the meta information of entity counts from the system in given format (json / text).")
    @Produces({ "text/plain;charset=utf-8", MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    public Response getSystemCountMetaInformation(@Parameter(description = "Date after which resources have been modified.", in = ParameterIn.QUERY) @QueryParam("modifiedAfter") final String modifiedAfter,
                                                  @Parameter(description = "Date after which resources have been created.", in = ParameterIn.QUERY) @QueryParam("createdAfter") final String createdAfter,
                                                  @Parameter(description = "Format of output. Supports json and text, defaults to json.", in = ParameterIn.QUERY) @QueryParam("format") @DefaultValue("json") final String format) {
        final SystemMetaCountDTO countMeta;
        if (modifiedAfter != null) {
            final Date modifiedAfterDate = parseDateFromString(modifiedAfter);
            countMeta = new SystemMetaCountDTO(codeRegistryRepository.modifiedAfterCount(modifiedAfterDate), codeSchemeRepository.modifiedAfterCount(modifiedAfterDate), codeRepository.modifiedAfterCount(modifiedAfterDate), extensionRepository.modifiedAfterCount(modifiedAfterDate), memberRepository.modifiedAfterCount(modifiedAfterDate));
            return createCountMetaResponse(countMeta, format);
        } else if (createdAfter != null) {
            final Date createdAfterDate = parseDateFromString(createdAfter);
            countMeta = new SystemMetaCountDTO(codeRegistryRepository.createdAfterCount(createdAfterDate), codeSchemeRepository.createdAfterCount(createdAfterDate), codeRepository.createdAfterCount(createdAfterDate), extensionRepository.createdAfterCount(createdAfterDate), memberRepository.createdAfterCount(createdAfterDate));
            return createCountMetaResponse(countMeta, format);
        } else {
            countMeta = new SystemMetaCountDTO(codeRegistryRepository.count(), codeSchemeRepository.count(), codeRepository.count(), extensionRepository.count(), memberRepository.count());
            return createCountMetaResponse(countMeta, format);
        }
    }

    @GET
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Operation(summary = "Get configuration values as JSON")
    @ApiResponse(responseCode = "200", description = "Returns the configuration JSON element to the frontend related to this service.")
    public Response getConfig() {
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode configJson = mapper.createObjectNode();

        final String groupManagementPublicUrl = apiUtils.getGroupmanagementPublicUrl();
        final ObjectNode groupManagementConfig = mapper.createObjectNode();
        groupManagementConfig.put("url", groupManagementPublicUrl);
        configJson.set("groupManagementConfig", groupManagementConfig);

        final String datamodelPublicUrl = apiUtils.getDataModelPublicUrl();
        final ObjectNode dataModelConfig = mapper.createObjectNode();
        dataModelConfig.put("url", datamodelPublicUrl);
        configJson.set("dataModelConfig", dataModelConfig);

        final String terminologyPublicUrl = apiUtils.getTerminologyPublicUrl();
        final ObjectNode terminologyConfig = mapper.createObjectNode();
        terminologyConfig.put("url", terminologyPublicUrl);
        configJson.set("terminologyConfig", terminologyConfig);

        final String commentsPublicUrl = apiUtils.getCommentsPublicUrl();
        final ObjectNode commentsConfig = mapper.createObjectNode();
        commentsConfig.put("url", commentsPublicUrl);
        configJson.set("commentsConfig", commentsConfig);

        final boolean messagingEnabled = apiUtils.getMessagingEnabled();
        final ObjectNode messagingConfig = mapper.createObjectNode();
        messagingConfig.put("enabled", messagingEnabled);
        configJson.set("messagingConfig", messagingConfig);

        configJson.put("env", apiUtils.getEnv());
        configJson.put("defaultStatus", apiUtils.getDefaultStatus());
        configJson.put("codeSchemeSortMode", apiUtils.getCodeSchemeSortMode());
        return Response.ok(configJson).build();
    }

    private Date parseDateFromString(final String dateString) {
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return df.parse(dateString);
        } catch (final ParseException e) {
            LOG.error("Error parsing date from string in meta api: " + dateString);
        }
        return null;
    }

    private Response createCountMetaResponse(final SystemMetaCountDTO countMeta,
                                             final String format) {
        if (format.startsWith("json") || format.startsWith(MediaType.APPLICATION_JSON)) {
            return Response.ok(countMeta).build();
        } else if (format.startsWith("text")) {
            return Response.ok(createResponseStringMessage(countMeta)).build();
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "Format not supported for the system meta counts API: " + format));
        }
    }

    private String createResponseStringMessage(final SystemMetaCountDTO systemMetaCount) {
        final StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("CodeRegistries: " + systemMetaCount.getCodeRegistryCount());
        stringBuffer.append("\n");
        stringBuffer.append("CodeSchemes: " + systemMetaCount.getCodeSchemeCount());
        stringBuffer.append("\n");
        stringBuffer.append("Codes: " + systemMetaCount.getCodeCount());
        stringBuffer.append("\n");
        stringBuffer.append("Extensions: " + systemMetaCount.getExtensionCount());
        stringBuffer.append("\n");
        stringBuffer.append("Members: " + systemMetaCount.getMemberCount());
        return stringBuffer.toString();
    }
}
