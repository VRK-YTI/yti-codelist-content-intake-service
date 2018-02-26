package fi.vm.yti.codelist.intake.exception.exceptionmapping;

import fi.vm.yti.codelist.intake.exception.MissingRowValueStatusException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class MissingRowValueStatusExceptionMapper extends BaseExceptionMapper implements ExceptionMapper<MissingRowValueStatusException> {
    @Override
    public Response toResponse(MissingRowValueStatusException ex) {
        return getResponse(ex);
    }
}
