package fi.vm.yti.codelist.intake.exception;

import fi.vm.yti.codelist.common.model.ErrorModel;

public class YtiCodeListException extends RuntimeException {

    protected ErrorModel errorModel;

    public YtiCodeListException(final ErrorModel errorModel) {
        super(errorModel.getMessage());
        this.errorModel = errorModel;
    }

    public ErrorModel getErrorModel() {
        return errorModel;
    }

    public void setErrorModel(final ErrorModel errorModel) {
        this.errorModel = errorModel;
    }
}
