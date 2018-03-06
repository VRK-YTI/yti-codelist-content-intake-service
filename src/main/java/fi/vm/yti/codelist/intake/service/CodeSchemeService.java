package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.ErrorModel;
import fi.vm.yti.codelist.intake.exception.ErrorConstants;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.indexing.Indexing;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.parser.CodeSchemeParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_CSV;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_EXCEL;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_JSON;

@Component
public class CodeSchemeService {

    private final AuthorizationManager authorizationManager;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeSchemeParser codeSchemeParser;
    private final Indexing indexing;

    @Inject
    public CodeSchemeService(final AuthorizationManager authorizationManager,
                             final Indexing indexing,
                             final CodeRegistryRepository codeRegistryRepository,
                             final CodeSchemeRepository codeSchemeRepository,
                             final CodeSchemeParser codeSchemeParser) {
        this.authorizationManager = authorizationManager;
        this.indexing = indexing;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeSchemeParser = codeSchemeParser;
    }

    @Transactional
    public Set<CodeScheme> parseAndPersistCodeSchemesFromSourceData(final String codeRegistryCodeValue,
                                                                    final String format,
                                                                    final InputStream inputStream,
                                                                    final String jsonPayload) {
        Set<CodeScheme> codeSchemes;
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
            }
            switch (format.toLowerCase()) {
                case FORMAT_JSON:
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        codeSchemes = codeSchemeParser.parseCodeSchemesFromJsonData(codeRegistry, jsonPayload);
                    } else {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "No JSON payload found."));
                    }
                    break;
                case FORMAT_EXCEL:
                    codeSchemes = codeSchemeParser.parseCodeSchemesFromExcelInputStream(codeRegistry, inputStream);
                    break;
                case FORMAT_CSV:
                    codeSchemes = codeSchemeParser.parseCodeSchemesFromCsvInputStream(codeRegistry, inputStream);
                    break;
                default:
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unknown format used in CodeService: " + format));
            }
            if (codeSchemes != null && !codeSchemes.isEmpty()) {
                codeSchemeRepository.save(codeSchemes);
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "CodeRegistry with CodeValue: " + codeRegistryCodeValue + " does not exist yet, please create registry first."));
        }
        return codeSchemes;
    }

    @Transactional
    public CodeScheme parseAndPersistCodeSchemeFromJson(final String codeRegistryCodeValue,
                                                        final String codeSchemeId,
                                                        final String jsonPayload) {
        CodeScheme codeScheme = null;
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
            }
            try {
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    codeScheme = codeSchemeParser.parseCodeSchemeFromJsonData(codeRegistry, jsonPayload);
                    if (codeScheme.getId().toString().equalsIgnoreCase(codeSchemeId)) {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "Id mismatch with API call and incoming data!"));
                    }
                    codeSchemeRepository.save(codeScheme);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "No JSON payload found."));
                }
            } catch (final Exception e) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorConstants.ERR_MSG_USER_500));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "CodeRegistry with CodeValue: " + codeRegistryCodeValue + " does not exist yet, please create registry first."));
        }
        return codeScheme;
    }

    public void indexCodeScheme(final CodeScheme codeScheme) {
        final Set<CodeScheme> codeSchemes = new HashSet<>();
        codeSchemes.add(codeScheme);
        indexCodeSchemes(codeSchemes);
    }

    public void indexCodeSchemes(final Set<CodeScheme> codeSchemes) {
        indexing.updateCodeSchemes(codeSchemes);
    }
}
