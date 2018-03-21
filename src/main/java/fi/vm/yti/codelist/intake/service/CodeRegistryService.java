package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.intake.dao.CodeRegistryDaoImpl;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.ErrorModel;
import fi.vm.yti.codelist.intake.parser.CodeRegistryParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Component
public class CodeRegistryService extends BaseService {

    private static final Logger LOG = LoggerFactory.getLogger(CodeRegistryService.class);
    private final AuthorizationManager authorizationManager;
    private final CodeRegistryParser codeRegistryParser;
    private final CodeRegistryDaoImpl codeRegistryDao;

    @Inject
    public CodeRegistryService(final AuthorizationManager authorizationManager,
                               final CodeRegistryRepository codeRegistryRepository,
                               final CodeRegistryParser codeRegistryParser,
                               final CodeRegistryDaoImpl codeRegistryDao) {
        this.authorizationManager = authorizationManager;
        this.codeRegistryParser = codeRegistryParser;
        this.codeRegistryDao = codeRegistryDao;
    }

    @Transactional
    public Set<CodeRegistryDTO> findAll() {
        return mapCodeRegistryDtos(codeRegistryDao.findAll());
    }

    @Transactional
    @Nullable
    public CodeRegistryDTO findByCodeValue(final String codeValue) {
        CodeRegistry registry = codeRegistryDao.findByCodeValue(codeValue);
        if (registry == null)
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
                    codeRegistries = codeRegistryDao.updateCodeRegistriesFromDto(codeRegistryParser.parseCodeRegistriesFromJsonData(jsonPayload));
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                }
                break;
            case FORMAT_EXCEL:
                codeRegistries = codeRegistryDao.updateCodeRegistriesFromDto(codeRegistryParser.parseCodeRegistriesFromExcelInputStream(inputStream));
                break;
            case FORMAT_CSV:
                codeRegistries = codeRegistryDao.updateCodeRegistriesFromDto(codeRegistryParser.parseCodeRegistriesFromCsvInputStream(inputStream));
                break;
            default:
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return mapCodeRegistryDtos(codeRegistries);
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
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
                }
            } catch (final YtiCodeListException e) {
                throw e;
            } catch (final Exception e) {
                LOG.error("Caught exception in parseAndPersistCodeRegistryFromJson.", e);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "CodeRegistry with CodeValue: " + codeRegistryCodeValue + " does not exist yet, please create registry first."));
        }
        return mapCodeRegistryDto(codeRegistry);
    }
}
