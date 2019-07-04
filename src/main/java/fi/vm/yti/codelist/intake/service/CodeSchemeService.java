package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import fi.vm.yti.codelist.common.dto.CodeDTO;
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
                                                                final String jsonPayload,
                                                                final boolean userIsCreatingANewVersionOfACodeSchene,
                                                                final String originalCodeSchemeId,
                                                                final boolean updatingExistingCodeScheme);

    Set<CodeSchemeDTO> parseAndPersistCodeSchemesFromSourceData(final String codeRegistryCodeValue,
                                                                final String format,
                                                                final InputStream inputStream,
                                                                final String jsonPayload,
                                                                final boolean userIsCreatingANewVersionOfACodeSchene,
                                                                final String originalCodeSchemeId,
                                                                final boolean updatingExistingCodeScheme);

    CodeSchemeDTO parseAndPersistCodeSchemeFromJson(final String codeRegistryCodeValue,
                                                    final String codeSchemeCodeValue,
                                                    final String jsonPayload);

    CodeSchemeDTO deleteCodeScheme(final String codeRegistryCodeValue,
                                   final String codeSchemeCodeValue,
                                   final LinkedHashSet<CodeSchemeDTO> codeSchemeDTOsToIndex);

    CodeSchemeDTO updateCodeSchemeFromDto(final String codeRegistryCodeValue,
                                          final CodeSchemeDTO codeSchemeDTO);

    CodeSchemeDTO updateCodeSchemeFromDto(final boolean internal,
                                          final String codeRegistryCodeValue,
                                          final CodeSchemeDTO codeSchemeDTO);

    void populateAllVersionsToCodeSchemeDTO(final CodeSchemeDTO codeSchemeDTO);

    LinkedHashSet<CodeSchemeDTO> getPreviousVersions(final UUID uuid,
                                                     final LinkedHashSet<CodeSchemeDTO> result);

    boolean canANewVersionOfACodeSchemeBeCreatedFromTheIncomingFileDirectly(final String codeRegistryCodeValue,
                                                                            final String format,
                                                                            final InputStream inputStream);

    LinkedHashSet<CodeDTO> getPossiblyMissingSetOfCodesOfANewVersionOfCumulativeCodeScheme(final Set<CodeDTO> previousVersionsCodes,
                                                                                           final Set<CodeDTO> codeDtos);

    LinkedHashSet<CodeSchemeDTO> handleMissingCodesOfACumulativeCodeScheme(final LinkedHashSet<CodeDTO> missingCodes);
}