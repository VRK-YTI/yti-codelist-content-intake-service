package fi.vm.yti.codelist.intake.parser;

import java.io.InputStream;
import java.util.Set;

import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;

public interface ExternalReferenceParser {

    ExternalReferenceDTO parseExternalReferenceFromJson(final String jsonPayload);

    Set<ExternalReferenceDTO> parseExternalReferencesFromJson(final String jsonPayload);

    Set<ExternalReferenceDTO> parseExternalReferencesFromCsvInputStream(final InputStream inputStream);

    Set<ExternalReferenceDTO> parseExternalReferencesFromExcelInputStream(final InputStream inputStream);
}
