package fi.vm.yti.codelist.intake.dao.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.dao.CodeDao;
import fi.vm.yti.codelist.intake.exception.ExistingCodeException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ErrorModel;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Component
public class CodeDaoImpl implements CodeDao {

    private final ApiUtils apiUtils;
    private final AuthorizationManager authorizationManager;
    private final CodeRepository codeRepository;
    private final CodeSchemeRepository codeSchemeRepository;

    public CodeDaoImpl(final ApiUtils apiUtils,
                       final AuthorizationManager authorizationManager,
                       final CodeRepository codeRepository,
                       final CodeSchemeRepository codeSchemeRepository) {
        this.apiUtils = apiUtils;
        this.authorizationManager = authorizationManager;
        this.codeRepository = codeRepository;
        this.codeSchemeRepository = codeSchemeRepository;
    }

    public void save(final Set<Code> codes) {
        codeRepository.save(codes);
    }

    public void save(final Code code) {
        codeRepository.save(code);
    }

    public void delete(final Code code) {
        codeRepository.delete(code);
    }

    public Set<Code> findAll() {
        return codeRepository.findAll();
    }

    public Code findByCodeSchemeAndCodeValue(final CodeScheme codeScheme,
                                             final String codeValue) {
        return codeRepository.findByCodeSchemeAndCodeValue(codeScheme, codeValue);
    }

    public Code findByCodeSchemeAndCodeValueAndBroaderCodeId(final CodeScheme codeScheme,
                                                             final String codeValue,
                                                             final UUID broaderCodeId) {
        return codeRepository.findByCodeSchemeAndCodeValueAndBroaderCodeId(codeScheme, codeValue, broaderCodeId);
    }

    public Code findById(UUID id) {
        return codeRepository.findById(id);
    }

    public Set<Code> findByCodeSchemeId(final UUID codeSchemeId) {
        return codeRepository.findByCodeSchemeId(codeSchemeId);
    }

    public Set<Code> findByCodeSchemeIdAndBroaderCodeIdIsNull(final UUID codeSchemeId) {
        return codeRepository.findByCodeSchemeIdAndBroaderCodeIdIsNull(codeSchemeId);
    }

    public Set<Code> findByBroaderCodeId(final UUID broaderCodeId) {
        return codeRepository.findByBroaderCodeId(broaderCodeId);
    }

    @Transactional
    public Code updateCodeFromDto(final CodeScheme codeScheme,
                                  final CodeDTO codeDto) {
        Code code = null;
        if (codeScheme != null) {
            code = createOrUpdateCode(codeScheme, codeDto);
        }
        codeRepository.save(code);
        codeSchemeRepository.save(codeScheme);
        return code;
    }

    @Transactional
    public Set<Code> updateCodesFromDtos(final CodeScheme codeScheme,
                                         final Set<CodeDTO> codeDtos,
                                         final Map<String, String> broaderCodeMapping) {
        final Set<Code> codes = new HashSet<>();
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
