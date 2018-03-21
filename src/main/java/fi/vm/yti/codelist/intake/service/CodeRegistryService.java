package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.Set;

import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;

public interface CodeRegistryService {

    Set<CodeRegistryDTO> findAll();

    CodeRegistryDTO findByCodeValue(final String codeValue);

    Set<CodeRegistryDTO> parseAndPersistCodeRegistriesFromSourceData(final String format,
                                                                     final InputStream inputStream,
                                                                     final String jsonPayload);

    CodeRegistryDTO parseAndPersistCodeRegistryFromJson(final String codeRegistryCodeValue,
                                                        final String jsonPayload);
}
