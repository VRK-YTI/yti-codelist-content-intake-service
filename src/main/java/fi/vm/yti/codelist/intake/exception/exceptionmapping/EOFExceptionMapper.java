package fi.vm.yti.codelist.intake.exception.exceptionmapping;

import java.io.EOFException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import fi.vm.yti.codelist.common.dto.Meta;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.exception.ErrorConstants;

@Provider
public class EOFExceptionMapper implements ExceptionMapper<EOFException> {

    private static final Logger LOG = LoggerFactory.getLogger(EOFExceptionMapper.class);

    @Override
    public Response toResponse(final EOFException e) {
        LOG.error("EOFException: " + e.getMessage(), e);
        final ResponseWrapper wrapper = new ResponseWrapper();
        final Meta meta = new Meta();
        meta.setMessage(ErrorConstants.ERR_MSG_USER_FILE_TRANSMISSION_ERROR);
        meta.setCode(HttpStatus.BAD_REQUEST.value());
        wrapper.setMeta(meta);
        return Response.status(HttpStatus.BAD_REQUEST.value()).entity(wrapper).build();
    }
}
