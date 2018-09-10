package fi.vm.yti.codelist.intake.exception.exceptionmapping;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.exception.ErrorConstants;
import fi.vm.yti.codelist.intake.model.Meta;

@Provider
public class UncaughtExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = LoggerFactory.getLogger(UncaughtExceptionMapper.class);

    @Override
    public Response toResponse(final Exception e) {
        LOG.error("Uncaught exception: " + e.getMessage(), e);
        final ResponseWrapper wrapper = new ResponseWrapper();
        final Meta meta = new Meta();
        meta.setMessage(ErrorConstants.ERR_MSG_USER_500);
        meta.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        wrapper.setMeta(meta);
        return Response.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).entity(wrapper).build();
    }
}
