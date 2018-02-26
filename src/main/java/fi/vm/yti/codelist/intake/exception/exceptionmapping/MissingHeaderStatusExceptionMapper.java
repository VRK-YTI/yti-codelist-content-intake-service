package fi.vm.yti.codelist.intake.exception.exceptionmapping;

import fi.vm.yti.codelist.intake.exception.MissingHeaderStatusException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class MissingHeaderStatusExceptionMapper extends BaseExceptionMapper implements ExceptionMapper<MissingHeaderStatusException> {
    @Override
    public Response toResponse(MissingHeaderStatusException ex) {
        return getResponse(ex);
    }
}

