package fi.vm.yti.codelist.intake.dao.impl;

import java.util.HashMap;
import java.util.Map;

import fi.vm.yti.codelist.intake.language.LanguageService;
import fi.vm.yti.codelist.intake.model.CodeScheme;

public class AbstractDao {

    private LanguageService languageService;

    public AbstractDao(final LanguageService languageService) {
        this.languageService = languageService;
    }

    protected Map<String, String> validateAndAppendLanguagesForCodeScheme(final Map<String, String> localizable,
                                                                          final CodeScheme codeScheme) {
        final Map<String, String> newLocalizable = new HashMap<>();
        if (localizable != null && !localizable.isEmpty()) {
            localizable.keySet().forEach(key -> {
                final String language = languageService.validateInputLanguageForCodeScheme(codeScheme, key, false);
                newLocalizable.put(language, localizable.get(key));
            });
        }
        return newLocalizable;
    }

    protected Map<String, String> validateLanguagesForLocalizable(final Map<String, String> localizable) {
        final Map<String, String> languageCorrectedLocalizable = new HashMap<>();
        if (localizable != null && !localizable.isEmpty()) {
            localizable.keySet().forEach(key -> {
                final String language = languageService.getLanguageCodeCodeValue(key);
                languageCorrectedLocalizable.put(language, localizable.get(key));
            });
        }
        return languageCorrectedLocalizable;
    }
}
