package fi.vm.yti.codelist.intake.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.springframework.http.HttpStatus;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_ERROR_DECODING_STRING;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_ERROR_ENCODING_STRING;

public interface EncodingUtils {

    static String urlDecodeString(final String string) {
        try {
            return URLDecoder.decode(string, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_ERROR_DECODING_STRING));
        }
    }

    static String urlDecodeCodeValue(final String codeValue) {
        final String stringToDecode;
        switch (codeValue) {
            case "U+002E":
            case "U%2B002E":
                stringToDecode = ".";
                break;
            case "U%2B002EU%2B002E":
            case "U+002EU+002E":
                stringToDecode = "..";
                break;
            default:
                stringToDecode = codeValue;
                break;
        }
        try {
            return URLDecoder.decode(stringToDecode, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_ERROR_DECODING_STRING));
        }
    }

    static String urlEncodeCodeValue(final String codeValue) {
        try {
            final String codeValueToBeEncoded;
            switch (codeValue) {
                case ".":
                    codeValueToBeEncoded = "U+002E";
                    break;
                case "..":
                    codeValueToBeEncoded = "U+002EU+002E";
                    break;
                default:
                    codeValueToBeEncoded = codeValue;
                    break;
            }
            return URLEncoder.encode(codeValueToBeEncoded, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_ERROR_ENCODING_STRING));
        }
    }
}
