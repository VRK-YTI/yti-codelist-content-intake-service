package fi.vm.yti.codelist.intake.dao.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.dao.CodeDao;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.dao.ExtensionDao;
import fi.vm.yti.codelist.intake.dao.ExternalReferenceDao;
import fi.vm.yti.codelist.intake.dao.MemberDao;
import fi.vm.yti.codelist.intake.exception.ExistingCodeException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.language.LanguageService;
import fi.vm.yti.codelist.intake.log.EntityChangeLogger;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;
import static fi.vm.yti.codelist.intake.parser.impl.AbstractBaseParser.validateCodeCodeValue;

@Component
public class CodeDaoImpl implements CodeDao {

    private static final int MAX_LEVEL = 10;

    private final EntityChangeLogger entityChangeLogger;
    private final ApiUtils apiUtils;
    private final AuthorizationManager authorizationManager;
    private final CodeRepository codeRepository;
    private final CodeSchemeRepository codeSchemeRepository;
    private final ExternalReferenceDao externalReferenceDao;
    private final LanguageService languageService;
    private final ExtensionDao extensionDao;
    private final MemberDao memberDao;
    private final CodeSchemeDao codeSchemeDao;

    public CodeDaoImpl(final EntityChangeLogger entityChangeLogger,
                       final ApiUtils apiUtils,
                       final AuthorizationManager authorizationManager,
                       final CodeRepository codeRepository,
                       final CodeSchemeRepository codeSchemeRepository,
                       final ExternalReferenceDao externalReferenceDao,
                       final LanguageService languageService,
                       @Lazy final ExtensionDao extensionDao,
                       @Lazy final MemberDao memberDao,
                       final CodeSchemeDao codeSchemeDao) {
        this.entityChangeLogger = entityChangeLogger;
        this.apiUtils = apiUtils;
        this.authorizationManager = authorizationManager;
        this.codeRepository = codeRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.externalReferenceDao = externalReferenceDao;
        this.languageService = languageService;
        this.extensionDao = extensionDao;
        this.memberDao = memberDao;
        this.codeSchemeDao = codeSchemeDao;
    }

    @Transactional
    public int getCodeCount() {
        return codeRepository.getCodeCount();
    }

    @Transactional
    public void save(final Code code) {
        save(code, true);
    }

    @Transactional
    public void save(final Code code,
                     final boolean logChange) {
        codeRepository.save(code);
        if (logChange) {
            entityChangeLogger.logCodeChange(code);
        }
    }

    @Transactional
    public void save(final Set<Code> codes,
                     final boolean logChange) {
        codeRepository.save(codes);
        if (logChange) {
            entityChangeLogger.logCodesChange(codes);
        }
    }

    @Transactional
    public void save(final Set<Code> codes) {
        save(codes, true);
    }

    @Transactional
    public void delete(final Code code) {
        entityChangeLogger.logCodeChange(code);
        codeRepository.delete(code);
    }

    @Transactional
    public void delete(final Set<Code> codes) {
        entityChangeLogger.logCodesChange(codes);
        codeRepository.delete(codes);
    }

    @Transactional
    public Set<Code> findAll(final PageRequest pageRequest) {
        return new HashSet<>(codeRepository.findAll(pageRequest).getContent());
    }

    @Transactional
    public Set<Code> findAll() {
        return codeRepository.findAll();
    }

    @Transactional
    public Code findByUri(final String uri) {
        return codeRepository.findByUriIgnoreCase(uri);
    }

    @Transactional
    public Code findByCodeSchemeAndCodeValue(final CodeScheme codeScheme,
                                             final String codeValue) {
        return codeRepository.findByCodeSchemeAndCodeValueIgnoreCase(codeScheme, codeValue);
    }

    @Transactional
    public Code findByCodeSchemeAndCodeValueAndBroaderCodeId(final CodeScheme codeScheme,
                                                             final String codeValue,
                                                             final UUID broaderCodeId) {
        return codeRepository.findByCodeSchemeAndCodeValueIgnoreCaseAndBroaderCodeId(codeScheme, codeValue, broaderCodeId);
    }

    @Transactional
    public Code findById(UUID id) {
        return codeRepository.findById(id);
    }

    @Transactional
    public Set<Code> findByCodeSchemeId(final UUID codeSchemeId) {
        return codeRepository.findByCodeSchemeId(codeSchemeId);
    }

    @Transactional
    public Set<Code> findByCodeSchemeIdAndBroaderCodeIdIsNull(final UUID codeSchemeId) {
        return codeRepository.findByCodeSchemeIdAndBroaderCodeIdIsNull(codeSchemeId);
    }

    @Transactional
    public Set<Code> findByBroaderCodeId(final UUID broaderCodeId) {
        return codeRepository.findByBroaderCodeId(broaderCodeId);
    }

    @Transactional
    public Set<Code> updateCodeFromDto(final CodeScheme codeScheme,
                                       final CodeDTO codeDto) {
        final Code code = createOrUpdateCode(codeScheme, codeDto, null, null, null);
        updateExternalReferences(codeScheme, code, codeDto);
        checkCodeHierarchyLevels(code);
        final Set<Code> codesAffected = new HashSet<>();
        codesAffected.add(code);
        evaluateAndSetHierarchyLevels(codesAffected, findByCodeSchemeId(codeScheme.getId()));
        save(code);
        setCodeExtensionMemberValues(codeDto);
        codeSchemeRepository.save(codeScheme);
        return codesAffected;
    }

    private void setCodeExtensionMemberValues(final CodeDTO code) {
        final Set<ExtensionDTO> codeExtensionDtos = code.getCodeExtensions();
        if (codeExtensionDtos != null && !codeExtensionDtos.isEmpty()) {
            codeExtensionDtos.forEach(extensionDto -> {
                final Extension extension = extensionDao.findById(extensionDto.getId());
                if (extension != null) {
                    final Set<MemberDTO> members = extensionDto.getMembers();
                    members.forEach(member -> {
                        if (member.getCode() == null) {
                            member.setCode(code);
                        }
                    });
                    memberDao.updateMemberEntitiesFromDtos(extension, members);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EXTENSION_NOT_FOUND));
                }
            });
        }
    }

    @Transactional
    public Set<Code> updateCodesFromDtos(final CodeScheme codeScheme,
                                         final Set<CodeDTO> codeDtos,
                                         final Map<String, String> broaderCodeMapping,
                                         final boolean updateExternalReferences) {
        final Set<Code> codesAffected = new HashSet<>();
        MutableInt nextOrder = new MutableInt(getNextOrderInSequence(codeScheme));
        final Set<Code> existingCodes = codeRepository.findByCodeSchemeId(codeScheme.getId());
        for (final CodeDTO codeDto : codeDtos) {
            final Code code = createOrUpdateCode(codeScheme, codeDto, existingCodes, codesAffected, nextOrder);
            save(code, false);
            setCodeExtensionMemberValues(codeDto);
            if (updateExternalReferences) {
                updateExternalReferences(codeScheme, code, codeDto);
            }
            if (code != null) {
                codesAffected.add(code);
                save(code, false);
            }
        }
        if (!codesAffected.isEmpty()) {
            codesAffected.forEach(this::checkCodeHierarchyLevels);
            setBroaderCodesAndEvaluateHierarchyLevels(broaderCodeMapping, codesAffected, codeScheme);
            save(codesAffected);
            codeSchemeRepository.save(codeScheme);
        }
        return codesAffected;
    }

    private void updateExternalReferences(final CodeScheme codeScheme,
                                          final Code code,
                                          final CodeDTO codeDto) {
        final Set<ExternalReference> externalReferences = externalReferenceDao.updateExternalReferenceEntitiesFromDtos(codeDto.getExternalReferences(), codeScheme);
        code.setExternalReferences(externalReferences);
    }

    private Code findExistingCodeFromSet(final Set<Code> existingCodes,
                                         final String codeValue) {
        for (final Code code : existingCodes) {
            if (code.getCodeValue().equalsIgnoreCase(codeValue)) {
                return code;
            }
        }
        return null;
    }

    @Transactional
    public Code createOrUpdateCode(final CodeScheme codeScheme,
                                   final CodeDTO codeDto,
                                   final Set<Code> existingCodes,
                                   final Set<Code> codes,
                                   final MutableInt nextOrder) {
        validateCodeForCodeScheme(codeDto);
        final Code existingCode;
        if (codeDto.getId() != null) {
            existingCode = codeRepository.findById(codeDto.getId());
            if (existingCode == null) {
                checkForExistingCodeInCodeScheme(codeScheme, codeDto);
            }
            validateCodeScheme(existingCode, codeScheme);
        } else if (existingCodes != null) {
            existingCode = findExistingCodeFromSet(existingCodes, codeDto.getCodeValue());
        } else {
            existingCode = codeRepository.findByCodeSchemeAndCodeValueIgnoreCase(codeScheme, codeDto.getCodeValue());
        }
        final Code code;
        if (existingCode != null) {
            code = updateCode(codeScheme, existingCode, codeDto, codes, nextOrder);
        } else {
            code = createCode(codeScheme, codeDto, codes, nextOrder);
        }
        return code;
    }

    private void validateCodeScheme(final Code code,
                                    final CodeScheme codeScheme) {
        if (code != null && code.getCodeScheme() != codeScheme) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODE_CODESCHEME_DOES_NOT_MATCH));
        }
    }

    private void checkOrderAndShiftExistingCodeOrderIfInUse(final CodeScheme codeScheme,
                                                            final CodeDTO fromCode,
                                                            final Set<Code> codes) {
        final Code code = codeRepository.findByCodeSchemeAndOrder(codeScheme, fromCode.getOrder());
        if (code != null && !code.getCodeValue().equalsIgnoreCase(fromCode.getCodeValue())) {
            code.setOrder(getNextOrderInSequence(codeScheme));
            save(code);
            codes.add(code);
        }
    }

    private Code updateCode(final CodeScheme codeScheme,
                            final Code existingCode,
                            final CodeDTO fromCode,
                            final Set<Code> codes,
                            final MutableInt nextOrder) {
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
                checkOrderAndShiftExistingCodeOrderIfInUse(codeScheme, fromCode, codes);
                existingCode.setOrder(fromCode.getOrder());
                if (fromCode.getOrder() > nextOrder.getValue()) {
                    nextOrder.setValue(fromCode.getOrder() + 1);
                }
            } else if (fromCode.getOrder() == null && existingCode.getOrder() == null) {
                final Integer next = getNextOrderInSequence(codeScheme);
                existingCode.setOrder(next);
                nextOrder.setValue(next + 1);
            } else {
                existingCode.setOrder(nextOrder.getValue());
                nextOrder.setValue(nextOrder.getValue() + 1);
            }
        }
        existingCode.setBroaderCode(resolveBroaderCode(fromCode, codeScheme));
        for (final Map.Entry<String, String> entry : fromCode.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguageForCodeScheme(codeScheme, language);
            final String value = entry.getValue();
            if (!Objects.equals(existingCode.getPrefLabel(language), value)) {
                existingCode.setPrefLabel(language, value);
            }
        }
        for (final Map.Entry<String, String> entry : fromCode.getDescription().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguageForCodeScheme(codeScheme, language);
            final String value = entry.getValue();
            if (!Objects.equals(existingCode.getDescription(language), value)) {
                existingCode.setDescription(language, value);
            }
        }
        for (final Map.Entry<String, String> entry : fromCode.getDefinition().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguageForCodeScheme(codeScheme, language);
            final String value = entry.getValue();
            if (!Objects.equals(existingCode.getDefinition(language), value)) {
                existingCode.setDefinition(language, value);
            }
        }
        if (fromCode.getSubCodeScheme() != null) {
            final CodeScheme subCodeScheme = codeSchemeDao.findByUri(fromCode.getCodeScheme().getUri());
            if (subCodeScheme != null) {
                if (subCodeScheme != existingCode.getSubCodeScheme()) {
                    existingCode.setSubCodeScheme(subCodeScheme);
                }
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_SUBCODESCHEME_NOT_FOUND));
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
        existingCode.setModified(new Date(System.currentTimeMillis()));
        return existingCode;
    }

    private Code createCode(final CodeScheme codeScheme,
                            final CodeDTO fromCode,
                            final Set<Code> codes,
                            final MutableInt nextOrder) {
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
        code.setBroaderCode(resolveBroaderCode(fromCode, codeScheme));
        if (fromCode.getOrder() != null) {
            checkOrderAndShiftExistingCodeOrderIfInUse(codeScheme, fromCode, codes);
            code.setOrder(fromCode.getOrder());
            if (fromCode.getOrder() > nextOrder.getValue()) {
                nextOrder.setValue(fromCode.getOrder() + 1);
            }
        } else if (nextOrder == null) {
            final int order = getNextOrderInSequence(codeScheme);
            code.setOrder(order);
        } else {
            code.setOrder(nextOrder.getValue());
            nextOrder.setValue(nextOrder.getValue() + 1);
        }
        for (Map.Entry<String, String> entry : fromCode.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguageForCodeScheme(codeScheme, language);
            code.setPrefLabel(language, entry.getValue());
        }
        for (Map.Entry<String, String> entry : fromCode.getDescription().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguageForCodeScheme(codeScheme, language);
            code.setDescription(language, entry.getValue());
        }
        for (Map.Entry<String, String> entry : fromCode.getDefinition().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguageForCodeScheme(codeScheme, language);
            code.setDefinition(language, entry.getValue());
        }
        if (fromCode.getSubCodeScheme() != null) {
            final CodeScheme subCodeScheme = codeSchemeDao.findByUri(fromCode.getCodeScheme().getUri());
            if (subCodeScheme != null) {
                code.setSubCodeScheme(subCodeScheme);
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_SUBCODESCHEME_NOT_FOUND));
            }
        }
        code.setStartDate(fromCode.getStartDate());
        code.setEndDate(fromCode.getEndDate());
        code.setUri(apiUtils.createCodeUri(codeScheme.getCodeRegistry(), codeScheme, code));
        code.setConceptUriInVocabularies(fromCode.getConceptUriInVocabularies());
        final Date timeStamp = new Date(System.currentTimeMillis());
        code.setCreated(timeStamp);
        code.setModified(timeStamp);
        return code;
    }

    private Code resolveBroaderCode(final CodeDTO fromCode,
                                    final CodeScheme codeScheme) {
        if (fromCode != null && fromCode.getBroaderCode() != null) {
            final Code broaderCode = findById(fromCode.getBroaderCode().getId());
            if (broaderCode != null && broaderCode.getCodeScheme() != codeScheme) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_EXISTING_CODE_MISMATCH));
            }
            return broaderCode;
        }
        return null;
    }

    private void validateCodeForCodeScheme(final CodeDTO code) {
        if (code.getId() != null) {
            final Code existingCode = codeRepository.findById(code.getId());
            if (existingCode != null && !existingCode.getCodeValue().equalsIgnoreCase(code.getCodeValue())) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_EXISTING_CODE_MISMATCH));
            }
        }
    }

    private Integer getNextOrderInSequence(final CodeScheme codeScheme) {
        final Integer maxOrder = codeRepository.getCodeMaxOrder(codeScheme.getId());
        if (maxOrder == null) {
            return 1;
        } else {
            return maxOrder + 1;
        }
    }

    private void checkForExistingCodeInCodeScheme(final CodeScheme codeScheme,
                                                  final CodeDTO fromCode) {
        final Code code = codeRepository.findByCodeSchemeAndCodeValueIgnoreCase(codeScheme, fromCode.getCodeValue());
        if (code != null) {
            throw new ExistingCodeException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ALREADY_EXISTING_CODE, code.getCodeValue()));
        }
    }

    private void setBroaderCodesAndEvaluateHierarchyLevels(final Map<String, String> broaderCodeMapping,
                                                           final Set<Code> codesAffected,
                                                           final CodeScheme codeScheme) {
        setBroaderCodes(broaderCodeMapping, codesAffected, codeScheme);
        save(codesAffected);
        evaluateAndSetHierarchyLevels(codesAffected, findByCodeSchemeId(codeScheme.getId()));
    }

    private void setBroaderCodes(final Map<String, String> broaderCodeMapping,
                                 final Set<Code> affectedCodes,
                                 final CodeScheme codeScheme) {
        affectedCodes.forEach(code -> {
            final String broaderCodeCodeValue = broaderCodeMapping.get(code.getCodeValue().toLowerCase());
            if (broaderCodeCodeValue != null) {
                final Code broaderCode = findByCodeSchemeAndCodeValue(codeScheme, broaderCodeCodeValue);
                if (broaderCode == null) {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_BROADER_CODE_DOES_NOT_EXIST, broaderCodeCodeValue));
                } else if (broaderCode.getCodeValue().equalsIgnoreCase(code.getCodeValue())) {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_BROADER_CODE_SELF_REFERENCE));
                }
                code.setBroaderCode(broaderCode);
            }
        });
    }

    public void evaluateAndSetHierarchyLevels(final Set<Code> codesToEvaluate,
                                              final Set<Code> codeSchemeCodes) {
        if (codeSchemeCodes != null && !codeSchemeCodes.isEmpty()) {
            final Set<Code> allCodes = new HashSet<>(codeSchemeCodes);
            final Map<Integer, Set<UUID>> hierarchyMapping = new HashMap<>();
            int hierarchyLevel = 0;
            while (!allCodes.isEmpty()) {
                ++hierarchyLevel;
                if (hierarchyLevel > 10) {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODE_HIERARCHY_MAXLEVEL_REACHED));
                }
                evaluateAndSetHierarchyLevels(allCodes, codesToEvaluate, hierarchyMapping, hierarchyLevel);
            }
        }
    }

    private void evaluateAndSetHierarchyLevels(final Set<Code> allCodes,
                                               final Set<Code> codesAffected,
                                               final Map<Integer, Set<UUID>> hierarchyMapping,
                                               final Integer hierarchyLevel) {
        final Set<Code> toRemove = new HashSet<>();
        allCodes.forEach(code -> {
            if ((hierarchyLevel == 1 && code.getBroaderCode() == null) ||
                (hierarchyLevel > 1 && code.getBroaderCode() != null && code.getBroaderCode().getId() != null && hierarchyMapping.get(hierarchyLevel - 1) != null && hierarchyMapping.get(hierarchyLevel - 1).contains(code.getBroaderCode().getId()))) {
                if (!hierarchyLevel.equals(code.getHierarchyLevel())) {
                    boolean match = false;
                    for (final Code codeOrig : codesAffected) {
                        if (codeOrig.getId().equals(code.getId())) {
                            match = true;
                            codeOrig.setHierarchyLevel(hierarchyLevel);
                        }
                    }
                    if (!match) {
                        code.setHierarchyLevel(hierarchyLevel);
                        codesAffected.add(code);
                    }
                }
                if (hierarchyMapping.get(hierarchyLevel) != null) {
                    hierarchyMapping.get(hierarchyLevel).add(code.getId());
                } else {
                    final Set<UUID> uuids = new HashSet<>();
                    uuids.add(code.getId());
                    hierarchyMapping.put(hierarchyLevel, uuids);
                }
                toRemove.add(code);
            }
        });
        allCodes.removeAll(toRemove);
    }

    private void checkCodeHierarchyLevels(final Code code) {
        final Set<Code> chainedCodes = new HashSet<>();
        chainedCodes.add(code);
        checkCodeHierarchyLevels(chainedCodes, code, 1);
    }

    private void checkCodeHierarchyLevels(final Set<Code> chainedCodes,
                                          final Code code,
                                          final int level) {
        if (level > MAX_LEVEL) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODE_HIERARCHY_MAXLEVEL_REACHED));
        }
        final Code broaderCode = code.getBroaderCode();
        if (broaderCode != null) {
            if (chainedCodes.contains(broaderCode)) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODE_CYCLIC_DEPENDENCY_ISSUE));
            }
            chainedCodes.add(broaderCode);
            checkCodeHierarchyLevels(chainedCodes, broaderCode, level + 1);
        }
    }
}
