package fi.vm.yti.codelist.intake.service;

import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ExternalReference;

import java.util.Map;
import java.util.UUID;

public interface CloningService {

    CodeScheme findById(final UUID id);

    CodeScheme findCodeSchemeAndEagerFetchTheChildren(final UUID id);

    Code cloneCode(final Code code,
                   final CodeScheme newCodeScheme,
                   final Map<UUID, ExternalReference> externalReferenceMap);

    CodeSchemeDTO cloneCodeSchemeWithAllThePlumbing(final CodeSchemeDTO codeSchemeDTO,
                                                    final String codeRegistryCodeValue,
                                                    final String originalCodeSchemeUuid);
}
