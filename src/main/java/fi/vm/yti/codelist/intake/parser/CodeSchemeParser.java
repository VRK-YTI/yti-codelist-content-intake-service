package fi.vm.yti.codelist.intake.parser;

import java.io.InputStream;
import java.util.Set;

import org.apache.poi.ss.usermodel.Workbook;

import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.intake.model.CodeRegistry;

public interface CodeSchemeParser {

    CodeSchemeDTO parseCodeSchemeFromJsonData(final String jsonPayload);

    Set<CodeSchemeDTO> parseCodeSchemesFromJsonData(final String jsonPayload);

    Set<CodeSchemeDTO> parseCodeSchemesFromCsvInputStream(final CodeRegistry codeRegistry,
                                                          final InputStream inputStream);

    Set<CodeSchemeDTO> parseCodeSchemesFromExcelWorkbook(final CodeRegistry codeRegistry,
                                                         final Workbook workbook);
}
