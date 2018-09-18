package fi.vm.yti.codelist.intake.parser;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Workbook;

import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;

public interface ExtensionSchemeParser {

    ExtensionSchemeDTO parseExtensionSchemeFromJson(final String jsonPayload);

    Set<ExtensionSchemeDTO> parseExtensionSchemesFromJson(final String jsonPayload);

    Set<ExtensionSchemeDTO> parseExtensionSchemesFromCsvInputStream(final InputStream inputStream);

    Set<ExtensionSchemeDTO> parseExtensionSchemesFromExcelInputStream(final InputStream inputStream,
                                                                      final String sheetName,
                                                                      final Map<ExtensionSchemeDTO, String> membersSheetNames);

    Set<ExtensionSchemeDTO> parseExtensionSchemesFromExcelWorkbook(final Workbook workbook,
                                                                   final String sheetName,
                                                                   final Map<ExtensionSchemeDTO, String> membersSheetNames);
}
