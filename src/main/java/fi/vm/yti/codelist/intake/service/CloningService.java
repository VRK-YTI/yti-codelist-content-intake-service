package fi.vm.yti.codelist.intake.service;

import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;

import java.util.UUID;

public interface CloningService {

    CodeScheme findById(final UUID id);
    CodeScheme findCodeSchemeAndEagerFetchTheChildren(final UUID id);

    Code cloneCode(Code code, CodeScheme newCodeScheme);

    CodeScheme cloneCodeScheme(CodeScheme codeScheme);

    CodeSchemeDTO topLevelClone(CodeSchemeDTO codeSchemeDTO, String codeRegistryCodeValue, String originalCodeSchemeUuid);
}
