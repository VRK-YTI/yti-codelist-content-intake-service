package fi.vm.yti.codelist.intake.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.springframework.http.HttpStatus;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_ERROR_DECODING_STRING;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_ERROR_ENCODING_STRING;

public interface StringUtils {

    static String urlDecodeString(final String string) {
        try {
            final String stringToDecode = string.replaceAll("\\+", "%2b");
            return URLDecoder.decode(stringToDecode, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_ERROR_DECODING_STRING));
        }
    }

    static String urlEncodeString(final String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_ERROR_ENCODING_STRING));
        }
    }
}
