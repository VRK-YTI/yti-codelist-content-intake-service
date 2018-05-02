package fi.vm.yti.codelist.intake.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import fi.vm.yti.codelist.common.dto.ErrorModel;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class UnreachableTerminologyApiException extends YtiCodeListException {

    public UnreachableTerminologyApiException(final String errorMessage) {
        super(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorMessage));
    }
}
