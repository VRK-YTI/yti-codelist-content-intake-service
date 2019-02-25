package fi.vm.yti.codelist.intake.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import fi.vm.yti.codelist.common.dto.ErrorModel;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class IncompleteSetOfCodesTryingToGetImportedToACumulativeCodeScheme extends YtiCodeListException {
    public IncompleteSetOfCodesTryingToGetImportedToACumulativeCodeScheme(final ErrorModel errorModel) {
        super(errorModel);
    }
}