package fi.vm.yti.codelist.intake.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
import fi.vm.yti.codelist.intake.dao.MemberDao;
import fi.vm.yti.codelist.intake.exception.UnauthorizedException;
import fi.vm.yti.codelist.intake.jpa.CodeSchemeRepository;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.model.Member;
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
    private final MemberDao memberDao;
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
                              final ApiUtils apiUtils,
                              final MemberDao memberDao) {
        this.codeSchemeRepository = codeSchemeRepository;
        this.codeSchemeService = codeSchemeService;
        this.codeSchemeDao = codeSchemeDao;
        this.codeDao = codeDao;
        this.externalReferenceDao = externalReferenceDao;
        this.extensionDao = extensionDao;
        this.authorizationManager = authorizationManager;
        this.dtoMapperService = dtoMapperService;
        this.apiUtils = apiUtils;
        this.memberDao = memberDao;
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

        Set<Extension> clonedExtensions = handleExtensions(newCodeScheme,
            originalCodeScheme,
            newCodes);

        Set<Extension> originalExtensions = new HashSet<>();
        originalCodeScheme.getExtensions().forEach(ext ->
            originalExtensions.add(extensionDao.findById(ext.getId()))
        );
        if (!clonedExtensions.isEmpty()) {
            handleMembers(originalExtensions, newCodes, originalCodeScheme, clonedExtensions);
            final Set<ExtensionDTO> extensionDTOS = new HashSet<>();
            for (final Extension e : clonedExtensions) {
                ExtensionDTO dto = dtoMapperService.mapExtensionDto(e, true);
                extensionDTOS.add(dto);
            }
            codeSchemeWithUserChangesFromUi.setExtensions(extensionDTOS);
        }
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
    protected Set<Extension> handleExtensions(final CodeScheme newCodeScheme,
                                              final CodeScheme originalCodeScheme,
                                              final Set<Code> newCodes) {
        final Set<Extension> originalExtensions = originalCodeScheme.getExtensions();
        final Set<Extension> clonedExtensions = new HashSet<>();
        for (final Extension originalExtension : originalExtensions) {
            clonedExtensions.add(cloneExtension(originalExtension, newCodeScheme, newCodes, originalCodeScheme));
        }
        extensionDao.save(clonedExtensions);
        return clonedExtensions;
    }

    private HashSet<Member> handleMembers(final Set<Extension> originalExtensions,
                                          final Set<Code> newCodes,
                                          final CodeScheme originalCodeScheme,
                                          final Set<Extension> clonedExtensions) {
        HashSet<Member> newMembers = null;
        HashMap<String, Extension> originalToNewExtensionPointerMap = new HashMap<>();

        for (Extension originalExtension : originalExtensions) {
            String key = originalExtension.getCodeValue();
            Extension value = null;
            for (Extension clonedExtension : clonedExtensions) {
                if (clonedExtension.getCodeValue().equals(key)) {
                    value = clonedExtension;
                }
            }
            originalToNewExtensionPointerMap.put(key, value);
        }

        for (Extension extension : originalExtensions) {
            newMembers = new HashSet<>();
            Set<Member> originalMembers = memberDao.findByExtensionId(extension.getId());
            Extension clonedExtension = originalToNewExtensionPointerMap.get(extension.getCodeValue());
            HashMap<UUID, UUID> oldIdToNewIdPointerMap = new HashMap<>();
            HashMap<UUID, UUID> oldIdToOldRelatedMemberIdMap = new HashMap<>();
            HashMap<UUID, Member> newMembersMap = new HashMap<>();

            for (final Member originalMember : originalMembers) {
                Member newMember = new Member();
                newMember = populateMember(newCodes,
                    clonedExtension,
                    new Date(System.currentTimeMillis()),
                    originalMember,
                    newMember,
                    originalCodeScheme);
                newMembers.add(newMember);
                newMembersMap.put(newMember.getId(), newMember);
                oldIdToNewIdPointerMap.put(originalMember.getId(), UUID.randomUUID());
                oldIdToOldRelatedMemberIdMap.put(originalMember.getId(),
                    originalMember.getRelatedMember() != null ?
                        originalMember.getRelatedMember().getId() : null);
            }

            for (Member newMember : newMembers) {
                Member relatedMember = newMembersMap.get(oldIdToOldRelatedMemberIdMap.get(newMember.getId()));
                newMember.setRelatedMember(relatedMember);
                newMember.setId(oldIdToNewIdPointerMap.get(newMember.getId())); //Only at this point new UUIDs to everyone!!
            }

            LinkedHashSet<Member> membersInOrderOfTheirLevelTopLevelFirst = new LinkedHashSet<>();
            Long nrOfItems = (long) newMembers.size();
            Long nrOfItemsProcessed = 0L;
            LinkedHashSet<Member> topLevel = new LinkedHashSet<>();

            for (Member member : newMembers) {
                if (member.getRelatedMember() == null) {
                    topLevel.add(member);
                    nrOfItemsProcessed++;
                }
            }

            membersInOrderOfTheirLevelTopLevelFirst.addAll(topLevel);
            if (nrOfItemsProcessed < nrOfItems) {
                getNextLevelItemsOfMembers(newMembers, nrOfItemsProcessed, nrOfItems, topLevel, membersInOrderOfTheirLevelTopLevelFirst);
            }

            memberDao.save(membersInOrderOfTheirLevelTopLevelFirst);
            clonedExtension.setMembers(membersInOrderOfTheirLevelTopLevelFirst);//order just to avoid referential integrity problems
            extensionDao.save(clonedExtension);
        }
        return newMembers;
    }

    private void getNextLevelItemsOfMembers(Set<Member> newMembers,
                                            Long nrOfItemsProcessed,
                                            Long nrOfItems,
                                            LinkedHashSet<Member> currentLevelParents,
                                            LinkedHashSet<Member> ordered) {
        LinkedHashSet<Member> newParents = new LinkedHashSet<>();

        for (Member member : newMembers) {
            if (currentLevelParents.contains(member.getRelatedMember())) {
                ordered.add(member);
                newParents.add(member);
                nrOfItemsProcessed++;
            }
        }

        if (nrOfItemsProcessed < nrOfItems) {
            getNextLevelItemsOfMembers(newMembers, nrOfItemsProcessed, nrOfItems, newParents, ordered);
        }
    }

    @Transactional
    protected Extension cloneExtension(final Extension original,
                                       final CodeScheme newCodeScheme,
                                       final Set<Code> newCodes,
                                       final CodeScheme originalCodeScheme) {
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
        final Date timeStamp = new Date(System.currentTimeMillis());
        copy.setCreated(timeStamp);
        copy.setModified(timeStamp);
        return copy;
    }

    private Member populateMember(final Set<Code> newCodes,
                                  final Extension extension,
                                  final Date timeStamp,
                                  final Member originalMember,
                                  final Member newMember,
                                  final CodeScheme originalCodeScheme) {
        getCodeForMember(newCodes, originalMember, newMember, originalCodeScheme);
        newMember.setId(originalMember.getId());
        newMember.setExtension(extension);
        newMember.setOrder(originalMember.getOrder());
        newMember.setMemberValue_1(originalMember.getMemberValue_1());
        newMember.setMemberValue_2(originalMember.getMemberValue_2());
        newMember.setMemberValue_3(originalMember.getMemberValue_3());
        newMember.setPrefLabel(originalMember.getPrefLabel());
        newMember.setCreated(timeStamp);
        newMember.setModified(timeStamp);
        return newMember;
    }

    /**
     * If the code of the member we are copying is an inner code, that is, it is pointing to a code from the codescheme
     * we are currently cloning (that is, making a new version of), then the code in the new member must point to the
     * newly created code.
     * <p>
     * But if the code of the member we are copying points to somewhere else, that is perhaps to an older version of the
     * codescheme, or to another codescheme entirely, then in that case we leave the code to point to that original
     * location as is.
     */
    private void getCodeForMember(final Set<Code> newCodes,
                                  final Member orig,
                                  final Member newMember,
                                  final CodeScheme codeSchemeWeAreCloningFrom) {
        Optional<Code> desiredCodeToPopulateIntoTheNewExtension = null;
        if (orig.getCode().getCodeScheme().getId().compareTo(codeSchemeWeAreCloningFrom.getId()) == 0) {
            String codeValueOfTheOriginalCodeInTheExtension = orig.getCode().getCodeValue();
            desiredCodeToPopulateIntoTheNewExtension =
                newCodes.stream()
                    .filter(code -> code.getCodeValue() != null &&
                        code.getCodeValue().equals(codeValueOfTheOriginalCodeInTheExtension))
                    .findFirst();
            if (desiredCodeToPopulateIntoTheNewExtension.isPresent()) {
                newMember.setCode(desiredCodeToPopulateIntoTheNewExtension.get());
            }
        } else {
            newMember.setCode(orig.getCode());
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
        }

        LinkedHashSet<Code> codesInOrderOfTheirLevelTopLevelFirst = new LinkedHashSet<>();
        Long nrOfItems = (long) clonedCodes.size();
        Long nrOfItemsProcessed = 0L;
        LinkedHashSet<Code> topLevel = new LinkedHashSet<>();

        for (Code code : clonedCodes) {
            if (code.getBroaderCode() == null) {
                topLevel.add(code);
                nrOfItemsProcessed++;
            }
        }
        codesInOrderOfTheirLevelTopLevelFirst.addAll(topLevel);
        LinkedHashSet<Code> currentLevelParents = topLevel;
        if (nrOfItemsProcessed < nrOfItems) {
            getNextLevelItemsOfCodes(clonedCodes, nrOfItemsProcessed, nrOfItems, currentLevelParents, codesInOrderOfTheirLevelTopLevelFirst);
        }
        ArrayList<Code> orderedCodes = new ArrayList(codesInOrderOfTheirLevelTopLevelFirst); //ArrayList retains order in forEach
        orderedCodes.forEach( code -> {
            codeDao.save(code);
            final CodeDTO clonedCodeDTO = dtoMapperService.mapDeepCodeDto(code);
            clonedCodeDTOs.add(clonedCodeDTO);
        });

        codeSchemeWithUserChangesFromUi.setCodes(clonedCodeDTOs);
        return clonedCodes;
    }

    private void getNextLevelItemsOfCodes(Set<Code> codes,
                                            Long nrOfItemsProcessed,
                                            Long nrOfItems,
                                            LinkedHashSet<Code> currentLevelParents,
                                            LinkedHashSet<Code> ordered) {
        LinkedHashSet<Code> newParents = new LinkedHashSet<>();

        for (Code code : codes) {
            if (currentLevelParents.contains(code.getBroaderCode())) {
                ordered.add(code);
                newParents.add(code);
                nrOfItemsProcessed++;
            }
        }
        
        if (nrOfItemsProcessed < nrOfItems) {
            getNextLevelItemsOfCodes(codes, nrOfItemsProcessed, nrOfItems, newParents, ordered);
        }
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
