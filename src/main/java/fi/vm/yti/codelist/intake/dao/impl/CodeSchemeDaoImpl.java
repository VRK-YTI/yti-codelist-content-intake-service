package fi.vm.yti.codelist.intake.dao.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.exception.ExistingCodeException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ErrorModel;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;
import static fi.vm.yti.codelist.intake.parser.AbstractBaseParser.JUPO_REGISTRY;
import static fi.vm.yti.codelist.intake.parser.AbstractBaseParser.YTI_DATACLASSIFICATION_CODESCHEME;

@Component
public class CodeSchemeDaoImpl implements CodeSchemeDao {

    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRepository codeRepository;
    private final AuthorizationManager authorizationManager;
    private final ApiUtils apiUtils;

    @Inject
    public CodeSchemeDaoImpl(final CodeRegistryRepository codeRegistryRepository,
                             final CodeSchemeRepository codeSchemeRepository,
                             final CodeRepository codeRepository,
                             final AuthorizationManager authorizationManager,
                             final ApiUtils apiUtils) {
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
        this.authorizationManager = authorizationManager;
        this.apiUtils = apiUtils;
    }

    public void delete(final CodeScheme codeScheme) {
        codeSchemeRepository.delete(codeScheme);
    }

    public void save(final CodeScheme codeScheme) {
        codeSchemeRepository.save(codeScheme);
    }

    public void save(final Set<CodeScheme> codeSchemes) {
        codeSchemeRepository.save(codeSchemes);
    }

    public CodeScheme findById(final UUID id) {
        return codeSchemeRepository.findById(id);
    }

    public CodeScheme findByCodeRegistryAndCodeValue(final CodeRegistry codeRegistry,
                                                     final String codeValue) {
        return codeSchemeRepository.findByCodeRegistryAndCodeValueIgnoreCase(codeRegistry, codeValue);
    }

    public CodeScheme findByCodeRegistryCodeValueAndCodeValue(final String codeRegistryCodeValue,
                                                              final String codeSchemeCodeValue) {
        return codeSchemeRepository.findByCodeRegistryCodeValueAndCodeValueIgnoreCase(codeRegistryCodeValue, codeSchemeCodeValue);
    }

    public Set<CodeScheme> findAll() {
        return codeSchemeRepository.findAll();
    }

    @Transactional
    public CodeScheme updateCodeSchemeFromDto(final CodeRegistry codeRegistry,
                                              final CodeSchemeDTO codeSchemeDto) {
        CodeScheme codeScheme = null;
        if (codeRegistry != null) {
            codeScheme = createOrUpdateCodeScheme(codeRegistry, codeSchemeDto);
        }
        codeSchemeRepository.save(codeScheme);
        codeRegistryRepository.save(codeRegistry);
        return codeScheme;
    }

    @Transactional
    public Set<CodeScheme> updateCodeSchemesFromDtos(final CodeRegistry codeRegistry,
                                                     final Set<CodeSchemeDTO> codeSchemeDtos) {
        final Set<CodeScheme> codeSchemes = new HashSet<>();
        if (codeRegistry != null) {
            for (final CodeSchemeDTO fromCodeScheme : codeSchemeDtos) {
                final CodeScheme codeScheme = createOrUpdateCodeScheme(codeRegistry, fromCodeScheme);
                if (codeScheme != null) {
                    codeSchemes.add(codeScheme);
                }
            }
        }
        if (!codeSchemes.isEmpty()) {
            codeSchemeRepository.save(codeSchemes);
            codeRegistryRepository.save(codeRegistry);
        }
        return codeSchemes;
    }

    private CodeScheme createOrUpdateCodeScheme(final CodeRegistry codeRegistry,
                                                final CodeSchemeDTO fromCodeScheme) {
        validateCodeSchemeForCodeRegistry(fromCodeScheme);
        final CodeScheme existingCodeScheme;
        if (fromCodeScheme.getId() != null) {
            existingCodeScheme = codeSchemeRepository.findById(fromCodeScheme.getId());
            if (existingCodeScheme == null) {
                checkForExistingCodeSchemeInRegistry(codeRegistry, fromCodeScheme);
            }
        } else {
            existingCodeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValueIgnoreCase(codeRegistry, fromCodeScheme.getCodeValue());
        }
        final CodeScheme codeScheme;
        if (existingCodeScheme != null) {
            codeScheme = updateCodeScheme(codeRegistry, existingCodeScheme, fromCodeScheme);
        } else {
            codeScheme = createCodeScheme(codeRegistry, fromCodeScheme);
        }
        return codeScheme;
    }

    private void checkForExistingCodeSchemeInRegistry(final CodeRegistry codeRegistry,
                                                      final CodeSchemeDTO codeScheme) {
        final CodeScheme existingCodeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValueIgnoreCase(codeRegistry, codeScheme.getCodeValue());
        if (existingCodeScheme != null) {
            throw new ExistingCodeException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(),
                ERR_MSG_USER_ALREADY_EXISTING_CODE_SCHEME, existingCodeScheme.getCodeValue()));
        }
    }

    private CodeScheme updateCodeScheme(final CodeRegistry codeRegistry,
                                        final CodeScheme existingCodeScheme,
                                        final CodeSchemeDTO fromCodeScheme) {
        boolean hasChanges = false;
        if (!Objects.equals(existingCodeScheme.getStatus(), fromCodeScheme.getStatus())) {
            if (!authorizationManager.isSuperUser() && Status.valueOf(existingCodeScheme.getStatus()).ordinal() >= Status.VALID.ordinal() && Status.valueOf(fromCodeScheme.getStatus()).ordinal() < Status.VALID.ordinal()) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_STATUS_CHANGE_NOT_ALLOWED));
            }
            existingCodeScheme.setStatus(fromCodeScheme.getStatus());
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getCodeRegistry(), codeRegistry)) {
            existingCodeScheme.setCodeRegistry(codeRegistry);
            hasChanges = true;
        }
        final Set<Code> classifications = resolveDataClassificationsFromDtos(fromCodeScheme.getDataClassifications());
        if (!Objects.equals(existingCodeScheme.getDataClassifications(), classifications)) {
            if (classifications != null && !classifications.isEmpty()) {
                existingCodeScheme.setDataClassifications(classifications);
            } else {
                existingCodeScheme.setDataClassifications(null);
            }
            hasChanges = true;
        }
        final String uri = apiUtils.createCodeSchemeUri(codeRegistry, existingCodeScheme);
        if (!Objects.equals(existingCodeScheme.getUri(), uri)) {
            existingCodeScheme.setUri(uri);
            hasChanges = true;
        }
        final String url = apiUtils.createCodeSchemeUrl(codeRegistry, existingCodeScheme);
        if (!Objects.equals(existingCodeScheme.getUrl(), url)) {
            existingCodeScheme.setUrl(url);
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getSource(), fromCodeScheme.getSource())) {
            existingCodeScheme.setSource(fromCodeScheme.getSource());
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getLegalBase(), fromCodeScheme.getLegalBase())) {
            existingCodeScheme.setLegalBase(fromCodeScheme.getLegalBase());
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getGovernancePolicy(), fromCodeScheme.getGovernancePolicy())) {
            existingCodeScheme.setGovernancePolicy(fromCodeScheme.getGovernancePolicy());
            hasChanges = true;
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCodeScheme.getPrefLabel(language), value)) {
                existingCodeScheme.setPrefLabel(language, value);
                hasChanges = true;
            }
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getDescription().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCodeScheme.getDescription(language), value)) {
                existingCodeScheme.setDescription(language, value);
                hasChanges = true;
            }
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getDefinition().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCodeScheme.getDefinition(language), value)) {
                existingCodeScheme.setDefinition(language, value);
                hasChanges = true;
            }
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getChangeNote().entrySet()) {
            final String language = entry.getKey();
            final String value = entry.getValue();
            if (!Objects.equals(existingCodeScheme.getChangeNote(language), value)) {
                existingCodeScheme.setChangeNote(language, value);
                hasChanges = true;
            }
        }
        if (!Objects.equals(existingCodeScheme.getVersion(), fromCodeScheme.getVersion())) {
            existingCodeScheme.setVersion(fromCodeScheme.getVersion());
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getStartDate(), fromCodeScheme.getStartDate())) {
            existingCodeScheme.setStartDate(fromCodeScheme.getStartDate());
            hasChanges = true;
        }
        if (!Objects.equals(existingCodeScheme.getEndDate(), fromCodeScheme.getEndDate())) {
            existingCodeScheme.setEndDate(fromCodeScheme.getEndDate());
            hasChanges = true;
        }
        if (hasChanges) {
            final Date timeStamp = new Date(System.currentTimeMillis());
            existingCodeScheme.setModified(timeStamp);
        }
        return existingCodeScheme;
    }

    private CodeScheme createCodeScheme(final CodeRegistry codeRegistry,
                                        final CodeSchemeDTO fromCodeScheme) {
        final CodeScheme codeScheme = new CodeScheme();
        codeScheme.setCodeRegistry(codeRegistry);
        codeScheme.setDataClassifications(resolveDataClassificationsFromDtos(fromCodeScheme.getDataClassifications()));
        if (fromCodeScheme.getId() != null) {
            codeScheme.setId(fromCodeScheme.getId());
        } else {
            final UUID uuid = UUID.randomUUID();
            codeScheme.setId(uuid);
        }
        codeScheme.setCodeValue(fromCodeScheme.getCodeValue());
        codeScheme.setSource(fromCodeScheme.getSource());
        codeScheme.setLegalBase(fromCodeScheme.getLegalBase());
        codeScheme.setGovernancePolicy(fromCodeScheme.getGovernancePolicy());
        final Date timeStamp = new Date(System.currentTimeMillis());
        codeScheme.setModified(timeStamp);
        for (final Map.Entry<String, String> entry : fromCodeScheme.getPrefLabel().entrySet()) {
            codeScheme.setPrefLabel(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getDescription().entrySet()) {
            codeScheme.setDescription(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getDefinition().entrySet()) {
            codeScheme.setDefinition(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getChangeNote().entrySet()) {
            codeScheme.setChangeNote(entry.getKey(), entry.getValue());
        }
        codeScheme.setVersion(fromCodeScheme.getVersion());
        codeScheme.setStatus(fromCodeScheme.getStatus());
        codeScheme.setStartDate(fromCodeScheme.getStartDate());
        codeScheme.setEndDate(fromCodeScheme.getEndDate());
        codeScheme.setUri(apiUtils.createCodeSchemeUri(codeRegistry, codeScheme));
        codeScheme.setUrl(apiUtils.createCodeSchemeUrl(codeRegistry, codeScheme));
        return codeScheme;
    }

    private Set<Code> resolveDataClassificationsFromDtos(final Set<CodeDTO> codeDtos) {
        final Set<Code> codes;
        if (codeDtos != null && !codeDtos.isEmpty()) {
            codes = new HashSet<>();
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryCodeValueAndCodeValueIgnoreCase(JUPO_REGISTRY, YTI_DATACLASSIFICATION_CODESCHEME);
            codeDtos.forEach(codeDto -> {
                final Code code = codeRepository.findByCodeSchemeAndCodeValueIgnoreCase(codeScheme, codeDto.getCodeValue());
                if (code != null && code.getHierarchyLevel() == 1) {
                    codes.add(codeRepository.findByCodeSchemeAndCodeValueIgnoreCase(codeScheme, codeDto.getCodeValue()));
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_BAD_CLASSIFICATION));
                }
            });
        } else {
            codes = null;
        }
        return codes;
    }

    private void validateCodeSchemeForCodeRegistry(final CodeSchemeDTO codeScheme) {
        if (codeScheme.getId() != null) {
            final CodeScheme existingCodeScheme = codeSchemeRepository.findById(codeScheme.getId());
            if (existingCodeScheme != null && !existingCodeScheme.getCodeValue().equalsIgnoreCase(codeScheme.getCodeValue())) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_EXISTING_CODE_MISMATCH));
            }
        }
    }
}
