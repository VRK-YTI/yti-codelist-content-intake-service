package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ErrorModel;
import fi.vm.yti.codelist.intake.parser.CodeParser;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Component
public class CodeService extends BaseService {

    private static final Logger LOG = LoggerFactory.getLogger(CodeService.class);
    private final AuthorizationManager authorizationManager;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRepository codeRepository;
    private final CodeParser codeParser;

    @Inject
    public CodeService(final AuthorizationManager authorizationManager,
                       final CodeRegistryRepository codeRegistryRepository,
                       final CodeSchemeRepository codeSchemeRepository,
                       final CodeRepository codeRepository,
                       final CodeParser codeParser) {
        this.authorizationManager = authorizationManager;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
        this.codeParser = codeParser;
    }

    @Transactional
    public Set<CodeDTO> findAll() {
        return mapDeepCodeDtos(codeRepository.findAll());
    }

    @Transactional
    public Set<CodeDTO> findByCodeSchemeId(final UUID codeSchemeId) {
        return mapDeepCodeDtos(codeRepository.findByCodeSchemeId(codeSchemeId));
    }

    @Transactional
    public Set<CodeDTO> parseAndPersistCodesFromSourceData(final String codeRegistryCodeValue,
                                                           final String codeSchemeCodeValue,
                                                           final String format,
                                                           final InputStream inputStream,
                                                           final String jsonPayload) {
        Set<Code> codes;
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValue(codeRegistry, codeSchemeCodeValue);
            if (codeScheme != null) {
                switch (format.toLowerCase()) {
                    case FORMAT_JSON:
                        if (jsonPayload != null && !jsonPayload.isEmpty()) {
                            codes = codeParser.parseCodesFromJsonData(codeScheme, jsonPayload);
                        } else {
                            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
                        }
                        break;
                    case FORMAT_EXCEL:
                        codes = codeParser.parseCodesFromExcelInputStream(codeScheme, inputStream);
                        break;
                    case FORMAT_CSV:
                        codes = codeParser.parseCodesFromCsvInputStream(codeScheme, inputStream);
                        break;
                    default:
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
                }
                if (codes != null && !codes.isEmpty()) {
                    codeRepository.save(codes);
                    codeSchemeRepository.save(codeScheme);
                }
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return mapDeepCodeDtos(codes);
    }

    @Transactional
    public CodeDTO parseAndPersistCodeFromJson(final String codeRegistryCodeValue,
                                               final String codeSchemeCodeValue,
                                               final String codeCodeValue,
                                               final String jsonPayload) {
        Code code = null;
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValue(codeRegistry, codeSchemeCodeValue);
            if (codeScheme != null) {
                try {
                    if (jsonPayload != null && !jsonPayload.isEmpty()) {
                        code = codeParser.parseCodeFromJsonData(codeScheme, jsonPayload);
                        if (!code.getCodeValue().equalsIgnoreCase(codeCodeValue)) {
                            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_PATH_CODE_MISMATCH));
                        }
                        codeRepository.save(code);
                        codeSchemeRepository.save(codeScheme);
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
        return mapDeepCodeDto(code);
    }

    @Transactional
    public Set<CodeDTO> removeBroaderCodeId(final UUID broaderCodeId) {
        final Set<Code> referencedCodes = codeRepository.findByBroaderCodeId(broaderCodeId);
        referencedCodes.forEach(code -> code.setBroaderCodeId(null));
        codeRepository.save(referencedCodes);
        return mapDeepCodeDtos(referencedCodes);
    }

    @Transactional
    public CodeDTO deleteCode(final String codeRegistryCodeValue,
                              final String codeSchemeCodeValue,
                              final String codeCodeValue) {
        if (authorizationManager.isSuperUser()) {
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryCodeValueAndCodeValue(codeRegistryCodeValue, codeSchemeCodeValue);
            final Code code = codeRepository.findByCodeSchemeAndCodeValue(codeScheme, codeCodeValue);
            final CodeDTO codeDto = mapCodeDto(code, false);
            codeRepository.delete(code);
            return codeDto;
        } else {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }
    }

    @Transactional
    @Nullable
    public CodeDTO findByCodeRegistryCodeValueAndCodeSchemeCodeValueAndCodeValue(String codeRegistryCodeValue, String codeSchemeCodeValue, String codeCodeValue) {
        CodeRegistry registry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        CodeScheme scheme = codeSchemeRepository.findByCodeRegistryAndCodeValue(registry, codeSchemeCodeValue);
        Code code = codeRepository.findByCodeSchemeAndCodeValue(scheme, codeCodeValue);
        if (code == null)
            return null;
        return mapDeepCodeDto(code);
    }
}
