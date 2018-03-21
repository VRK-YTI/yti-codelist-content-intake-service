package fi.vm.yti.codelist.intake.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.Transactional;

import org.apache.poi.POIXMLException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.intake.dao.CodeRegistryDao;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.exception.ExcelParsingException;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ErrorModel;
import fi.vm.yti.codelist.intake.parser.CodeSchemeParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.CodeSchemeService;
import fi.vm.yti.codelist.intake.service.CodeService;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Singleton
@Service
public class CodeSchemeServiceImpl extends BaseService implements CodeSchemeService {

    private static final Logger LOG = LoggerFactory.getLogger(CodeSchemeServiceImpl.class);
    private final AuthorizationManager authorizationManager;
    private final CodeRegistryDao codeRegistryDao;
    private final CodeSchemeDao codeSchemeDao;
    private final CodeSchemeParser codeSchemeParser;
    private final CodeService codeService;

    @Inject
    public CodeSchemeServiceImpl(final AuthorizationManager authorizationManager,
                                 final CodeRegistryDao codeRegistryDao,
                                 final CodeSchemeDao codeSchemeDao,
                                 final CodeSchemeParser codeSchemeParser,
                                 final CodeService codeService) {
        this.codeRegistryDao = codeRegistryDao;
        this.authorizationManager = authorizationManager;
        this.codeSchemeParser = codeSchemeParser;
        this.codeService = codeService;
        this.codeSchemeDao = codeSchemeDao;
    }

    @Transactional
    public Set<CodeSchemeDTO> findAll() {
        return mapDeepCodeSchemeDtos(codeSchemeDao.findAll());
    }

    @Transactional
    public CodeSchemeDTO findById(final UUID id) {
        return mapDeepCodeSchemeDto(codeSchemeDao.findById(id));
    }

    @Transactional
    @Nullable
    public CodeSchemeDTO findByCodeRegistryCodeValueAndCodeValue(final String codeRegistryCodeValue,
                                                                 final String codeSchemeCodeValue) {
        CodeScheme scheme = codeSchemeDao.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (scheme == null) {
            return null;
        }
        return mapDeepCodeSchemeDto(scheme);
    }

    @Transactional
    public Set<CodeSchemeDTO> parseAndPersistCodeSchemesFromSourceData(final String codeRegistryCodeValue,
                                                                       final String format,
                                                                       final InputStream inputStream,
                                                                       final String jsonPayload) {
        final Set<CodeScheme> codeSchemes;
        final CodeRegistry codeRegistry = codeRegistryDao.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            switch (format.toLowerCase()) {
                case FORMAT_JSON:
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        codeSchemes = codeSchemeDao.updateCodeSchemesFromDtos(codeRegistry, codeSchemeParser.parseCodeSchemesFromJsonData(jsonPayload));
                    } else {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                    }
                    break;
                case FORMAT_EXCEL:
                    try (final Workbook workbook = WorkbookFactory.create(inputStream)) {
                        codeSchemes = codeSchemeDao.updateCodeSchemesFromDtos(codeRegistry, codeSchemeParser.parseCodeSchemesFromExcelWorkbook(codeRegistry, workbook));
                        if (codeSchemes.size() == 1 && workbook.getSheet(EXCEL_SHEET_CODES) != null) {
                            final CodeScheme codeScheme = codeSchemes.iterator().next();
                            codeService.parseAndPersistCodesFromExcelWorkbook(codeRegistryCodeValue, codeScheme.getCodeValue(), workbook);
                        }
                    } catch (final InvalidFormatException | IOException | POIXMLException e) {
                        throw new ExcelParsingException(ERR_MSG_USER_ERROR_PARSING_EXCEL_FILE);
                    }
                    break;
                case FORMAT_CSV:
                    codeSchemes = codeSchemeDao.updateCodeSchemesFromDtos(codeRegistry, codeSchemeParser.parseCodeSchemesFromCsvInputStream(codeRegistry, inputStream));
                    break;
                default:
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return mapCodeSchemeDtos(codeSchemes, true);
    }

    @Transactional
    public CodeSchemeDTO parseAndPersistCodeSchemeFromJson(final String codeRegistryCodeValue,
                                                           final String codeSchemeCodeValue,
                                                           final String jsonPayload) {
        CodeScheme codeScheme = null;
        final CodeRegistry codeRegistry = codeRegistryDao.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            try {
                if (jsonPayload != null && !jsonPayload.isEmpty()) {
                    final CodeSchemeDTO codeSchemeDto = codeSchemeParser.parseCodeSchemeFromJsonData(jsonPayload);
                    if (!codeSchemeDto.getCodeValue().equalsIgnoreCase(codeSchemeCodeValue)) {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_PATH_CODE_MISMATCH));
                    }
                    codeScheme = codeSchemeDao.updateCodeSchemeFromDto(codeRegistry, codeSchemeDto);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                }
            } catch (final YtiCodeListException e) {
                throw e;
            } catch (final Exception e) {
                LOG.error("Caught exception in parseAndPersistCodeSchemeFromJson.", e);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return mapCodeSchemeDto(codeScheme, true);
    }

    @Transactional
    public CodeSchemeDTO deleteCodeScheme(final String codeRegistryCodeValue,
                                          final String codeSchemeCodeValue) {
        if (authorizationManager.isSuperUser()) {
            final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
            final CodeSchemeDTO codeSchemeDto = mapCodeSchemeDto(codeScheme, false);
            codeSchemeDao.delete(codeScheme);
            return codeSchemeDto;
        } else {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
    }
}
