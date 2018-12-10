package fi.vm.yti.codelist.intake.dao.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.intake.api.ApiUtils;
import fi.vm.yti.codelist.intake.configuration.UriSuomiProperties;
import fi.vm.yti.codelist.intake.dao.CodeDao;
import fi.vm.yti.codelist.intake.dao.MemberDao;
import fi.vm.yti.codelist.intake.dao.MemberValueDao;
import fi.vm.yti.codelist.intake.exception.NotFoundException;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.MemberRepository;
import fi.vm.yti.codelist.intake.language.LanguageService;
import fi.vm.yti.codelist.intake.log.EntityChangeLogger;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.Member;
import fi.vm.yti.codelist.intake.model.MemberValue;
import fi.vm.yti.codelist.intake.model.ValueType;
import static fi.vm.yti.codelist.common.constants.ApiConstants.CODE_EXTENSION;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Component
public class MemberDaoImpl implements MemberDao {

    private static final int MAX_LEVEL = 10;
    private static final int MAX_LEVEL_FOR_CROSS_REFERENCE_LIST = 2;

    private final EntityChangeLogger entityChangeLogger;
    private final MemberRepository memberRepository;
    private final CodeDao codeDao;
    private final UriSuomiProperties uriSuomiProperties;
    private final LanguageService languageService;
    private final MemberValueDao memberValueDao;
    private final ApiUtils apiUtils;

    @Inject
    public MemberDaoImpl(final EntityChangeLogger entityChangeLogger,
                         final MemberRepository memberRepository,
                         final CodeDao codeDao,
                         final UriSuomiProperties uriSuomiProperties,
                         final LanguageService languageService,
                         final MemberValueDao memberValueDao,
                         final ApiUtils apiUtils) {
        this.entityChangeLogger = entityChangeLogger;
        this.memberRepository = memberRepository;
        this.codeDao = codeDao;
        this.uriSuomiProperties = uriSuomiProperties;
        this.languageService = languageService;
        this.memberValueDao = memberValueDao;
        this.apiUtils = apiUtils;
    }

    @Transactional
    public void delete(final Member member) {
        entityChangeLogger.logMemberChange(member);
        memberRepository.delete(member);
    }

    @Transactional
    public void delete(final Set<Member> members) {
        entityChangeLogger.logMemberChanges(members);
        memberRepository.delete(members);
    }

    @Transactional
    public void save(final Member member) {
        memberRepository.save(member);
        entityChangeLogger.logMemberChange(member);
    }

    @Transactional
    public void save(final Set<Member> members,
                     final boolean logChange) {
        memberRepository.save(members);
        if (logChange) {
            entityChangeLogger.logMemberChanges(members);
        }
    }

    @Transactional
    public void save(final Set<Member> members) {
        save(members, true);
    }

    @Transactional
    public Set<Member> findAll() {
        return memberRepository.findAll();
    }

    @Transactional
    public Member findById(final UUID id) {
        return memberRepository.findById(id);
    }

    @Transactional
    public Set<Member> findByCodeId(final UUID id) {
        return memberRepository.findByCodeId(id);
    }

    @Transactional
    public Set<Member> findByRelatedMemberId(final UUID id) {
        return memberRepository.findByRelatedMemberId(id);
    }

    @Transactional
    public Set<Member> findByExtensionId(final UUID id) {
        return memberRepository.findByExtensionId(id);
    }

    @Transactional
    public Set<Member> updateMemberEntityFromDto(final Extension extension,
                                                 final MemberDTO fromMemberDto) {
        final Set<Member> affectedMembers = new HashSet<>();
        final Map<String, Code> codesMap = new HashMap<>();
        final CodeScheme parentCodeScheme = extension.getParentCodeScheme();
        final Set<Code> codes = codeDao.findByCodeSchemeId(parentCodeScheme.getId());
        if (codes != null) {
            codes.forEach(code -> codesMap.put(code.getUri(), code));
        }
        final Set<Member> existingMembers = findByExtensionId(extension.getId());
        final Set<CodeScheme> allowedCodeSchemes = gatherAllowedCodeSchemes(parentCodeScheme, extension);
        final Member member = createOrUpdateMember(extension, existingMembers, codesMap, allowedCodeSchemes, fromMemberDto, affectedMembers);
        existingMembers.add(member);
        fromMemberDto.setId(member.getId());
        updateMemberMemberValues(extension, member, fromMemberDto);
        save(member);
        resolveMemberRelation(extension, existingMembers, member, fromMemberDto);
        affectedMembers.add(member);
        resolveAffectedRelatedMembers(existingMembers, affectedMembers, member.getId());
        return affectedMembers;
    }

    @Transactional
    public Set<Member> updateMemberEntitiesFromDtos(final Extension extension,
                                                    final Set<MemberDTO> memberDtos) {
        final Set<Member> affectedMembers = new HashSet<>();
        final Map<String, Code> codesMap = new HashMap<>();
        final CodeScheme parentCodeScheme = extension.getParentCodeScheme();
        final Set<Code> codes = codeDao.findByCodeSchemeId(parentCodeScheme.getId());
        if (codes != null) {
            codes.forEach(code -> codesMap.put(code.getUri(), code));
        }
        final Set<CodeScheme> allowedCodeSchemes = gatherAllowedCodeSchemes(parentCodeScheme, extension);
        final Set<Member> membersToBeStored = new HashSet<>();
        if (memberDtos != null) {
            final Set<Member> existingMembers = findByExtensionId(extension.getId());
            for (final MemberDTO memberDto : memberDtos) {
                final Member member = createOrUpdateMember(extension, existingMembers, codesMap, allowedCodeSchemes, memberDto, affectedMembers);
                existingMembers.add(member);
                memberDto.setId(member.getId());
                affectedMembers.add(member);
                updateMemberMemberValues(extension, member, memberDto);
                membersToBeStored.add(member);
                resolveAffectedRelatedMembers(existingMembers, affectedMembers, member.getId());
            }
            save(membersToBeStored);
            resolveMemberRelations(extension, existingMembers, affectedMembers, memberDtos);
        }
        return affectedMembers;
    }

    private void resolveAffectedRelatedMembers(final Set<Member> existingMembers,
                                               final Set<Member> affectedMembers,
                                               final UUID memberId) {
        final Set<Member> relatedMembers = new HashSet<>();
        existingMembers.forEach(member -> {
            if (member.getId() == memberId) {
                relatedMembers.add(member);
            }
        });
        affectedMembers.addAll(relatedMembers);
    }

    private void updateMemberMemberValues(final Extension extension,
                                          final Member member,
                                          final MemberDTO fromMemberDto) {
        final Set<MemberValue> memberValues = memberValueDao.updateMemberValueEntitiesFromDtos(member, fromMemberDto.getMemberValues());
        ensureThatRequiredMemberValuesArePresent(extension.getPropertyType().getValueTypes(), memberValues);
        member.setMemberValues(memberValues);
    }

    private void ensureThatRequiredMemberValuesArePresent(final Set<ValueType> valueTypes,
                                                          final Set<MemberValue> memberValues) {
        if (!valueTypes.isEmpty()) {
            final Set<ValueType> requiredValueTypes = valueTypes.stream().filter(ValueType::getRequired).collect(Collectors.toSet());
            if (!requiredValueTypes.isEmpty()) {
                requiredValueTypes.forEach(valueType -> {
                    if (!memberValues.isEmpty()) {
                        if (memberValues.stream().noneMatch(member -> member.getValueType() == valueType)) {
                            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBERVALUE_NOT_SET));
                        }
                    } else {
                        throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBERVALUE_NOT_SET));
                    }
                });
            }
        }
    }

    private UUID getUuidFromString(final String uuid) {
        try {
            return UUID.fromString(uuid);
        } catch (final IllegalArgumentException e) {
            // Ignore exception on purpose and return null here.
            return null;
        }
    }

    private void checkDuplicateCode(final Set<Member> members,
                                    final String identifier) {
        boolean found = false;
        for (final Member member : members) {
            final Code code = member.getCode();
            if (code == null) {
                throw new NotFoundException();
            }
            if ((identifier.startsWith(uriSuomiProperties.getUriSuomiAddress()) && code.getUri().equalsIgnoreCase(identifier)) || code.getCodeValue().equalsIgnoreCase(identifier)) {
                if (found) {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBERS_HAVE_DUPLICATE_CODE_USE_UUID));
                }
                found = true;
            }
        }
    }

    private void linkMemberWithId(final Member member,
                                  final UUID id) {
        final Member relatedMember = findById(id);
        linkMembers(member, relatedMember);
    }

    private void linkMembers(final Member member,
                             final Member relatedMember) {
        if (relatedMember != null) {
            member.setRelatedMember(relatedMember);
        }
    }

    private Set<Member> resolveMemberRelation(final Extension extension,
                                              final Set<Member> existingMembers,
                                              final Member member,
                                              final MemberDTO fromMember) {
        final MemberDTO relatedMember = fromMember.getRelatedMember();
        if (CODE_EXTENSION.equalsIgnoreCase(extension.getPropertyType().getContext()) && relatedMember != null) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_RELATION_NOT_ALLOWED_FOR_CODE_EXTENSION));
        }
        if (relatedMember != null && relatedMember.getId() != null && fromMember.getId() != null && member.getId().equals(relatedMember.getId())) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_RELATION_SET_TO_ITSELF));
        }
        final Set<Member> linkedMembers = new HashSet<>();
        if (relatedMember != null && relatedMember.getId() != null) {
            linkMemberWithId(member, relatedMember.getId());
            linkedMembers.add(member);
        } else if (relatedMember != null && relatedMember.getCode() != null) {
            final String identifier = relatedMember.getCode().getCodeValue();
            final UUID uuid = getUuidFromString(identifier);
            if (uuid != null) {
                linkMemberWithId(member, uuid);
                linkedMembers.add(member);
            }
            for (final Member extensionMember : existingMembers) {
                if ((identifier.startsWith(uriSuomiProperties.getUriSuomiAddress()) && extensionMember.getCode() != null && extensionMember.getCode().getUri().equalsIgnoreCase(identifier)) ||
                    (extensionMember.getCode() != null && extensionMember.getCode().getCodeValue().equalsIgnoreCase(identifier))) {
                    checkDuplicateCode(existingMembers, identifier);
                    linkMembers(member, extensionMember);
                    linkedMembers.add(member);
                }
            }
        } else if (relatedMember == null) {
            member.setRelatedMember(null);
        }
        linkedMembers.forEach(mem -> validateMemberHierarchyLevels(mem, extension));
        return linkedMembers;
    }

    private void validateMemberHierarchyLevels(final Member member,
                                               final Extension extension) {
        final Set<Member> chainedMembers = new HashSet<>();
        chainedMembers.add(member);
        validateMemberHierarchyLevels(chainedMembers, member, 1, extension);
    }

    private void validateMemberHierarchyLevels(final Set<Member> chainedMembers,
                                               final Member member,
                                               final int level,
                                               final Extension extension) {
        if (extension.getPropertyType().getLocalName().equals("crossReferenceList")) {
            if (level > MAX_LEVEL_FOR_CROSS_REFERENCE_LIST) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_HIERARCHY_MAXLEVEL_REACHED));
            }
        } else {
            if (level > MAX_LEVEL) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_HIERARCHY_MAXLEVEL_REACHED));
            }
        }

        final Member relatedMember = member.getRelatedMember();
        if (relatedMember != null) {
            if (chainedMembers.contains(relatedMember)) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_CYCLIC_DEPENDENCY_ISSUE));
            }
            chainedMembers.add(relatedMember);
            validateMemberHierarchyLevels(chainedMembers, relatedMember, level + 1, extension);
        }
    }

    private void resolveMemberRelations(final Extension extension,
                                        final Set<Member> existingMembers,
                                        final Set<Member> members,
                                        final Set<MemberDTO> fromMembers) {
        final Set<Member> linkedMembersToBeStored = new HashSet<>();
        fromMembers.forEach(fromMember -> {
            Member member = null;
            for (final Member mem : members) {
                if (fromMember.getId().equals(mem.getId())) {
                    member = mem;
                }
            }
            if (member != null) {
                final Set<Member> linkedMembers = resolveMemberRelation(extension, existingMembers, member, fromMember);
                if (linkedMembers != null && !linkedMembers.isEmpty()) {
                    linkedMembersToBeStored.addAll(linkedMembers);
                }
            }
        });
        save(linkedMembersToBeStored, false);
    }

    @Transactional
    public Member createOrUpdateMember(final Extension extension,
                                       final Set<Member> existingMembers,
                                       final Map<String, Code> codesMap,
                                       final Set<CodeScheme> allowedCodeSchemes,
                                       final MemberDTO fromMember,
                                       final Set<Member> members) {
        Member existingMember = null;
        if (extension != null) {
            if (fromMember.getId() != null) {
                for (final Member member : existingMembers) {
                    if (member.getId() == fromMember.getId()) {
                        existingMember = member;
                        break;
                    }
                }
                if (existingMember != null) {
                    validateExtension(existingMember, extension);
                }
            } else {
                existingMember = null;
            }
            final Member member;
            if (existingMember != null) {
                member = updateMember(extension.getParentCodeScheme(), codesMap, allowedCodeSchemes, extension, existingMember, fromMember, members);
            } else {
                member = createMember(extension.getParentCodeScheme(), codesMap, allowedCodeSchemes, extension, fromMember, members);
            }
            return member;
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EXTENSION_NOT_FOUND));
        }
    }

    private Member updateMember(final CodeScheme codeScheme,
                                final Map<String, Code> codesMap,
                                final Set<CodeScheme> allowedCodeSchemes,
                                final Extension extension,
                                final Member existingMember,
                                final MemberDTO fromMember,
                                final Set<Member> members) {

        for (final Map.Entry<String, String> entry : fromMember.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguageForCodeScheme(codeScheme, language);
            final String value = entry.getValue();
            if (!Objects.equals(existingMember.getPrefLabel(language), value)) {
                existingMember.setPrefLabel(language, value);
            }
        }
        if (fromMember.getOrder() != null && !Objects.equals(existingMember.getOrder(), fromMember.getOrder())) {
            checkOrderAndShiftExistingMemberOrderIfInUse(extension, fromMember.getOrder(), members);
            existingMember.setOrder(fromMember.getOrder());
        } else if (existingMember.getOrder() == null && fromMember.getOrder() == null) {
            existingMember.setOrder(getNextOrderInSequence(extension));
        }
        if (fromMember.getCode() != null) {
            final Code code = findCodeUsingCodeValueOrUri(codeScheme, codesMap, allowedCodeSchemes, fromMember);
            if (!Objects.equals(existingMember.getCode(), code)) {
                existingMember.setCode(code);
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_CODE_NOT_SET));
        }
        if (!Objects.equals(existingMember.getStartDate(), fromMember.getStartDate())) {
            existingMember.setStartDate(fromMember.getStartDate());
        }
        if (!Objects.equals(existingMember.getEndDate(), fromMember.getEndDate())) {
            existingMember.setEndDate(fromMember.getEndDate());
        }
        existingMember.setModified(new Date(System.currentTimeMillis()));
        return existingMember;
    }

    private Member createMember(final CodeScheme codeScheme,
                                final Map<String, Code> codesMap,
                                final Set<CodeScheme> allowedCodeSchemes,
                                final Extension extension,
                                final MemberDTO fromMember,
                                final Set<Member> members) {
        final Member member = new Member();
        if (fromMember.getId() != null) {
            member.setId(fromMember.getId());
        } else {
            final UUID uuid = UUID.randomUUID();
            member.setId(uuid);
        }
        for (final Map.Entry<String, String> entry : fromMember.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguageForCodeScheme(codeScheme, language);
            member.setPrefLabel(language, entry.getValue());
        }
        if (fromMember.getOrder() != null) {
            checkOrderAndShiftExistingMemberOrderIfInUse(extension, fromMember.getOrder(), members);
            member.setOrder(fromMember.getOrder());
        } else {
            member.setOrder(getNextOrderInSequence(extension));
        }
        if (fromMember.getCode() != null) {
            final Code code = findCodeUsingCodeValueOrUri(codeScheme, codesMap, allowedCodeSchemes, fromMember);
            member.setCode(code);
        }
        member.setStartDate(fromMember.getStartDate());
        member.setEndDate(fromMember.getEndDate());
        member.setExtension(extension);
        final Date timeStamp = new Date(System.currentTimeMillis());
        member.setCreated(timeStamp);
        member.setModified(timeStamp);
        member.setUri(apiUtils.createMemberUri(member));
        return member;
    }

    private void validateExtension(final Member member,
                                   final Extension extension) {
        if (member.getExtension() != extension) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_EXTENSION_DOES_NOT_MATCH));
        }
    }

    private Code findCodeUsingCodeValueOrUri(final CodeScheme parentCodeScheme,
                                             final Map<String, Code> codesMap,
                                             final Set<CodeScheme> allowedCodeSchemes,
                                             final MemberDTO member) {
        final CodeDTO fromCode = member.getCode();
        final Code code;
        final String codeUri = resolveCodeUri(parentCodeScheme, fromCode);
        if (codeUri != null && !codeUri.isEmpty()) {
            if (codesMap.containsKey(codeUri)) {
                code = codesMap.get(codeUri);
            } else {
                code = codeDao.findByUri(codeUri);
            }
            if (code != null) {
                checkThatCodeIsInAllowedCodeScheme(code.getCodeScheme(), allowedCodeSchemes);
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_CODE_NOT_FOUND));
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_CODE_NOT_FOUND));
        }
        return code;
    }

    private String resolveCodeUri(final CodeScheme parentCodeScheme,
                                  final CodeDTO fromCode) {
        final String codeUri;
        if (fromCode.getUri() != null) {
            codeUri = fromCode.getUri();
        } else if (fromCode.getCodeValue() != null && !fromCode.getCodeValue().isEmpty()) {
            codeUri = createCodeUriForCodeScheme(parentCodeScheme, fromCode.getCodeValue());
        } else {
            codeUri = null;
        }
        return codeUri;
    }

    private String createCodeUriForCodeScheme(final CodeScheme codeScheme,
                                              final String codeValue) {
        return codeScheme.getUri() + "/code/" + codeValue;
    }

    private void checkThatCodeIsInAllowedCodeScheme(final CodeScheme codeSchemeForCode,
                                                    final Set<CodeScheme> allowedCodeSchemes) {
        if (!allowedCodeSchemes.contains(codeSchemeForCode)) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_CODE_NOT_ALLOWED));
        }
    }

    private void checkOrderAndShiftExistingMemberOrderIfInUse(final Extension extension,
                                                              final Integer order,
                                                              final Set<Member> members) {
        final Set<Member> membersWithOrder = memberRepository.findByExtensionAndOrder(extension, order);
        if (membersWithOrder != null && !membersWithOrder.isEmpty()) {
            membersWithOrder.forEach(member -> {
                member.setOrder(getNextOrderInSequence(extension));
                save(member);
                members.add(member);
            });
        }
    }

    public Integer getNextOrderInSequence(final Extension extension) {
        final Integer maxOrder = memberRepository.getMemberMaxOrder(extension.getId());
        if (maxOrder == null) {
            return 1;
        } else {
            return maxOrder + 1;
        }
    }

    private Set<CodeScheme> gatherAllowedCodeSchemes(final CodeScheme parentCodeScheme,
                                                     final Extension extension) {
        final Set<CodeScheme> allowedCodeSchemes = new HashSet<>();
        allowedCodeSchemes.add(parentCodeScheme);
        final Set<CodeScheme> extensionCodeSchemes = extension.getCodeSchemes();
        if (extensionCodeSchemes != null && !extensionCodeSchemes.isEmpty()) {
            allowedCodeSchemes.addAll(extension.getCodeSchemes());
        }
        return allowedCodeSchemes;
    }
}
