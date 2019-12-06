package fi.vm.yti.codelist.intake.jpa;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.Member;

@Repository
@Transactional
public interface MemberRepository extends CrudRepository<Member, String> {

    Set<Member> findAll();

    Page<Member> findAll(final Pageable pageable);

    Set<Member> findByCodeId(final UUID id);

    @Query(value = "SELECT m.memberorder FROM member as m WHERE m.extension_id = :extensionId ORDER BY m.memberorder DESC LIMIT 1", nativeQuery = true)
    Integer getMemberMaxOrder(@Param("extensionId") final UUID extensionId);

    Set<Member> findByRelatedMemberId(final UUID id);

    Set<Member> findByRelatedMemberCode(final Code id);

    Set<Member> findByExtensionId(final UUID id);

    Member findById(final UUID id);

    @Query(value = "SELECT nextval(:sequenceName)", nativeQuery = true)
    Integer getMemberSequenceId(@Param("sequenceName") final String sequenceName);

    Member findByExtensionAndSequenceId(final Extension extension,
                                        final Integer sequenceId);

    @Query("SELECT COUNT(m) FROM Member as m")
    int getMemberCount();

    @Query(value = "SELECT COUNT(m) FROM member AS m WHERE m.modified >= :modifiedAfter", nativeQuery = true)
    long modifiedAfterCount(@Param("modifiedAfter") final Date modifiedAfter);

    @Query(value = "SELECT COUNT(m) FROM member AS m WHERE m.created >= :createdAfter", nativeQuery = true)
    long createdAfterCount(@Param("createdAfter") final Date createdAfter);
}
