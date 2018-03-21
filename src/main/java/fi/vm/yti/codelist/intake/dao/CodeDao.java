package fi.vm.yti.codelist.intake.dao;

import java.util.Map;
import java.util.Set;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;

public interface CodeDao {

    Code updateCodeFromDto(final CodeScheme codeScheme, final CodeDTO codeDto);

    Set<Code> updateCodesFromDtos(final CodeScheme codeScheme, final Set<CodeDTO> codes, final Map<String, String> broaderCodeMapping);
}
