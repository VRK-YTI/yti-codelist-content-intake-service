package fi.vm.yti.codelist.intake.exception;

import fi.vm.yti.codelist.intake.model.ErrorModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class UnreachableTerminologyApiException extends YtiCodeListException {

    public UnreachableTerminologyApiException(String errorMessage) {
        super(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorMessage));
    }
}
