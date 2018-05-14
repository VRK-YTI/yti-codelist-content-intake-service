package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Workbook;

import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
import fi.vm.yti.codelist.intake.model.CodeScheme;

public interface ExtensionSchemeService {

    Set<ExtensionSchemeDTO> findAll();

    ExtensionSchemeDTO findById(final UUID id);

    Set<ExtensionSchemeDTO> findByCodeSchemeId(final UUID codeSchemeId);

    ExtensionSchemeDTO findByCodeSchemeIdAndCodeValue(final UUID codeSchemeId,
                                                      final String codeValue);

    Set<ExtensionSchemeDTO> parseAndPersistExtensionSchemesFromSourceData(final String codeRegistryCodeValue,
                                                                          final String codeSchemeCodeValue,
                                                                          final String format,
                                                                          final InputStream inputStream,
                                                                          final String jsonPayload);

    Set<ExtensionSchemeDTO> parseAndPersistExtensionSchemesFromExcelWorkbook(final CodeScheme codeScheme,
                                                                             final Workbook workbook,
                                                                             final String sheetName);

    ExtensionSchemeDTO parseAndPersistExtensionSchemeFromJson(final UUID extensionSchemeId,
                                                              final String jsonPayload);

    ExtensionSchemeDTO deleteExtensionScheme(final UUID extensionSchemeId);
}
