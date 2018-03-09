package fi.vm.yti.codelist.intake.exception.exceptionmapping;

import fi.vm.yti.codelist.common.model.Meta;
import fi.vm.yti.codelist.intake.api.ResponseWrapper;
import fi.vm.yti.codelist.intake.exception.ErrorConstants;
import org.springframework.http.HttpStatus;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class UncaughtExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(final Exception ex) {
        ResponseWrapper wrapper = new ResponseWrapper();
        Meta meta = new Meta();
        meta.setMessage(ErrorConstants.ERR_MSG_USER_500);
        meta.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        wrapper.setMeta(meta);
        return Response.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).entity(wrapper).build();
    }
}
