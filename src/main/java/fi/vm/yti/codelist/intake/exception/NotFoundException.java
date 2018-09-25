package fi.vm.yti.codelist.intake.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_404;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends YtiCodeListException {

    public NotFoundException() {
        super(new ErrorModel(HttpStatus.NOT_FOUND.value(), ERR_MSG_USER_404));
    }
}
