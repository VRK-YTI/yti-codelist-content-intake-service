package fi.vm.yti.codelist.intake.dao.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.OrganizationDTO;
import fi.vm.yti.codelist.common.model.CodeSchemeListItem;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.dao.ExternalReferenceDao;
import fi.vm.yti.codelist.intake.exception.ExistingCodeException;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.CodeRegistryRepository;
import fi.vm.yti.codelist.intake.jpa.CodeRepository;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.jpa.OrganizationRepository;
import fi.vm.yti.codelist.intake.language.LanguageService;
import fi.vm.yti.codelist.intake.log.EntityChangeLogger;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.model.Organization;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;
import static fi.vm.yti.codelist.intake.parser.impl.AbstractBaseParser.*;

@Component
public class CodeSchemeDaoImpl implements CodeSchemeDao {

    private final EntityChangeLogger entityChangeLogger;
    private final ApiUtils apiUtils;
    private final CodeRegistryRepository codeRegistryRepository;
    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeRepository codeRepository;
    private final AuthorizationManager authorizationManager;
    private final ExternalReferenceDao externalReferenceDao;
    private final LanguageService languageService;
    private final OrganizationRepository organizationRepository;

    @Inject
    public CodeSchemeDaoImpl(final EntityChangeLogger entityChangeLogger,
                             final ApiUtils apiUtils,
                             final CodeRegistryRepository codeRegistryRepository,
                             final CodeSchemeRepository codeSchemeRepository,
                             final CodeRepository codeRepository,
                             final AuthorizationManager authorizationManager,
                             final ExternalReferenceDao externalReferenceDao,
                             final LanguageService languageService,
                             final OrganizationRepository organizationRepository) {
        this.entityChangeLogger = entityChangeLogger;
        this.apiUtils = apiUtils;
        this.codeRegistryRepository = codeRegistryRepository;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeRepository = codeRepository;
        this.authorizationManager = authorizationManager;
        this.externalReferenceDao = externalReferenceDao;
        this.languageService = languageService;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public void delete(final CodeScheme codeScheme) {
        entityChangeLogger.logCodeSchemeChange(codeScheme);
        codeSchemeRepository.delete(codeScheme);
    }

    @Transactional
    public void save(final CodeScheme codeScheme) {
        codeSchemeRepository.save(codeScheme);
        entityChangeLogger.logCodeSchemeChange(codeScheme);
    }

    @Transactional
    public void save(final Set<CodeScheme> codeSchemes,
                     final boolean logChange) {
        codeSchemeRepository.save(codeSchemes);
        if (logChange) {
            codeSchemes.forEach(entityChangeLogger::logCodeSchemeChange);
        }
    }

    @Transactional
    public void save(final Set<CodeScheme> codeSchemes) {
        save(codeSchemes, true);
    }

    @Transactional
    public CodeScheme findById(final UUID id) {
        return codeSchemeRepository.findById(id);
    }

    @Transactional
    public CodeScheme findByUri(final String uri) {
        return codeSchemeRepository.findByUriIgnoreCase(uri);
    }

    public Set<CodeScheme> findByCodeRegistryCodeValue(final String codeRegistryCodeValue) {
        return codeSchemeRepository.findByCodeRegistryCodeValueIgnoreCase(codeRegistryCodeValue);
    }

    @Transactional
    public CodeScheme findByCodeRegistryAndCodeValue(final CodeRegistry codeRegistry,
                                                     final String codeValue) {
        return codeSchemeRepository.findByCodeRegistryAndCodeValueIgnoreCase(codeRegistry, codeValue);
    }

    @Transactional
    public CodeScheme findByCodeRegistryCodeValueAndCodeValue(final String codeRegistryCodeValue,
                                                              final String codeSchemeCodeValue) {
        final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValueIgnoreCase(codeRegistryCodeValue);
        if (codeRegistry != null) {
            return codeSchemeRepository.findByCodeRegistryAndCodeValueIgnoreCase(codeRegistry, codeSchemeCodeValue);
        }
        return null;
    }

    @Transactional
    public Set<CodeScheme> findAll() {
        return codeSchemeRepository.findAll();
    }

    @Transactional
    public CodeScheme updateCodeSchemeFromDto(final CodeRegistry codeRegistry,
                                              final CodeSchemeDTO codeSchemeDto) {
        return updateCodeSchemeFromDto(false, codeRegistry, codeSchemeDto);
    }

    @Transactional
    public CodeScheme updateCodeSchemeFromDto(final boolean isAuthorized,
                                              final CodeRegistry codeRegistry,
                                              final CodeSchemeDTO codeSchemeDto) {
        CodeScheme codeScheme = null;
        if (codeRegistry != null) {
            codeScheme = createOrUpdateCodeScheme(codeRegistry, codeSchemeDto);
            updateExternalReferences(codeScheme, codeSchemeDto);
        }
        save(codeScheme);
        codeRegistryRepository.save(codeRegistry);
        return codeScheme;
    }

    @Transactional
    public Set<CodeScheme> updateCodeSchemesFromDtos(final boolean isAuthorized,
                                                     final CodeRegistry codeRegistry,
                                                     final Set<CodeSchemeDTO> codeSchemeDtos,
                                                     final boolean updateExternalReferences) {
        final Set<CodeScheme> codeSchemes = new HashSet<>();
        if (codeRegistry != null) {
            for (final CodeSchemeDTO codeSchemeDto : codeSchemeDtos) {
                final CodeScheme codeScheme = createOrUpdateCodeScheme(isAuthorized, codeRegistry, codeSchemeDto);
                save(codeScheme);
                if (updateExternalReferences) {
                    updateExternalReferences(codeScheme, codeSchemeDto);
                }
                codeSchemes.add(codeScheme);
                save(codeScheme);
            }
        }
        if (!codeSchemes.isEmpty()) {
            codeRegistryRepository.save(codeRegistry);
        }
        return codeSchemes;
    }

    private void updateExternalReferences(final CodeScheme codeScheme,
                                          final CodeSchemeDTO codeSchemeDto) {
        final Set<ExternalReference> externalReferences = externalReferenceDao.updateExternalReferenceEntitiesFromDtos(codeSchemeDto.getExternalReferences(), codeScheme);
        codeScheme.setExternalReferences(externalReferences);
    }

    @Transactional
    public CodeScheme createOrUpdateCodeScheme(final CodeRegistry codeRegistry,
                                               final CodeSchemeDTO fromCodeScheme) {
        return createOrUpdateCodeScheme(false, codeRegistry, fromCodeScheme);
    }

    @Transactional
    public CodeScheme createOrUpdateCodeScheme(final boolean isAuthorized,
                                               final CodeRegistry codeRegistry,
                                               final CodeSchemeDTO fromCodeScheme) {
        validateCodeSchemeForCodeRegistry(fromCodeScheme);
        final CodeScheme existingCodeScheme;
        if (fromCodeScheme.getId() != null) {
            existingCodeScheme = codeSchemeRepository.findById(fromCodeScheme.getId());
            if (existingCodeScheme == null) {
                checkForExistingCodeSchemeInRegistry(codeRegistry, fromCodeScheme);
            }
            validateCodeRegistry(existingCodeScheme, codeRegistry);
        } else {
            existingCodeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValueIgnoreCase(codeRegistry, fromCodeScheme.getCodeValue());
        }
        final CodeScheme codeScheme;
        if (existingCodeScheme != null) {
            if (!isAuthorized && !authorizationManager.canBeModifiedByUserInOrganization(existingCodeScheme.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            codeScheme = updateCodeScheme(codeRegistry, existingCodeScheme, fromCodeScheme);
        } else {
            if (!isAuthorized && !authorizationManager.canBeModifiedByUserInOrganization(codeRegistry.getOrganizations())) {
                throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
            }
            codeScheme = createCodeScheme(codeRegistry, fromCodeScheme);
        }
        return codeScheme;
    }

    private void validateCodeRegistry(final CodeScheme codeScheme,
                                      final CodeRegistry codeRegistry) {
         if (codeScheme != null && codeScheme.getCodeRegistry() != codeRegistry) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
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
        if (!Objects.equals(existingCodeScheme.getStatus(), fromCodeScheme.getStatus())) {
            if (!authorizationManager.isSuperUser() && Status.valueOf(existingCodeScheme.getStatus()).ordinal() >= Status.VALID.ordinal() && Status.valueOf(fromCodeScheme.getStatus()).ordinal() < Status.VALID.ordinal()) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_STATUS_CHANGE_NOT_ALLOWED));
            }
            existingCodeScheme.setStatus(fromCodeScheme.getStatus());
        }
        if (!Objects.equals(existingCodeScheme.getCodeRegistry(), codeRegistry)) {
            existingCodeScheme.setCodeRegistry(codeRegistry);
        }
        existingCodeScheme.setOrganizations(resolveOrganizationsFromDtosOrCodeRegistry(fromCodeScheme.getOrganizations(), codeRegistry));
        final Set<Code> classifications = resolveDataClassificationsFromDtos(fromCodeScheme.getDataClassifications());
        if (!Objects.equals(existingCodeScheme.getDataClassifications(), classifications)) {
            if (classifications != null && !classifications.isEmpty()) {
                existingCodeScheme.setDataClassifications(classifications);
            } else {
                existingCodeScheme.setDataClassifications(null);
            }
        }
        final Set<Code> languageCodes = resolveLanguageCodesFromDtos(fromCodeScheme.getLanguageCodes());
        existingCodeScheme.setLanguageCodes(languageCodes);
        final String uri = apiUtils.createCodeSchemeUri(codeRegistry, existingCodeScheme);
        if (!Objects.equals(existingCodeScheme.getUri(), uri)) {
            existingCodeScheme.setUri(uri);
        }
        if (!Objects.equals(existingCodeScheme.getSource(), fromCodeScheme.getSource())) {
            existingCodeScheme.setSource(fromCodeScheme.getSource());
        }
        if (!Objects.equals(existingCodeScheme.getLegalBase(), fromCodeScheme.getLegalBase())) {
            existingCodeScheme.setLegalBase(fromCodeScheme.getLegalBase());
        }
        if (!Objects.equals(existingCodeScheme.getGovernancePolicy(), fromCodeScheme.getGovernancePolicy())) {
            existingCodeScheme.setGovernancePolicy(fromCodeScheme.getGovernancePolicy());
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguageForCodeScheme(existingCodeScheme, language, false);
            final String value = entry.getValue();
            if (!Objects.equals(existingCodeScheme.getPrefLabel(language), value)) {
                existingCodeScheme.setPrefLabel(language, value);
            }
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getDescription().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguageForCodeScheme(existingCodeScheme, language, false);
            final String value = entry.getValue();
            if (!Objects.equals(existingCodeScheme.getDescription(language), value)) {
                existingCodeScheme.setDescription(language, value);
            }
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getDefinition().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguageForCodeScheme(existingCodeScheme, language, false);
            final String value = entry.getValue();
            if (!Objects.equals(existingCodeScheme.getDefinition(language), value)) {
                existingCodeScheme.setDefinition(language, value);
            }
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getChangeNote().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguageForCodeScheme(existingCodeScheme, language, false);
            final String value = entry.getValue();
            if (!Objects.equals(existingCodeScheme.getChangeNote(language), value)) {
                existingCodeScheme.setChangeNote(language, value);
            }
        }
        if (!Objects.equals(existingCodeScheme.getVersion(), fromCodeScheme.getVersion())) {
            existingCodeScheme.setVersion(fromCodeScheme.getVersion());
        }
        if (!Objects.equals(existingCodeScheme.getStartDate(), fromCodeScheme.getStartDate())) {
            existingCodeScheme.setStartDate(fromCodeScheme.getStartDate());
        }
        if (!Objects.equals(existingCodeScheme.getEndDate(), fromCodeScheme.getEndDate())) {
            existingCodeScheme.setEndDate(fromCodeScheme.getEndDate());
        }
        if (!Objects.equals(existingCodeScheme.getConceptUriInVocabularies(), fromCodeScheme.getConceptUriInVocabularies())) {
            existingCodeScheme.setConceptUriInVocabularies(fromCodeScheme.getConceptUriInVocabularies());
        }
        if (fromCodeScheme.getDefaultCode() != null && fromCodeScheme.getDefaultCode().getCodeValue() != null) {
            final Code defaultCode = codeRepository.findByCodeSchemeAndCodeValueIgnoreCase(existingCodeScheme, fromCodeScheme.getDefaultCode().getCodeValue());
            if (!Objects.equals(existingCodeScheme.getDefaultCode(), defaultCode)) {
                existingCodeScheme.setDefaultCode(defaultCode);
            }
        } else {
            existingCodeScheme.setDefaultCode(null);
        }

        final LinkedHashSet<CodeScheme> variants = new LinkedHashSet<>();
        for (final CodeSchemeListItem variant : fromCodeScheme.getVariantsOfThisCodeScheme()) {
            CodeScheme codeScheme = this.findById(variant.getId());
            variants.add(codeScheme);
        }
        existingCodeScheme.setVariants(variants);

        final LinkedHashSet<CodeScheme> variantMothers = new LinkedHashSet<>();
        for (final CodeSchemeListItem variantMother : fromCodeScheme.getVariantMothersOfThisCodeScheme()) {
            CodeScheme codeScheme = this.findById(variantMother.getId());
            variantMothers.add(codeScheme);
        }
        existingCodeScheme.setVariantMothers(variantMothers);

        existingCodeScheme.setLastCodeschemeId(fromCodeScheme.getLastCodeschemeId());
        existingCodeScheme.setPrevCodeschemeId(fromCodeScheme.getPrevCodeschemeId());
        existingCodeScheme.setNextCodeschemeId(fromCodeScheme.getNextCodeschemeId());
        existingCodeScheme.setModified(new Date(System.currentTimeMillis()));

        return existingCodeScheme;
    }

    private CodeScheme createCodeScheme(final CodeRegistry codeRegistry,
                                        final CodeSchemeDTO fromCodeScheme) {
        final CodeScheme codeScheme = new CodeScheme();
        codeScheme.setCodeRegistry(codeRegistry);
        codeScheme.setDataClassifications(resolveDataClassificationsFromDtos(fromCodeScheme.getDataClassifications()));
        final Set<Code> languageCodes = resolveLanguageCodesFromDtos(fromCodeScheme.getLanguageCodes());
        codeScheme.setLanguageCodes(languageCodes);
        if (fromCodeScheme.getId() != null) {
            codeScheme.setId(fromCodeScheme.getId());
        } else {
            final UUID uuid = UUID.randomUUID();
            codeScheme.setId(uuid);
        }
        codeScheme.setOrganizations(resolveOrganizationsFromDtosOrCodeRegistry(fromCodeScheme.getOrganizations(), codeRegistry));
        final String codeValue = fromCodeScheme.getCodeValue();
        validateCodeValue(codeValue);
        codeScheme.setCodeValue(codeValue);
        codeScheme.setSource(fromCodeScheme.getSource());
        codeScheme.setLegalBase(fromCodeScheme.getLegalBase());
        codeScheme.setGovernancePolicy(fromCodeScheme.getGovernancePolicy());
        for (final Map.Entry<String, String> entry : fromCodeScheme.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguageForCodeScheme(codeScheme, language, false);
            codeScheme.setPrefLabel(language, entry.getValue());
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getDescription().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguageForCodeScheme(codeScheme, language, false);
            codeScheme.setDescription(language, entry.getValue());
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getDefinition().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguageForCodeScheme(codeScheme, language, false);
            codeScheme.setDefinition(language, entry.getValue());
        }
        for (final Map.Entry<String, String> entry : fromCodeScheme.getChangeNote().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguageForCodeScheme(codeScheme, language, false);
            codeScheme.setChangeNote(language, entry.getValue());
        }
        codeScheme.setVersion(fromCodeScheme.getVersion());
        codeScheme.setStatus(fromCodeScheme.getStatus());
        codeScheme.setStartDate(fromCodeScheme.getStartDate());
        codeScheme.setEndDate(fromCodeScheme.getEndDate());
        codeScheme.setUri(apiUtils.createCodeSchemeUri(codeRegistry, codeScheme));
        codeScheme.setConceptUriInVocabularies(fromCodeScheme.getConceptUriInVocabularies());
        final Date timeStamp = new Date(System.currentTimeMillis());
        codeScheme.setCreated(timeStamp);
        codeScheme.setModified(timeStamp);
        Set<CodeScheme> variants = new LinkedHashSet<>();
        for (CodeSchemeListItem variant : fromCodeScheme.getVariantsOfThisCodeScheme()) {
            CodeScheme variantCodeScheme = this.findById(variant.getId());
            variants.add(variantCodeScheme);
        }
        codeScheme.setVariants(variants);

        Set<CodeScheme> variantMothers = new LinkedHashSet<>();
        for (CodeSchemeListItem variantMother : fromCodeScheme.getVariantMothersOfThisCodeScheme()) {
            CodeScheme variantMotherEntity = this.findById(variantMother.getId());
            variantMothers.add(variantMotherEntity);
        }
        codeScheme.setVariantMothers(variantMothers);

        codeScheme.setNextCodeschemeId(fromCodeScheme.getNextCodeschemeId());
        codeScheme.setPrevCodeschemeId(fromCodeScheme.getPrevCodeschemeId());
        codeScheme.setLastCodeschemeId(fromCodeScheme.getLastCodeschemeId());
        return codeScheme;
    }

    private Set<Code> resolveDataClassificationsFromDtos(final Set<CodeDTO> codeDtos) {
        final Set<Code> codes;
        if (codeDtos != null && !codeDtos.isEmpty()) {
            codes = new HashSet<>();
            final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValueIgnoreCase(JUPO_REGISTRY);
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValueIgnoreCase(codeRegistry, YTI_DATACLASSIFICATION_CODESCHEME);
            codeDtos.forEach(codeDto -> {
                final Code code = codeRepository.findByCodeSchemeAndCodeValueIgnoreCase(codeScheme, codeDto.getCodeValue());
                if (code != null && code.getHierarchyLevel() == 1) {
                    codes.add(code);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_BAD_CLASSIFICATION));
                }
            });
        } else {
            codes = null;
        }
        return codes;
    }

    private Set<Code> resolveLanguageCodesFromDtos(final Set<CodeDTO> codeDtos) {
        final Set<Code> codes;
        if (codeDtos != null && !codeDtos.isEmpty()) {
            codes = new HashSet<>();
            final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValueIgnoreCase(YTI_REGISTRY);
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValueIgnoreCase(codeRegistry, YTI_LANGUAGECODE_CODESCHEME);
            codeDtos.forEach(codeDto -> {
                final Code code = codeRepository.findByCodeSchemeAndCodeValueIgnoreCase(codeScheme, codeDto.getCodeValue());
                if (code != null) {
                    codes.add(code);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_BAD_LANGUAGECODE));
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

    private Set<Organization> resolveOrganizationsFromDtosOrCodeRegistry(final Set<OrganizationDTO> organizationDtos,
                                                                         final CodeRegistry codeRegistry) {
        final Set<Organization> organizations = new HashSet<>();
        if (organizationDtos != null && !organizationDtos.isEmpty()) {
            organizationDtos.forEach(organizationDto -> {
                final Organization organization = organizationRepository.findById(organizationDto.getId());
                if (organization != null) {
                    organizations.add(organization);
                }
            });
        }
        if (organizations.isEmpty()) {
            organizations.addAll(codeRegistry.getOrganizations());
        }
        if (organizations.isEmpty()) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODESCHEME_NO_ORGANIZATION));
        }
        return organizations;
    }
}


