package fi.vm.yti.codelist.intake.dao;

import java.util.Set;
import java.util.UUID;

import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;
import fi.vm.yti.codelist.intake.model.PropertyType;

public interface PropertyTypeDao {

    PropertyType findByLocalName(final String propertyTypeLocalName);

    PropertyType findById(final UUID id);

    Set<PropertyType> findAll();

    PropertyType updatePropertyTypeFromDto(final PropertyTypeDTO propertyTypeDTO);

    Set<PropertyType> updatePropertyTypesFromDtos(final Set<PropertyTypeDTO> propertyTypeDtos);
}
