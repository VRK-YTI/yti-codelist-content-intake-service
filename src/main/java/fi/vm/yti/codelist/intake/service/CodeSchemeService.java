package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;

public interface CodeSchemeService {

    Set<CodeSchemeDTO> findAll();

    CodeSchemeDTO findById(final UUID id);

    Set<CodeSchemeDTO> findByCodeRegistryCodeValue(final String codeRegistryCodeValue);

    CodeSchemeDTO findByCodeRegistryCodeValueAndCodeValue(final String codeRegistryCodeValue,
                                                          final String codeSchemeCodeValue);

    Set<CodeSchemeDTO> parseAndPersistCodeSchemesFromSourceData(final boolean internal,
                                                                final String codeRegistryCodeValue,
                                                                final String format,
                                                                final InputStream inputStream,
                                                                final String jsonPayload);

    Set<CodeSchemeDTO> parseAndPersistCodeSchemesFromSourceData(final String codeRegistryCodeValue,
                                                                final String format,
                                                                final InputStream inputStream,
                                                                final String jsonPayload);

    CodeSchemeDTO parseAndPersistCodeSchemeFromJson(final String codeRegistryCodeValue,
                                                    final String codeSchemeCodeValue,
                                                    final String jsonPayload);

    CodeSchemeDTO deleteCodeScheme(final String codeRegistryCodeValue,
                                   final String codeSchemeCodeValue,
                                   final Set<CodeSchemeDTO> codeSchemeDTOsToIndex);

    CodeSchemeDTO updateCodeSchemeFromDto(final String codeRegistryCodeValue,
                                          final CodeSchemeDTO codeSchemeDTO);

    CodeSchemeDTO updateCodeSchemeFromDto(final boolean internal,
                                          final String codeRegistryCodeValue,
                                          final CodeSchemeDTO codeSchemeDTO);

    void populateAllVersionsToCodeSchemeDTO(final CodeSchemeDTO codeSchemeDTO);

    LinkedHashSet<CodeSchemeDTO> getPreviousVersions(final UUID uuid,
                                                     final LinkedHashSet<CodeSchemeDTO> result);
}