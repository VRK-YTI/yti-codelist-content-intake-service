package fi.vm.yti.codelist.intake.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import fi.vm.yti.codelist.intake.model.ErrorModel;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class UnauthorizedException extends YtiCodeListException {

    public UnauthorizedException(final ErrorModel errorModel) {
        super(errorModel);
    }
}
