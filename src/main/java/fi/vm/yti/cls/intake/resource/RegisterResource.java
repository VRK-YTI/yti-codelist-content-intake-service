package fi.vm.yti.cls.intake.resource;

import fi.vm.yti.cls.common.model.Meta;
import fi.vm.yti.cls.common.model.Register;
import fi.vm.yti.cls.common.model.RegisterItem;
import fi.vm.yti.cls.common.model.Status;
import fi.vm.yti.cls.intake.api.MetaResponseWrapper;
import fi.vm.yti.cls.intake.domain.Domain;
import fi.vm.yti.cls.intake.domain.DomainConstants;
import fi.vm.yti.cls.intake.jpa.RegisterItemRepository;
import fi.vm.yti.cls.intake.jpa.RegisterRepository;
import fi.vm.yti.cls.intake.parser.RegisterItemParser;
import fi.vm.yti.cls.intake.parser.RegisterParser;
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
 * Content Intake Service: REST resources for registryItems.
 */
@Component
@Path("/v1/registers")
@Api(value = "registers", description = "Operations for creating, deleting and updating registerItems.")
@Produces("text/plain")
public class RegisterResource {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterResource.class);

    private final Domain m_domain;

    private final RegisterParser m_registerParser;

    private final RegisterRepository m_registerRepository;

    private final RegisterItemParser m_registerItemParser;

    private final RegisterItemRepository m_registerItemRepository;


    @Inject
    public RegisterResource(final Domain domain,
                            final RegisterParser registerParser,
                            final RegisterRepository registerRepository,
                            final RegisterItemParser registerItemParser,
                            final RegisterItemRepository registerItemRepository) {

        m_domain = domain;

        m_registerParser = registerParser;

        m_registerRepository = registerRepository;

        m_registerItemParser = registerItemParser;

        m_registerItemRepository = registerItemRepository;

    }


    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses registers from CSV-source file with ',' delimiter.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdateRegisters(@ApiParam(value = "Input-file") @FormDataParam("file") final InputStream inputStream) {

        LOG.info("/v1/registers/ POST request.");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final List<Register> registers = m_registerParser.parseRegistersFromClsInputStream(DomainConstants.SOURCE_INTERNAL, inputStream);

        for (final Register register : registers) {
            LOG.info("Register parsed from input: " + register.getCode());
        }

        if (!registers.isEmpty()) {
            m_domain.persistRegisters(registers);
            m_domain.reIndexRegisters();
        }

        meta.setMessage("Registers added or modified: " + registers.size());
        meta.setCode(200);

        return Response.ok(responseWrapper).build();

    }


    @POST
    @Path("{register}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Parses registerItems from CSV-source file with ',' delimiter.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response addOrUpdateRegisterItems(@ApiParam(value = "Register code") @PathParam("register") final String registerCode,
                                             @ApiParam(value = "Input-file") @FormDataParam("file") final InputStream inputStream) {

        LOG.info("/v1/registers/ POST request for register: " + registerCode);

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        if (m_registerRepository.findByCode(registerCode) != null) {

            final List<RegisterItem> registerItems = m_registerItemParser.parseRegisterItemsFromClsInputStream(registerCode, DomainConstants.SOURCE_INTERNAL, inputStream);

            for (final RegisterItem registerItem : registerItems) {
                LOG.info("RegisterItem parsed from input: " + registerItem.getCode());
            }

            if (!registerItems.isEmpty()) {
                m_domain.persistRegisterItems(registerItems);
                m_domain.reIndexRegisterItems(registerCode);
            }

            meta.setMessage("RegisterItems added or modified: " + registerItems.size());
            meta.setCode(200);
            return Response.ok(responseWrapper).build();
        }

        meta.setMessage("Register with code: " + registerCode + " does not exist yet, please creater register first.");
        meta.setCode(404);

        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(responseWrapper).build();



    }


    @DELETE
    @Path("{register}/{code}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @ApiOperation(value = "Deletes a single registerItem. This means that the item status is set to Status.RETIRED.")
    @ApiResponse(code = 200, message = "Returns success.")
    public Response retireRegisterItem(@ApiParam(value = "Register code.") @PathParam("code") final String register,
                                       @ApiParam(value = "RegisterItem code.") @PathParam("code") final String code) {

        LOG.info("/v1/registers/" + register + "/" + code + " DELETE request.");

        final Meta meta = new Meta();

        final MetaResponseWrapper responseWrapper = new MetaResponseWrapper(meta);

        final RegisterItem registerItem = m_registerItemRepository.findByRegisterAndCode(register, code);

        if (registerItem != null) {
            registerItem.setStatus(Status.RETIRED.toString());
            m_registerItemRepository.save(registerItem);
            m_domain.reIndexRegisterItems(register);
        }

        meta.setMessage("RegisterItem marked as RETIRED!");
        meta.setCode(200);

        return Response.ok(responseWrapper).build();

    }

}
