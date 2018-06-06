package fi.vm.yti.codelist.intake.service.impl;

import fi.vm.yti.codelist.common.dto.*;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.dao.*;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.jpa.CommitRepository;
import fi.vm.yti.codelist.intake.model.*;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.*;
import org.springframework.stereotype.Service;

import javax.inject.Singleton;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Singleton
@Service
public class CloningServiceImpl extends BaseService implements CloningService {

    AuthorizationManager authorizationManager;
    CodeSchemeRepository codeSchemeRepository;
    CodeSchemeService  codeSchemeService;
    CodeService codeService;
    CodeSchemeDao codeSchemeDao;
    CodeDao codeDao;
    ExternalReferenceDao externalReferenceDao;
    ExtensionSchemeService extensionSchemeService;
    ExtensionSchemeDao extensionSchemeDao;
    ExtensionService extensionService;
    ExtensionDao extensionDao;

    public CloningServiceImpl(final AuthorizationManager authorizationManager,
                              final CodeSchemeRepository codeSchemeRepository,
                              final CodeSchemeService codeSchemeService,
                              final CodeService codeService,
                              final CodeSchemeDao codeSchemeDao,
                              final CodeDao codeDao,
                              final ExternalReferenceDao externalReferenceDao,
                              final ExtensionSchemeService extensionSchemeService,
                              final ExtensionSchemeDao extensionSchemeDao,
                              final ExtensionService extensionService,
                              final ExtensionDao extensionDao,
                              final ApiUtils apiUtils,
                              final CommitRepository commitRepository) {
        super(apiUtils, commitRepository);
        this.authorizationManager = authorizationManager;
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeSchemeService = codeSchemeService;
        this.codeService = codeService;
        this.codeSchemeDao = codeSchemeDao;
        this.codeDao = codeDao;
        this.externalReferenceDao = externalReferenceDao;
        this.extensionDao = extensionDao;
        this.extensionSchemeDao = extensionSchemeDao;
        this.extensionSchemeService = extensionSchemeService;
        this.extensionService = extensionService;
    }

    @Transactional
    public CodeSchemeDTO cloneCodeSchemeWithAllThePlumbing(CodeSchemeDTO codeSchemeWithUserChangesFromUi, String codeRegistryCodeValue, String originalCodeSchemeUuid) {

        CodeScheme originalCodeScheme = findCodeSchemeAndEagerFetchTheChildren(UUID.fromString(originalCodeSchemeUuid));

        codeSchemeWithUserChangesFromUi.setStatus(Status.DRAFT.toString());

        codeSchemeWithUserChangesFromUi = codeSchemeService.updateCodeSchemeFromDto(codeRegistryCodeValue, codeSchemeWithUserChangesFromUi);

        CodeScheme newCodeScheme = codeSchemeDao.findById(codeSchemeWithUserChangesFromUi.getId());

        Set<Code> codesFromExtensionSchemes = getCodesWhichAreActuallyExtensionsInExtensionSchemes(originalCodeScheme);
        //TODO final cleanup for this class!
        Set<Code> codesWhichDirectlyBelongToTheCodeSchemeToBeCloned = getCodesWhichDirectlyBelongToTheCodeSchemeToBeCloned(originalCodeScheme, codesFromExtensionSchemes);

        handleCodes(codeSchemeWithUserChangesFromUi,
                originalCodeScheme.getCodes(),
                //codesWhichDirectlyBelongToTheCodeSchemeToBeCloned,
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
            Extension e = new Extension();
            e.setCode(orig.getCode());
            e.setId(UUID.randomUUID());
            e.setExtensionScheme(copy);
            e.setExtension(orig.getExtension());
            e.setOrder(orig.getOrder());
            e.setExtensionValue(orig.getExtensionValue());
            newExtensions.add(e);
        }
        copy.setExtensions(newExtensions);
        return copy;
    }

    private Set<Code> getCodesWhichDirectlyBelongToTheCodeSchemeToBeCloned(CodeScheme originalCodeScheme, Set<Code> codesFromExtensionSchemes) {
        Set<Code> result = new HashSet<>();
        result.addAll(originalCodeScheme.getCodes());
        Set<Code> codesMarkedForRemoval = new HashSet<>();
        int nrOfRemovedCodes = 0;
        for (Code code: result) {
            for (Code codeExt: codesFromExtensionSchemes) {
                if (code.getId().compareTo(codeExt.getId()) == 0) {
                    codesMarkedForRemoval.add(code);
                    nrOfRemovedCodes++;
                }
            }
        }
        result.removeAll(codesMarkedForRemoval);
        return result;
    }

    private Set<Code> getCodesWhichAreActuallyExtensionsInExtensionSchemes(CodeScheme originalCodeScheme) {
        Set<Code> result = new HashSet<>();
        Set<ExtensionScheme> extensionSchemes = originalCodeScheme.getExtensionSchemes();
        for (ExtensionScheme extSch : extensionSchemes) {
            for (Extension ext : extSch.getExtensions()) {
                result.add(ext.getCode());
            }
        }
        return result;
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
            Code cc = cloneCode(code, newCodeScheme);
            codeDao.save(cc);
            CodeDTO clonedCodeDTO = mapCodeDto(cc, true);
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

    @Transactional ExternalReference cloneExternalReference(ExternalReference e, CodeScheme newCodeScheme) {
        ExternalReference copy = new ExternalReference();
        copy.setParentCodeScheme(newCodeScheme);
        copy.setId(UUID.randomUUID());
        copy.setDescription(e.getDescription());
        copy.setGlobal(e.getGlobal());
        copy.setHref(e.getHref());
        copy.setPropertyType(e.getPropertyType());
        copy.setTitle(e.getTitle());
        copy.setCodes(e.getCodes());
        copy.setCodeSchemes(e.getCodeSchemes());
        return copy;
    }

    @Transactional
    public Code cloneCode(Code code, CodeScheme newCodeScheme) {
        Code copy = new Code();
        copy.setId(UUID.randomUUID());
        copy.setCodeScheme(newCodeScheme);
        copy.setCodeValue(code.getCodeValue());
        copy.setConceptUriInVocabularies(code.getConceptUriInVocabularies());
        copy.setBroaderCodeId(code.getBroaderCodeId());
        copy.setHierarchyLevel(code.getHierarchyLevel());
        copy.setDefinition(code.getDefinition());
        copy.setDescription(code.getDescription());
        copy.setOrder(code.getOrder());
        copy.setPrefLabel(code.getPrefLabel());
        copy.setShortName(code.getShortName());
        copy.setExternalReferences(code.getExternalReferences());
        for (ExternalReference extRef : copy.getExternalReferences()) {
            extRef.setId(null);
        }
        copy.setExtensions(code.getExtensions());
        copy.setStatus(Status.DRAFT.toString());
        copy.setEndDate(code.getEndDate());
        copy.setStartDate(code.getStartDate());
        copy.setUri(apiUtils.createCodeUri(copy));
        return copy;
    }
}
