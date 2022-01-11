package fi.vm.yti.codelist.intake.dao.impl;

import java.util.*;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.OrganizationDTO;
import fi.vm.yti.codelist.common.model.CodeSchemeListItem;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.configuration.ApplicationConstants;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.dao.ExternalReferenceDao;
import fi.vm.yti.codelist.intake.exception.ErrorConstants;
import fi.vm.yti.codelist.intake.exception.ExistingCodeException;
import fi.vm.yti.codelist.intake.exception.InvalidStatusAtCreationTimeException;
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
import static fi.vm.yti.codelist.intake.parser.impl.AbstractBaseParser.JUPO_REGISTRY;
import static fi.vm.yti.codelist.intake.parser.impl.AbstractBaseParser.validateCodeValue;

@Component
public class CodeSchemeDaoImpl extends AbstractDao implements CodeSchemeDao {

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
        super(languageService);
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
        codeSchemeRepository.saveAll(codeSchemes);
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
            save(codeScheme);
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
                if (updateExternalReferences) {
                    save(codeScheme);
                    updateExternalReferences(codeScheme, codeSchemeDto);
                }
                codeSchemes.add(codeScheme);
            }
        }
        if (!codeSchemes.isEmpty()) {
            save(codeSchemes);
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
    public void updateContentModified(final UUID codeSchemeId) {
        updateContentModified(codeSchemeId, new Date(System.currentTimeMillis()));
    }

    @Transactional
    public void updateContentModified(final UUID codeSchemeId,
                                      final Date timeStamp) {
        codeSchemeRepository.updateContentModified(codeSchemeId, timeStamp);
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

        final CodeScheme existingCodeScheme;
        existingCodeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValueIgnoreCase(codeRegistry, fromCodeScheme.getCodeValue());

        if (existingCodeScheme == null) {
            checkForExistingCodeSchemeInRegistry(codeRegistry, fromCodeScheme);
        } else {
            validateCodeSchemeCodeValueForExistingCodeScheme(fromCodeScheme);
            validateCodeRegistry(existingCodeScheme, codeRegistry);
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
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_CODESCHEME_CODEREGISTRY_DOES_NOT_MATCH));
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
        final Date timeStamp = new Date(System.currentTimeMillis());
        if (!Objects.equals(existingCodeScheme.getStatus(), fromCodeScheme.getStatus())) {
            if (!authorizationManager.isSuperUser() && Status.valueOf(existingCodeScheme.getStatus()).ordinal() >= Status.VALID.ordinal() && Status.valueOf(fromCodeScheme.getStatus()).ordinal() < Status.VALID.ordinal()) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_STATUS_CHANGE_NOT_ALLOWED));
            }
            existingCodeScheme.setStatusModified(timeStamp);
            existingCodeScheme.setStatus(fromCodeScheme.getStatus());
        }
        if (!Objects.equals(existingCodeScheme.getCodeRegistry(), codeRegistry)) {
            existingCodeScheme.setCodeRegistry(codeRegistry);
        }
        existingCodeScheme.setOrganizations(resolveOrganizationsFromDtosOrCodeRegistry(fromCodeScheme.getOrganizations(), codeRegistry));
        final Set<Code> infoDomains = resolveInfoDomainsFromDtos(fromCodeScheme.getInfoDomains());
        if (!Objects.equals(existingCodeScheme.getInfoDomains(), infoDomains)) {
            if (infoDomains != null && !infoDomains.isEmpty()) {
                existingCodeScheme.setInfoDomains(infoDomains);
            } else {
                existingCodeScheme.setInfoDomains(null);
            }
        }
        final Set<Code> languageCodes = resolveLanguageCodesFromDtosAndCodeScheme(fromCodeScheme.getLanguageCodes(), existingCodeScheme.getId());
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
        mapPrefLabel(fromCodeScheme, existingCodeScheme);
        mapDescription(fromCodeScheme, existingCodeScheme);
        mapDefinition(fromCodeScheme, existingCodeScheme);
        mapChangeNote(fromCodeScheme, existingCodeScheme);
        mapFeedbackChannel(fromCodeScheme, existingCodeScheme);
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
        existingCodeScheme.setModified(timeStamp);
        existingCodeScheme.setCumulative(fromCodeScheme.isCumulative());

        return existingCodeScheme;
    }

    private CodeScheme createCodeScheme(final CodeRegistry codeRegistry,
                                        final CodeSchemeDTO fromCodeScheme) {
        final Date timeStamp = new Date(System.currentTimeMillis());
        final CodeScheme codeScheme = new CodeScheme();
        codeScheme.setCodeRegistry(codeRegistry);
        codeScheme.setInfoDomains(resolveInfoDomainsFromDtos(fromCodeScheme.getInfoDomains()));
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
        mapPrefLabel(fromCodeScheme, codeScheme);
        mapDescription(fromCodeScheme, codeScheme);
        mapDefinition(fromCodeScheme, codeScheme);
        mapChangeNote(fromCodeScheme, codeScheme);
        mapFeedbackChannel(fromCodeScheme, codeScheme);
        codeScheme.setVersion(fromCodeScheme.getVersion());
        String status = fromCodeScheme.getStatus();

        // Below, at creation time STATUS value has restrictions - except when setting up the system or running integrations tests (dcat needs to be allowed as VALID for example).
        boolean anyStatusIsFineAtCreationTime = false;
        if (authorizationManager.isSuperUser()) { // super user can always create a new code list with any STATUS he/she wishes.
            anyStatusIsFineAtCreationTime = true;
        } else {
            for (final String initCodeScheme : ApplicationConstants.initializationCodeSchemes()) {
                if (initCodeScheme.equals(fromCodeScheme.getCodeValue())) {
                    anyStatusIsFineAtCreationTime = true;
                    break;
                }
            }
        }

        if (anyStatusIsFineAtCreationTime) {
            codeScheme.setStatus(fromCodeScheme.getStatus());
        } else {
            if (status.equals(Status.DRAFT.toString()) || status.equals(Status.INCOMPLETE.toString())) {
                codeScheme.setStatus(fromCodeScheme.getStatus());
            } else {
                throw new InvalidStatusAtCreationTimeException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ErrorConstants.ERR_MSG_STATUS_NOT_VALID, status));
            }
        }
        codeScheme.setStatusModified(timeStamp);

        codeScheme.setStartDate(fromCodeScheme.getStartDate());
        codeScheme.setEndDate(fromCodeScheme.getEndDate());
        codeScheme.setUri(apiUtils.createCodeSchemeUri(codeRegistry, codeScheme));
        codeScheme.setConceptUriInVocabularies(fromCodeScheme.getConceptUriInVocabularies());
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
        codeScheme.setCumulative(fromCodeScheme.isCumulative());
        return codeScheme;
    }

    private Set<Code> resolveInfoDomainsFromDtos(final Set<CodeDTO> codeDtos) {
        final Set<Code> codes;
        if (codeDtos != null && !codeDtos.isEmpty()) {
            codes = new HashSet<>();
            final CodeRegistry codeRegistry = codeRegistryRepository.findByCodeValueIgnoreCase(JUPO_REGISTRY);
            final CodeScheme codeScheme = codeSchemeRepository.findByCodeRegistryAndCodeValueIgnoreCase(codeRegistry, ApplicationConstants.YTI_DATACLASSIFICATION_INFODOMAIN_CODESCHEME);
            codeDtos.forEach(codeDto -> {
                final Code code = codeRepository.findByCodeSchemeAndCodeValueIgnoreCase(codeScheme, codeDto.getCodeValue());
                if (code != null && code.getHierarchyLevel() == 1) {
                    codes.add(code);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_BAD_INFORMATIONDOMAIN));
                }
            });
        } else {
            codes = null;
        }
        return codes;
    }

    private Set<Code> resolveLanguageCodesFromDtos(final Set<CodeDTO> languageCodeDtos) {
        final Set<Code> languageCodes;
        if (languageCodeDtos != null && !languageCodeDtos.isEmpty()) {
            languageCodes = new HashSet<>();
            languageCodeDtos.forEach(codeDto -> {
                final String languageCodeCodeValue = codeDto.getCodeValue();
                final Code code = languageService.getLanguageCode(languageCodeCodeValue);
                if (code != null) {
                    languageCodes.add(code);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_BAD_LANGUAGECODE, languageCodeCodeValue));
                }
            });
        } else {
            languageCodes = null;
        }
        return languageCodes;

    }

    private Set<Code> resolveLanguageCodesFromDtosAndCodeScheme(final Set<CodeDTO> languageCodeDtos,
                                                                final UUID codeSchemmeId) {
        final Set<Code> inputLanguageCodes = resolveLanguageCodesFromDtos(languageCodeDtos);
        final Set<Code> languageCodes;
        if (inputLanguageCodes != null) {
            languageCodes = inputLanguageCodes;
        } else {
            languageCodes = new HashSet<>();
        }
        if (codeSchemmeId != null) {
            final Set<String> usedLanguages = codeSchemeRepository.getUsedLanguagesInContent(codeSchemmeId);
            usedLanguages.forEach(language -> {
                final Code code = languageService.getLanguageCode(language);
                languageCodes.add(code);
            });
        }
        if (languageCodes.isEmpty()) {
            return null;
        }
        return languageCodes;
    }

    private void validateCodeSchemeCodeValueForExistingCodeScheme(final CodeSchemeDTO codeScheme) {
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

    private void mapPrefLabel(final CodeSchemeDTO fromCodeScheme,
                              final CodeScheme codeScheme) {

        final Map<String, String> prefLabel = validateAndAppendLanguagesForCodeScheme(fromCodeScheme.getPrefLabel(), codeScheme);
        codeScheme.setPrefLabel(prefLabel);
    }

    private void mapDefinition(final CodeSchemeDTO fromCodeScheme,
                               final CodeScheme codeScheme) {
        final Map<String, String> definition = validateAndAppendLanguagesForCodeScheme(fromCodeScheme.getDefinition(), codeScheme);
        codeScheme.setDefinition(definition);
    }

    private void mapDescription(final CodeSchemeDTO fromCodeScheme,
                                final CodeScheme codeScheme) {
        final Map<String, String> description = validateAndAppendLanguagesForCodeScheme(fromCodeScheme.getDescription(), codeScheme);
        codeScheme.setDescription(description);
    }

    private void mapChangeNote(final CodeSchemeDTO fromCodeScheme,
                               final CodeScheme codeScheme) {
        final Map<String, String> changeNote = validateAndAppendLanguagesForCodeScheme(fromCodeScheme.getChangeNote(), codeScheme);
        codeScheme.setChangeNote(changeNote);
    }

    private void mapFeedbackChannel(final CodeSchemeDTO fromCodeScheme,
                                    final CodeScheme codeScheme) {
        final Map<String, String> feedbackChannel = validateAndAppendLanguagesForCodeScheme(fromCodeScheme.getFeedbackChannel(), codeScheme);
        codeScheme.setFeedbackChannel(feedbackChannel);
    }
}


