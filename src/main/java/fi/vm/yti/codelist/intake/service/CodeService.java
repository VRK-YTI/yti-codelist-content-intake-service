package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import fi.vm.yti.codelist.intake.model.Code;
import org.apache.poi.ss.usermodel.Workbook;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.intake.model.CodeScheme;

public interface CodeService {

    Set<CodeDTO> findAll();

    Set<CodeDTO> findByCodeSchemeId(final UUID codeSchemeId);

    Set<CodeDTO> parseAndPersistCodesFromExcelWorkbook(final String codeRegistryCodeValue,
                                                       final String codeSchemeCodeValue,
                                                       final Workbook workbook,
                                                       final String sheetName);

    Set<CodeDTO> parseAndPersistCodesFromExcelWorkbook(final Workbook workbook,
                                                       final String sheetName,
                                                       final CodeScheme codeScheme);

    Set<CodeDTO> parseAndPersistCodesFromSourceData(final boolean internal,
                                                    final String codeRegistryCodeValue,
                                                    final String codeSchemeCodeValue,
                                                    final String format,
                                                    final InputStream inputStream,
                                                    final String jsonPayload);

    Set<CodeDTO> parseAndPersistCodesFromSourceData(final String codeRegistryCodeValue,
                                                    final String codeSchemeCodeValue,
                                                    final String format,
                                                    final InputStream inputStream,
                                                    final String jsonPayload);

    CodeDTO parseAndPersistCodeFromJson(final String codeRegistryCodeValue,
                                        final String codeSchemeCodeValue,
                                        final String codeCodeValue,
                                        final String jsonPayload);

    Set<CodeDTO> removeBroaderCodeId(final UUID broaderCodeId);

    CodeDTO deleteCode(final String codeRegistryCodeValue,
                       final String codeSchemeCodeValue,
                       final String codeCodeValue);

    CodeDTO findByCodeRegistryCodeValueAndCodeSchemeCodeValueAndCodeValue(final String codeRegistryCodeValue,
                                                                          final String codeSchemeCodeValue,
                                                                          final String codeCodeValue);

    public Set<Code> updateCodesFromDtos(final CodeScheme codeScheme,
                                         final Set<CodeDTO> codeDtos,
                                         final Map<String, String> broaderCodeMapping,
                                         final boolean updateExternalReferences);
}
