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
import fi.vm.yti.codelist.intake.dao.MemberDao;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Member;
import fi.vm.yti.codelist.intake.parser.impl.CodeParserImpl;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.CodeService;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Singleton
@Service
public class CodeServiceImpl implements CodeService, AbstractBaseService {

    private static final Logger LOG = LoggerFactory.getLogger(CodeServiceImpl.class);
    private final AuthorizationManager authorizationManager;
    private final CodeRegistryDao codeRegistryDao;
    private final CodeSchemeDao codeSchemeDao;
    private final CodeDao codeDao;
    private final CodeParserImpl codeParser;
    private final DtoMapperService dtoMapperService;
    private final MemberDao memberDao;

    @Inject
    public CodeServiceImpl(final AuthorizationManager authorizationManager,
                           final CodeRegistryDao codeRegistryDao,
                           final CodeSchemeDao codeSchemeDao,
                           final CodeParserImpl codeParser,
                           final CodeDao codeDao,
                           final DtoMapperService dtoMapperService,
                           final MemberDao memberDao) {
        this.authorizationManager = authorizationManager;
        this.codeRegistryDao = codeRegistryDao;
        this.codeSchemeDao = codeSchemeDao;
        this.codeParser = codeParser;
        this.codeDao = codeDao;
        this.dtoMapperService = dtoMapperService;
        this.memberDao = memberDao;
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
    public CodeDTO findById(final UUID codeId) {
        return dtoMapperService.mapDeepCodeDto(codeDao.findById(codeId));
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
            codes = codeDao.updateCodesFromDtos(codeScheme, codeDtos, broaderCodeMapping, true);
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODESCHEME_NOT_FOUND));
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
                            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_JSON_PAYLOAD_EMPTY));
                        }
                        break;
                    case FORMAT_EXCEL:
                        codes = codeDao.updateCodesFromDtos(codeScheme, codeParser.parseCodesFromExcelInputStream(inputStream, ApiConstants.EXCEL_SHEET_CODES, broaderCodeMapping), broaderCodeMapping, false);
                        break;
                    case FORMAT_CSV:
                        codes = codeDao.updateCodesFromDtos(codeScheme, codeParser.parseCodesFromCsvInputStream(inputStream, broaderCodeMapping), broaderCodeMapping, false);
                        break;
                    default:
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_INVALID_FORMAT));
                }
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODESCHEME_NOT_FOUND));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODEREGISTRY_NOT_FOUND));
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
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_JSON_PAYLOAD_EMPTY));
                    }
                } catch (final YtiCodeListException e) {
                    throw e;
                } catch (final Exception e) {
                    LOG.error("Caught exception in parseAndPersistCodeFromJson.", e);
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_JSON_PARSING_ERROR));
                }
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODESCHEME_NOT_FOUND));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODEREGISTRY_NOT_FOUND));
        }
        return dtoMapperService.mapDeepCodeDtos(codes);
    }

    private void removeBroaderCodeId(final UUID broaderCodeId,
                                     final Set<CodeDTO> affectedCodes) {
        final Set<Code> childCodes = codeDao.findByBroaderCodeId(broaderCodeId);
        if (childCodes != null && !childCodes.isEmpty()) {
            childCodes.forEach(code -> {
                code.setBroaderCode(null);
                code.setHierarchyLevel(1);
                removeBroaderCodeId(code.getId(), affectedCodes);
            });
            affectedCodes.addAll(dtoMapperService.mapDeepCodeDtos(childCodes));
            codeDao.save(childCodes);
        }
    }

    @Transactional
    public CodeDTO deleteCode(final String codeRegistryCodeValue,
                              final String codeSchemeCodeValue,
                              final String codeCodeValue,
                              final Set<CodeDTO> affectedCodes) {
        final CodeScheme codeScheme = codeSchemeDao.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
        if (isServiceClassificationCodeScheme(codeScheme) || isLanguageCodeCodeScheme(codeScheme)) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODE_CANNOT_BE_DELETED));
        }
        if (codeScheme != null) {
            final Code codeToBeDeleted = codeDao.findByCodeSchemeAndCodeValue(codeScheme, codeCodeValue);
            if (codeToBeDeleted != null) {
                if (authorizationManager.canCodeBeDeleted(codeToBeDeleted)) {
                    final Set<Member> filteredMembers = filterRelatedMembers(codeScheme, codeToBeDeleted);
                    if (!filteredMembers.isEmpty()) {
                        final StringBuilder identifier = new StringBuilder();
                        for (final Member relatedMember : codeToBeDeleted.getMembers()) {
                            if (identifier.length() == 0) {
                                identifier.append(relatedMember.getUri());
                            } else {
                                identifier.append("\n").append(relatedMember.getUri());
                            }
                        }
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODE_DELETE_IN_USE, identifier.toString()));
                    }
                    final Set<Member> membersToBeDeleted = filterToBeDeletedMembers(codeScheme, codeToBeDeleted);
                    if (!membersToBeDeleted.isEmpty()) {
                        memberDao.delete(membersToBeDeleted);
                    }
                    if (codeScheme.getDefaultCode() != null && codeScheme.getDefaultCode().getCodeValue().equalsIgnoreCase(codeToBeDeleted.getCodeValue())) {
                        codeScheme.setDefaultCode(null);
                        codeSchemeDao.save(codeScheme);
                    }
                    removeBroaderCodeId(codeToBeDeleted.getId(), affectedCodes);
                    final CodeDTO codeToBeDeletedDTO = dtoMapperService.mapCodeDto(codeToBeDeleted, true, true, true);
                    codeToBeDeleted.setMembers(null);
                    codeDao.delete(codeToBeDeleted);
                    return codeToBeDeletedDTO;
                } else {
                    throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
                }
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODE_NOT_FOUND));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODESCHEME_NOT_FOUND));
        }
    }

    private Set<Member> filterRelatedMembers(final CodeScheme codeScheme,
                                             final Code code) {
        final Set<Member> filteredMembers = new HashSet<>();
        final Set<Member> relatedMembers = code.getMembers();
        if (relatedMembers != null) {
            for (final Member member : relatedMembers) {
                if ((codeScheme != member.getExtension().getParentCodeScheme() && !member.getExtension().getPropertyType().getLocalName().equals("crossReferenceList") ) ||
                    member.getExtension().getPropertyType().getLocalName().equals("crossReferenceList")) {
                    filteredMembers.add(member);
                }
            }
        }
        return filteredMembers;
    }

    private Set<Member> filterToBeDeletedMembers(final CodeScheme codeScheme,
                                                 final Code code) {
        final Set<Member> filteredMembers = new HashSet<>();
        final Set<Member> relatedMembers = code.getMembers();
        if (relatedMembers != null) {
            for (final Member member : relatedMembers) {
                if (codeScheme == member.getExtension().getParentCodeScheme()) {
                    filteredMembers.add(member);
                }
            }
        }
        return filteredMembers;
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
