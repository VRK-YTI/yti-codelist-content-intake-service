package fi.vm.yti.codelist.intake.dao.impl;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
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
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_406;

@Component
public class ExtensionDaoImpl implements ExtensionDao {

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

    public Set<Extension> findByExtensionSchemeId(final UUID id) {
        return extensionRepository.findByExtensionSchemeId(id);
    }

    public Extension updateExtensionEntityFromDto(final CodeScheme codeScheme,
                                                  final ExtensionSchemeDTO extensionScheme,
                                                  final ExtensionDTO extensionDto) {
        final Extension extension = createOrUpdateExtension(codeScheme, extensionScheme, extensionDto);
        save(extension);
        return extension;
    }

    public Set<Extension> updateExtensionEntitiesFromDtos(final CodeScheme codeScheme,
                                                          final ExtensionSchemeDTO extensionScheme,
                                                          final Set<ExtensionDTO> extensionDtos) {
        final Set<Extension> extensions = new HashSet<>();
        if (extensionDtos != null) {
            for (final ExtensionDTO extensionDto : extensionDtos) {
                final Extension extension = createOrUpdateExtension(codeScheme, extensionScheme, extensionDto);
                extensions.add(extension);
            }
            if (!extensions.isEmpty()) {
                save(extensions);
            }
        }
        return extensions;
    }

    private Extension createOrUpdateExtension(final CodeScheme codeScheme,
                                              final ExtensionSchemeDTO extensionSchemeDto,
                                              final ExtensionDTO fromExtension) {
        final Extension existingExtension;
        final ExtensionScheme extensionScheme = extensionSchemeDao.findById(extensionSchemeDto.getId());
        if (extensionScheme != null) {
            if (fromExtension.getId() != null) {
                existingExtension = extensionRepository.findByExtensionSchemeAndId(extensionScheme, fromExtension.getId());
            } else {
                existingExtension = null;
            }
            final Extension extension;
            if (existingExtension != null) {
                extension = updateExtension(codeScheme, existingExtension, fromExtension);
            } else {
                extension = createExtension(codeScheme, extensionScheme, fromExtension);
            }
            return extension;
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }

    private Extension updateExtension(final CodeScheme codeScheme,
                                      final Extension existingExtension,
                                      final ExtensionDTO fromExtension) {
        if (!Objects.equals(existingExtension.getExtensionValue(), fromExtension.getExtensionValue())) {
            existingExtension.setExtensionValue(fromExtension.getExtensionValue());
        }
        if (!Objects.equals(existingExtension.getExtensionOrder(), fromExtension.getExtensionOrder())) {
            existingExtension.setExtensionOrder(fromExtension.getExtensionOrder());
        }
        if (fromExtension.getCode() != null) {
            final Code code = codeDao.findByCodeSchemeAndCodeValue(codeScheme, fromExtension.getCode().getCodeValue());
            if (code == null) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
            }
            if (!Objects.equals(existingExtension.getCode(), code)) {
                existingExtension.setCode(code);
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        final Extension refExtension;
        if (fromExtension.getExtension() != null) {
            refExtension = findById(fromExtension.getExtension().getId());
        } else {
            refExtension = null;
        }
        if (!Objects.equals(existingExtension.getExtension(), refExtension)) {
            existingExtension.setExtension(refExtension);
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
        extension.setExtensionOrder(fromExtension.getExtensionOrder());
        if (fromExtension.getCode() != null) {
            final Code code = codeDao.findByCodeSchemeAndCodeValue(codeScheme, fromExtension.getCode().getCodeValue());
            if (code == null) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
            }
            extension.setCode(code);
        }
        if (fromExtension.getExtension() != null) {
            final Extension refExtension = findById(fromExtension.getExtension().getId());
            if (refExtension != null) {
                extension.setExtension(refExtension);
            }
        }
        extension.setExtensionScheme(extensionScheme);
        return extension;
    }
}
