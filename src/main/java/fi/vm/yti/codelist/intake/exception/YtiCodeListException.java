package fi.vm.yti.codelist.intake.exception;

import fi.vm.yti.codelist.common.model.ErrorModel;

public class YtiCodeListException extends RuntimeException {

    ErrorModel errorModel;

    public YtiCodeListException(ErrorModel errorModel) {
        super(errorModel.getMessage());
        this.errorModel = errorModel;
    }

    public ErrorModel getErrorModel() {
        return errorModel;
    }

    public void setErrorModel(ErrorModel errorModel) {
        this.errorModel = errorModel;
    }
}
