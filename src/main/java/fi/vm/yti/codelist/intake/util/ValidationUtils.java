package fi.vm.yti.codelist.intake.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.exception.CodeStatusTransitionWrongEndStatusException;
import fi.vm.yti.codelist.intake.exception.CodeStatusTransitionWrongInitialStatusException;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_CODE_STATUS_TRANSITION_WRONG_END_STATUS;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_CODE_STATUS_TRANSITION_WRONG_INITIAL_STATUS;

public interface ValidationUtils {

    static boolean validateStringAgainstRegexp(final String input,
                                               final String regexp) {
        final Pattern pattern = Pattern.compile(regexp);
        return pattern.matcher(input).matches();
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    static void validateCodeStatusTransitions(final String initialCodeStatus,
                                              final String endCodeStatus) {
        final Map<String, List<String>> allowedTransitions = new HashMap<>();
        allowedTransitions.put(Status.INCOMPLETE.toString(), new ArrayList<>(Arrays.asList(Status.DRAFT.toString())));
        allowedTransitions.put(Status.DRAFT.toString(), new ArrayList<>(Arrays.asList(Status.INCOMPLETE.toString(), Status.VALID.toString())));
        allowedTransitions.put(Status.VALID.toString(), new ArrayList<>(Arrays.asList(Status.RETIRED.toString(), Status.INVALID.toString())));
        allowedTransitions.put(Status.RETIRED.toString(), new ArrayList<>(Arrays.asList(Status.VALID.toString(), Status.INVALID.toString())));
        allowedTransitions.put(Status.INVALID.toString(), new ArrayList<>(Arrays.asList(Status.VALID.toString(), Status.RETIRED.toString())));

        if (!allowedTransitions.keySet().contains(initialCodeStatus)) {
            throw new CodeStatusTransitionWrongInitialStatusException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_CODE_STATUS_TRANSITION_WRONG_INITIAL_STATUS, initialCodeStatus));
        }

        if (!allowedTransitions.get(initialCodeStatus).contains(endCodeStatus)) {
            throw new CodeStatusTransitionWrongEndStatusException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_CODE_STATUS_TRANSITION_WRONG_END_STATUS, endCodeStatus));
        }
    }

    static boolean statusIsValidOrLater(Status status) {
        return status.compareTo(Status.VALID) == 0 || status.compareTo(Status.INVALID) == 0 || status.compareTo(Status.SUPERSEDED) == 0 ||
            status.compareTo(Status.RETIRED) == 0;
    }

    static boolean canDeleteCode(final String codeSchemeStatus) {
        if (!statusIsValidOrLater(Status.valueOf(codeSchemeStatus))) {
            return true;
        } else {
            return false;
        }
    }

    static boolean canDeleteMember(final String codeSchemeStatus) {
        if (!statusIsValidOrLater(Status.valueOf(codeSchemeStatus))) {
            return true;
        } else {
            return false;
        }
    }
}
