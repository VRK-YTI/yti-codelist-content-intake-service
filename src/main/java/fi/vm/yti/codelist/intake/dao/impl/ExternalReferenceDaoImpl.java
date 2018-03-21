package fi.vm.yti.codelist.intake.dao.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.dao.ExternalReferenceDao;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.jpa.ExternalReferenceRepository;
import fi.vm.yti.codelist.intake.jpa.PropertyTypeRepository;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ErrorModel;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.model.PropertyType;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_PATH_EXTERNALREFERENCES;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_500;

@Component
public class ExternalReferenceDaoImpl implements ExternalReferenceDao {

    private final ApiUtils apiUtils;
    private final ExternalReferenceRepository externalReferenceRepository;
    private final CodeSchemeRepository codeSchemeRepository;
    private final PropertyTypeRepository propertyTypeRepository;

    public ExternalReferenceDaoImpl(final ApiUtils apiUtils,
                                    final ExternalReferenceRepository externalReferenceRepository,
                                    final CodeSchemeRepository codeSchemeRepository,
                                    final PropertyTypeRepository propertyTypeRepository) {
        this.apiUtils = apiUtils;
        this.externalReferenceRepository = externalReferenceRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.propertyTypeRepository = propertyTypeRepository;
    }

    @Transactional
    public ExternalReference updateExternalReferenceFromDto(final ExternalReferenceDTO externalReferenceDto,
                                                            final CodeScheme codeScheme) {
        ExternalReference externalReference = createOrUpdateExternalReference(externalReferenceDto, codeScheme);
        externalReferenceRepository.save(externalReference);
        return externalReference;
    }

    @Transactional
    public Set<ExternalReference> updateExternalReferenceEntitiesFromDtos(final Set<ExternalReferenceDTO> externalReferenceDtos,
                                                                          final CodeScheme codeScheme) {
        final Set<ExternalReference> externalReferences = new HashSet<>();
        for (final ExternalReferenceDTO externalReferenceDto : externalReferenceDtos) {
            final ExternalReference externalReference = createOrUpdateExternalReference(externalReferenceDto, codeScheme);
            if (externalReference != null) {
                externalReferences.add(externalReference);
            }
        }
        if (!externalReferences.isEmpty()) {
            externalReferenceRepository.save(externalReferences);
        }
        return externalReferences;
    }

    public ExternalReference createOrUpdateExternalReference(final ExternalReferenceDTO fromExternalReference,
                                                             final CodeScheme codeScheme) {
        final boolean isGlobal = fromExternalReference.getGlobal() != null ? fromExternalReference.getGlobal() : true;
        final ExternalReference existingExternalReference;
        if (fromExternalReference.getId() != null && codeScheme != null && !isGlobal) {
            existingExternalReference = externalReferenceRepository.findByIdAndParentCodeScheme(fromExternalReference.getId(), codeScheme);
        } else if (fromExternalReference.getId() != null && isGlobal) {
            existingExternalReference = externalReferenceRepository.findById(fromExternalReference.getId());
        } else {
            existingExternalReference = null;
        }
        final ExternalReference externalReference;
        if (existingExternalReference != null && isGlobal) {
            externalReference = existingExternalReference;
        } else if (existingExternalReference != null) {
            externalReference = updateExternalReference(existingExternalReference, fromExternalReference);
        } else if (!isGlobal) {
            externalReference = createExternalReference(fromExternalReference);
        } else if (codeScheme == null) {
            externalReference = createExternalReference(fromExternalReference);
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
        }
        return externalReference;
    }

    private ExternalReference updateExternalReference(final ExternalReference existingExternalReference,
                                                      final ExternalReferenceDTO fromExternalReference) {
        boolean hasChanges = false;
        final String uri = apiUtils.createResourceUrl(API_PATH_EXTERNALREFERENCES, fromExternalReference.getId().toString());
        if (!Objects.equals(existingExternalReference.getUri(), uri)) {
            existingExternalReference.setUri(uri);
            hasChanges = true;
        }
        if (!Objects.equals(existingExternalReference.getUrl(), fromExternalReference.getUrl())) {
            existingExternalReference.setUrl(fromExternalReference.getUrl());
            hasChanges = true;
        }
        final CodeScheme parentCodeScheme = codeSchemeRepository.findById(fromExternalReference.getParentCodeScheme().getId());
        if (!Objects.equals(existingExternalReference.getParentCodeScheme(), parentCodeScheme)) {
            existingExternalReference.setParentCodeScheme(parentCodeScheme);
            existingExternalReference.setGlobal(parentCodeScheme == null);
            hasChanges = true;
        }
        final PropertyType propertyType = propertyTypeRepository.findByLocalName(fromExternalReference.getPropertyType().getLocalName());
        if (!Objects.equals(existingExternalReference.getPropertyType(), propertyType)) {
            existingExternalReference.setPropertyType(propertyType);
            hasChanges = true;
        }
        for (final Map.Entry<String, String> entry : fromExternalReference.getTitle().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingExternalReference.getTitle(language), value)) {
                existingExternalReference.setTitle(language, value);
                hasChanges = true;
            }
        }
        for (final Map.Entry<String, String> entry : fromExternalReference.getDescription().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingExternalReference.getDescription(language), value)) {
                existingExternalReference.setDescription(language, value);
                hasChanges = true;
            }
        }
        if (hasChanges) {
            final Date timeStamp = new Date(System.currentTimeMillis());
            existingExternalReference.setModified(timeStamp);
        }
        return existingExternalReference;
    }

    private ExternalReference createExternalReference(final ExternalReferenceDTO fromExternalReference) {
        final ExternalReference externalReference = new ExternalReference();
        final String uri;
        if (fromExternalReference.getId() != null) {
            uri = apiUtils.createResourceUrl(API_PATH_EXTERNALREFERENCES, fromExternalReference.getId().toString());
            externalReference.setId(fromExternalReference.getId());
        } else {
            final UUID uuid = UUID.randomUUID();
            uri = apiUtils.createResourceUrl(API_PATH_EXTERNALREFERENCES, uuid.toString());
            externalReference.setId(uuid);
        }
        final CodeScheme parentCodeScheme = codeSchemeRepository.findById(fromExternalReference.getParentCodeScheme().getId());
        externalReference.setParentCodeScheme(parentCodeScheme);
        externalReference.setGlobal(parentCodeScheme == null);
        externalReference.setPropertyType(propertyTypeRepository.findByLocalName(fromExternalReference.getPropertyType().getLocalName()));
        externalReference.setUri(uri);
        externalReference.setUrl(fromExternalReference.getUrl());
        for (final Map.Entry<String, String> entry : fromExternalReference.getTitle().entrySet()) {
            externalReference.setTitle(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, String> entry : fromExternalReference.getDescription().entrySet()) {
            externalReference.setDescription(entry.getKey(), entry.getValue());
        }
        final Date timeStamp = new Date(System.currentTimeMillis());
        externalReference.setModified(timeStamp);
        return externalReference;
    }
}