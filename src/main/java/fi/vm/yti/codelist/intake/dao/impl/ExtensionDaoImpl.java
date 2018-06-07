package fi.vm.yti.codelist.intake.dao.impl;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.intake.dao.CodeDao;
import fi.vm.yti.codelist.intake.dao.ExtensionDao;
import fi.vm.yti.codelist.intake.dao.ExtensionSchemeDao;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.ExtensionRepository;
import fi.vm.yti.codelist.intake.log.EntityChangeLogger;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Component
public class ExtensionDaoImpl implements ExtensionDao {

    private static final int MAX_LEVEL = 10;
    private final EntityChangeLogger entityChangeLogger;
    private final ExtensionRepository extensionRepository;
    private final CodeDao codeDao;
    private final ExtensionSchemeDao extensionSchemeDao;

    @Inject
    public ExtensionDaoImpl(final EntityChangeLogger entityChangeLogger,
                            final ExtensionRepository extensionRepository,
                            final CodeDao codeDao,
                            final ExtensionSchemeDao extensionSchemeDao) {
        this.entityChangeLogger = entityChangeLogger;
        this.extensionRepository = extensionRepository;
        this.codeDao = codeDao;
        this.extensionSchemeDao = extensionSchemeDao;
    }

    public void delete(final Extension extension) {
        entityChangeLogger.logExtensionChange(extension);
        extensionRepository.delete(extension);
    }

    public void delete(final Set<Extension> extensions) {
        extensions.forEach(entityChangeLogger::logExtensionChange);
        extensionRepository.delete(extensions);
    }

    public void save(final Extension extension) {
        extensionRepository.save(extension);
        entityChangeLogger.logExtensionChange(extension);
    }

    public void save(final Set<Extension> extensions) {
        extensionRepository.save(extensions);
        extensions.forEach(entityChangeLogger::logExtensionChange);
    }

    public Set<Extension> findAll() {
        return extensionRepository.findAll();
    }

    public Extension findById(final UUID id) {
        return extensionRepository.findById(id);
    }

    public Set<Extension> findByCodeId(final UUID id) {
        return extensionRepository.findByCodeId(id);
    }

    public Set<Extension> findByExtensionId(final UUID id) {
        return extensionRepository.findByExtensionId(id);
    }

    public Set<Extension> findByExtensionSchemeId(final UUID id) {
        return extensionRepository.findByExtensionSchemeId(id);
    }

    public Extension updateExtensionEntityFromDto(final ExtensionScheme extensionScheme,
                                                  final ExtensionDTO extensionDto) {
        final Extension extension = createOrUpdateExtension(extensionScheme, extensionDto);
        extensionDto.setId(extension.getId());
        save(extension);
        resolveExtensionRelation(extensionScheme, extensionDto);
        return extension;
    }

    public Set<Extension> updateExtensionEntitiesFromDtos(final ExtensionScheme extensionScheme,
                                                          final Set<ExtensionDTO> extensionDtos) {
        final Set<Extension> extensions = new HashSet<>();
        if (extensionDtos != null) {
            for (final ExtensionDTO extensionDto : extensionDtos) {
                final Extension extension = createOrUpdateExtension(extensionScheme, extensionDto);
                extensionDto.setId(extension.getId());
                extensions.add(extension);
            }
            if (!extensions.isEmpty()) {
                save(extensions);
            }
            resolveExtensionRelations(extensionScheme, extensionDtos);
        }
        return extensions;
    }

    private void resolveExtensionRelation(final ExtensionScheme extensionScheme,
                                          final ExtensionDTO fromExtension) {
        final ExtensionDTO relatedExtension = fromExtension.getExtension();
        if (relatedExtension != null && relatedExtension.getId() != null && fromExtension.getId() != null && relatedExtension.getId().equals(fromExtension.getId())) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        if (relatedExtension != null && relatedExtension.getCode() != null) {
            final Set<Extension> extensions = findByExtensionSchemeId(extensionScheme.getId());
            final Set<Extension> toExtensions = new HashSet<>();
            for (final Extension extension : extensions) {
                if (extension.getCode() != null && relatedExtension.getCode() != null && (extension.getCode().getCodeValue().equalsIgnoreCase(relatedExtension.getCode().getCodeValue()) || extension.getCode().getUri().equalsIgnoreCase(relatedExtension.getCode().getCodeValue()))) {
                    final Extension toExtension = findById(fromExtension.getId());
                    if (toExtension != null) {
                        toExtension.setExtension(extension);
                        toExtensions.add(toExtension);
                    }
                }
            }
            toExtensions.forEach(extension -> checkExtensionHierarchyLevels(extension, 1));
            save(toExtensions);
        }
    }

    private void checkExtensionHierarchyLevels(final Extension extension,
                                               final int level) {
        if (level > MAX_LEVEL) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EXTENSION_HIERARCHY_MAXLEVEL_REACHED));
        }
        if (extension.getExtension() != null) {
            checkExtensionHierarchyLevels(extension.getExtension(), level + 1);
        }
    }

    private void resolveExtensionRelations(final ExtensionScheme extensionScheme,
                                           final Set<ExtensionDTO> fromExtensions) {
        fromExtensions.forEach(fromExtension -> resolveExtensionRelation(extensionScheme, fromExtension));
    }

    private Extension createOrUpdateExtension(final ExtensionScheme extensionSchemeDto,
                                              final ExtensionDTO fromExtension) {
        final Extension existingExtension;
        final ExtensionScheme extensionScheme = extensionSchemeDao.findById(extensionSchemeDto.getId());
        if (extensionScheme != null) {
            if (fromExtension.getId() != null) {
                existingExtension = extensionRepository.findByExtensionSchemeAndId(extensionScheme, fromExtension.getId());
            } else if (fromExtension.getCode() != null && fromExtension.getCode().getCodeValue() != null) {
                existingExtension = extensionRepository.findByExtensionSchemeAndCodeCodeValueIgnoreCase(extensionScheme, fromExtension.getCode().getCodeValue());
            } else if (fromExtension.getCode() != null && fromExtension.getCode().getUri() != null) {
                existingExtension = extensionRepository.findByExtensionSchemeAndCodeUriIgnoreCase(extensionScheme, fromExtension.getCode().getUri());
            } else {
                existingExtension = null;
            }
            final Extension extension;
            if (existingExtension != null) {
                extension = updateExtension(extensionScheme.getParentCodeScheme(), extensionScheme, existingExtension, fromExtension);
            } else {
                extension = createExtension(extensionScheme.getParentCodeScheme(), extensionScheme, fromExtension);
            }
            return extension;
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }

    private Extension updateExtension(final CodeScheme codeScheme,
                                      final ExtensionScheme extensionScheme,
                                      final Extension existingExtension,
                                      final ExtensionDTO fromExtension) {
        if (!Objects.equals(existingExtension.getExtensionValue(), fromExtension.getExtensionValue())) {
            existingExtension.setExtensionValue(fromExtension.getExtensionValue());
        }
        if (!Objects.equals(existingExtension.getOrder(), fromExtension.getOrder())) {
            existingExtension.setOrder(fromExtension.getOrder());
        }
        if (fromExtension.getCode() != null) {
            final Code code = findCodeUsingCodeValueOrUri(codeScheme, extensionScheme, fromExtension);
            if (!Objects.equals(existingExtension.getCode(), code)) {
                existingExtension.setCode(code);
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return existingExtension;
    }

    private Extension createExtension(final CodeScheme codeScheme,
                                      final ExtensionScheme extensionScheme,
                                      final ExtensionDTO fromExtension) {
        final Extension extension = new Extension();
        if (fromExtension.getId() != null) {
            extension.setId(fromExtension.getId());
        } else {
            final UUID uuid = UUID.randomUUID();
            extension.setId(uuid);
        }
        extension.setExtensionValue(fromExtension.getExtensionValue());
        extension.setOrder(fromExtension.getOrder());
        if (fromExtension.getCode() != null) {
            final Code code = findCodeUsingCodeValueOrUri(codeScheme, extensionScheme, fromExtension);
            extension.setCode(code);
        }
        extension.setExtensionScheme(extensionScheme);
        return extension;
    }

    private Code findCodeUsingCodeValueOrUri(final CodeScheme codeScheme,
                                             final ExtensionScheme extensionScheme,
                                             final ExtensionDTO extension) {
        final CodeDTO fromCode = extension.getCode();
        final Code code;
        if (fromCode != null && fromCode.getUri() != null && !fromCode.getUri().isEmpty()) {
            code = codeDao.findByUri(fromCode.getUri());
            checkThatCodeIsInAllowedCodeScheme(code.getCodeScheme(), codeScheme, extensionScheme);
        } else if (fromCode != null && codeScheme != null && fromCode.getCodeValue() != null && !fromCode.getCodeValue().isEmpty()) {
            code = codeDao.findByCodeSchemeAndCodeValue(codeScheme, extension.getCode().getCodeValue());
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EXTENSION_CODE_NOT_FOUND));
        }
        if (code == null) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EXTENSION_CODE_NOT_FOUND));
        }
        return code;
    }

    private void checkThatCodeIsInAllowedCodeScheme(final CodeScheme codeSchemeForCode,
                                                    final CodeScheme parentCodeScheme,
                                                    final ExtensionScheme extensionScheme) {
        final Set<CodeScheme> codeSchemes = extensionScheme.getCodeSchemes();
        if (codeSchemeForCode == parentCodeScheme || (codeSchemes != null && codeSchemes.contains(codeSchemeForCode))) {
            return;
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EXTENSION_CODE_NOT_ALLOWED));
        }
    }
}
