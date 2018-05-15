package fi.vm.yti.codelist.intake.dao.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.dao.ExtensionSchemeDao;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.ExtensionSchemeRepository;
import fi.vm.yti.codelist.intake.jpa.PropertyTypeRepository;
import fi.vm.yti.codelist.intake.log.EntityChangeLogger;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;
import fi.vm.yti.codelist.intake.model.PropertyType;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_STATUS_CHANGE_NOT_ALLOWED;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_406;

@Component
public class ExtensionSchemeDaoImpl implements ExtensionSchemeDao {

    private static final String CONTEXT_EXTENSIONSCHEME = "ExtensionScheme";

    private final AuthorizationManager authorizationManager;
    private final EntityChangeLogger entityChangeLogger;
    private final ExtensionSchemeRepository extensionSchemeRepository;
    private final PropertyTypeRepository propertyTypeRepository;

    @Inject
    public ExtensionSchemeDaoImpl(final AuthorizationManager authorizationManager,
                                  final EntityChangeLogger entityChangeLogger,
                                  final ExtensionSchemeRepository extensionSchemeRepository,
                                  final PropertyTypeRepository propertyTypeRepository) {
        this.authorizationManager = authorizationManager;
        this.entityChangeLogger = entityChangeLogger;
        this.extensionSchemeRepository = extensionSchemeRepository;
        this.propertyTypeRepository = propertyTypeRepository;
    }

    public void delete(final ExtensionScheme extensionScheme) {
        entityChangeLogger.logExtensionSchemeChange(extensionScheme);
        extensionSchemeRepository.delete(extensionScheme);
    }

    public void delete(final Set<ExtensionScheme> extensionSchemes) {
        extensionSchemes.forEach(entityChangeLogger::logExtensionSchemeChange);
        extensionSchemeRepository.delete(extensionSchemes);
    }

    public void save(final ExtensionScheme extensionScheme) {
        extensionSchemeRepository.save(extensionScheme);
        entityChangeLogger.logExtensionSchemeChange(extensionScheme);
    }

    public void save(final Set<ExtensionScheme> extensionSchemes) {
        extensionSchemeRepository.save(extensionSchemes);
        extensionSchemes.forEach(entityChangeLogger::logExtensionSchemeChange);
    }

    public Set<ExtensionScheme> findAll() {
        return extensionSchemeRepository.findAll();
    }

    public ExtensionScheme findById(final UUID id) {
        return extensionSchemeRepository.findById(id);
    }

    public Set<ExtensionScheme> findByCodeScheme(final CodeScheme codeScheme) {
        return extensionSchemeRepository.findByCodeScheme(codeScheme);
    }

    public Set<ExtensionScheme> findByCodeSchemeId(final UUID codeSchemeId) {
        return extensionSchemeRepository.findByCodeSchemeId(codeSchemeId);
    }

    public ExtensionScheme findByCodeSchemeIdAndCodeValue(final UUID codeSchemeId,
                                                          final String codeValue) {
        return extensionSchemeRepository.findByCodeSchemeIdAndCodeValue(codeSchemeId, codeValue);
    }

    public ExtensionScheme findByCodeSchemeAndCodeValue(final CodeScheme codeScheme,
                                                        final String codeValue) {
        return extensionSchemeRepository.findByCodeSchemeAndCodeValue(codeScheme, codeValue);
    }

    @Override
    public ExtensionScheme updateExtensionSchemeEntityFromDtos(final CodeScheme codeScheme,
                                                               final ExtensionSchemeDTO extensionSchemeDto) {
        final ExtensionScheme extensionScheme = createOrUpdateExtensionScheme(codeScheme, extensionSchemeDto);
        save(extensionScheme);
        return extensionScheme;
    }

    @Override
    public Set<ExtensionScheme> updateExtensionSchemeEntitiesFromDtos(final CodeScheme codeScheme,
                                                                      final Set<ExtensionSchemeDTO> extensionSchemeDtos) {
        final Set<ExtensionScheme> extensionSchemes = new HashSet<>();
        if (extensionSchemeDtos != null) {
            for (final ExtensionSchemeDTO extensionSchemeDto : extensionSchemeDtos) {
                final ExtensionScheme extensionScheme = createOrUpdateExtensionScheme(codeScheme, extensionSchemeDto);
                extensionSchemes.add(extensionScheme);
            }
            if (!extensionSchemes.isEmpty()) {
                save(extensionSchemes);
            }
        }
        return extensionSchemes;
    }

    private ExtensionScheme createOrUpdateExtensionScheme(final CodeScheme codeScheme,
                                                          final ExtensionSchemeDTO fromExtensionScheme) {
        ExtensionScheme existingExtensionScheme = null;
        if (fromExtensionScheme.getId() != null) {
            existingExtensionScheme = extensionSchemeRepository.findById(fromExtensionScheme.getId());
        } else {
            existingExtensionScheme = extensionSchemeRepository.findByCodeSchemeAndCodeValue(codeScheme, fromExtensionScheme.getCodeValue());
        }
        final ExtensionScheme extensionScheme;
        if (existingExtensionScheme != null) {
            extensionScheme = updateExtensionScheme(existingExtensionScheme, fromExtensionScheme, codeScheme);
        } else {
            extensionScheme = createExtensionScheme(fromExtensionScheme, codeScheme);
        }
        return extensionScheme;
    }

    private ExtensionScheme updateExtensionScheme(final ExtensionScheme existingExtensionScheme,
                                                  final ExtensionSchemeDTO fromExtensionScheme,
                                                  final CodeScheme codeScheme) {
        if (!Objects.equals(existingExtensionScheme.getStatus(), fromExtensionScheme.getStatus())) {
            if (!authorizationManager.isSuperUser() && Status.valueOf(existingExtensionScheme.getStatus()).ordinal() >= Status.VALID.ordinal() && Status.valueOf(fromExtensionScheme.getStatus()).ordinal() < Status.VALID.ordinal()) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_STATUS_CHANGE_NOT_ALLOWED));
            }
            existingExtensionScheme.setStatus(fromExtensionScheme.getStatus());
        }
        final PropertyType propertyType = propertyTypeRepository.findByContextAndLocalName(CONTEXT_EXTENSIONSCHEME, fromExtensionScheme.getPropertyType().getLocalName());
        if (propertyType == null) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        if (!Objects.equals(existingExtensionScheme.getPropertyType(), propertyType)) {
            existingExtensionScheme.setPropertyType(propertyType);
        }
        if (!Objects.equals(existingExtensionScheme.getCodeScheme(), codeScheme)) {
            existingExtensionScheme.setCodeScheme(codeScheme);
        }
        for (final Map.Entry<String, String> entry : fromExtensionScheme.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingExtensionScheme.getPrefLabel(language), value)) {
                existingExtensionScheme.setPrefLabel(language, value);
            }
        }
        if (!Objects.equals(existingExtensionScheme.getStartDate(), fromExtensionScheme.getStartDate())) {
            existingExtensionScheme.setStartDate(fromExtensionScheme.getStartDate());
        }
        if (!Objects.equals(existingExtensionScheme.getEndDate(), fromExtensionScheme.getEndDate())) {
            existingExtensionScheme.setEndDate(fromExtensionScheme.getEndDate());
        }

        return existingExtensionScheme;
    }

    private ExtensionScheme createExtensionScheme(final ExtensionSchemeDTO fromExtensionScheme,
                                                  final CodeScheme codeScheme) {
        final ExtensionScheme extensionScheme = new ExtensionScheme();
        if (fromExtensionScheme.getId() != null) {
            extensionScheme.setId(fromExtensionScheme.getId());
        } else {
            final UUID uuid = UUID.randomUUID();
            extensionScheme.setId(uuid);
        }
        extensionScheme.setCodeValue(fromExtensionScheme.getCodeValue());
        extensionScheme.setStartDate(fromExtensionScheme.getStartDate());
        extensionScheme.setEndDate(fromExtensionScheme.getEndDate());
        extensionScheme.setStatus(fromExtensionScheme.getStatus());
        final PropertyType propertyType = propertyTypeRepository.findByContextAndLocalName(CONTEXT_EXTENSIONSCHEME, fromExtensionScheme.getPropertyType().getLocalName());
        if (propertyType == null) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        extensionScheme.setPropertyType(propertyType);
        for (final Map.Entry<String, String> entry : fromExtensionScheme.getPrefLabel().entrySet()) {
            extensionScheme.setPrefLabel(entry.getKey(), entry.getValue());
        }
//        save(extensionScheme);
        extensionScheme.setCodeScheme(codeScheme);
        return extensionScheme;
    }
}
