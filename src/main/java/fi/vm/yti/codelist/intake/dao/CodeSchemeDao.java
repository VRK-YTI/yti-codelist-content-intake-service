package fi.vm.yti.codelist.intake.dao;

import java.util.Set;
import java.util.UUID;

import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;

public interface CodeSchemeDao {

    void delete(final CodeScheme codeScheme);

    void save(final CodeScheme codeScheme);

    void save(final Set<CodeScheme> codeSchemes);

    CodeScheme findById(final UUID id);

    CodeScheme findByUri(final String uri);

    Set<CodeScheme> findAll();

    CodeScheme findByCodeRegistryAndCodeValue(final CodeRegistry codeRegistry, final String codeValue);

    CodeScheme findByCodeRegistryCodeValueAndCodeValue(final String codeRegistryCodeValue, final String codeSchemeCodeValue);

    CodeScheme updateCodeSchemeFromDto(final CodeRegistry codeRegistry, final CodeSchemeDTO codeSchemeDto);

    Set<CodeScheme> updateCodeSchemesFromDtos(final CodeRegistry codeRegistry, final Set<CodeSchemeDTO> codeSchemeDtos, final boolean updateExternalReferences);
}
