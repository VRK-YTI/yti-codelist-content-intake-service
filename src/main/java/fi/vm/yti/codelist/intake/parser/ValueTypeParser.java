package fi.vm.yti.codelist.intake.parser;

import java.io.InputStream;
import java.util.Set;

import fi.vm.yti.codelist.common.dto.ValueTypeDTO;

public interface ValueTypeParser {

    ValueTypeDTO parseValueTypeFromJson(final String jsonPayload);

    Set<ValueTypeDTO> parseValueTypesFromJson(final String jsonPayload);

    Set<ValueTypeDTO> parseValueTypesFromCsvInputStream(final InputStream inputStream);

    Set<ValueTypeDTO> parseValueTypesFromExcelInputStream(final InputStream inputStream);
}
