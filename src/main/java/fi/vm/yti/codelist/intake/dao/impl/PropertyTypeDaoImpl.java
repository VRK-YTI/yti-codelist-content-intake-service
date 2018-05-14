package fi.vm.yti.codelist.intake.dao.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.intake.dao.PropertyTypeDao;
import fi.vm.yti.codelist.intake.jpa.PropertyTypeRepository;
import fi.vm.yti.codelist.intake.log.EntityChangeLogger;
import fi.vm.yti.codelist.intake.model.PropertyType;

@Component
public class PropertyTypeDaoImpl implements PropertyTypeDao {

    private final EntityChangeLogger entityChangeLogger;
    private final PropertyTypeRepository propertyTypeRepository;

    public PropertyTypeDaoImpl(final EntityChangeLogger entityChangeLogger,
                               final PropertyTypeRepository propertyTypeRepository) {
        this.entityChangeLogger = entityChangeLogger;
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
        entityChangeLogger.logPropertyTypeChange(propertyType);
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
            propertyTypes.forEach(entityChangeLogger::logPropertyTypeChange);
        }
        return propertyTypes;
    }

    private PropertyType createOrUpdatePropertyType(final PropertyTypeDTO fromPropertyType) {
        final PropertyType existingPropertyType;
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
        if (!Objects.equals(existingPropertyType.getPropertyUri(), fromPropertyType.getPropertyUri())) {
            existingPropertyType.setPropertyUri(fromPropertyType.getPropertyUri());
        }
        if (!Objects.equals(existingPropertyType.getContext(), fromPropertyType.getContext())) {
            existingPropertyType.setContext(fromPropertyType.getContext());
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
        if (fromPropertyType.getId() != null) {
            propertyType.setId(fromPropertyType.getId());
        } else {
            final UUID uuid = UUID.randomUUID();
            propertyType.setId(uuid);
        }
        propertyType.setContext(fromPropertyType.getContext());
        propertyType.setLocalName(fromPropertyType.getLocalName());
        propertyType.setType(fromPropertyType.getType());
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
