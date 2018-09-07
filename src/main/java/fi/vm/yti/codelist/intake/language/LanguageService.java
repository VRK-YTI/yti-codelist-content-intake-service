package fi.vm.yti.codelist.intake.language;

import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.intake.dao.CodeDao;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_BAD_LANGUAGECODE;
import static fi.vm.yti.codelist.intake.parser.impl.AbstractBaseParser.YTI_LANGUAGECODE_CODESCHEME;
import static fi.vm.yti.codelist.intake.parser.impl.AbstractBaseParser.YTI_REGISTRY;

@Component
public class LanguageService {

    private CodeDao codeDao;
    private CodeSchemeDao codeSchemeDao;

    public LanguageService(@Lazy final CodeDao codeDao,
                           @Lazy final CodeSchemeDao codeSchemeDao) {

        this.codeDao = codeDao;
        this.codeSchemeDao = codeSchemeDao;
    }

    private Set<String> getAllLanguageCodes() {
        final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryCodeValueAndCodeValue(YTI_REGISTRY, YTI_LANGUAGECODE_CODESCHEME);
        return codeDao.getCodeSchemeCodeValues(codeScheme.getId());
    }

    public void validateInputLanguage(final CodeScheme codeScheme,
                                      final String languageCodeCodeValue) {
        final CodeScheme languageCodeScheme = codeSchemeDao.findByCodeRegistryCodeValueAndCodeValue(YTI_REGISTRY, YTI_LANGUAGECODE_CODESCHEME);
        final Code inputLanguageCode = codeDao.findByCodeSchemeAndCodeValue(languageCodeScheme, languageCodeCodeValue);
        if (!getAllLanguageCodes().contains(languageCodeCodeValue)) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_BAD_LANGUAGECODE));
        }
        boolean found = false;
        Set<Code> codeSchemeLanguageCodes = codeScheme.getLanguageCodes();
        if (codeSchemeLanguageCodes == null) {
            codeSchemeLanguageCodes = new HashSet<>();
        }
        for (final Code languageCode : codeSchemeLanguageCodes) {
            if (languageCode.getCodeValue().equals(languageCodeCodeValue)) {
                found = true;
            }
        }
        if (!found) {
            codeSchemeLanguageCodes.add(inputLanguageCode);
            codeSchemeDao.save(codeScheme);
        }
    }

}
