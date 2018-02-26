package fi.vm.yti.codelist.intake.exception.exceptionmapping;

import fi.vm.yti.codelist.intake.exception.MissingHeaderCodeValueException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ExceptionMapper;

@Provider
public class MissingHeaderCodeValueExceptionMapper extends BaseExceptionMapper implements ExceptionMapper<MissingHeaderCodeValueException> {
    @Override
    public Response toResponse(MissingHeaderCodeValueException ex) {
        return getResponse(ex);
    }
}
