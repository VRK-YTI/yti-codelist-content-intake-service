package fi.vm.yti.codelist.intake.dao;

import java.util.Set;

import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ExternalReference;

public interface ExternalReferenceDao {

    ExternalReference updateExternalReferenceFromDto(final ExternalReferenceDTO externalReferenceDto,
                                                     final CodeScheme codeScheme);

    Set<ExternalReference> updateExternalReferenceEntitiesFromDtos(final Set<ExternalReferenceDTO> externalReferenceDtos,
                                                                   final CodeScheme codeScheme);

    Set<ExternalReference> updateExternalReferenceEntitiesFromDtos(final boolean internal,
                                                                   final Set<ExternalReferenceDTO> externalReferenceDtos,
                                                                   final CodeScheme codeScheme);
}
