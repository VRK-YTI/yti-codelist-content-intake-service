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
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.parser.CodeRegistryParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_CSV;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_EXCEL;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_JSON;

@Component
public class CodeRegistryService {

    private final AuthorizationManager authorizationManager;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRepository codeRepository;
    private final CodeRegistryParser codeRegistryParser;
    private final Indexing indexing;

    @Inject
    public CodeRegistryService(final AuthorizationManager authorizationManager,
                               final Indexing indexing,
                               final CodeRegistryRepository codeRegistryRepository,
                               final CodeSchemeRepository codeSchemeRepository,
                               final CodeRepository codeRepository,
                               final CodeRegistryParser codeRegistryParser) {
        this.authorizationManager = authorizationManager;
        this.indexing = indexing;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
        this.codeRegistryParser = codeRegistryParser;
    }

    @Transactional
    public Set<CodeRegistry> parseAndPersistCodeRegistriesFromSourceData(final String format,
                                                                         final InputStream inputStream,
                                                                         final String jsonPayload) {
        Set<CodeRegistry> codeRegistries;
        if (!authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
        }
        switch (format.toLowerCase()) {
            case FORMAT_JSON:
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    codeRegistries = codeRegistryParser.parseCodeRegistriesFromJsonData(jsonPayload);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "No JSON payload found."));
                }
                break;
            case FORMAT_EXCEL:
                codeRegistries = codeRegistryParser.parseCodeRegistriesFromExcelInputStream(inputStream);
                break;
            case FORMAT_CSV:
                codeRegistries = codeRegistryParser.parseCodeRegistriesFromCsvInputStream(inputStream);
                break;
            default:
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unknown format used in CodeRegistryService: " + format));
        }
        if (codeRegistries != null && !codeRegistries.isEmpty()) {
            codeRegistryRepository.save(codeRegistries);
        }
        return codeRegistries;
    }

    @Transactional
    public CodeRegistry parseAndPersistCodeRegistryFromJson(final String codeRegistryCodeValue,
                                                            final String jsonPayload) {
        final CodeRegistry existingCodeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        final CodeRegistry codeRegistry;
        if (existingCodeRegistry != null) {
            if (!authorizationManager.isSuperUser()) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ErrorConstants.ERR_MSG_USER_401));
            }
            try {
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    codeRegistry = codeRegistryParser.parseCodeRegistryFromJsonData(jsonPayload);
                    if (existingCodeRegistry.getCodeValue().equalsIgnoreCase(codeRegistryCodeValue)) {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "Id mismatch with API call and incoming data!"));
                    }
                    codeRegistryRepository.save(codeRegistry);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "No JSON payload found."));
                }
            } catch (final Exception e) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorConstants.ERR_MSG_USER_500));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "CodeRegistry with CodeValue: " + codeRegistryCodeValue + " does not exist yet, please create registry first."));
        }
        return codeRegistry;
    }

    public void indexCodeRegistry(final CodeRegistry codeRegistry) {
        final Set<CodeRegistry> codeRegistries = new HashSet<>();
        codeRegistries.add(codeRegistry);
        indexCodeRegistries(codeRegistries);
    }

    public void indexCodeRegistries(final Set<CodeRegistry> codeRegistries) {
        indexing.updateCodeRegistries(codeRegistries);
        for (final CodeRegistry codeRegistry : codeRegistries) {
            final Set<CodeScheme> codeSchemes = codeSchemeRepository.findByCodeRegistry(codeRegistry);
            indexing.updateCodeSchemes(codeSchemes);
            for (final CodeScheme codeScheme : codeSchemes) {
                indexing.updateCodes(codeRepository.findByCodeScheme(codeScheme));
            }
        }
    }
}
