package fi.vm.yti.codelist.intake.dao.impl;

import java.util.Date;
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
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Component
public class MemberDaoImpl implements MemberDao {

    private static int MAX_LEVEL = 10;
    private static int MAX_LEVEL_FOR_CROSS_REFERENCE_LIST = 2;

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
        members.forEach(entityChangeLogger::logMemberChange);
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
            members.forEach(entityChangeLogger::logMemberChange);
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
        final Member member = createOrUpdateMember(extension, fromMemberDto, affectedMembers);
        fromMemberDto.setId(member.getId());
        save(member);
        updateMemberMemberValues(extension, member, fromMemberDto);
        resolveMemberRelation(extension, member, fromMemberDto);
        affectedMembers.add(member);
        resolveAffectedRelatedMembers(affectedMembers, member.getId());
        return affectedMembers;
    }

    @Transactional
    public Set<Member> updateMemberEntitiesFromDtos(final Extension extension,
                                                    final Set<MemberDTO> memberDtos) {
        final Set<Member> affectedMembers = new HashSet<>();
        if (memberDtos != null) {
            for (final MemberDTO memberDto : memberDtos) {
                final Member member = createOrUpdateMember(extension, memberDto, affectedMembers);
                memberDto.setId(member.getId());
                affectedMembers.add(member);
                save(member);
                updateMemberMemberValues(extension, member, memberDto);
                resolveAffectedRelatedMembers(affectedMembers, member.getId());
            }
            resolveMemberRelations(extension, affectedMembers, memberDtos);
        }
        return affectedMembers;
    }

    private void resolveAffectedRelatedMembers(final Set<Member> affectedMembers,
                                               final UUID memberId) {
        final Set<Member> relatedMembers = findByRelatedMemberId(memberId);
        affectedMembers.addAll(relatedMembers);
    }

    private void updateMemberMemberValues(final Extension extension,
                                          final Member member,
                                          final MemberDTO fromMemberDto) {
        final Set<MemberValue> memberValues = memberValueDao.updateMemberValueEntitiesFromDtos(member, fromMemberDto.getMemberValues());
        ensureThatRequiredMemberValuesArePresent(extension.getPropertyType().getValueTypes(), memberValues);
        member.setMemberValues(memberValues);
        save(member);
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

    private void resolveMemberRelation(final Extension extension,
                                       final Member member,
                                       final MemberDTO fromMember) {
        final MemberDTO relatedMember = fromMember.getRelatedMember();
        if (relatedMember != null && relatedMember.getId() != null && fromMember.getId() != null && member.getId().equals(relatedMember.getId())) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_RELATION_SET_TO_ITSELF));
        }
        final Set<Member> linkedMembers = new HashSet<>();
        if (relatedMember != null && relatedMember.getId() != null) {
            linkMemberWithId(member, relatedMember.getId());
            linkedMembers.add(member);
        } else if (relatedMember != null && relatedMember.getCode() != null) {
            final Set<Member> members = findByExtensionId(extension.getId());
            final String identifier = relatedMember.getCode().getCodeValue();
            final UUID uuid = getUuidFromString(identifier);
            if (uuid != null) {
                linkMemberWithId(member, uuid);
                linkedMembers.add(member);
            }
            for (final Member extensionMember : members) {
                if ((identifier.startsWith(uriSuomiProperties.getUriSuomiAddress()) && extensionMember.getCode() != null && extensionMember.getCode().getUri().equalsIgnoreCase(identifier)) ||
                    (extensionMember.getCode() != null && extensionMember.getCode().getCodeValue().equalsIgnoreCase(identifier))) {
                    checkDuplicateCode(members, identifier);
                    linkMembers(member, extensionMember);
                    linkedMembers.add(member);
                }
            }
        } else if (relatedMember == null) {
            member.setRelatedMember(null);
        }
        linkedMembers.forEach(m -> checkExtensionHierarchyLevels(m, extension));
        save(linkedMembers);
    }

    private void checkExtensionHierarchyLevels(final Member member, final Extension extension) {
        final Set<Member> chainedMembers = new HashSet<>();
        chainedMembers.add(member);
        checkExtensionHierarchyLevels(chainedMembers, member, 1, extension);
    }

    private void checkExtensionHierarchyLevels(final Set<Member> chainedMembers,
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
            checkExtensionHierarchyLevels(chainedMembers, relatedMember, level + 1, extension);
        }
    }

    private void resolveMemberRelations(final Extension extension,
                                        final Set<Member> members,
                                        final Set<MemberDTO> fromMembers) {
        fromMembers.forEach(fromMember -> {
            Member member = null;
            for (final Member mem : members) {
                if (fromMember.getId().equals(mem.getId())) {
                    member = mem;
                }
            }
            if (member != null) {
                resolveMemberRelation(extension, member, fromMember);
            }
        });
    }

    @Transactional
    public Member createOrUpdateMember(final Extension extension,
                                       final MemberDTO fromMember,
                                       final Set<Member> members) {
        final Member existingMember;
        if (extension != null) {
            if (fromMember.getId() != null) {
                existingMember = memberRepository.findByExtensionAndId(extension, fromMember.getId());
                if (existingMember != null) {
                    validateExtension(existingMember, extension);
                }
            } else {
                existingMember = null;
            }
            final Member member;
            if (existingMember != null) {
                member = updateMember(extension.getParentCodeScheme(), extension, existingMember, fromMember, members);
            } else {
                member = createMember(extension.getParentCodeScheme(), extension, fromMember, members);
            }
            return member;
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_EXTENSION_NOT_FOUND));
        }
    }

    private Member updateMember(final CodeScheme codeScheme,
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
            final Code code = findCodeUsingCodeValueOrUri(codeScheme, extension, fromMember);
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
            final Code code = findCodeUsingCodeValueOrUri(codeScheme, extension, fromMember);
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

    private Code findCodeUsingCodeValueOrUri(final CodeScheme codeScheme,
                                             final Extension extension,
                                             final MemberDTO member) {
        final CodeDTO fromCode = member.getCode();
        final Code code;
        if (fromCode != null && fromCode.getUri() != null && !fromCode.getUri().isEmpty()) {
            code = codeDao.findByUri(fromCode.getUri());
            if (code != null) {
                checkThatCodeIsInAllowedCodeScheme(code.getCodeScheme(), codeScheme, extension);
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_CODE_NOT_FOUND));
            }
        } else if (fromCode != null && codeScheme != null && fromCode.getCodeValue() != null && !fromCode.getCodeValue().isEmpty()) {
            code = codeDao.findByCodeSchemeAndCodeValue(codeScheme, member.getCode().getCodeValue());
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_CODE_NOT_FOUND));
        }
        if (code == null) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_CODE_NOT_FOUND));
        }
        return code;
    }

    private void checkThatCodeIsInAllowedCodeScheme(final CodeScheme codeSchemeForCode,
                                                    final CodeScheme parentCodeScheme,
                                                    final Extension extension) {
        final Set<CodeScheme> codeSchemes = extension.getCodeSchemes();
        if (codeSchemeForCode != parentCodeScheme && (codeSchemes == null || !codeSchemes.contains(codeSchemeForCode))) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_CODE_NOT_ALLOWED));
        }
    }

    private void checkOrderAndShiftExistingMemberOrderIfInUse(final Extension extension,
                                                              final Integer order,
                                                              final Set<Member> members) {
        final Member member = memberRepository.findByExtensionAndOrder(extension, order);
        if (member != null) {
            member.setOrder(getNextOrderInSequence(extension));
            save(member);
            members.add(member);
        }
    }

    private Integer getNextOrderInSequence(final Extension extension) {
        final Integer maxOrder = memberRepository.getMemberMaxOrder(extension.getId());
        if (maxOrder == null) {
            return 1;
        } else {
            return maxOrder + 1;
        }
    }
}
