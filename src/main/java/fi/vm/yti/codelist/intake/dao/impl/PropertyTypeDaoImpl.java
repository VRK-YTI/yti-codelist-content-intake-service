package fi.vm.yti.codelist.intake.dao.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.common.dto.ValueTypeDTO;
import fi.vm.yti.codelist.intake.dao.PropertyTypeDao;
import fi.vm.yti.codelist.intake.dao.ValueTypeDao;
import fi.vm.yti.codelist.intake.jpa.PropertyTypeRepository;
import fi.vm.yti.codelist.intake.log.EntityChangeLogger;
import fi.vm.yti.codelist.intake.model.PropertyType;
import fi.vm.yti.codelist.intake.model.ValueType;

@Component
public class PropertyTypeDaoImpl implements PropertyTypeDao {

    private final EntityChangeLogger entityChangeLogger;
    private final PropertyTypeRepository propertyTypeRepository;
    private final ValueTypeDao valueTypeDao;

    public PropertyTypeDaoImpl(final EntityChangeLogger entityChangeLogger,
                               final PropertyTypeRepository propertyTypeRepository,
                               final ValueTypeDao valueTypeDao) {
        this.entityChangeLogger = entityChangeLogger;
        this.propertyTypeRepository = propertyTypeRepository;
        this.valueTypeDao = valueTypeDao;
    }

    @Transactional
    public PropertyType findById(final UUID id) {
        return propertyTypeRepository.findById(id);
    }

    @Transactional
    public PropertyType findByContextAndLocalName(final String context,
                                                  final String propertyTypeLocalName) {
        return propertyTypeRepository.findByLocalName(propertyTypeLocalName);
    }

    @Transactional
    public PropertyType findByLocalName(final String propertyTypeLocalName) {
        return propertyTypeRepository.findByLocalName(propertyTypeLocalName);
    }

    @Transactional
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
                propertyTypeRepository.save(propertyType);
            }
        }
        if (!propertyTypes.isEmpty()) {
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
        if (!Objects.equals(existingPropertyType.getUri(), fromPropertyType.getUri())) {
            existingPropertyType.setUri(fromPropertyType.getUri());
        }
        if (!Objects.equals(existingPropertyType.getContext(), fromPropertyType.getContext())) {
            existingPropertyType.setContext(fromPropertyType.getContext());
        }
        if (!Objects.equals(existingPropertyType.getLocalName(), fromPropertyType.getLocalName())) {
            existingPropertyType.setLocalName(fromPropertyType.getLocalName());
        }
        mapPrefLabel(fromPropertyType, existingPropertyType);
        mapDefinition(fromPropertyType, existingPropertyType);
        for (final Map.Entry<String, String> entry : fromPropertyType.getDefinition().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingPropertyType.getDefinition(language), value)) {
                existingPropertyType.setDefinition(language, value);
            }
        }
        existingPropertyType.setValueTypes(resolveValueTypesFromDtos(fromPropertyType.getValueTypes()));
        existingPropertyType.setModified(new Date(System.currentTimeMillis()));
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
        propertyType.setUri(fromPropertyType.getUri());
        mapPrefLabel(fromPropertyType, propertyType);
        mapDefinition(fromPropertyType, propertyType);
        propertyType.setValueTypes(resolveValueTypesFromDtos(fromPropertyType.getValueTypes()));
        final Date timeStamp = new Date(System.currentTimeMillis());
        propertyType.setCreated(timeStamp);
        propertyType.setModified(timeStamp);
        return propertyType;
    }

    private Set<ValueType> resolveValueTypesFromDtos(final Set<ValueTypeDTO> valueTypeDtos) {
        final Set<ValueType> valueTypes = new HashSet<>();
        if (valueTypeDtos != null && !valueTypeDtos.isEmpty()) {
            valueTypeDtos.forEach(valueTypeDto -> {
                final ValueType valueType = valueTypeDao.findByLocalName(valueTypeDto.getLocalName());
                if (valueType != null) {
                    valueTypes.add(valueType);
                }
            });
        }
        return valueTypes;
    }

    private void mapPrefLabel(final PropertyTypeDTO fromPropertyType,
                              final PropertyType propertyType) {
        final Map<String, String> prefLabel = fromPropertyType.getPrefLabel();
        if (prefLabel != null && !prefLabel.isEmpty()) {
            for (final Map.Entry<String, String> entry : prefLabel.entrySet()) {
                final String language = entry.getKey();
                final String value = entry.getValue();
                if (!Objects.equals(propertyType.getPrefLabel(language), value)) {
                    propertyType.setPrefLabel(language, value);
                }
            }
        } else {
            propertyType.setPrefLabel(null);
        }
    }

    private void mapDefinition(final PropertyTypeDTO fromPropertyType,
                               final PropertyType propertyType) {
        final Map<String, String> definition = fromPropertyType.getPrefLabel();
        if (definition != null && !definition.isEmpty()) {
            for (final Map.Entry<String, String> entry : definition.entrySet()) {
                final String language = entry.getKey();
                final String value = entry.getValue();
                if (!Objects.equals(propertyType.getDefinition(language), value)) {
                    propertyType.setDefinition(language, value);
                }
            }
        } else {
            propertyType.setDefinition(null);
        }
    }
}
