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

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.intake.dto.SystemMetaCountDTO;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.jpa.ExtensionRepository;
import fi.vm.yti.codelist.intake.jpa.MemberRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;

@Component
@Path("/v1/system")
@Api(value = "system")
@Produces({ "text/plain;charset=utf-8", MediaType.APPLICATION_JSON + ";charset=UTF-8" })
public class SystemResource implements AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(SystemResource.class);

    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRepository codeRepository;
    private final ExtensionRepository extensionRepository;
    private final MemberRepository memberRepository;

    public SystemResource(final CodeRegistryRepository codeRegistryRepository,
                          final CodeSchemeRepository codeSchemeRepository,
                          final CodeRepository codeRepository,
                          final ExtensionRepository extensionRepository,
                          final MemberRepository memberRepository) {
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
        this.extensionRepository = extensionRepository;
        this.memberRepository = memberRepository;
    }

    @GET
    @Path("counts")
    @ApiOperation(value = "Get entity count meta information from the system", response = String.class)
    @ApiResponse(code = 200, message = "Returns the meta information of entity counts from the system in given format (json / text).")
    @Produces({ "text/plain;charset=utf-8", MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    public Response getSystemCountMetaInformation(@ApiParam(value = "Date after which resources have been modified.") @QueryParam("modifiedAfter") final String modifiedAfter,
                                                  @ApiParam(value = "Date after which resources have been created.") @QueryParam("createdAfter") final String createdAfter,
                                                  @ApiParam(value = "Format of output. Supports json and text, defaults to json.") @QueryParam("format") @DefaultValue("json") final String format) {
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
        if (format.equalsIgnoreCase("json") || format.equalsIgnoreCase(MediaType.APPLICATION_JSON)) {
            return Response.ok(countMeta).build();
        } else if (format != null && format.startsWith("text")) {
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
