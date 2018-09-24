package fi.vm.yti.codelist.intake.dao;

import java.util.Set;
import java.util.UUID;

import fi.vm.yti.codelist.common.dto.ValueTypeDTO;
import fi.vm.yti.codelist.intake.model.ValueType;

public interface ValueTypeDao {

    ValueType findByLocalName(final String localName);

    ValueType findById(final UUID id);

    Set<ValueType> findAll();

    ValueType updateValueTypeFromDto(final ValueTypeDTO valueTypeDTO);

    Set<ValueType> updateValueTypesFromDtos(final Set<ValueTypeDTO> valueTypeDtos);
}
