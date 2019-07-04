package fi.vm.yti.codelist.intake.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import fi.vm.yti.codelist.common.dto.ErrorModel;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class InconsistencyInVersionHierarchyException extends YtiCodeListException {

    public InconsistencyInVersionHierarchyException(final ErrorModel errorModel) {
        super(errorModel);
    }
}
