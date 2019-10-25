package fi.vm.yti.codelist.intake.dao;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;

public interface CodeSchemeDao {

    void delete(final CodeScheme codeScheme);

    void save(final CodeScheme codeScheme);

    void save(final Set<CodeScheme> codeSchemes);

    void save(final Set<CodeScheme> codeSchemes,
              final boolean logChange);

    CodeScheme findById(final UUID id);

    CodeScheme findByUri(final String uri);

    Set<CodeScheme> findAll();

    Set<CodeScheme> findByCodeRegistryCodeValue(final String codeRegistryCodeValue);

    CodeScheme findByCodeRegistryAndCodeValue(final CodeRegistry codeRegistry,
                                              final String codeValue);

    CodeScheme findByCodeRegistryCodeValueAndCodeValue(final String codeRegistryCodeValue,
                                                       final String codeSchemeCodeValue);

    CodeScheme updateCodeSchemeFromDto(final boolean isAuthorized,
                                       final CodeRegistry codeRegistry,
                                       final CodeSchemeDTO codeSchemeDto);

    CodeScheme updateCodeSchemeFromDto(final CodeRegistry codeRegistry,
                                       final CodeSchemeDTO codeSchemeDto);

    Set<CodeScheme> updateCodeSchemesFromDtos(final boolean isAuthorized,
                                              final CodeRegistry codeRegistry,
                                              final Set<CodeSchemeDTO> codeSchemeDtos,
                                              final boolean updateExternalReferences);

    void updateContentModified(final UUID codeSchemeId);

    void updateContentModified(final UUID codeSchemeId,
                               final Date timeStamp);
}
