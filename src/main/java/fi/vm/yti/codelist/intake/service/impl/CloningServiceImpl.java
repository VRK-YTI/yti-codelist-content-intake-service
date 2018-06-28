package fi.vm.yti.codelist.intake.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Singleton;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ExtensionSchemeDTO;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.dao.CodeDao;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.dao.ExtensionSchemeDao;
import fi.vm.yti.codelist.intake.dao.ExternalReferenceDao;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.service.CloningService;
import fi.vm.yti.codelist.intake.service.CodeSchemeService;

@Singleton
@Service
public class CloningServiceImpl extends BaseService implements CloningService {

    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeSchemeService codeSchemeService;
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
                              final ApiUtils apiUtils) {
        super(apiUtils);
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeSchemeService = codeSchemeService;
        this.codeSchemeDao = codeSchemeDao;
        this.codeDao = codeDao;
        this.externalReferenceDao = externalReferenceDao;
        this.extensionSchemeDao = extensionSchemeDao;
    }

    @Transactional
    public CodeSchemeDTO cloneCodeSchemeWithAllThePlumbing(CodeSchemeDTO codeSchemeWithUserChangesFromUi,
                                                           final String codeRegistryCodeValue,
                                                           final String originalCodeSchemeUuid) {

        final CodeScheme originalCodeScheme = findCodeSchemeAndEagerFetchTheChildren(UUID.fromString(originalCodeSchemeUuid));

        codeSchemeWithUserChangesFromUi.setStatus(Status.DRAFT.toString());

        codeSchemeWithUserChangesFromUi = codeSchemeService.updateCodeSchemeFromDto(codeRegistryCodeValue, codeSchemeWithUserChangesFromUi);

        final CodeScheme newCodeScheme = codeSchemeDao.findById(codeSchemeWithUserChangesFromUi.getId());

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
    protected void handleExtensionSchemes(final CodeSchemeDTO codeSchemeWithUserChangesFromUi,
                                          final CodeScheme newCodeScheme,
                                          final CodeScheme originalCodeScheme) {
        final Set<ExtensionScheme> originalExtensionSchemes = originalCodeScheme.getExtensionSchemes();
        final Set<ExtensionScheme> clonedExtensionSchemes = new HashSet<>();
        for (ExtensionScheme origExtSch : originalExtensionSchemes) {
            clonedExtensionSchemes.add(cloneExtensionScheme(origExtSch, newCodeScheme));
        }
        extensionSchemeDao.save(clonedExtensionSchemes);
        final Set<ExtensionSchemeDTO> extensionSchemeDTOS = new HashSet<>();
        for (ExtensionScheme e : clonedExtensionSchemes) {
            ExtensionSchemeDTO dto = mapExtensionSchemeDto(e, true);
            extensionSchemeDTOS.add(dto);
        }
        codeSchemeWithUserChangesFromUi.setExtensionSchemes(extensionSchemeDTOS);
    }

    @Transactional
    protected ExtensionScheme cloneExtensionScheme(final ExtensionScheme original,
                                                   final CodeScheme newCodeScheme) {
        final ExtensionScheme copy = new ExtensionScheme();
        copy.setId(UUID.randomUUID());
        copy.setEndDate(original.getEndDate());
        copy.setStartDate(original.getStartDate());
        copy.setStatus(Status.DRAFT.toString());
        copy.setParentCodeScheme(newCodeScheme);
        copy.setCodeValue(original.getCodeValue());
        copy.setPropertyType(original.getPropertyType());
        copy.setPrefLabel(original.getPrefLabel());
        copy.setCodeSchemes(original.getCodeSchemes());
        final Set<Extension> newExtensions = new HashSet<>();
        for (Extension orig : original.getExtensions()) {
            final Extension newExtension = new Extension();
            newExtension.setCode(orig.getCode());
            newExtension.setId(UUID.randomUUID());
            newExtension.setExtensionScheme(copy);
            newExtension.setExtension(orig.getExtension());
            newExtension.setOrder(orig.getOrder());
            newExtension.setExtensionValue(orig.getExtensionValue());
            newExtensions.add(newExtension);
        }
        final Date timeStamp = new Date(System.currentTimeMillis());
        copy.setCreated(timeStamp);
        copy.setModified(timeStamp);
        copy.setExtensions(newExtensions);
        return copy;
    }

    @Transactional
    protected void handleExternalReferences(final CodeSchemeDTO codeSchemeWithUserChangesFromUi,
                                            final CodeScheme originalCodeScheme,
                                            final CodeScheme newCodeScheme) {
        final Set<ExternalReference> originalExternalReferences = originalCodeScheme.getExternalReferences();
        final Set<ExternalReference> newExternalReferences = new HashSet<>();
        originalExternalReferences.forEach(originalExternalReference -> {
            final ExternalReference newExternalReference;
            if (!originalExternalReference.getGlobal()) {
                newExternalReference = cloneExternalReference(originalExternalReference, newCodeScheme);
                externalReferenceDao.save(newExternalReference);
                newExternalReferences.add(newExternalReference);
            } else {
                newExternalReferences.add(originalExternalReference);
            }
        });
        final Set<ExternalReferenceDTO> extRefDtos = mapExternalReferenceDtos(newExternalReferences, true);
        codeSchemeWithUserChangesFromUi.setExternalReferences(extRefDtos);
    }

    @Transactional
    protected void handleCodes(final CodeSchemeDTO codeSchemeWithUserChangesFromUi,
                               final Set<Code> originalCodesParam,
                               final CodeScheme newCodeScheme) {
        final Set<Code> originalCodes = originalCodesParam;
        final Set<Code> clonedCodes = new HashSet<>();

        final Map<UUID, Code> originalCodesMap = originalCodes.stream().collect(Collectors.toMap(Code::getId,
            code -> code));

        final Map<String, Code> clonedCodesByCodeValueMap = new HashMap<>();

        //needed to match kid to parent in hiearchical codeschemes (mapping is based on codeValue)
        final Map<String, String> childToParentPointerMap = new HashMap<>();

        final Set<CodeDTO> clonedCodeDTOs = new HashSet<>();

        for (final Code code : originalCodes) {
            final Code clonedCode = cloneCode(code, newCodeScheme);
            if (clonedCode.getBroaderCodeId() != null) {
                childToParentPointerMap.put(clonedCode.getCodeValue(), originalCodesMap.get(clonedCode.getBroaderCodeId()).getCodeValue());
            }
            clonedCodesByCodeValueMap.put(clonedCode.getCodeValue(), clonedCode);
            clonedCodes.add(clonedCode);
        }

        for (final Code clonedCode : clonedCodes) {
            if (childToParentPointerMap.keySet().contains(clonedCode.getCodeValue())) {
                String parentCodeValue = childToParentPointerMap.get(clonedCode.getCodeValue());
                Code parentCode = clonedCodesByCodeValueMap.get(parentCodeValue);
                clonedCode.setBroaderCodeId(parentCode.getId());
            }
            codeDao.save(clonedCode);
            final CodeDTO clonedCodeDTO = mapCodeDto(clonedCode, true);
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

    @Transactional
    ExternalReference cloneExternalReference(final ExternalReference original,
                                             final CodeScheme newCodeScheme) {
        final ExternalReference copy = new ExternalReference();
        copy.setParentCodeScheme(newCodeScheme);
        copy.setId(UUID.randomUUID());
        copy.setDescription(original.getDescription());
        copy.setGlobal(original.getGlobal());
        copy.setHref(original.getHref());
        copy.setPropertyType(original.getPropertyType());
        copy.setTitle(original.getTitle());
        copy.setCodes(original.getCodes());
        copy.setCodeSchemes(original.getCodeSchemes());
        final Date timeStamp = new Date(System.currentTimeMillis());
        copy.setCreated(timeStamp);
        copy.setModified(timeStamp);
        return copy;
    }

    @Transactional
    public Code cloneCode(final Code original,
                          final CodeScheme newCodeScheme) {
        final Code copy = new Code();
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
        for (final ExternalReference extRef : copy.getExternalReferences()) {
            if (!extRef.getGlobal()) {
                extRef.setId(null);
            }
        }
        copy.setExtensions(original.getExtensions());
        copy.setStatus(Status.DRAFT.toString());
        copy.setEndDate(original.getEndDate());
        copy.setStartDate(original.getStartDate());
        copy.setUri(apiUtils.createCodeUri(copy));
        final Date timeStamp = new Date(System.currentTimeMillis());
        copy.setCreated(timeStamp);
        copy.setModified(timeStamp);
        return copy;
    }
}
