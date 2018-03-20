package fi.vm.yti.codelist.intake.service;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.exception.ExistingCodeException;
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
    private final ApiUtils apiUtils;

    @Inject
    public CodeService(final AuthorizationManager authorizationManager,
                       final CodeRegistryRepository codeRegistryRepository,
                       final CodeSchemeRepository codeSchemeRepository,
                       final CodeRepository codeRepository,
                       final CodeParser codeParser,
                       final ApiUtils apiUtils) {
        this.authorizationManager = authorizationManager;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
        this.codeParser = codeParser;
        this.apiUtils = apiUtils;
    }

    @Transactional
    public Set<CodeDTO> findAll() {
        return mapDeepCodeDtos(codeRepository.findAll());
    }

    @Transactional
    public CodeDTO findByCodeSchemeAndCodeValueAndBroaderCodeId(final CodeScheme codeScheme, final String codeValue, final UUID broaderCodeId) {
        return mapDeepCodeDto(codeRepository.findByCodeSchemeAndCodeValueAndBroaderCodeId(codeScheme, codeValue, broaderCodeId));
    }

    @Transactional
    public Set<CodeDTO> findByCodeSchemeId(final UUID codeSchemeId) {
        return mapDeepCodeDtos(codeRepository.findByCodeSchemeId(codeSchemeId));
    }

    @Transactional
    public Set<CodeDTO> parseAndPersistCodesFromExcelWorkbook(final String codeRegistryCodeValue,
                                                              final String codeSchemeCodeValue,
                                                              final Workbook workbook) {
        Set<Code> codes;
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValue(codeRegistryCodeValue);
        if (codeRegistry != null) {
            if (!authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValue(codeRegistry, codeSchemeCodeValue);
            final HashMap<String, String> broaderCodeMapping = new HashMap<>();
            if (codeScheme != null) {
                final Set<CodeDTO> codeDtos = codeParser.parseCodesFromExcelWorkbook(workbook, broaderCodeMapping);
                codes = updateCodeEntities(codeScheme, codeDtos);
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        return mapDeepCodeDtos(codes);
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
            final HashMap<String, String> broaderCodeMapping = new HashMap<>();
            if (codeScheme != null) {
                switch (format.toLowerCase()) {
                    case FORMAT_JSON:
                        if (jsonPayload != null && !jsonPayload.isEmpty()) {
                            codes = updateCodeEntities(codeScheme, codeParser.parseCodesFromJsonData(jsonPayload));
                        } else {
                            throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), ERR_MSG_USER_500));
                        }
                        break;
                    case FORMAT_EXCEL:
                        codes = updateCodeEntities(codeScheme, codeParser.parseCodesFromExcelInputStream(inputStream, broaderCodeMapping));
                        if (!broaderCodeMapping.isEmpty()) {
                            setBroaderCodesAndEvaluateHierarchyLevels(broaderCodeMapping, codes);
                        }
                        break;
                    case FORMAT_CSV:
                        codes = updateCodeEntities(codeScheme, codeParser.parseCodesFromCsvInputStream(inputStream, broaderCodeMapping));
                        if (!broaderCodeMapping.isEmpty()) {
                            setBroaderCodesAndEvaluateHierarchyLevels(broaderCodeMapping, codes);
                        }
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
        return mapDeepCodeDtos(codes);
    }

    private void validateCodeForCodeScheme(final CodeDTO code) {
        if (code.getId() != null) {
            final Code existingCode = codeRepository.findById(code.getId());
            if (existingCode != null && !existingCode.getCodeValue().equalsIgnoreCase(code.getCodeValue())) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_EXISTING_CODE_MISMATCH));
            }
        }
    }

    private void checkForExistingCodeInCodeScheme(final CodeScheme codeScheme,
                                                  final CodeDTO fromCode) {
        final Code code = codeRepository.findByCodeSchemeAndCodeValue(codeScheme, fromCode.getCodeValue());
        if (code != null) {
            throw new ExistingCodeException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ALREADY_EXISTING_CODE, code.getCodeValue()));
        }
    }

    public Code updateCodeEntity(final CodeScheme codeScheme,
                                 final CodeDTO codeDto) {
        Code code = null;
        if (codeScheme != null) {
            code = createOrUpdateCode(codeScheme, codeDto);
        }
        codeRepository.save(code);
        codeSchemeRepository.save(codeScheme);
        return code;
    }

    public Set<Code> updateCodeEntities(final CodeScheme codeScheme,
                                        final Set<CodeDTO> codeDtos) {
        final Set<Code> codes = new HashSet<>();
        final Map<String, String> broaderCodeMapping = new HashMap<>();
        if (codeScheme != null) {
            for (final CodeDTO fromCode : codeDtos) {
                final Code code = createOrUpdateCode(codeScheme, fromCode);
                if (code != null) {
                    codes.add(code);
                }
            }
            setBroaderCodesAndEvaluateHierarchyLevels(broaderCodeMapping, codes);
        }
        if (!codes.isEmpty()) {
            codeRepository.save(codes);
            codeSchemeRepository.save(codeScheme);
        }
        return codes;
    }

    private Code createOrUpdateCode(final CodeScheme codeScheme,
                                    final CodeDTO fromCode) {
        validateCodeForCodeScheme(fromCode);
        final Code existingCode;
        if (fromCode.getId() != null) {
            existingCode = codeRepository.findById(fromCode.getId());
            if (existingCode == null) {
                checkForExistingCodeInCodeScheme(codeScheme, fromCode);
            }
        } else {
            existingCode = codeRepository.findByCodeSchemeAndCodeValue(codeScheme, fromCode.getCodeValue());
        }
        final Code code;
        if (existingCode != null) {
            code = updateCode(codeScheme, existingCode, fromCode);
        } else {
            code = createCode(codeScheme, fromCode);
        }
        return code;
    }

    private Code updateCode(final CodeScheme codeScheme,
                            final Code existingCode,
                            final CodeDTO fromCode) {
        final String uri = apiUtils.createCodeUri(codeScheme.getCodeRegistry(), codeScheme, existingCode);
        final String url = apiUtils.createCodeUrl(codeScheme.getCodeRegistry(), codeScheme, existingCode);
        boolean hasChanges = false;
        if (!Objects.equals(existingCode.getStatus(), fromCode.getStatus())) {
            if (!authorizationManager.isSuperUser() && Status.valueOf(existingCode.getStatus()).ordinal() >= Status.VALID.ordinal() && Status.valueOf(fromCode.getStatus()).ordinal() < Status.VALID.ordinal()) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_STATUS_CHANGE_NOT_ALLOWED));
            }
            existingCode.setStatus(fromCode.getStatus());
            hasChanges = true;
        }
        if (!Objects.equals(existingCode.getCodeScheme(), codeScheme)) {
            existingCode.setCodeScheme(codeScheme);
            hasChanges = true;
        }
        if (!Objects.equals(existingCode.getUri(), uri)) {
            existingCode.setUri(uri);
            hasChanges = true;
        }
        if (!Objects.equals(existingCode.getUrl(), url)) {
            existingCode.setUrl(url);
            hasChanges = true;
        }
        if (!Objects.equals(existingCode.getShortName(), fromCode.getShortName())) {
            existingCode.setShortName(fromCode.getShortName());
            hasChanges = true;
        }
        if (!Objects.equals(existingCode.getHierarchyLevel(), fromCode.getHierarchyLevel())) {
            existingCode.setHierarchyLevel(fromCode.getHierarchyLevel());
            hasChanges = true;
        }
        if (!Objects.equals(existingCode.getBroaderCodeId(), fromCode.getBroaderCodeId())) {
            existingCode.setBroaderCodeId(fromCode.getBroaderCodeId());
            hasChanges = true;
        }
        for (final Map.Entry<String, String> entry : fromCode.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCode.getPrefLabel(language), value)) {
                existingCode.setPrefLabel(language, value);
                hasChanges = true;
            }
        }
        for (final Map.Entry<String, String> entry : fromCode.getDescription().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCode.getDescription(language), value)) {
                existingCode.setDescription(language, value);
                hasChanges = true;
            }
        }
        for (final Map.Entry<String, String> entry : fromCode.getDefinition().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCode.getDefinition(language), value)) {
                existingCode.setDefinition(language, value);
                hasChanges = true;
            }
        }
        if (!Objects.equals(existingCode.getStartDate(), fromCode.getStartDate())) {
            existingCode.setStartDate(fromCode.getStartDate());
            hasChanges = true;
        }
        if (!Objects.equals(existingCode.getEndDate(), fromCode.getEndDate())) {
            existingCode.setEndDate(fromCode.getEndDate());
            hasChanges = true;
        }
        if (hasChanges) {
            final Date timeStamp = new Date(System.currentTimeMillis());
            existingCode.setModified(timeStamp);
        }
        return existingCode;
    }

    private Code createCode(final CodeScheme codeScheme,
                            final CodeDTO fromCode) {
        final Code code = new Code();
        if (fromCode.getId() != null) {
            code.setId(fromCode.getId());
        } else {
            final UUID uuid = UUID.randomUUID();
            code.setId(uuid);
        }
        code.setStatus(fromCode.getStatus());
        code.setCodeScheme(codeScheme);
        code.setCodeValue(fromCode.getCodeValue());
        code.setShortName(fromCode.getShortName());
        code.setHierarchyLevel(fromCode.getHierarchyLevel());
        code.setBroaderCodeId(fromCode.getBroaderCodeId());
        final Date timeStamp = new Date(System.currentTimeMillis());
        code.setModified(timeStamp);
        for (Map.Entry<String, String> entry : fromCode.getPrefLabel().entrySet()) {
            code.setPrefLabel(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : fromCode.getDescription().entrySet()) {
            code.setDescription(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : fromCode.getDefinition().entrySet()) {
            code.setDefinition(entry.getKey(), entry.getValue());
        }
        code.setStartDate(fromCode.getStartDate());
        code.setEndDate(fromCode.getEndDate());
        code.setUri(apiUtils.createCodeUri(codeScheme.getCodeRegistry(), codeScheme, code));
        code.setUrl(apiUtils.createCodeUrl(codeScheme.getCodeRegistry(), codeScheme, code));
        return code;
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
                        final CodeDTO codeDto = codeParser.parseCodeFromJsonData(jsonPayload);
                        if (!codeDto.getCodeValue().equalsIgnoreCase(codeCodeValue)) {
                            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_PATH_CODE_MISMATCH));
                        }
                        code = updateCodeEntity(codeScheme, codeDto);
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

    private Set<CodeDTO> decreaseChildHierarchyLevel(final UUID broaderCodeId) {
        final Set<Code> childCodes = codeRepository.findByBroaderCodeId(broaderCodeId);
        childCodes.forEach(code -> {
            code.setHierarchyLevel(code.getHierarchyLevel() - 1);
            if (code.getBroaderCodeId() != null) {
                decreaseChildHierarchyLevel(code.getBroaderCodeId());
            }
        });
        codeRepository.save(childCodes);
        return mapDeepCodeDtos(childCodes);
    }

    @Transactional
    public Set<CodeDTO> removeBroaderCodeId(final UUID broaderCodeId) {
        final Set<CodeDTO> updateCodes = new HashSet<>();
        final Set<Code> childCodes = codeRepository.findByBroaderCodeId(broaderCodeId);
        if (childCodes != null && !childCodes.isEmpty()) {
            childCodes.forEach(code -> {
                code.setBroaderCodeId(null);
                code.setHierarchyLevel(1);
                updateCodes.addAll(decreaseChildHierarchyLevel(code.getId()));
            });
            updateCodes.addAll(mapDeepCodeDtos(childCodes));
            codeRepository.save(childCodes);
        }
        return updateCodes;
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

    private void setBroaderCodesAndEvaluateHierarchyLevels(final Map<String, String> broaderCodeMapping,
                                                           final Set<Code> codes) {
        final Map<String, Code> codeMap = new HashMap<>();
        codes.forEach(code -> codeMap.put(code.getCodeValue(), code));
        setBroaderCodes(broaderCodeMapping, codeMap);
        evaluateAndSetHierarchyLevels(codes);
    }

    private void setBroaderCodes(final Map<String, String> broaderCodeMapping,
                                 final Map<String, Code> codes) {
        broaderCodeMapping.forEach((codeCodeValue, broaderCodeCodeValue) -> {
            final Code code = codes.get(codeCodeValue);
            final Code broaderCode = codes.get(broaderCodeCodeValue);
            if (broaderCode == null && broaderCodeCodeValue != null) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_BROADER_CODE_DOES_NOT_EXIST));
            } else if (broaderCode != null && broaderCode.getCodeValue().equalsIgnoreCase(code.getCodeValue())) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_BROADER_CODE_SELF_REFERENCE));
            } else {
                code.setBroaderCodeId(broaderCode != null ? broaderCode.getId() : null);
            }
        });
    }

    public void evaluateAndSetHierarchyLevels(final Set<Code> codes) {
        final Set<Code> codesToEvaluate = new HashSet<>(codes);
        final Map<Integer, Set<UUID>> hierarchyMapping = new HashMap<>();
        int hierarchyLevel = 0;
        while (!codesToEvaluate.isEmpty()) {
            ++hierarchyLevel;
            resolveAndSetCodeHierarchyLevels(codesToEvaluate, hierarchyMapping, hierarchyLevel);
        }
    }

    private void resolveAndSetCodeHierarchyLevels(final Set<Code> codesToEvaluate,
                                                  final Map<Integer, Set<UUID>> hierarchyMapping,
                                                  final Integer hierarchyLevel) {
        final Set<Code> toRemove = new HashSet<>();
        codesToEvaluate.forEach(code -> {
            if (hierarchyLevel == 1 && code.getBroaderCodeId() == null || hierarchyLevel > 1 && code.getBroaderCodeId() != null && hierarchyMapping.get(hierarchyLevel - 1).contains(code.getBroaderCodeId())) {
                code.setHierarchyLevel(hierarchyLevel);
                final Set<UUID> uuids = hierarchyMapping.computeIfAbsent(hierarchyLevel, k -> new HashSet<>());
                uuids.add(code.getId());
                toRemove.add(code);
            }
        });
        codesToEvaluate.removeAll(toRemove);
    }
}
