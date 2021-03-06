package fi.vm.yti.codelist.intake.dao;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;

public interface CodeDao {

    void save(final Code code);

    void save(final Code code,
              final boolean logChange);

    void save(final Set<Code> codes);

    void save(final Set<Code> codes,
              final boolean logChange);

    void delete(final Code code);

    void delete(final Set<Code> code);

    Code findByUri(final String uri);

    Set<Code> findBySubCodeScheme(final CodeScheme subCodeScheme);

    Code findByCodeSchemeAndCodeValue(final CodeScheme codeScheme,
                                      final String codeValue);

    Code findByCodeSchemeAndCodeValueAndBroaderCodeId(final CodeScheme codeScheme,
                                                      final String codeValue,
                                                      final UUID broaderCodeId);

    Code findById(final UUID id);

    Set<Code> findByCodeSchemeAndStatus(final CodeScheme codeScheme,
                                        final String status);

    Set<Code> findByCodeSchemeId(final UUID codeSchemeId);

    Set<Code> findByCodeSchemeIdAndBroaderCodeIdIsNull(final UUID codeSchemeId);

    Set<Code> findByBroaderCodeId(final UUID broaderCodeId);

    Set<Code> findAll();

    int getCodeCount();

    Set<Code> findAll(final PageRequest pageRequest);

    Set<Code> updateCodeFromDto(final CodeScheme codeScheme,
                                final CodeDTO codeDto);

    Set<Code> updateCodesFromDtos(final CodeScheme codeScheme,
                                  final Set<CodeDTO> codes,
                                  final Map<String, String> broaderCodeMapping,
                                  final boolean updateExternalReferences);
}
