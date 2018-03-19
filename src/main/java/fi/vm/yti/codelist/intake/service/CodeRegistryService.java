package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.ErrorModel;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.parser.CodeRegistryParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;
import javax.annotation.Nullable;

@Component
public class CodeRegistryService extends BaseService {

    private final AuthorizationManager authorizationManager;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeRegistryParser codeRegistryParser;

    @Inject
    public CodeRegistryService(final AuthorizationManager authorizationManager,
                               final CodeRegistryRepository codeRegistryRepository,
                               final CodeRegistryParser codeRegistryParser) {
        this.authorizationManager = authorizationManager;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeRegistryParser = codeRegistryParser;
    }

    @Transactional
    public Set<CodeRegistryDTO> findAll() {
        return mapCodeRegistryDtos(codeRegistryRepository.findAll());
    }

    @Transactional
    @Nullable
    public CodeRegistryDTO findByCodeValue(final String codeValue) {
        CodeRegistry registry = codeRegistryRepository.findByCodeValue(codeValue);
        if(registry == null)
            return null;
        return mapCodeRegistryDto(registry);
    }

    @Transactional
    public Set<CodeRegistryDTO> parseAndPersistCodeRegistriesFromSourceData(final String format,
                                                                            final InputStream inputStream,
                                                                            final String jsonPayload) {
        Set<CodeRegistry> codeRegistries;
        if (!authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        switch (format.toLowerCase()) {
            case FORMAT_JSON:
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    codeRegistries = codeRegistryParser.parseCodeRegistriesFromJsonData(jsonPayload);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                }
                break;
            case FORMAT_EXCEL:
                codeRegistries = codeRegistryParser.parseCodeRegistriesFromExcelInputStream(inputStream);
                break;
            case FORMAT_CSV:
                codeRegistries = codeRegistryParser.parseCodeRegistriesFromCsvInputStream(inputStream);
                break;
            default:
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        if (codeRegistries != null && !codeRegistries.isEmpty()) {
            codeRegistryRepository.save(codeRegistries);
        }
        return mapCodeRegistryDtos(codeRegistries);
    }

    @Transactional
    public CodeRegistryDTO parseAndPersistCodeRegistryFromJson(final String codeRegistryCodeValue,
                                                               final String jsonPayload) {
        final CodeRegistry existingCodeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        final CodeRegistry codeRegistry;
        if (existingCodeRegistry != null) {
            if (!authorizationManager.isSuperUser()) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            try {
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    codeRegistry = codeRegistryParser.parseCodeRegistryFromJsonData(jsonPayload);
                    if (!existingCodeRegistry.getCodeValue().equalsIgnoreCase(codeRegistryCodeValue)) {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_PATH_CODE_MISMATCH));
                    }
                    codeRegistryRepository.save(codeRegistry);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
                }
            } catch (final YtiCodeListException e) {
                throw e;
            } catch (final Exception e) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "CodeRegistry with CodeValue: " + codeRegistryCodeValue + " does not exist yet, please create registry first."));
        }
        return mapCodeRegistryDto(codeRegistry);
    }
}
