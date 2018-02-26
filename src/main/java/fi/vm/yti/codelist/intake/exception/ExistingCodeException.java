package fi.vm.yti.codelist.intake.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import fi.vm.yti.codelist.common.model.ErrorModel;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class ExistingCodeException extends YtiCodeListException {
    public ExistingCodeException(ErrorModel errorModel) {
        super(errorModel);
    }

}
