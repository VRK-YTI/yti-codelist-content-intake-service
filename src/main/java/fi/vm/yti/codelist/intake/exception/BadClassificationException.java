package fi.vm.yti.codelist.intake.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import fi.vm.yti.codelist.intake.model.ErrorModel;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class BadClassificationException extends YtiCodeListException {

    public BadClassificationException(final ErrorModel errorModel) {
        super(errorModel);
    }
}
