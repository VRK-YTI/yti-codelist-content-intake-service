package fi.vm.yti.codelist.intake.service.impl;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.Transactional;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.common.constants.ApiConstants;
import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.intake.dao.CodeDao;
import fi.vm.yti.codelist.intake.dao.CodeRegistryDao;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.parser.impl.CodeParserImpl;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.CodeService;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Singleton
@Service
public class CodeServiceImpl implements CodeService {

    private static final Logger LOG = LoggerFactory.getLogger(CodeServiceImpl.class);
    private final AuthorizationManager authorizationManager;
    private final CodeRegistryDao codeRegistryDao;
    private final CodeSchemeDao codeSchemeDao;
    private final CodeDao codeDao;
    private final CodeParserImpl codeParser;
    private final DtoMapperService dtoMapperService;

    @Inject
    public CodeServiceImpl(final AuthorizationManager authorizationManager,
                           final CodeRegistryDao codeRegistryDao,
                           final CodeSchemeDao codeSchemeDao,
                           final CodeParserImpl codeParser,
                           final CodeDao codeDao,
                           final DtoMapperService dtoMapperService) {
        this.authorizationManager = authorizationManager;
        this.codeRegistryDao = codeRegistryDao;
        this.codeSchemeDao = codeSchemeDao;
        this.codeParser = codeParser;
        this.codeDao = codeDao;
        this.dtoMapperService = dtoMapperService;
    }

    @Transactional
    public Set<CodeDTO> findAll(final PageRequest pageRequest) {
        final Set<Code> codes = codeDao.findAll(pageRequest);
        return dtoMapperService.mapDeepCodeDtos(codes);
    }

    @Transactional
    public int getCodeCount() {
        return codeDao.getCodeCount();
    }

    @Transactional
    public Set<CodeDTO> findAll() {
        return dtoMapperService.mapDeepCodeDtos(codeDao.findAll());
    }

    @Transactional
    public CodeDTO findByCodeSchemeAndCodeValueAndBroaderCodeId(final CodeScheme codeScheme,
                                                                final String codeValue,
                                                                final UUID broaderCodeId) {
        return dtoMapperService.mapDeepCodeDto(codeDao.findByCodeSchemeAndCodeValueAndBroaderCodeId(codeScheme, codeValue, broaderCodeId));
    }

    @Transactional
    public Set<CodeDTO> findByCodeSchemeId(final UUID codeSchemeId) {
        return dtoMapperService.mapDeepCodeDtos(codeDao.findByCodeSchemeId(codeSchemeId));
    }

    @Transactional
    public Set<CodeDTO> parseAndPersistCodesFromExcelWorkbook(final Workbook workbook,
                                                              final String sheetName,
                                                              final CodeScheme codeScheme) {
        Set<Code> codes;
        if (codeScheme != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeScheme.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            final HashMap<String, String> broaderCodeMapping = new HashMap<>();
            final Set<CodeDTO> codeDtos = codeParser.parseCodesFromExcelWorkbook(workbook, sheetName, broaderCodeMapping);
            codes = codeDao.updateCodesFromDtos(codeScheme, codeDtos, broaderCodeMapping, false);
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return dtoMapperService.mapDeepCodeDtos(codes);
    }

    @Transactional
    public Set<CodeDTO> parseAndPersistCodesFromSourceData(final String codeRegistryCodeValue,
                                                           final String codeSchemeCodeValue,
                                                           final String format,
                                                           final InputStream inputStream,
                                                           final String jsonPayload) {
        return parseAndPersistCodesFromSourceData(false, codeRegistryCodeValue, codeSchemeCodeValue, format, inputStream, jsonPayload);
    }

    @Transactional
    public Set<CodeDTO> parseAndPersistCodesFromSourceData(final boolean isAuthorized,
                                                           final String codeRegistryCodeValue,
                                                           final String codeSchemeCodeValue,
                                                           final String format,
                                                           final InputStream inputStream,
                                                           final String jsonPayload) {
        final Set<Code> codes;
        final CodeRegistry codeRegistry = codeRegistryDao.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryAndCodeValue(codeRegistry, codeSchemeCodeValue);
            final HashMap<String, String> broaderCodeMapping = new HashMap<>();
            if (codeScheme != null) {
                if (!isAuthorized && !authorizationManager.canBeModifiedByUserInOrganization(codeScheme.getOrganizations())) {
                    throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
                }
                switch (format.toLowerCase()) {
                    case FORMAT_JSON:
                        if (jsonPayload != null && !jsonPayload.isEmpty()) {
                            final Set<CodeDTO> codeDtos = codeParser.parseCodesFromJsonData(jsonPayload);
                            codes = codeDao.updateCodesFromDtos(codeScheme, codeDtos, broaderCodeMapping, true);
                        } else {
                            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
                        }
                        break;
                    case FORMAT_EXCEL:
                        codes = codeDao.updateCodesFromDtos(codeScheme, codeParser.parseCodesFromExcelInputStream(inputStream, ApiConstants.EXCEL_SHEET_CODES, broaderCodeMapping), broaderCodeMapping, false);
                        break;
                    case FORMAT_CSV:
                        codes = codeDao.updateCodesFromDtos(codeScheme, codeParser.parseCodesFromCsvInputStream(inputStream, broaderCodeMapping), broaderCodeMapping, false);
                        break;
                    default:
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
                }
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return dtoMapperService.mapDeepCodeDtos(codes);
    }

    @Transactional
    public Set<CodeDTO> parseAndPersistCodeFromJson(final String codeRegistryCodeValue,
                                                    final String codeSchemeCodeValue,
                                                    final String codeCodeValue,
                                                    final String jsonPayload) {
        final Set<Code> codes;
        final CodeRegistry codeRegistry = codeRegistryDao.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryAndCodeValue(codeRegistry, codeSchemeCodeValue);
            if (codeScheme != null) {
                if (!authorizationManager.canBeModifiedByUserInOrganization(codeScheme.getOrganizations())) {
                    throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
                }
                try {
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        final CodeDTO codeDto = codeParser.parseCodeFromJsonData(jsonPayload);
                        if (!codeDto.getCodeValue().equalsIgnoreCase(codeCodeValue)) {
                            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_PATH_CODE_MISMATCH));
                        }
                        codes = codeDao.updateCodeFromDto(codeScheme, codeDto);
                    } else {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
                    }
                } catch (final YtiCodeListException e) {
                    throw e;
                } catch (final Exception e) {
                    LOG.error("Caught exception in parseAndPersistCodeFromJson.", e);
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
                }
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return dtoMapperService.mapDeepCodeDtos(codes);
    }

    private Set<CodeDTO> decreaseChildHierarchyLevel(final UUID broaderCodeId) {
        final Set<Code> childCodes = codeDao.findByBroaderCodeId(broaderCodeId);
        childCodes.forEach(code -> {
            final int hierarchyLevel;
            if (code.getHierarchyLevel() == null || code.getHierarchyLevel() < 1) {
                hierarchyLevel = 1;
            } else {
                hierarchyLevel = code.getHierarchyLevel() - 1;
            }
            code.setHierarchyLevel(hierarchyLevel);
            if (code.getBroaderCode() != null) {
                decreaseChildHierarchyLevel(code.getId());
            }
        });
        codeDao.save(childCodes);
        return dtoMapperService.mapDeepCodeDtos(childCodes);
    }

    @Transactional
    public Set<CodeDTO> removeBroaderCodeId(final UUID broaderCodeId) {
        final Set<CodeDTO> updateCodes = new HashSet<>();
        final Set<Code> childCodes = codeDao.findByBroaderCodeId(broaderCodeId);
        if (childCodes != null && !childCodes.isEmpty()) {
            childCodes.forEach(code -> {
                code.setBroaderCode(null);
                code.setHierarchyLevel(1);
                updateCodes.addAll(decreaseChildHierarchyLevel(code.getId()));
            });
            updateCodes.addAll(dtoMapperService.mapDeepCodeDtos(childCodes));
            codeDao.save(childCodes);
        }
        return updateCodes;
    }

    @Transactional
    public CodeDTO deleteCode(final String codeRegistryCodeValue,
                              final String codeSchemeCodeValue,
                              final String codeCodeValue,
                              final Set<CodeDTO> affectedCodes) {
        final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme != null) {
            final Code codeToBeDeleted = codeDao.findByCodeSchemeAndCodeValue(codeScheme, codeCodeValue);
            if (codeToBeDeleted != null) {
                if (authorizationManager.canCodeBeDeleted(codeToBeDeleted)) {
                    if (codeToBeDeleted.getMembers() != null && !codeToBeDeleted.getMembers().isEmpty()) {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODE_DELETE_IN_USE));
                    }
                    if (codeScheme.getDefaultCode() != null && codeScheme.getDefaultCode().getCodeValue().equalsIgnoreCase(codeToBeDeleted.getCodeValue())) {
                        codeScheme.setDefaultCode(null);
                        codeSchemeDao.save(codeScheme);
                    }
                    affectedCodes.addAll(removeBroaderCodeId(codeToBeDeleted.getId()));
                    final CodeDTO codeToBeDeletedDTO = dtoMapperService.mapCodeDto(codeToBeDeleted, true, true, true);
                    codeDao.delete(codeToBeDeleted);
                    return codeToBeDeletedDTO;
                } else {
                    throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
                }
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }

    @Transactional
    @Nullable
    public CodeDTO findByCodeRegistryCodeValueAndCodeSchemeCodeValueAndCodeValue(final String codeRegistryCodeValue,
                                                                                 final String codeSchemeCodeValue,
                                                                                 final String codeCodeValue) {
        CodeRegistry registry = codeRegistryDao.findByCodeValue(codeRegistryCodeValue);
        CodeScheme scheme = codeSchemeDao.findByCodeRegistryAndCodeValue(registry, codeSchemeCodeValue);
        Code code = codeDao.findByCodeSchemeAndCodeValue(scheme, codeCodeValue);
        if (code == null) {
            return null;
        }
        return dtoMapperService.mapDeepCodeDto(code);
    }
}
