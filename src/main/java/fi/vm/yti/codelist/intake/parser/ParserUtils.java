package fi.vm.yti.codelist.intake.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;

/**
 * Utility class for code parsers that contains helper methods to minimize repetition.
 */
@Component
public class ParserUtils {

    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRepository codeRepository;

    @Inject
    public ParserUtils(final CodeRegistryRepository codeRegistryRepository,
                       final CodeSchemeRepository codeSchemeRepository,
                       final CodeRepository codeRepository) {
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
    }

    public Map<String, CodeRegistry> getCodeRegistriesMap() {
        final List<CodeRegistry> codeRegistries = codeRegistryRepository.findAll();
        final Map<String, CodeRegistry> codeRegistriesMap = new HashMap<>();
        for (final CodeRegistry codeRegistry : codeRegistries) {
            codeRegistriesMap.put(codeRegistry.getCodeValue(), codeRegistry);
        }
        return codeRegistriesMap;
    }

    public Map<String, CodeScheme> getCodeSchemesMap() {
        final List<CodeScheme> codeSchemes = codeSchemeRepository.findAll();
        final Map<String, CodeScheme> codeSchemesMap = new HashMap<>();
        for (final CodeScheme codeScheme : codeSchemes) {
            codeSchemesMap.put(codeScheme.getCodeValue(), codeScheme);
        }
        return codeSchemesMap;
    }

    public Map<String, Code> getCodesMap(final CodeScheme codeScheme) {
        final List<Code> codes = codeRepository.findByCodeScheme(codeScheme);
        final Map<String, Code> codesMap = new HashMap<>();
        for (final Code code : codes) {
            codesMap.put(code.getCodeValue(), code);
        }
        return codesMap;
    }

}
