package fi.vm.yti.codelist.intake.parser;

import java.io.InputStream;
import java.util.Set;

import fi.vm.yti.codelist.common.dto.PropertyTypeDTO;

public interface PropertyTypeParser {

    PropertyTypeDTO parsePropertyTypeFromJson(final String jsonPayload);

    Set<PropertyTypeDTO> parsePropertyTypesFromJson(final String jsonPayload);

    Set<PropertyTypeDTO> parsePropertyTypesFromCsvInputStream(final InputStream inputStream);

    Set<PropertyTypeDTO> parsePropertyTypesFromExcelInputStream(final InputStream inputStream);
}
