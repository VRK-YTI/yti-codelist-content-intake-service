package fi.vm.yti.codelist.intake.parser;

import java.io.InputStream;
import java.util.Set;

import org.apache.poi.ss.usermodel.Workbook;

import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;

public interface ExtensionParser {

    ExtensionDTO parseExtensionFromJson(final String jsonPayload);

    Set<ExtensionDTO> parseExtensionsFromJson(final String jsonPayload);

    Set<ExtensionDTO> parseExtensionsFromCsvInputStream(final ExtensionScheme extensionScheme,
                                                        final InputStream inputStream);

    Set<ExtensionDTO> parseExtensionsFromExcelInputStream(final ExtensionScheme extensionScheme,
                                                          final InputStream inputStream,
                                                          final String sheetName);

    Set<ExtensionDTO> parseExtensionsFromExcelWorkbook(final ExtensionScheme extensionScheme,
                                                       final Workbook workbook,
                                                       final String sheetName);
}
