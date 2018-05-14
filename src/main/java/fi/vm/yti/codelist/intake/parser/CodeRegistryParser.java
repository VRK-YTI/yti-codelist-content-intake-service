package fi.vm.yti.codelist.intake.parser;

import java.io.InputStream;
import java.util.Set;

import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;

public interface CodeRegistryParser {

    CodeRegistryDTO parseCodeRegistryFromJsonData(final String jsonPayload);

    Set<CodeRegistryDTO> parseCodeRegistriesFromJsonData(final String jsonPayload);

    Set<CodeRegistryDTO> parseCodeRegistriesFromCsvInputStream(final InputStream inputStream);

    Set<CodeRegistryDTO> parseCodeRegistriesFromExcelInputStream(final InputStream inputStream);
}
