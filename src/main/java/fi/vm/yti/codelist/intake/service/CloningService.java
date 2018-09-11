package fi.vm.yti.codelist.intake.service;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;

import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ExternalReference;

public interface CloningService {

    CodeScheme findById(final UUID id);

    CodeScheme findCodeSchemeAndEagerFetchTheChildren(final UUID id);

    Code cloneCode(final Code code,
                   final CodeScheme newCodeScheme,
                   final Map<UUID, ExternalReference> externalReferenceMap);

    CodeSchemeDTO cloneCodeSchemeWithAllThePlumbing(final CodeSchemeDTO codeSchemeDTO,
                                                    final String codeRegistryCodeValue,
                                                    final String originalCodeSchemeUuid);

    LinkedHashSet<CodeScheme> getPreviousVersions(final UUID uuid,
                                                  final LinkedHashSet<CodeScheme> result);
}
