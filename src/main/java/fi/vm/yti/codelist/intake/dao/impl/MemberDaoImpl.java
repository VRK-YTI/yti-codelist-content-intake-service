package fi.vm.yti.codelist.intake.dao.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.intake.configuration.UriSuomiProperties;
import fi.vm.yti.codelist.intake.dao.CodeDao;
import fi.vm.yti.codelist.intake.dao.ExtensionSchemeDao;
import fi.vm.yti.codelist.intake.dao.MemberDao;
import fi.vm.yti.codelist.intake.exception.YtiCodeListException;
import fi.vm.yti.codelist.intake.jpa.MemberRepository;
import fi.vm.yti.codelist.intake.language.LanguageService;
import fi.vm.yti.codelist.intake.log.EntityChangeLogger;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;
import fi.vm.yti.codelist.intake.model.Member;
import static fi.vm.yti.codelist.intake.exception.ErrorConstants.*;

@Component
public class MemberDaoImpl implements MemberDao {

    private static final Logger LOG = LoggerFactory.getLogger(MemberDaoImpl.class);
    private static final int MAX_LEVEL = 10;
    private static final String CALCULATION_HIERARCHY = "calculationHierarchy";

    private final EntityChangeLogger entityChangeLogger;
    private final MemberRepository memberRepository;
    private final CodeDao codeDao;
    private final ExtensionSchemeDao extensionSchemeDao;
    private final UriSuomiProperties uriSuomiProperties;
    private final LanguageService languageService;

    @Inject
    public MemberDaoImpl(final EntityChangeLogger entityChangeLogger,
                         final MemberRepository memberRepository,
                         final CodeDao codeDao,
                         final ExtensionSchemeDao extensionSchemeDao,
                         final UriSuomiProperties uriSuomiProperties,
                         final LanguageService languageService) {
        this.entityChangeLogger = entityChangeLogger;
        this.memberRepository = memberRepository;
        this.codeDao = codeDao;
        this.extensionSchemeDao = extensionSchemeDao;
        this.uriSuomiProperties = uriSuomiProperties;
        this.languageService = languageService;
    }

    public void delete(final Member member) {
        entityChangeLogger.logExtensionChange(member);
        memberRepository.delete(member);
    }

    public void delete(final Set<Member> members) {
        members.forEach(entityChangeLogger::logExtensionChange);
        memberRepository.delete(members);
    }

    public void save(final Member member) {
        memberRepository.save(member);
        entityChangeLogger.logExtensionChange(member);
    }

    public void save(final Set<Member> members) {
        memberRepository.save(members);
        members.forEach(entityChangeLogger::logExtensionChange);
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

    public Set<Member> findByBroaderMemberId(final UUID id) {
        return memberRepository.findByBroaderMemberId(id);
    }

    public Set<Member> findByExtensionSchemeId(final UUID id) {
        return memberRepository.findByExtensionSchemeId(id);
    }

    @Transactional
    public Set<Member> updateMemberEntityFromDto(final ExtensionScheme extensionScheme,
                                                 final MemberDTO fromMemberDto) {
        final Set<Member> members = new HashSet<>();
        final Member member = createOrUpdateMember(extensionScheme, fromMemberDto, members);
        fromMemberDto.setId(member.getId());
        save(member);
        members.add(member);
        resolveMemberRelation(extensionScheme, member, fromMemberDto);
        return members;
    }

    @Transactional
    public Set<Member> updateMemberEntitiesFromDtos(final ExtensionScheme extensionScheme,
                                                    final Set<MemberDTO> memberDtos) {
        final Set<Member> members = new HashSet<>();
        if (memberDtos != null) {
            for (final MemberDTO memberDto : memberDtos) {
                final Member member = createOrUpdateMember(extensionScheme, memberDto, members);
                memberDto.setId(member.getId());
                members.add(member);
                save(member);
            }
            resolveMemberRelations(extensionScheme, memberDtos);
        }
        return members;
    }

    private UUID getUuidFromString(final String uuid) {
        try {
            return UUID.fromString(uuid);
        } catch (final IllegalArgumentException e) {
            LOG.error("Error parsing UUID from string.", e);
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

    private void linkExtensionWithId(final Member member,
                                     final UUID id) {
        final Member relatedMember = findById(id);
        linkMembers(member, relatedMember);
    }

    private void linkMembers(final Member member,
                             final Member relatedMember) {
        if (relatedMember != null) {
            member.setBroaderMember(relatedMember);
        }
    }

    private void resolveMemberRelation(final ExtensionScheme extensionScheme,
                                       final Member member,
                                       final MemberDTO fromMember) {
        final MemberDTO broaderMember = fromMember.getBroaderMember();
        if (broaderMember != null && broaderMember.getId() != null && fromMember.getId() != null && member.getId().equals(broaderMember.getId())) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        final Set<Member> linkedMembers = new HashSet<>();
        if (broaderMember != null && broaderMember.getId() != null) {
            linkExtensionWithId(member, broaderMember.getId());
            linkedMembers.add(member);
        } else if (broaderMember != null && broaderMember.getCode() != null) {
            final Set<Member> members = findByExtensionSchemeId(extensionScheme.getId());
            final String identifier = broaderMember.getCode().getCodeValue();
            final UUID uuid = getUuidFromString(identifier);
            if (uuid != null) {
                linkExtensionWithId(member, uuid);
                linkedMembers.add(member);
            }
            for (final Member extensionSchemeMember : members) {
                if ((identifier.startsWith(uriSuomiProperties.getUriSuomiAddress()) && extensionSchemeMember.getCode() != null && extensionSchemeMember.getCode().getUri().equalsIgnoreCase(identifier)) ||
                    (extensionSchemeMember.getCode() != null && extensionSchemeMember.getCode().getCodeValue().equalsIgnoreCase(identifier))) {
                    checkDuplicateCode(members, identifier);
                    linkMembers(member, extensionSchemeMember);
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
        final Member relatedMember = member.getBroaderMember();
        if (relatedMember != null) {
            if (chainedMembers.contains(relatedMember)) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_CYCLIC_DEPENDENCY_ISSUE));
            }
            chainedMembers.add(relatedMember);
            checkExtensionHierarchyLevels(chainedMembers, relatedMember, level + 1);
        }
    }

    private void resolveMemberRelations(final ExtensionScheme extensionScheme,
                                        final Set<MemberDTO> fromExtensions) {
        fromExtensions.forEach(fromExtension -> {
            final Member member = findById(fromExtension.getId());
            resolveMemberRelation(extensionScheme, member, fromExtension);
        });
    }

    @Transactional
    public Member createOrUpdateMember(final ExtensionScheme extensionSchemeDto,
                                       final MemberDTO fromExtension,
                                       final Set<Member> members) {
        final Member existingMember;
        final ExtensionScheme extensionScheme = extensionSchemeDao.findById(extensionSchemeDto.getId());
        if (extensionScheme != null) {
            if (fromExtension.getId() != null) {
                existingMember = memberRepository.findByExtensionSchemeAndId(extensionScheme, fromExtension.getId());
                validateExtensionScheme(existingMember, extensionScheme);
            } else {
                existingMember = null;
            }
            final Member member;
            if (existingMember != null) {
                member = updateExtension(extensionScheme.getParentCodeScheme(), extensionScheme, existingMember, fromExtension, members);
            } else {
                member = createExtension(extensionScheme.getParentCodeScheme(), extensionScheme, fromExtension, members);
            }
            return member;
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }

    private void validateExtensionScheme(final Member member,
                                         final ExtensionScheme extensionScheme) {
        if (member.getExtensionScheme() != extensionScheme) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
    }

    private Member updateExtension(final CodeScheme codeScheme,
                                   final ExtensionScheme extensionScheme,
                                   final Member existingMember,
                                   final MemberDTO fromExtension,
                                   final Set<Member> members) {
        final String memberValue = fromExtension.getMemberValue();
        if (extensionScheme.getPropertyType().getLocalName().equalsIgnoreCase(CALCULATION_HIERARCHY)) {
            validateMemberValue(memberValue);
            if (!Objects.equals(existingMember.getMemberValue(), memberValue)) {
                existingMember.setMemberValue(memberValue);
            }
        }
        for (final Map.Entry<String, String> entry : fromExtension.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguage(codeScheme, language);
            final String value = entry.getValue();
            if (!Objects.equals(existingMember.getPrefLabel(language), value)) {
                existingMember.setPrefLabel(language, value);
            }
        }
        if (fromExtension.getOrder() != null && !Objects.equals(existingMember.getOrder(), fromExtension.getOrder())) {
            checkOrderAndShiftExistingExtensionOrderIfInUse(extensionScheme, fromExtension.getOrder(), members);
            existingMember.setOrder(fromExtension.getOrder());
        } else if (existingMember.getOrder() == null && fromExtension.getOrder() == null) {
            existingMember.setOrder(getNextOrderInSequence(extensionScheme));
        }
        setRelatedExtension(fromExtension, existingMember);
        if (fromExtension.getCode() != null) {
            final Code code = findCodeUsingCodeValueOrUri(codeScheme, extensionScheme, fromExtension);
            if (!Objects.equals(existingMember.getCode(), code)) {
                existingMember.setCode(code);
            }
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
        }
        if (!Objects.equals(existingMember.getStartDate(), fromExtension.getStartDate())) {
            existingMember.setStartDate(fromExtension.getStartDate());
        }
        if (!Objects.equals(existingMember.getEndDate(), fromExtension.getEndDate())) {
            existingMember.setEndDate(fromExtension.getEndDate());
        }
        existingMember.setModified(new Date(System.currentTimeMillis()));
        return existingMember;
    }

    private Member createExtension(final CodeScheme codeScheme,
                                   final ExtensionScheme extensionScheme,
                                   final MemberDTO fromExtension,
                                   final Set<Member> members) {
        final Member member = new Member();
        if (fromExtension.getId() != null) {
            member.setId(fromExtension.getId());
        } else {
            final UUID uuid = UUID.randomUUID();
            member.setId(uuid);
        }
        final String memberValue = fromExtension.getMemberValue();
        if (extensionScheme.getPropertyType().getLocalName().equalsIgnoreCase(CALCULATION_HIERARCHY)) {
            validateMemberValue(memberValue);
            member.setMemberValue(memberValue);
        }
        for (final Map.Entry<String, String> entry : fromExtension.getPrefLabel().entrySet()) {
            final String language = entry.getKey();
            languageService.validateInputLanguage(codeScheme, language);
            member.setPrefLabel(language, entry.getValue());
        }
        if (fromExtension.getOrder() != null) {
            checkOrderAndShiftExistingExtensionOrderIfInUse(extensionScheme, fromExtension.getOrder(), members);
            member.setOrder(fromExtension.getOrder());
        } else {
            member.setOrder(getNextOrderInSequence(extensionScheme));
        }
        if (fromExtension.getCode() != null) {
            final Code code = findCodeUsingCodeValueOrUri(codeScheme, extensionScheme, fromExtension);
            member.setCode(code);
        }
        setRelatedExtension(fromExtension, member);
        member.setStartDate(fromExtension.getStartDate());
        member.setEndDate(fromExtension.getEndDate());
        setRelatedExtension(fromExtension, member);
        member.setExtensionScheme(extensionScheme);
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

    private void setRelatedExtension(final MemberDTO fromExtension,
                                     final Member member) {
        if (fromExtension.getBroaderMember() != null && fromExtension.getBroaderMember().getId() != null) {
            final UUID broaderMemberId = fromExtension.getBroaderMember().getId();
            if (broaderMemberId != null && broaderMemberId == fromExtension.getId()) {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
            }
            if (broaderMemberId != null) {
                final Member broaderMember = findById(broaderMemberId);
                if (broaderMember != null) {
                    member.setBroaderMember(member);
                } else {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_406));
                }
            }
        } else {
            member.setBroaderMember(null);
        }
    }

    private Code findCodeUsingCodeValueOrUri(final CodeScheme codeScheme,
                                             final ExtensionScheme extensionScheme,
                                             final MemberDTO extension) {
        final CodeDTO fromCode = extension.getCode();
        final Code code;
        if (fromCode != null && fromCode.getUri() != null && !fromCode.getUri().isEmpty()) {
            code = codeDao.findByUri(fromCode.getUri());
            if (code != null) {
                checkThatCodeIsInAllowedCodeScheme(code.getCodeScheme(), codeScheme, extensionScheme);
            } else {
                throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_CODE_NOT_FOUND));
            }
        } else if (fromCode != null && codeScheme != null && fromCode.getCodeValue() != null && !fromCode.getCodeValue().isEmpty()) {
            code = codeDao.findByCodeSchemeAndCodeValue(codeScheme, extension.getCode().getCodeValue());
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
                                                    final ExtensionScheme extensionScheme) {
        final Set<CodeScheme> codeSchemes = extensionScheme.getCodeSchemes();
        if (codeSchemeForCode == parentCodeScheme || (codeSchemes != null && codeSchemes.contains(codeSchemeForCode))) {
            return;
        } else {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), ERR_MSG_USER_MEMBER_CODE_NOT_ALLOWED));
        }
    }

    private void checkOrderAndShiftExistingExtensionOrderIfInUse(final ExtensionScheme extensionScheme,
                                                                 final Integer order,
                                                                 final Set<Member> members) {
        final Member member = memberRepository.findByExtensionSchemeAndOrder(extensionScheme, order);
        if (member != null) {
            member.setOrder(getNextOrderInSequence(extensionScheme));
            save(member);
            members.add(member);
        }
    }

    private Integer getNextOrderInSequence(final ExtensionScheme extensionScheme) {
        final Integer maxOrder = memberRepository.getMemberMaxOrder(extensionScheme.getId());
        if (maxOrder == null) {
            return 1;
        } else {
            return maxOrder + 1;
        }
    }
}
