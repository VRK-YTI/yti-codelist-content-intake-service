package fi.vm.yti.codelist.intake.service.impl;

import java.io.InputStream;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.intake.dao.CodeRegistryDao;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.parser.impl.CodeRegistryParserImpl;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.CodeRegistryService;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Singleton
@Service
public class CodeRegistryServiceImpl implements CodeRegistryService {

    private static final Logger LOG = LoggerFactory.getLogger(CodeRegistryServiceImpl.class);
    private final AuthorizationManager authorizationManager;
    private final CodeRegistryParserImpl codeRegistryParser;
    private final CodeRegistryDao codeRegistryDao;
    private final DtoMapperService dtoMapperService;

    @Inject
    public CodeRegistryServiceImpl(final AuthorizationManager authorizationManager,
                                   final CodeRegistryParserImpl codeRegistryParser,
                                   final CodeRegistryDao codeRegistryDao,
                                   final DtoMapperService dtoMapperService) {
        this.authorizationManager = authorizationManager;
        this.codeRegistryParser = codeRegistryParser;
        this.codeRegistryDao = codeRegistryDao;
        this.dtoMapperService = dtoMapperService;
    }

    @Transactional
    public Set<CodeRegistryDTO> findAll() {
        return dtoMapperService.mapDeepCodeRegistryDtos(codeRegistryDao.findAll());
    }

    @Transactional
    @Nullable
    public CodeRegistryDTO findByCodeValue(final String codeValue) {
        CodeRegistry registry = codeRegistryDao.findByCodeValue(codeValue);
        if (registry == null) {
            return null;
        }
        return dtoMapperService.mapDeepCodeRegistryDto(registry);
    }

    @Transactional
    public Set<CodeRegistryDTO> parseAndPersistCodeRegistriesFromSourceData(final String format,
                                                                            final InputStream inputStream,
                                                                            final String jsonPayload) {
        return parseAndPersistCodeRegistriesFromSourceData(false, format, inputStream, jsonPayload);
    }

    @Transactional
    public Set<CodeRegistryDTO> parseAndPersistCodeRegistriesFromSourceData(final boolean isAuthorized,
                                                                            final String format,
                                                                            final InputStream inputStream,
                                                                            final String jsonPayload) {
        Set<CodeRegistry> codeRegistries;
        if (!isAuthorized && !authorizationManager.isSuperUser()) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
        switch (format.toLowerCase()) {
            case FORMAT_JSON:
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    codeRegistries = codeRegistryDao.updateCodeRegistriesFromDto(codeRegistryParser.parseCodeRegistriesFromJsonData(jsonPayload));
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_JSON_PAYLOAD_EMPTY));
                }
                break;
            case FORMAT_EXCEL:
                codeRegistries = codeRegistryDao.updateCodeRegistriesFromDto(codeRegistryParser.parseCodeRegistriesFromExcelInputStream(inputStream));
                break;
            case FORMAT_CSV:
                codeRegistries = codeRegistryDao.updateCodeRegistriesFromDto(codeRegistryParser.parseCodeRegistriesFromCsvInputStream(inputStream));
                break;
            default:
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_INVALID_FORMAT));
        }
        return dtoMapperService.mapDeepCodeRegistryDtos(codeRegistries);
    }

    @Transactional
    public CodeRegistryDTO parseAndPersistCodeRegistryFromJson(final String codeRegistryCodeValue,
                                                               final String jsonPayload) {
        final CodeRegistry existingCodeRegistry = codeRegistryDao.findByCodeValue(codeRegistryCodeValue);
        final CodeRegistry codeRegistry;
        if (existingCodeRegistry != null) {
            if (!authorizationManager.isSuperUser()) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            try {
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    final CodeRegistryDTO codeRegistryDto = codeRegistryParser.parseCodeRegistryFromJsonData(jsonPayload);
                    if (!codeRegistryDto.getCodeValue().equalsIgnoreCase(codeRegistryCodeValue)) {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_PATH_CODE_MISMATCH));
                    }
                    codeRegistry = codeRegistryDao.updateCodeRegistryFromDto(codeRegistryDto);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_JSON_PAYLOAD_EMPTY));
                }
            } catch (final YtiCodeListException e) {
                throw e;
            } catch (final Exception e) {
                LOG.error("Caught exception in parseAndPersistCodeRegistryFromJson.", e);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_JSON_PARSING_ERROR));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "CodeRegistry with CodeValue: " + codeRegistryCodeValue + " does not exist yet, please create registry first."));
        }
        return dtoMapperService.mapDeepCodeRegistryDto(codeRegistry);
    }

    @Transactional
    public CodeRegistryDTO deleteCodeRegistry(final String codeRegistryCodeValue) {
        final CodeRegistry codeRegistry = codeRegistryDao.findByCodeValue(codeRegistryCodeValue);
        if (authorizationManager.canCodeRegistryBeDeleted(codeRegistry)) {
            final CodeRegistryDTO codeRegistryDto = dtoMapperService.mapDeepCodeRegistryDto(codeRegistry);
            codeRegistryDao.delete(codeRegistry);
            return codeRegistryDto;
        } else {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
    }

}
