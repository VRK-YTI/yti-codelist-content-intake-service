package fi.vm.yti.codelist.intake.dao.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.dao.PropertyTypeDao;
import fi.vm.yti.codelist.intake.jpa.PropertyTypeRepository;
import fi.vm.yti.codelist.intake.model.PropertyType;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_PATH_PROPERTYTYPES;

@Component
public class PropertyTypeDaoImpl implements PropertyTypeDao {

    private final ApiUtils apiUtils;
    private final PropertyTypeRepository propertyTypeRepository;

    public PropertyTypeDaoImpl(final ApiUtils apiUtils,
                               final PropertyTypeRepository propertyTypeRepository) {
        this.apiUtils = apiUtils;
        this.propertyTypeRepository = propertyTypeRepository;
    }

    public PropertyType findById(final UUID id) {
        return propertyTypeRepository.findById(id);
    }

    public PropertyType findByLocalName(final String propertyTypeLocalName) {
        return propertyTypeRepository.findByLocalName(propertyTypeLocalName);
    }

    public Set<PropertyType> findAll() {
        return propertyTypeRepository.findAll();
    }

    @Transactional
    public PropertyType updatePropertyTypeFromDto(final PropertyTypeDTO propertyTypeDTO) {
        PropertyType propertyType = createOrUpdatePropertyType(propertyTypeDTO);
        propertyTypeRepository.save(propertyType);
        return propertyType;
    }

    @Transactional
    public Set<PropertyType> updatePropertyTypesFromDtos(final Set<PropertyTypeDTO> propertyTypeDtos) {
        final Set<PropertyType> propertyTypes = new HashSet<>();
        for (final PropertyTypeDTO propertyTypeDto : propertyTypeDtos) {
            final PropertyType propertyType = createOrUpdatePropertyType(propertyTypeDto);
            if (propertyType != null) {
                propertyTypes.add(propertyType);
            }
        }
        if (!propertyTypes.isEmpty()) {
            propertyTypeRepository.save(propertyTypes);
        }
        return propertyTypes;
    }

    private PropertyType createOrUpdatePropertyType(final PropertyTypeDTO fromPropertyType) {
        PropertyType existingPropertyType = null;
        if (fromPropertyType.getId() != null) {
            existingPropertyType = propertyTypeRepository.findById(fromPropertyType.getId());
        } else {
            existingPropertyType = null;
        }
        final PropertyType propertyType;
        if (existingPropertyType != null) {
            propertyType = updatePropertyType(existingPropertyType, fromPropertyType);
        } else {
            propertyType = createPropertyType(fromPropertyType);
        }
        return propertyType;
    }

    private PropertyType updatePropertyType(final PropertyType existingPropertyType,
                                            final PropertyTypeDTO fromPropertyType) {
        final String uri = apiUtils.createResourceUrl(API_PATH_PROPERTYTYPES, fromPropertyType.getId().toString());
        if (!Objects.equals(existingPropertyType.getPropertyUri(), fromPropertyType.getPropertyUri())) {
            existingPropertyType.setPropertyUri(fromPropertyType.getPropertyUri());
        }
        if (!Objects.equals(existingPropertyType.getUri(), uri)) {
            existingPropertyType.setUri(uri);
        }
        if (!Objects.equals(existingPropertyType.getContext(), fromPropertyType.getContext())) {
            existingPropertyType.setUri(fromPropertyType.getContext());
        }
        if (!Objects.equals(existingPropertyType.getLocalName(), fromPropertyType.getLocalName())) {
            existingPropertyType.setLocalName(fromPropertyType.getLocalName());
        }
        if (!Objects.equals(existingPropertyType.getType(), fromPropertyType.getType())) {
            existingPropertyType.setType(fromPropertyType.getType());
        }
        for (final Map.Entry<String, String> entry : fromPropertyType.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingPropertyType.getPrefLabel(language), value)) {
                existingPropertyType.setPrefLabel(language, value);
            }
        }
        for (final Map.Entry<String, String> entry : fromPropertyType.getDefinition().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingPropertyType.getDefinition(language), value)) {
                existingPropertyType.setDefinition(language, value);
            }
        }
        return existingPropertyType;
    }

    private PropertyType createPropertyType(final PropertyTypeDTO fromPropertyType) {
        final PropertyType propertyType = new PropertyType();
        final String uri;
        if (fromPropertyType.getId() != null) {
            propertyType.setId(fromPropertyType.getId());
            uri = apiUtils.createResourceUrl(API_PATH_PROPERTYTYPES, fromPropertyType.getId().toString());
        } else {
            final UUID uuid = UUID.randomUUID();
            uri = apiUtils.createResourceUrl(API_PATH_PROPERTYTYPES, uuid.toString());
            propertyType.setId(uuid);
        }
        propertyType.setContext(fromPropertyType.getContext());
        propertyType.setLocalName(fromPropertyType.getLocalName());
        propertyType.setType(fromPropertyType.getType());
        propertyType.setUri(uri);
        propertyType.setPropertyUri(fromPropertyType.getPropertyUri());
        for (final Map.Entry<String, String> entry : fromPropertyType.getPrefLabel().entrySet()) {
            propertyType.setPrefLabel(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, String> entry : fromPropertyType.getDefinition().entrySet()) {
            propertyType.setDefinition(entry.getKey(), entry.getValue());
        }
        return propertyType;
    }
}
