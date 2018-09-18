package fi.vm.yti.codelist.intake.dao;

import java.util.Set;
import java.util.UUID;

import fi.vm.yti.codelist.common.dto.MemberDTO;
import fi.vm.yti.codelist.intake.model.Member;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;

public interface MemberDao {

    void delete(final Member member);

    void delete(final Set<Member> members);

    void save(final Member member);

    void save(final Set<Member> members);

    Set<Member> findAll();

    Member findById(final UUID id);

    Set<Member> findByCodeId(final UUID id);

    Set<Member> findByBroaderMemberId(final UUID id);

    Set<Member> findByExtensionSchemeId(final UUID id);

    Set<Member> updateMemberEntityFromDto(final ExtensionScheme extensionScheme,
                                          final MemberDTO memberDto);

    Set<Member> updateMemberEntitiesFromDtos(final ExtensionScheme extensionScheme,
                                             final Set<MemberDTO> memberDtos);
}
