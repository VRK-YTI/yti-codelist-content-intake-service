package fi.vm.yti.codelist.intake.dao;

import java.util.Set;
import java.util.UUID;

import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;

public interface ExtensionSchemeDao {

    void delete(final ExtensionScheme extensionScheme);

    void delete(final Set<ExtensionScheme> extensionSchemes);

    void save(final ExtensionScheme extensionScheme);

    void save(final Set<ExtensionScheme> extensionSchemes);

    Set<ExtensionScheme> findAll();

    ExtensionScheme findById(final UUID id);

    Set<ExtensionScheme> findByParentCodeScheme(final CodeScheme codeScheme);

    Set<ExtensionScheme> findByParentCodeSchemeId(final UUID codeSchemeId);

    ExtensionScheme findByParentCodeSchemeIdAndCodeValue(final UUID codeSchemeId,
                                                         final String codeValue);

    ExtensionScheme findByParentCodeSchemeAndCodeValue(final CodeScheme codeScheme,
                                                       final String codeValue);

    ExtensionScheme updateExtensionSchemeEntityFromDtos(final CodeScheme codeScheme,
                                                        final ExtensionSchemeDTO extensionSchemeDto);

    Set<ExtensionScheme> updateExtensionSchemeEntitiesFromDtos(final CodeScheme codeScheme,
                                                               final Set<ExtensionSchemeDTO> extensionSchemeDtos);
}
