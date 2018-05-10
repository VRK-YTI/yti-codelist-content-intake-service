package fi.vm.yti.codelist.intake.dao;

import java.util.Set;
import java.util.UUID;

import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ExternalReference;

public interface ExternalReferenceDao {

    void delete(final ExternalReference externalReference);

    void delete(final Set<ExternalReference> externalReferences);

    ExternalReference updateExternalReferenceFromDto(final ExternalReferenceDTO externalReferenceDto,
                                                     final CodeScheme codeScheme);

    Set<ExternalReference> updateExternalReferenceEntitiesFromDtos(final Set<ExternalReferenceDTO> externalReferenceDtos,
                                                                   final CodeScheme codeScheme);

    Set<ExternalReference> updateExternalReferenceEntitiesFromDtos(final boolean internal,
                                                                   final Set<ExternalReferenceDTO> externalReferenceDtos,
                                                                   final CodeScheme codeScheme);

    ExternalReference findById(final UUID id);

    Set<ExternalReference> findAll();

    Set<ExternalReference> findByParentCodeSchemeId(final UUID parentCodeSchemeId);

}
