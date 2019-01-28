package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.Member;

@Repository
@Transactional
public interface MemberRepository extends CrudRepository<Member, String> {

    Set<Member> findAll();

    Set<Member> findByCodeId(final UUID id);

    Set<Member> findByExtensionAndOrder(final Extension extension,
                                        final Integer order);

    @Query(value = "SELECT m.memberorder FROM member as m WHERE m.extension_id = :extensionId ORDER BY m.memberorder DESC LIMIT 1", nativeQuery = true)
    Integer getMemberMaxOrder(@Param("extensionId") final UUID extensionId);

    Set<Member> findByRelatedMemberId(final UUID id);

    Set<Member> findByRelatedMemberCode(final Code id);

    Set<Member> findByExtensionId(final UUID id);

    Member findById(final UUID id);

    Member findByExtensionAndId(final Extension extension,
                                final UUID id);

    Member findByExtensionAndCodeCodeValueIgnoreCase(final Extension extension,
                                                     final String codeCodeValue);

    Member findByExtensionAndCodeUriIgnoreCase(final Extension extension,
                                               final String codeCodeValue);
}
