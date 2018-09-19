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
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.intake.configuration.UriSuomiProperties;
import fi.vm.yti.codelist.intake.dao.CodeDao;
import fi.vm.yti.codelist.intake.dao.MemberDao;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.MemberRepository;
import fi.vm.yti.codelist.intake.language.LanguageService;
import fi.vm.yti.codelist.intake.log.EntityChangeLogger;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.Member;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Component
public class MemberDaoImpl implements MemberDao {

    private static final int MAX_LEVEL = 10;
    private static final String CALCULATION_HIERARCHY = "calculationHierarchy";

    private final EntityChangeLogger entityChangeLogger;
    private final MemberRepository memberRepository;
    private final CodeDao codeDao;
    private final UriSuomiProperties uriSuomiProperties;
    private final LanguageService languageService;

    @Inject
    public MemberDaoImpl(final EntityChangeLogger entityChangeLogger,
                         final MemberRepository memberRepository,
                         final CodeDao codeDao,
                         final UriSuomiProperties uriSuomiProperties,
                         final LanguageService languageService) {
        this.entityChangeLogger = entityChangeLogger;
        this.memberRepository = memberRepository;
        this.codeDao = codeDao;
        this.uriSuomiProperties = uriSuomiProperties;
        this.languageService = languageService;
    }

    public void delete(final Member member) {
        entityChangeLogger.logMemberChange(member);
        memberRepository.delete(member);
    }

    public void delete(final Set<Member> members) {
        members.forEach(entityChangeLogger::logMemberChange);
        memberRepository.delete(members);
    }

    public void save(final Member member) {
        memberRepository.save(member);
        entityChangeLogger.logMemberChange(member);
    }

    public void save(final Set<Member> members) {
        memberRepository.save(members);
        members.forEach(entityChangeLogger::logMemberChange);
    }

    public Set<Member> findAll() {
        return memberRepository.findAll();
    }

    public Member findById(final UUID id) {
        return memberRepository.findById(id);
    }

    public Set<Member> findByCodeId(final UUID id) {
        return memberRepository.findByCodeId(id);
    }

    public Set<Member> findByRelatedMemberId(final UUID id) {
        return memberRepository.findByRelatedMemberId(id);
    }

    public Set<Member> findByExtensionId(final UUID id) {
        return memberRepository.findByExtensionId(id);
    }

    @Transactional
    public Set<Member> updateMemberEntityFromDto(final Extension extension,
                                                 final MemberDTO fromMemberDto) {
        final Set<Member> members = new HashSet<>();
        final Member member = createOrUpdateMember(extension, fromMemberDto, members);
        fromMemberDto.setId(member.getId());
        save(member);
        resolveMemberRelation(extension, member, fromMemberDto);
        members.add(member);
        return members;
    }

    @Transactional
    public Set<Member> updateMemberEntitiesFromDtos(final Extension extension,
                                                    final Set<MemberDTO> memberDtos) {
        final Set<Member> members = new HashSet<>();
        if (memberDtos != null) {
            for (final MemberDTO memberDto : memberDtos) {
                final Member member = createOrUpdateMember(extension, memberDto, members);
                memberDto.setId(member.getId());
                members.add(member);
                save(member);
            }
            resolveMemberRelations(extension, members, memberDtos);
        }
        return members;
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
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_CODE_NOT_FOUND));
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
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
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
        }
        linkedMembers.forEach(this::checkExtensionHierarchyLevels);
        save(linkedMembers);
    }

    private void checkExtensionHierarchyLevels(final Member member) {
        final Set<Member> chainedMembers = new HashSet<>();
        chainedMembers.add(member);
        checkExtensionHierarchyLevels(chainedMembers, member, 1);
    }

    private void checkExtensionHierarchyLevels(final Set<Member> chainedMembers,
                                               final Member member,
                                               final int level) {
        if (level > MAX_LEVEL) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_HIERARCHY_MAXLEVEL_REACHED));
        }
        final Member relatedMember = member.getRelatedMember();
        if (relatedMember != null) {
            if (chainedMembers.contains(relatedMember)) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_CYCLIC_DEPENDENCY_ISSUE));
            }
            chainedMembers.add(relatedMember);
            checkExtensionHierarchyLevels(chainedMembers, relatedMember, level + 1);
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
                validateExtension(existingMember, extension);
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
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }

    private void validateExtension(final Member member,
                                   final Extension extension) {
        if (member.getExtension() != extension) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }

    private Member updateMember(final CodeScheme codeScheme,
                                final Extension extension,
                                final Member existingMember,
                                final MemberDTO fromMember,
                                final Set<Member> members) {
        final String memberValue = fromMember.getMemberValue();
        if (extension.getPropertyType().getLocalName().equalsIgnoreCase(CALCULATION_HIERARCHY)) {
            validateMemberValue(memberValue);
            if (!Objects.equals(existingMember.getMemberValue(), memberValue)) {
                existingMember.setMemberValue(memberValue);
            }
        }
        for (final Map.Entry<String, String> entry : fromMember.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguage(codeScheme, language);
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
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
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
        final String memberValue = fromMember.getMemberValue();
        if (extension.getPropertyType().getLocalName().equalsIgnoreCase(CALCULATION_HIERARCHY)) {
            validateMemberValue(memberValue);
            member.setMemberValue(memberValue);
        }
        for (final Map.Entry<String, String> entry : fromMember.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguage(codeScheme, language);
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
        return member;
    }

    private void validateMemberValue(final String memberValue) {
        if (memberValue == null || memberValue.isEmpty()) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBERVALUE_NOT_SET));
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
