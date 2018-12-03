package fi.vm.yti.codelist.intake.dao;

import java.util.Set;
import java.util.UUID;

import fi.vm.yti.codelist.common.dto.MemberDTO;
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

    Member findById(final UUID id);

    Set<Member> findByCodeId(final UUID id);

    Set<Member> findByRelatedMemberId(final UUID id);

    Set<Member> findByExtensionId(final UUID id);

    Set<Member> updateMemberEntityFromDto(final Extension extension,
                                          final MemberDTO memberDto);

    Set<Member> updateMemberEntitiesFromDtos(final Extension extension,
                                             final Set<MemberDTO> memberDtos);

    Integer getNextOrderInSequence(final Extension extension);
}
