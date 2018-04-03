package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.Set;

import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;

public interface PropertyTypeService {

    Set<PropertyTypeDTO> findAll();

    PropertyTypeDTO findByLocalName(final String propertyTypeLocalName);

    Set<PropertyTypeDTO> parseAndPersistPropertyTypesFromSourceData(final String format,
                                                                    final InputStream inputStream,
                                                                    final String jsonPayload);

    Set<PropertyTypeDTO> parseAndPersistPropertyTypesFromSourceData(final boolean internal,
                                                                    final String format,
                                                                    final InputStream inputStream,
                                                                    final String jsonPayload);

    PropertyTypeDTO parseAndPersistPropertyTypeFromJson(final String propertyTypeId,
                                                        final String jsonPayload);
}
