package fi.vm.yti.codelist.intake.service.impl;

import java.util.HashSet;
import java.util.Set;

import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.intake.configuration.ApplicationConstants;
import fi.vm.yti.codelist.intake.dao.ExternalReferenceDao;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import static fi.vm.yti.codelist.intake.parser.impl.AbstractBaseParser.*;

public interface AbstractBaseService {

    boolean preventPossibleImplicitCodeDeletionDuringFileImport = false;

    default boolean isServiceClassificationCodeScheme(final CodeScheme codeScheme) {
        return isCodeSchemeWithRegistryAndCodeValue(codeScheme, JUPO_REGISTRY, ApplicationConstants.YTI_DATACLASSIFICATION_INFODOMAIN_CODESCHEME);
    }

    default boolean isLanguageCodeCodeScheme(final CodeScheme codeScheme) {
        return isCodeSchemeWithRegistryAndCodeValue(codeScheme, YTI_REGISTRY, ApplicationConstants.YTI_LANGUAGECODE_CODESCHEME);
    }

    default boolean isCodeSchemeWithRegistryAndCodeValue(final CodeScheme codeScheme,
                                                         final String codeRegistryCodeValue,
                                                         final String codeValue) {
        return codeScheme.getCodeRegistry().getCodeValue().equalsIgnoreCase(codeRegistryCodeValue) && codeScheme.getCodeValue().equalsIgnoreCase(codeValue);
    }

    default Set<ExternalReference> findOrCreateExternalReferences(final ExternalReferenceDao externalReferenceDao,
                                                                  final CodeScheme codeScheme,
                                                                  final Set<ExternalReferenceDTO> externalReferenceDtos) {
        if (externalReferenceDtos != null && !externalReferenceDtos.isEmpty()) {
            final Set<ExternalReference> externalReferences = new HashSet<>();
            externalReferenceDtos.forEach(externalReferenceDto -> {
                final ExternalReference externalReference = externalReferenceDao.createOrUpdateExternalReference(false, externalReferenceDto, codeScheme);
                if (externalReference != null) {
                    externalReferences.add(externalReference);
                }
            });
            return externalReferences;
        }
        return null;
    }

}
