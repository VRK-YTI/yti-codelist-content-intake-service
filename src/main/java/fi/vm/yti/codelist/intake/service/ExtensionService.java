package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Workbook;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
import fi.vm.yti.codelist.intake.model.CodeScheme;

public interface ExtensionService {

    ExtensionDTO deleteExtension(final UUID id);

    Set<ExtensionDTO> findAll();

    ExtensionDTO findById(final UUID id);

    Set<ExtensionDTO> findByExtensionSchemeId(final UUID id);

    ExtensionDTO parseAndPersistExtensionFromJson(final String jsonPayload);

    Set<ExtensionDTO> parseAndPersistExtensionsFromSourceData(final String codeRegistryCodeValue,
                                                              final String codeSchemeCodeValue,
                                                              final String extensionSchemeCodeValue,
                                                              final String format,
                                                              final InputStream inputStream,
                                                              final String jsonPayload);

    Set<ExtensionDTO> parseAndPersistExtensionsFromExcelWorkbook(final CodeScheme codeSCheme,
                                                                 final ExtensionSchemeDTO extensionScheme,
                                                                 final Workbook workbook,
                                                                 final String sheetName);
}
