package fi.vm.yti.codelist.intake.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import fi.vm.yti.codelist.common.model.ErrorModel;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class MissingHeaderStatusException extends YtiCodeListException {

    public MissingHeaderStatusException(final ErrorModel errorModel) {
        super(errorModel);
    }
}