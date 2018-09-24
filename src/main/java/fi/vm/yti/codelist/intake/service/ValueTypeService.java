package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import fi.vm.yti.codelist.common.dto.ValueTypeDTO;

public interface ValueTypeService {

    Set<ValueTypeDTO> findAll();

    ValueTypeDTO findByLocalName(final String valueTypeLocalName);

    Set<ValueTypeDTO> parseAndPersistValueTypesFromSourceData(final String format,
                                                              final InputStream inputStream,
                                                              final String jsonPayload);

    Set<ValueTypeDTO> parseAndPersistValueTypesFromSourceData(final boolean internal,
                                                              final String format,
                                                              final InputStream inputStream,
                                                              final String jsonPayload);

    ValueTypeDTO parseAndPersistValueTypeFromJson(final UUID valueTypeId,
                                                  final String jsonPayload);
}
