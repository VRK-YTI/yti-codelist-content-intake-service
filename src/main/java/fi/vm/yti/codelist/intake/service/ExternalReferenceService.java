package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.intake.model.CodeScheme;

public interface ExternalReferenceService {

    Set<ExternalReferenceDTO> findAll();

    Set<ExternalReferenceDTO> findByCodeSchemeId(final UUID codeSchemeId);

    Set<ExternalReferenceDTO> parseAndPersistExternalReferencesFromSourceData(final String format,
                                                                              final InputStream inputStream,
                                                                              final String jsonPayload,
                                                                              final CodeScheme codeScheme);

    ExternalReferenceDTO parseAndPersistExternalReferenceFromJson(final String externalReferenceId,
                                                                  final String jsonPayload,
                                                                  final CodeScheme codeScheme);
}
