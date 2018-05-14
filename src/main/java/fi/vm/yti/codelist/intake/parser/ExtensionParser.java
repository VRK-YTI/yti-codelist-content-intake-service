package fi.vm.yti.codelist.intake.parser;

import java.io.InputStream;
import java.util.Set;

import org.apache.poi.ss.usermodel.Workbook;

import fi.vm.yti.codelist.common.dto.ExtensionDTO;

public interface ExtensionParser {

    ExtensionDTO parseExtensionFromJson(final String jsonPayload);

    Set<ExtensionDTO> parseExtensionsFromJson(final String jsonPayload);

    Set<ExtensionDTO> parseExtensionsFromCsvInputStream(final InputStream inputStream);

    Set<ExtensionDTO> parseExtensionsFromExcelInputStream(final InputStream inputStream,
                                                          final String sheetName);

    Set<ExtensionDTO> parseExtensionsFromExcelWorkbook(final Workbook workbook,
                                                       final String sheetName);
}
