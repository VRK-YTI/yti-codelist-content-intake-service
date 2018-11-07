package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Workbook;

import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.intake.model.CodeScheme;

public interface ExternalReferenceService {

    Set<ExternalReferenceDTO> findAll();

    Set<ExternalReferenceDTO> findByParentCodeSchemeId(final UUID codeSchemeId);

    ExternalReferenceDTO findByParentCodeSchemeIdAndHref(final UUID parentCodeSchemeId,
                                                         final String href);

    Set<ExternalReferenceDTO> parseAndPersistExternalReferencesFromSourceData(final boolean internal,
                                                                              final String format,
                                                                              final InputStream inputStream,
                                                                              final String jsonPayload,
                                                                              final CodeScheme codeScheme);

    Set<ExternalReferenceDTO> parseAndPersistExternalReferencesFromSourceData(final String format,
                                                                              final InputStream inputStream,
                                                                              final String jsonPayload,
                                                                              final CodeScheme codeScheme);

    ExternalReferenceDTO parseAndPersistExternalReferenceFromJson(final String externalReferenceId,
                                                                  final String jsonPayload,
                                                                  final CodeScheme codeScheme);

    Set<ExternalReferenceDTO> parseAndPersistExternalReferencesFromExcelWorkbook(final Workbook workbook,
                                                                                 final String sheetName,
                                                                                 final CodeScheme codeScheme);
}
