package fi.vm.yti.codelist.intake.dao.impl;

import java.util.Map;

import fi.vm.yti.codelist.intake.language.LanguageService;
import fi.vm.yti.codelist.intake.model.CodeScheme;

public class AbstractDao {

    private LanguageService languageService;

    public AbstractDao(final LanguageService languageService) {
        this.languageService = languageService;
    }

    protected void validateAndAppendLanguagesForCodeScheme(final Map<String, String> localizable,
                                                           final CodeScheme codeScheme) {
        if (localizable != null && !localizable.isEmpty()) {
            localizable.keySet().forEach(key -> languageService.validateInputLanguageForCodeScheme(codeScheme, key, false));
        }
    }
}
