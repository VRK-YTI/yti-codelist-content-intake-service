package fi.vm.yti.codelist.intake.exception.exceptionmapping;

import fi.vm.yti.codelist.intake.exception.CodeParsingException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class CodeParsingExceptionMapper extends BaseExceptionMapper implements ExceptionMapper<CodeParsingException> {

    @Override
    public Response toResponse(CodeParsingException ex) {
        return getResponse(ex);
    }
}
