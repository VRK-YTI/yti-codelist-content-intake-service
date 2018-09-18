package fi.vm.yti.codelist.intake.dao.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.dao.ExtensionDao;
import fi.vm.yti.codelist.intake.dao.PropertyTypeDao;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.ExtensionRepository;
import fi.vm.yti.codelist.intake.language.LanguageService;
import fi.vm.yti.codelist.intake.log.EntityChangeLogger;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.PropertyType;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Component
public class ExtensionDaoImpl implements ExtensionDao {

    private static final String CONTEXT_EXTENSIONSCHEME = "Extension";

    private final AuthorizationManager authorizationManager;
    private final EntityChangeLogger entityChangeLogger;
    private final ExtensionRepository extensionRepository;
    private final PropertyTypeDao propertyTypeDao;
    private final CodeSchemeDao codeSchemeDao;
    private final LanguageService languageService;

    @Inject
    public ExtensionDaoImpl(final AuthorizationManager authorizationManager,
                            final EntityChangeLogger entityChangeLogger,
                            final ExtensionRepository extensionRepository,
                            final PropertyTypeDao propertyTypeDao,
                            final CodeSchemeDao codeSchemeDao,
                            final LanguageService languageService) {
        this.authorizationManager = authorizationManager;
        this.entityChangeLogger = entityChangeLogger;
        this.extensionRepository = extensionRepository;
        this.propertyTypeDao = propertyTypeDao;
        this.codeSchemeDao = codeSchemeDao;
        this.languageService = languageService;
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

    public Set<Extension> findByParentCodeScheme(final CodeScheme codeScheme) {
        return extensionRepository.findByParentCodeScheme(codeScheme);
    }

    public Set<Extension> findByParentCodeSchemeId(final UUID codeSchemeId) {
        return extensionRepository.findByParentCodeSchemeId(codeSchemeId);
    }

    public Extension findByParentCodeSchemeIdAndCodeValue(final UUID codeSchemeId,
                                                          final String codeValue) {
        return extensionRepository.findByParentCodeSchemeIdAndCodeValueIgnoreCase(codeSchemeId, codeValue);
    }

    public Extension findByParentCodeSchemeAndCodeValue(final CodeScheme codeScheme,
                                                        final String codeValue) {
        return extensionRepository.findByParentCodeSchemeAndCodeValueIgnoreCase(codeScheme, codeValue);
    }

    @Transactional
    public Extension updateExtensionEntityFromDto(final CodeScheme codeScheme,
                                                  final ExtensionDTO extensionDto) {
        final Extension extension = createOrUpdateExtensionScheme(codeScheme, extensionDto);
        save(extension);
        return extension;
    }

    @Transactional
    public Set<Extension> updateExtensionEntitiesFromDtos(final CodeScheme codeScheme,
                                                          final Set<ExtensionDTO> extensionDtos) {
        final Set<Extension> extensions = new HashSet<>();
        if (extensionDtos != null) {
            for (final ExtensionDTO extensionDto : extensionDtos) {
                final Extension extension = createOrUpdateExtensionScheme(codeScheme, extensionDto);
                extensions.add(extension);
                save(extension);
            }
        }
        return extensions;
    }

    private Extension createOrUpdateExtensionScheme(final CodeScheme codeScheme,
                                                    final ExtensionDTO fromExtensionScheme) {
        Extension existingExtension;
        if (fromExtensionScheme.getId() != null) {
            existingExtension = extensionRepository.findById(fromExtensionScheme.getId());
            validateParentCodeScheme(existingExtension, codeScheme);
        } else {
            existingExtension = extensionRepository.findByParentCodeSchemeAndCodeValueIgnoreCase(codeScheme, fromExtensionScheme.getCodeValue());
        }
        final Extension extension;
        if (existingExtension != null) {
            extension = updateExtensionScheme(existingExtension, fromExtensionScheme);
        } else {
            extension = createExtensionScheme(fromExtensionScheme, codeScheme);
        }
        return extension;
    }

    private void validateParentCodeScheme(final Extension extension,
                                          final CodeScheme codeScheme) {
        if (extension != null && extension.getParentCodeScheme() != codeScheme) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }

    private Extension updateExtensionScheme(final Extension existingExtension,
                                            final ExtensionDTO fromExtensionScheme) {
        if (!Objects.equals(existingExtension.getStatus(), fromExtensionScheme.getStatus())) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(existingExtension.getParentCodeScheme().getCodeRegistry().getOrganizations()) &&
                Status.valueOf(existingExtension.getStatus()).ordinal() >= Status.VALID.ordinal() &&
                Status.valueOf(fromExtensionScheme.getStatus()).ordinal() < Status.VALID.ordinal()) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_STATUS_CHANGE_NOT_ALLOWED));
            }
            existingExtension.setStatus(fromExtensionScheme.getStatus());
        }
        final PropertyType propertyType = propertyTypeDao.findByContextAndLocalName(CONTEXT_EXTENSIONSCHEME, fromExtensionScheme.getPropertyType().getLocalName());
        if (propertyType == null) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EXTENSIONSCHEME_PROPERTYTYPE_NOT_FOUND));
        }
        if (!Objects.equals(existingExtension.getPropertyType(), propertyType)) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EXTENSIONSCHEME_PROPERTYTYPE_CHANGE_NOT_ALLOWED));
        }
        final Set<CodeScheme> codeSchemes = new HashSet<>();
        if (fromExtensionScheme.getCodeSchemes() != null && !fromExtensionScheme.getCodeSchemes().isEmpty()) {
            for (final CodeSchemeDTO codeSchemeDto : fromExtensionScheme.getCodeSchemes()) {
                if (codeSchemeDto.getUri() != null && !codeSchemeDto.getUri().isEmpty()) {
                    final CodeScheme relatedCodeScheme = codeSchemeDao.findByUri(codeSchemeDto.getUri());
                    if (relatedCodeScheme != null) {
                        codeSchemes.add(relatedCodeScheme);
                    } else {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EXTENSIONSCHEME_CODESCHEME_NOT_FOUND));
                    }
                }
            }
        }
        existingExtension.setCodeSchemes(codeSchemes);
        for (final Map.Entry<String, String> entry : fromExtensionScheme.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguage(existingExtension.getParentCodeScheme(), language);
            final String value = entry.getValue();
            if (!Objects.equals(existingExtension.getPrefLabel(language), value)) {
                existingExtension.setPrefLabel(language, value);
            }
        }
        if (!Objects.equals(existingExtension.getStartDate(), fromExtensionScheme.getStartDate())) {
            existingExtension.setStartDate(fromExtensionScheme.getStartDate());
        }
        if (!Objects.equals(existingExtension.getEndDate(), fromExtensionScheme.getEndDate())) {
            existingExtension.setEndDate(fromExtensionScheme.getEndDate());
        }
        existingExtension.setModified(new Date(System.currentTimeMillis()));
        return existingExtension;
    }

    private Extension createExtensionScheme(final ExtensionDTO fromExtensionScheme,
                                            final CodeScheme codeScheme) {
        final Extension extension = new Extension();
        if (fromExtensionScheme.getId() != null) {
            extension.setId(fromExtensionScheme.getId());
        } else {
            final UUID uuid = UUID.randomUUID();
            extension.setId(uuid);
        }
        extension.setCodeValue(fromExtensionScheme.getCodeValue());
        extension.setStartDate(fromExtensionScheme.getStartDate());
        extension.setEndDate(fromExtensionScheme.getEndDate());
        extension.setStatus(fromExtensionScheme.getStatus());
        final PropertyType propertyType = propertyTypeDao.findByContextAndLocalName(CONTEXT_EXTENSIONSCHEME, fromExtensionScheme.getPropertyType().getLocalName());
        if (propertyType == null) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EXTENSIONSCHEME_PROPERTYTYPE_NOT_FOUND));
        }
        extension.setPropertyType(propertyType);
        for (final Map.Entry<String, String> entry : fromExtensionScheme.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguage(codeScheme, language);
            extension.setPrefLabel(language, entry.getValue());
        }
        final Set<CodeScheme> codeSchemes = new HashSet<>();
        if (fromExtensionScheme.getCodeSchemes() != null && !fromExtensionScheme.getCodeSchemes().isEmpty()) {
            fromExtensionScheme.getCodeSchemes().forEach(codeSchemeDto -> {
                final CodeScheme relatedCodeScheme = codeSchemeDao.findByUri(codeSchemeDto.getUri());
                if (relatedCodeScheme != null && relatedCodeScheme.getId() == codeScheme.getId()) {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EXTENSIONSCHEME_CODESCHEME_MAPPED_TO_PARENT));
                } else if (relatedCodeScheme != null) {
                    codeSchemes.add(relatedCodeScheme);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EXTENSIONSCHEME_CODESCHEME_NOT_FOUND));
                }
            });
            extension.setCodeSchemes(codeSchemes);
        }
        extension.setParentCodeScheme(codeScheme);
        addExtensionSchemeToParentCodeScheme(codeScheme, extension);
        final Date timeStamp = new Date(System.currentTimeMillis());
        extension.setCreated(timeStamp);
        extension.setModified(timeStamp);
        return extension;
    }

    private void addExtensionSchemeToParentCodeScheme(final CodeScheme codeScheme,
                                                      final Extension extension) {
        final Set<Extension> parentCodeSchemeExtensions = codeScheme.getExtensions();
        if (parentCodeSchemeExtensions != null) {
            parentCodeSchemeExtensions.add(extension);
        } else {
            final Set<Extension> extensions = new HashSet<>();
            extensions.add(extension);
            codeScheme.setExtensions(extensions);
        }
        codeSchemeDao.save(codeScheme);
    }
}
