package fi.vm.yti.codelist.intake.service.impl;

import fi.vm.yti.codelist.common.dto.*;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.dao.*;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.jpa.CommitRepository;
import fi.vm.yti.codelist.intake.model.*;
import fi.vm.yti.codelist.intake.service.*;
import org.springframework.stereotype.Service;

import javax.inject.Singleton;
import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Singleton
@Service
public class CloningServiceImpl extends BaseService implements CloningService {

    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeSchemeService  codeSchemeService;
    private final CodeSchemeDao codeSchemeDao;
    private final CodeDao codeDao;
    private final ExternalReferenceDao externalReferenceDao;
    private final ExtensionSchemeDao extensionSchemeDao;

    public CloningServiceImpl(final CodeSchemeRepository codeSchemeRepository,
                              final CodeSchemeService codeSchemeService,
                              final CodeSchemeDao codeSchemeDao,
                              final CodeDao codeDao,
                              final ExternalReferenceDao externalReferenceDao,
                              final ExtensionSchemeDao extensionSchemeDao,
                              final ApiUtils apiUtils,
                              final CommitRepository commitRepository) {
        super(apiUtils, commitRepository);
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeSchemeService = codeSchemeService;
        this.codeSchemeDao = codeSchemeDao;
        this.codeDao = codeDao;
        this.externalReferenceDao = externalReferenceDao;
        this.extensionSchemeDao = extensionSchemeDao;
    }

    @Transactional
    public CodeSchemeDTO cloneCodeSchemeWithAllThePlumbing(CodeSchemeDTO codeSchemeWithUserChangesFromUi, String codeRegistryCodeValue, String originalCodeSchemeUuid) {

        CodeScheme originalCodeScheme = findCodeSchemeAndEagerFetchTheChildren(UUID.fromString(originalCodeSchemeUuid));

        codeSchemeWithUserChangesFromUi.setStatus(Status.DRAFT.toString());

        codeSchemeWithUserChangesFromUi = codeSchemeService.updateCodeSchemeFromDto(codeRegistryCodeValue, codeSchemeWithUserChangesFromUi);

        CodeScheme newCodeScheme = codeSchemeDao.findById(codeSchemeWithUserChangesFromUi.getId());

        handleCodes(codeSchemeWithUserChangesFromUi,
                originalCodeScheme.getCodes(),
                newCodeScheme);

        handleExtensionSchemes(codeSchemeWithUserChangesFromUi,
                newCodeScheme,
                originalCodeScheme);

        handleExternalReferences(codeSchemeWithUserChangesFromUi,
                originalCodeScheme,
                newCodeScheme);

        return codeSchemeService.updateCodeSchemeFromDto(codeRegistryCodeValue, codeSchemeWithUserChangesFromUi);
    }

    @Transactional
    protected void handleExtensionSchemes(CodeSchemeDTO codeSchemeWithUserChangesFromUi, CodeScheme newCodeScheme, CodeScheme originalCodeScheme) {
        Set<ExtensionScheme> originalExtensionSchemes = originalCodeScheme.getExtensionSchemes();
        Set<ExtensionScheme> clonedExtensionSchemes = new HashSet<>();
        for (ExtensionScheme origExtSch : originalExtensionSchemes) {
            clonedExtensionSchemes.add(cloneExtensionScheme(origExtSch, newCodeScheme));
        }
        extensionSchemeDao.save(clonedExtensionSchemes);
        Set<ExtensionSchemeDTO> extensionSchemeDTOS = new HashSet<>();
        for (ExtensionScheme e: clonedExtensionSchemes) {
            ExtensionSchemeDTO dto = mapExtensionSchemeDto(e, true);
            extensionSchemeDTOS.add(dto);
        }

        codeSchemeWithUserChangesFromUi.setExtensionSchemes(extensionSchemeDTOS);
    }

    @Transactional
    protected ExtensionScheme cloneExtensionScheme(ExtensionScheme original, CodeScheme newCodeScheme) {
        ExtensionScheme copy = new ExtensionScheme();
        copy.setId(UUID.randomUUID());
        copy.setEndDate(original.getEndDate());
        copy.setStartDate(original.getStartDate());
        copy.setStatus(Status.DRAFT.toString());
        copy.setParentCodeScheme(newCodeScheme);
        copy.setCodeValue(original.getCodeValue());
        copy.setPropertyType(original.getPropertyType());
        copy.setPrefLabel(original.getPrefLabel());
        copy.setCodeSchemes(original.getCodeSchemes());
        Set<Extension> newExtensions = new HashSet<>();
        for (Extension orig : original.getExtensions()) {
            Extension newExtension = new Extension();
            newExtension.setCode(orig.getCode());
            newExtension.setId(UUID.randomUUID());
            newExtension.setExtensionScheme(copy);
            newExtension.setExtension(orig.getExtension());
            newExtension.setOrder(orig.getOrder());
            newExtension.setExtensionValue(orig.getExtensionValue());
            newExtensions.add(newExtension);
        }
        copy.setExtensions(newExtensions);
        return copy;
    }

    @Transactional
    protected void handleExternalReferences(final CodeSchemeDTO codeSchemeWithUserChangesFromUi, final CodeScheme originalCodeScheme, final CodeScheme newCodeScheme) {
        Set<ExternalReference> originalExternalReferences = originalCodeScheme.getExternalReferences();
        Set<ExternalReference> newExternalReferences = new HashSet<>();
        for (ExternalReference originalExternalReference : originalExternalReferences) {
            ExternalReference newExternalReference = cloneExternalReference(originalExternalReference, newCodeScheme);
            newExternalReferences.add(newExternalReference);
        }
        externalReferenceDao.save(newExternalReferences);
        Set<ExternalReferenceDTO> extRefDtos = mapExternalReferenceDtos(newExternalReferences, true);
        codeSchemeWithUserChangesFromUi.setExternalReferences(extRefDtos);
    }

    @Transactional
    protected void handleCodes(final CodeSchemeDTO codeSchemeWithUserChangesFromUi, final Set<Code> originalCodesThatAreNotExtensions, final CodeScheme newCodeScheme) {
        Set<Code> originalCodes = originalCodesThatAreNotExtensions;
        Set<CodeDTO> clonedCodeDTOs = new HashSet<>();
        for (Code code : originalCodes) {
            Code clonedCode = cloneCode(code, newCodeScheme);
            codeDao.save(clonedCode);
            CodeDTO clonedCodeDTO = mapCodeDto(clonedCode, true);
            clonedCodeDTOs.add(clonedCodeDTO);
        }
        codeSchemeWithUserChangesFromUi.setCodes(clonedCodeDTOs);
    }

    @Transactional
    public CodeScheme findById(final UUID id) {
        return codeSchemeRepository.findById(id);
    }

    @Transactional
    public CodeScheme findCodeSchemeAndEagerFetchTheChildren(final UUID id) {
        return codeSchemeRepository.findCodeSchemeAndEagerFetchTheChildren(id);
    }

    @Transactional ExternalReference cloneExternalReference(ExternalReference original, CodeScheme newCodeScheme) {
        ExternalReference copy = new ExternalReference();
        copy.setParentCodeScheme(newCodeScheme);
        copy.setId(UUID.randomUUID());
        copy.setDescription(original.getDescription());
        copy.setGlobal(original.getGlobal());
        copy.setHref(original.getHref());
        copy.setPropertyType(original.getPropertyType());
        copy.setTitle(original.getTitle());
        copy.setCodes(original.getCodes());
        copy.setCodeSchemes(original.getCodeSchemes());
        return copy;
    }

    @Transactional
    public Code cloneCode(Code original, CodeScheme newCodeScheme) {
        Code copy = new Code();
        copy.setId(UUID.randomUUID());
        copy.setCodeScheme(newCodeScheme);
        copy.setCodeValue(original.getCodeValue());
        copy.setConceptUriInVocabularies(original.getConceptUriInVocabularies());
        copy.setBroaderCodeId(original.getBroaderCodeId());
        copy.setHierarchyLevel(original.getHierarchyLevel());
        copy.setDefinition(original.getDefinition());
        copy.setDescription(original.getDescription());
        copy.setOrder(original.getOrder());
        copy.setPrefLabel(original.getPrefLabel());
        copy.setShortName(original.getShortName());
        copy.setExternalReferences(original.getExternalReferences());
        for (ExternalReference extRef : copy.getExternalReferences()) {
            extRef.setId(null);
        }
        copy.setExtensions(original.getExtensions());
        copy.setStatus(Status.DRAFT.toString());
        copy.setEndDate(original.getEndDate());
        copy.setStartDate(original.getStartDate());
        copy.setUri(apiUtils.createCodeUri(copy));
        return copy;
    }
}
