package fi.vm.yti.codelist.intake.dao;

import java.util.Set;
import java.util.UUID;

import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;

public interface ExtensionDao {

    void delete(final Extension extension);

    void delete(final Set<Extension> extensions);

    void save(final Set<Extension> extensions);

    Set<Extension> findAll();

    Extension findById(final UUID id);

    Set<Extension> findByCodeId(final UUID id);

    Set<Extension> findByExtensionId(final UUID id);

    Set<Extension> findByExtensionSchemeId(final UUID id);

    Extension updateExtensionEntityFromDto(final CodeScheme codeScheme,
                                           final ExtensionSchemeDTO extensionScheme,
                                           final ExtensionDTO extensionDto);

    Set<Extension> updateExtensionEntitiesFromDtos(final CodeScheme codeScheme,
                                                   final ExtensionSchemeDTO extensionScheme,
                                                   final Set<ExtensionDTO> extensionDtos);
}
