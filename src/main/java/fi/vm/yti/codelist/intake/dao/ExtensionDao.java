package fi.vm.yti.codelist.intake.dao;

import java.util.Set;
import java.util.UUID;

import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;

public interface ExtensionDao {

    void delete(final Extension extension);

    void delete(final Set<Extension> extensions);

    void save(final Extension extension);

    void save(final Set<Extension> extensions);

    void save(final Set<Extension> extensions,
              final boolean logChange);

    Set<Extension> findAll();

    Extension findById(final UUID id);

    Set<Extension> findByCodeSchemes(final CodeScheme codeScheme);

    Set<Extension> findByParentCodeScheme(final CodeScheme codeScheme);

    Set<Extension> findByParentCodeSchemeId(final UUID codeSchemeId);

    Extension findByParentCodeSchemeIdAndCodeValue(final UUID codeSchemeId,
                                                   final String codeValue);

    Extension findByParentCodeSchemeAndCodeValue(final CodeScheme codeScheme,
                                                 final String codeValue);

    Extension updateExtensionEntityFromDto(final CodeScheme codeScheme,
                                           final ExtensionDTO extensionDto,
                                           final boolean autoCreateMembers);

    Set<Extension> updateExtensionEntitiesFromDtos(final CodeScheme codeScheme,
                                                   final Set<ExtensionDTO> extensionDtos,
                                                   final boolean autoCreateMembers);
}
