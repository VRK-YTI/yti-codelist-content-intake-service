package fi.vm.yti.codelist.intake.dao;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;

import fi.vm.yti.codelist.common.dto.ExtensionDTO;
import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.Member;

public interface MemberDao {

    void delete(final Member member);

    void delete(final Set<Member> members);

    void save(final Member member);

    void save(final Set<Member> members);

    void save(final Set<Member> members,
              final boolean logChange);

    Set<Member> findAll();

    Set<Member> findAll(final PageRequest pageRequest);

    Member findById(final UUID id);

    Set<Member> findByCodeId(final UUID id);

    Set<Member> findByRelatedMemberId(final UUID id);

    Set<Member> findByRelatedMemberCode(final Code code);

    Set<Member> findByExtensionId(final UUID id);

    Member findByExtensionAndSequenceId(final  Extension extension,
                                          final Integer sequenceId);

    Set<Member> updateMemberEntityFromDto(final Extension extension,
                                          final MemberDTO memberDto);

    Set<Member> updateMemberEntitiesFromDtos(final Extension extension,
                                             final Set<MemberDTO> memberDtos);

    Integer getNextOrderInSequence(final Extension extension);

    Set<Member> createMissingMembersForAllCodesOfAllCodelistsOfAnExtension(final ExtensionDTO extension);

    int getMemberCount();
}
