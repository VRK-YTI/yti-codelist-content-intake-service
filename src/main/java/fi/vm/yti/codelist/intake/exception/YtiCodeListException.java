package fi.vm.yti.codelist.intake.exception;

import fi.vm.yti.codelist.common.dto.ErrorModel;

public class YtiCodeListException extends RuntimeException {

    protected final ErrorModel errorModel;

    public YtiCodeListException(final ErrorModel errorModel) {
        super(errorModel.getMessage());
        this.errorModel = errorModel;
    }

    public ErrorModel getErrorModel() {
        return errorModel;
    }
}
