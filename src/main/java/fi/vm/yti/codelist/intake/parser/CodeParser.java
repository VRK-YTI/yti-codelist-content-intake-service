package fi.vm.yti.codelist.intake.parser;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Workbook;

import fi.vm.yti.codelist.common.dto.CodeDTO;

public interface CodeParser {

    Set<CodeDTO> parseCodesFromCsvInputStream(final InputStream inputStream,
                                              final Map<String, String> broaderCodeMapping);

    Set<CodeDTO> parseCodesFromExcelInputStream(final InputStream inputStream,
                                                final String sheetName,
                                                final Map<String, String> broaderCodeMapping);

    Set<CodeDTO> parseCodesFromExcelWorkbook(final Workbook workbook,
                                             final String sheetName,
                                             final Map<String, String> broaderCodeMapping);

    CodeDTO parseCodeFromJsonData(final String jsonPayload);

    Set<CodeDTO> parseCodesFromJsonData(final String jsonPayload);
}
