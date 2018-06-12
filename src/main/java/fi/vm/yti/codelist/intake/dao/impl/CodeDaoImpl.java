package fi.vm.yti.codelist.intake.dao.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.dao.CodeDao;
import fi.vm.yti.codelist.intake.dao.ExternalReferenceDao;
import fi.vm.yti.codelist.intake.exception.ExistingCodeException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.log.EntityChangeLogger;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;
import static fi.vm.yti.codelist.intake.parser.impl.AbstractBaseParser.validateCodeCodeValue;

@Component
public class CodeDaoImpl implements CodeDao {

    private final EntityChangeLogger entityChangeLogger;
    private final ApiUtils apiUtils;
    private final AuthorizationManager authorizationManager;
    private final CodeRepository codeRepository;
    private final CodeSchemeRepository codeSchemeRepository;
    private final ExternalReferenceDao externalReferenceDao;

    public CodeDaoImpl(final EntityChangeLogger entityChangeLogger,
                       final ApiUtils apiUtils,
                       final AuthorizationManager authorizationManager,
                       final CodeRepository codeRepository,
                       final CodeSchemeRepository codeSchemeRepository,
                       final ExternalReferenceDao externalReferenceDao) {
        this.entityChangeLogger = entityChangeLogger;
        this.apiUtils = apiUtils;
        this.authorizationManager = authorizationManager;
        this.codeRepository = codeRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.externalReferenceDao = externalReferenceDao;
    }

    public int getCodeCount() {
        return codeRepository.getCodeCount();
    }

    public void save(final Code code) {
        codeRepository.save(code);
        entityChangeLogger.logCodeChange(code);
    }

    public void save(final Set<Code> codes) {
        codeRepository.save(codes);
        codes.forEach(entityChangeLogger::logCodeChange);
    }

    public void delete(final Code code) {
        entityChangeLogger.logCodeChange(code);
        codeRepository.delete(code);
    }

    public void delete(final Set<Code> codes) {
        codes.forEach(entityChangeLogger::logCodeChange);
        codeRepository.delete(codes);
    }

    public Set<Code> findAll(final PageRequest pageRequest) {
        return new HashSet<>(codeRepository.findAll(pageRequest).getContent());
    }

    public Set<Code> findAll() {
        return codeRepository.findAll();
    }

    public Code findByUri(final String uri) {
        return codeRepository.findByUriIgnoreCase(uri);
    }

    public Code findByCodeSchemeAndCodeValue(final CodeScheme codeScheme,
                                             final String codeValue) {
        return codeRepository.findByCodeSchemeCodeValueIgnoreCaseAndCodeValueIgnoreCase(codeScheme.getCodeValue(), codeValue);
    }

    public Code findByCodeSchemeAndCodeValueAndBroaderCodeId(final CodeScheme codeScheme,
                                                             final String codeValue,
                                                             final UUID broaderCodeId) {
        return codeRepository.findByCodeSchemeCodeValueIgnoreCaseAndCodeValueIgnoreCaseAndBroaderCodeId(codeScheme.getCodeValue(), codeValue, broaderCodeId);
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
            updateExternalReferences(codeScheme, code, codeDto);
        }
        save(code);
        codeSchemeRepository.save(codeScheme);
        return code;
    }

    @Transactional
    public Set<Code> updateCodesFromDtos(final CodeScheme codeScheme,
                                         final Set<CodeDTO> codeDtos,
                                         final Map<String, String> broaderCodeMapping,
                                         final boolean updateExternalReferences) {
        final Set<Code> codes = new HashSet<>();
        if (codeScheme != null) {
            for (final CodeDTO codeDto : codeDtos) {
                final Code code = createOrUpdateCode(codeScheme, codeDto);
                if (updateExternalReferences) {
                    updateExternalReferences(codeScheme, code, codeDto);
                }
                if (code != null) {
                    codes.add(code);
                    save(code);
                }
            }
            setBroaderCodesAndEvaluateHierarchyLevels(broaderCodeMapping, codes);
        }
        if (!codes.isEmpty()) {
            save(codes);
            codeSchemeRepository.save(codeScheme);
        }
        return codes;
    }

    private void updateExternalReferences(final CodeScheme codeScheme,
                                          final Code code,
                                          final CodeDTO codeDto) {
        final Set<ExternalReference> externalReferences = externalReferenceDao.updateExternalReferenceEntitiesFromDtos(codeDto.getExternalReferences(), codeScheme);
        code.setExternalReferences(externalReferences);
    }

    @Transactional
    public Code createOrUpdateCode(final CodeScheme codeScheme,
                                   final CodeDTO codeDto) {
        validateCodeForCodeScheme(codeDto);
        final Code existingCode;
        if (codeDto.getId() != null) {
            existingCode = codeRepository.findById(codeDto.getId());
            if (existingCode == null) {
                checkForExistingCodeInCodeScheme(codeScheme, codeDto);
            }
        } else {
            existingCode = codeRepository.findByCodeSchemeCodeValueIgnoreCaseAndCodeValueIgnoreCase(codeScheme.getCodeValue(), codeDto.getCodeValue());
        }
        final Code code;
        if (existingCode != null) {
            code = updateCode(codeScheme, existingCode, codeDto);
        } else {
            code = createCode(codeScheme, codeDto);
        }
        return code;
    }

    private Code updateCode(final CodeScheme codeScheme,
                            final Code existingCode,
                            final CodeDTO fromCode) {
        final String uri = apiUtils.createCodeUri(codeScheme.getCodeRegistry(), codeScheme, existingCode);
        if (!Objects.equals(existingCode.getStatus(), fromCode.getStatus())) {
            if (!authorizationManager.isSuperUser() && Status.valueOf(existingCode.getStatus()).ordinal() >= Status.VALID.ordinal() && Status.valueOf(fromCode.getStatus()).ordinal() < Status.VALID.ordinal()) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_STATUS_CHANGE_NOT_ALLOWED));
            }
            existingCode.setStatus(fromCode.getStatus());
        }
        if (!Objects.equals(existingCode.getCodeScheme(), codeScheme)) {
            existingCode.setCodeScheme(codeScheme);
        }
        if (!Objects.equals(existingCode.getUri(), uri)) {
            existingCode.setUri(uri);
        }
        if (!Objects.equals(existingCode.getShortName(), fromCode.getShortName())) {
            existingCode.setShortName(fromCode.getShortName());
        }
        if (!Objects.equals(existingCode.getHierarchyLevel(), fromCode.getHierarchyLevel())) {
            existingCode.setHierarchyLevel(fromCode.getHierarchyLevel());
        }
        if (!Objects.equals(existingCode.getOrder(), fromCode.getOrder())) {
            if (fromCode.getOrder() != null) {
                existingCode.setOrder(fromCode.getOrder());
            } else {
                existingCode.setOrder(getNextOrderInSequence(codeScheme));
            }
        }
        if (!Objects.equals(existingCode.getBroaderCodeId(), fromCode.getBroaderCodeId())) {
            existingCode.setBroaderCodeId(fromCode.getBroaderCodeId());
        }
        for (final Map.Entry<String, String> entry : fromCode.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCode.getPrefLabel(language), value)) {
                existingCode.setPrefLabel(language, value);
            }
        }
        for (final Map.Entry<String, String> entry : fromCode.getDescription().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCode.getDescription(language), value)) {
                existingCode.setDescription(language, value);
            }
        }
        for (final Map.Entry<String, String> entry : fromCode.getDefinition().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCode.getDefinition(language), value)) {
                existingCode.setDefinition(language, value);
            }
        }
        if (!Objects.equals(existingCode.getStartDate(), fromCode.getStartDate())) {
            existingCode.setStartDate(fromCode.getStartDate());
        }
        if (!Objects.equals(existingCode.getEndDate(), fromCode.getEndDate())) {
            existingCode.setEndDate(fromCode.getEndDate());
        }
        if (!Objects.equals(existingCode.getConceptUriInVocabularies(), fromCode.getConceptUriInVocabularies())) {
            existingCode.setConceptUriInVocabularies(fromCode.getConceptUriInVocabularies());
        }
        return existingCode;
    }

    private Integer getNextOrderInSequence(final CodeScheme codeScheme) {
        final List<Integer> codes = codeRepository.getInMaxOrder(codeScheme);
        if (codes.isEmpty()) {
            return 1;
        } else {
            final Integer maxOrder = codes.iterator().next();
            if (maxOrder != null) {
                return maxOrder + 1;
            } else {
                return 1;
            }
        }
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
        final String codeValue = fromCode.getCodeValue();
        validateCodeCodeValue(codeValue);
        code.setCodeValue(codeValue);
        code.setShortName(fromCode.getShortName());
        code.setHierarchyLevel(fromCode.getHierarchyLevel());
        code.setBroaderCodeId(fromCode.getBroaderCodeId());
        if (fromCode.getOrder() != null) {
            code.setOrder(fromCode.getOrder());
        } else {
            code.setOrder(getNextOrderInSequence(codeScheme));
        }

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
        code.setConceptUriInVocabularies(fromCode.getConceptUriInVocabularies());
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
        final Code code = codeRepository.findByCodeSchemeCodeValueIgnoreCaseAndCodeValueIgnoreCase(codeScheme.getCodeValue(), fromCode.getCodeValue());
        if (code != null) {
            throw new ExistingCodeException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ALREADY_EXISTING_CODE, code.getCodeValue()));
        }
    }

    private void setBroaderCodesAndEvaluateHierarchyLevels(final Map<String, String> broaderCodeMapping,
                                                           final Set<Code> codes) {
        final Map<String, Code> codeMap = new HashMap<>();
        codes.forEach(code -> codeMap.put(code.getCodeValue().toLowerCase(), code));
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
            } else if (code == null) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
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
