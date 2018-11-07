package fi.vm.yti.codelist.intake.parser;

import java.io.InputStream;
import java.util.Set;

import org.apache.poi.ss.usermodel.Workbook;

import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.intake.model.CodeScheme;

public interface ExternalReferenceParser {

    ExternalReferenceDTO parseExternalReferenceFromJson(final String jsonPayload);

    Set<ExternalReferenceDTO> parseExternalReferencesFromJson(final String jsonPayload);

    Set<ExternalReferenceDTO> parseExternalReferencesFromCsvInputStream(final InputStream inputStream);

    Set<ExternalReferenceDTO> parseExternalReferencesFromExcelInputStream(final InputStream inputStream,
                                                                          final String sheetName);

    Set<ExternalReferenceDTO> parseExternalReferencesFromExcelWorkbook(final Workbook workbook,
                                                                       final String sheetName,
                                                                       final CodeScheme codeScheme);
}
