package fi.vm.yti.codelist.intake.exception.exceptionmapping;

import fi.vm.yti.codelist.intake.exception.MissingRowValueCodeValueException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class MissingRowValueCodeValueExceptionMapper extends BaseExceptionMapper implements ExceptionMapper<MissingRowValueCodeValueException> {
    @Override
    public Response toResponse(MissingRowValueCodeValueException ex) {
        return getResponse(ex);
    }
}