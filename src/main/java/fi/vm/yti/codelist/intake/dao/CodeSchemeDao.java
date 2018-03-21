package fi.vm.yti.codelist.intake.dao;

import java.util.Set;
import java.util.UUID;

import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;

public interface CodeSchemeDao {

    void delete(final CodeScheme codeScheme);

    CodeScheme findById(final UUID id);

    Set<CodeScheme> findAll();

    CodeScheme findByCodeRegistryCodeValueAndCodeValue(final String codeRegistryCodeValue, final String codeSchemeCodeValue);

    CodeScheme updateCodeSchemeFromDto(final CodeRegistry codeRegistry, final CodeSchemeDTO codeSchemeDto);

    Set<CodeScheme> updateCodeSchemesFromDtos(final CodeRegistry codeRegistry, final Set<CodeSchemeDTO> codeSchemeDtos);
}
