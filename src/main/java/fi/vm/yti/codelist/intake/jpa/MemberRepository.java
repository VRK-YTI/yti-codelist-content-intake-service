package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.Member;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;

@Repository
@Transactional
public interface MemberRepository extends CrudRepository<Member, String> {

    Set<Member> findAll();

    Set<Member> findByCodeId(final UUID id);

    Member findByExtensionSchemeAndOrder(final ExtensionScheme extensionScheme,
                                         final Integer order);

    @Query(value = "SELECT m.memberorder FROM member as m WHERE m.extensionscheme_id = :extensionSchemeId ORDER BY m.memberorder DESC LIMIT 1", nativeQuery = true)
    Integer getMemberMaxOrder(@Param("extensionSchemeId") final UUID extensionSchemeId);

    Set<Member> findByBroaderMemberId(final UUID id);

    Set<Member> findByExtensionSchemeId(final UUID id);

    Member findById(final UUID id);

    Member findByExtensionSchemeAndId(final ExtensionScheme extensionScheme,
                                      final UUID id);

    Member findByExtensionSchemeAndCodeCodeValueIgnoreCase(final ExtensionScheme extensionScheme,
                                                           final String codeCodeValue);

    Member findByExtensionSchemeAndCodeUriIgnoreCase(final ExtensionScheme extensionScheme,
                                                     final String codeCodeValue);
}
