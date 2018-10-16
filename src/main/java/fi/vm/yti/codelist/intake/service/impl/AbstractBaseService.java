package fi.vm.yti.codelist.intake.service.impl;

import fi.vm.yti.codelist.intake.model.CodeScheme;
import static fi.vm.yti.codelist.intake.parser.impl.AbstractBaseParser.*;

public interface AbstractBaseService {

    default boolean isServiceClassificationCodeScheme(final CodeScheme codeScheme) {
        return isCodeSchemeWithRegistryAndCodeValue(codeScheme, JUPO_REGISTRY, YTI_DATACLASSIFICATION_INFODOMAIN_CODESCHEME);
    }

    default boolean isLanguageCodeCodeScheme(final CodeScheme codeScheme) {
        return isCodeSchemeWithRegistryAndCodeValue(codeScheme, YTI_REGISTRY, YTI_LANGUAGECODE_CODESCHEME);
    }

    default boolean isCodeSchemeWithRegistryAndCodeValue(final CodeScheme codeScheme,
                                                         final String codeRegistryCodeValue,
                                                         final String codeValue) {
        return codeScheme.getCodeRegistry().getCodeValue().equalsIgnoreCase(codeRegistryCodeValue) && codeScheme.getCodeValue().equalsIgnoreCase(codeValue);
    }
}
