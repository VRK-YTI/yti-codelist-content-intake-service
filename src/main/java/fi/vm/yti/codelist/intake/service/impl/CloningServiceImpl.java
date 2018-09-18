package fi.vm.yti.codelist.intake.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Singleton;
import javax.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.ExternalReferenceDTO;
import fi.vm.yti.codelist.common.model.CodeSchemeListItem;
import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.dao.CodeDao;
import fi.vm.yti.codelist.intake.dao.CodeSchemeDao;
import fi.vm.yti.codelist.intake.dao.ExtensionDao;
import fi.vm.yti.codelist.intake.dao.ExternalReferenceDao;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.Member;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import fi.vm.yti.codelist.intake.service.CloningService;
import fi.vm.yti.codelist.intake.service.CodeSchemeService;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.ERR_MSG_USER_401;

@Singleton
@Service
public class CloningServiceImpl implements CloningService {

    private final CodeSchemeRepository codeSchemeRepository;
    private final CodeSchemeService codeSchemeService;
    private final CodeSchemeDao codeSchemeDao;
    private final CodeDao codeDao;
    private final ExternalReferenceDao externalReferenceDao;
    private final ExtensionDao extensionDao;
    private final AuthorizationManager authorizationManager;
    private final DtoMapperService dtoMapperService;
    private final ApiUtils apiUtils;

    public CloningServiceImpl(final CodeSchemeRepository codeSchemeRepository,
                              final CodeSchemeService codeSchemeService,
                              final CodeSchemeDao codeSchemeDao,
                              final CodeDao codeDao,
                              final ExternalReferenceDao externalReferenceDao,
                              final ExtensionDao extensionDao,
                              final AuthorizationManager authorizationManager,
                              final DtoMapperService dtoMapperService,
                              final ApiUtils apiUtils) {
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeSchemeService = codeSchemeService;
        this.codeSchemeDao = codeSchemeDao;
        this.codeDao = codeDao;
        this.externalReferenceDao = externalReferenceDao;
        this.extensionDao = extensionDao;
        this.authorizationManager = authorizationManager;
        this.dtoMapperService = dtoMapperService;
        this.apiUtils = apiUtils;
    }

    @Transactional
    public CodeSchemeDTO cloneCodeSchemeWithAllThePlumbing(CodeSchemeDTO codeSchemeWithUserChangesFromUi,
                                                           final String codeRegistryCodeValue,
                                                           final String originalCodeSchemeUuid) {

        final CodeScheme originalCodeScheme = findCodeSchemeAndEagerFetchTheChildren(UUID.fromString(originalCodeSchemeUuid));

        if (!authorizationManager.canBeModifiedByUserInOrganization(originalCodeScheme.getOrganizations())) {
            throw new UnauthorizedException(new ErrorModel(HttpStatus.UNAUTHORIZED.value(), ERR_MSG_USER_401));
        }

        codeSchemeWithUserChangesFromUi.setStatus(Status.DRAFT.toString());
        codeSchemeWithUserChangesFromUi.setNextCodeschemeId(null);
        codeSchemeWithUserChangesFromUi.setPrevCodeschemeId(originalCodeScheme.getId());

        codeSchemeWithUserChangesFromUi = codeSchemeService.updateCodeSchemeFromDto(codeRegistryCodeValue, codeSchemeWithUserChangesFromUi);
        codeSchemeWithUserChangesFromUi.setLastCodeschemeId(codeSchemeWithUserChangesFromUi.getId());

        originalCodeScheme.setNextCodeschemeId(codeSchemeWithUserChangesFromUi.getId());
        originalCodeScheme.setLastCodeschemeId(codeSchemeWithUserChangesFromUi.getId());

        codeSchemeDao.save(originalCodeScheme);

        final CodeScheme newCodeScheme = codeSchemeDao.findById(codeSchemeWithUserChangesFromUi.getId());

        final LinkedHashSet<CodeSchemeListItem> versionHistory = new LinkedHashSet<>();

        LinkedHashSet<CodeScheme> previousVersions = new LinkedHashSet<>();
        previousVersions = getPreviousVersions(originalCodeScheme.getId(), previousVersions);
        for (CodeScheme codeScheme : previousVersions) {
            codeScheme.setLastCodeschemeId(codeSchemeWithUserChangesFromUi.getId());
            CodeSchemeListItem olderVersion = new CodeSchemeListItem(codeScheme.getId(), codeScheme.getPrefLabel(), codeScheme.getUri(), codeScheme.getStartDate(), codeScheme.getEndDate(), codeScheme.getStatus());
            versionHistory.add(olderVersion);
        }
        codeSchemeDao.save(previousVersions);

        CodeSchemeListItem newVersionListItem = new CodeSchemeListItem(codeSchemeWithUserChangesFromUi.getId(), codeSchemeWithUserChangesFromUi.getPrefLabel(),
            codeSchemeWithUserChangesFromUi.getUri(), codeSchemeWithUserChangesFromUi.getStartDate(),
            codeSchemeWithUserChangesFromUi.getEndDate(), codeSchemeWithUserChangesFromUi.getStatus());
        codeSchemeWithUserChangesFromUi.setLastCodeschemeId(newCodeScheme.getId());

        LinkedHashSet<CodeSchemeListItem> allVersions = new LinkedHashSet<>();
        allVersions.add(newVersionListItem);
        allVersions.addAll(versionHistory);
        codeSchemeWithUserChangesFromUi.setAllVersions(allVersions);
        final Map<UUID, ExternalReference> externalReferenceMap = handleParentExternalReferences(originalCodeScheme, newCodeScheme);

        handleCodeSchemeExternalReferences(codeSchemeWithUserChangesFromUi,
            originalCodeScheme,
            externalReferenceMap);

        Set<Code> newCodes = handleCodes(codeSchemeWithUserChangesFromUi,
            originalCodeScheme.getCodes(),
            newCodeScheme,
            externalReferenceMap);

        handleExtensionSchemes(codeSchemeWithUserChangesFromUi,
            newCodeScheme,
            originalCodeScheme,
                newCodes);

        return codeSchemeService.updateCodeSchemeFromDto(true, codeRegistryCodeValue, codeSchemeWithUserChangesFromUi);
    }

    @Transactional
    public LinkedHashSet<CodeScheme> getPreviousVersions(final UUID uuid,
                                                         final LinkedHashSet<CodeScheme> result) {
        final CodeScheme prevVersion = codeSchemeDao.findById(uuid);
        if (prevVersion == null) {
            return result;
        } else {
            result.add(prevVersion);
            if (prevVersion.getPrevCodeschemeId() == null) {
                return result;
            } else {
                return getPreviousVersions(prevVersion.getPrevCodeschemeId(), result);
            }
        }
    }

    @Transactional
    protected void handleExtensionSchemes(final CodeSchemeDTO codeSchemeWithUserChangesFromUi,
                                          final CodeScheme newCodeScheme,
                                          final CodeScheme originalCodeScheme,
                                          final Set<Code> newCodes) {
        final Set<Extension> originalExtensions = originalCodeScheme.getExtensions();
        final Set<Extension> clonedExtensions = new HashSet<>();
        for (final Extension origExtSch : originalExtensions) {
            clonedExtensions.add(cloneExtensionScheme(origExtSch, newCodeScheme, newCodes));
        }
        extensionDao.save(clonedExtensions);
        final Set<ExtensionDTO> extensionDTOS = new HashSet<>();
        for (final Extension e : clonedExtensions) {
            ExtensionDTO dto = dtoMapperService.mapExtensionDto(e, true);
            extensionDTOS.add(dto);
        }
        codeSchemeWithUserChangesFromUi.setExtensions(extensionDTOS);
    }

    @Transactional
    protected Extension cloneExtensionScheme(final Extension original,
                                             final CodeScheme newCodeScheme,
                                             final Set<Code> newCodes) {
        final Extension copy = new Extension();
        copy.setId(UUID.randomUUID());
        copy.setEndDate(original.getEndDate());
        copy.setStartDate(original.getStartDate());
        copy.setStatus(Status.DRAFT.toString());
        copy.setParentCodeScheme(newCodeScheme);
        copy.setCodeValue(original.getCodeValue());
        copy.setPropertyType(original.getPropertyType());
        copy.setPrefLabel(original.getPrefLabel());
        copy.setCodeSchemes(original.getCodeSchemes());
        final Set<Member> newMembers = new HashSet<>();
        final Date timeStamp = new Date(System.currentTimeMillis());
        for (final Member orig : original.getMembers()) {
            final Member newMember = new Member();
            getCodeForExtension(newCodes, orig, newMember);
            newMember.setId(UUID.randomUUID());
            newMember.setExtension(copy);
            newMember.setBroaderMember(orig.getBroaderMember());
            newMember.setOrder(orig.getOrder());
            newMember.setMemberValue(orig.getMemberValue());
            newMember.setPrefLabel(orig.getPrefLabel());
            newMember.setCreated(timeStamp);
            newMember.setModified(timeStamp);
            newMembers.add(newMember);
        }
        copy.setCreated(timeStamp);
        copy.setModified(timeStamp);
        copy.setMembers(newMembers);
        return copy;
    }

    private void getCodeForExtension(final Set<Code> newCodes, final Member orig, final Member newMember) {
        String codeValueOfTheOriginalCodeInTheExtension = orig.getCode().getCodeValue();
        Optional<Code> desiredCodeToPopulateIntoTheNewExtension = null;
        desiredCodeToPopulateIntoTheNewExtension =
                newCodes.stream()
                        .filter(code -> code.getCodeValue() != null &&
                                code.getCodeValue().equals(codeValueOfTheOriginalCodeInTheExtension))
                        .findFirst();
        if (desiredCodeToPopulateIntoTheNewExtension.isPresent()) {
            newMember.setCode(desiredCodeToPopulateIntoTheNewExtension.get());
        }
    }

    @Transactional
    public Map<UUID, ExternalReference> handleParentExternalReferences(final CodeScheme originalCodeScheme,
                                                                       final CodeScheme newCodeScheme) {
        final Map<UUID, ExternalReference> externalReferenceMap = new HashMap<>();
        final Set<ExternalReference> externalReferences = externalReferenceDao.findByParentCodeSchemeId(originalCodeScheme.getId());
        externalReferences.forEach(originalExternalReference -> {
            final ExternalReference newExternalReference;
            if (!originalExternalReference.getGlobal()) {
                newExternalReference = cloneExternalReference(originalExternalReference, newCodeScheme);
                externalReferenceDao.save(newExternalReference);
                externalReferenceMap.put(originalExternalReference.getId(), newExternalReference);
            }
        });
        return externalReferenceMap;
    }

    @Transactional
    protected void handleCodeSchemeExternalReferences(final CodeSchemeDTO codeSchemeWithUserChangesFromUi,
                                                      final CodeScheme originalCodeScheme,
                                                      final Map<UUID, ExternalReference> externalReferenceMap) {
        final Set<ExternalReference> originalExternalReferences = originalCodeScheme.getExternalReferences();
        final Set<ExternalReference> newExternalReferences = new HashSet<>();
        originalExternalReferences.forEach(originalExternalReference -> {
            final ExternalReference newExternalReference;
            if (!originalExternalReference.getGlobal()) {
                newExternalReference = externalReferenceMap.get(originalExternalReference.getId());
                newExternalReferences.add(newExternalReference);
            } else {
                newExternalReferences.add(originalExternalReference);
            }
        });
        final Set<ExternalReferenceDTO> extRefDtos = dtoMapperService.mapExternalReferenceDtos(newExternalReferences, true);
        codeSchemeWithUserChangesFromUi.setExternalReferences(extRefDtos);
    }

    @Transactional
    protected Set<Code> handleCodes(final CodeSchemeDTO codeSchemeWithUserChangesFromUi,
                               final Set<Code> originalCodes,
                               final CodeScheme newCodeScheme,
                               final Map<UUID, ExternalReference> externalReferenceMap) {
        final Set<Code> clonedCodes = new HashSet<>();

        final Map<UUID, Code> originalCodesMap = originalCodes.stream().collect(Collectors.toMap(Code::getId,
            code -> code));

        final Map<String, Code> clonedCodesByCodeValueMap = new HashMap<>();

        //needed to match kid to parent in hierarchical codeschemes (mapping is based on codeValue)
        final Map<String, String> childToParentPointerMap = new HashMap<>();

        final Set<CodeDTO> clonedCodeDTOs = new HashSet<>();

        for (final Code code : originalCodes) {
            final Code clonedCode = cloneCode(code, newCodeScheme, externalReferenceMap);
            if (clonedCode.getBroaderCode() != null) {
                childToParentPointerMap.put(clonedCode.getCodeValue(), originalCodesMap.get(clonedCode.getBroaderCode().getId()).getCodeValue());
            }
            clonedCodesByCodeValueMap.put(clonedCode.getCodeValue(), clonedCode);
            clonedCodes.add(clonedCode);
        }

        for (final Code clonedCode : clonedCodes) {
            if (childToParentPointerMap.keySet().contains(clonedCode.getCodeValue())) {
                final String parentCodeValue = childToParentPointerMap.get(clonedCode.getCodeValue());
                final Code parentCode = clonedCodesByCodeValueMap.get(parentCodeValue);
                clonedCode.setBroaderCode(parentCode);
            }
            codeDao.save(clonedCode);
            final CodeDTO clonedCodeDTO = dtoMapperService.mapDeepCodeDto(clonedCode);
            clonedCodeDTOs.add(clonedCodeDTO);
        }

        codeSchemeWithUserChangesFromUi.setCodes(clonedCodeDTOs);
        return clonedCodes;
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
                          final CodeScheme newCodeScheme,
                          final Map<UUID, ExternalReference> externalReferenceMap) {
        final Code copy = new Code();
        copy.setId(UUID.randomUUID());
        copy.setCodeScheme(newCodeScheme);
        copy.setCodeValue(original.getCodeValue());
        copy.setConceptUriInVocabularies(original.getConceptUriInVocabularies());
        copy.setBroaderCode(original.getBroaderCode());
        copy.setHierarchyLevel(original.getHierarchyLevel());
        copy.setDefinition(original.getDefinition());
        copy.setDescription(original.getDescription());
        copy.setOrder(original.getOrder());
        copy.setPrefLabel(original.getPrefLabel());
        copy.setShortName(original.getShortName());
        final Set<ExternalReference> externalReferences = new HashSet<>();
        original.getExternalReferences().forEach(originalExternalReference -> {
            if (!originalExternalReference.getGlobal()) {
                externalReferences.add(externalReferenceMap.get(originalExternalReference.getId()));
            } else {
                externalReferences.add(originalExternalReference);
            }
        });
        copy.setExternalReferences(externalReferences);
        copy.setMembers(original.getMembers());
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
