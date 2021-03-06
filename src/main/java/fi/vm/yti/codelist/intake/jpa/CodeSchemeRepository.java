package fi.vm.yti.codelist.intake.jpa;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;

@Repository
@Transactional
public interface CodeSchemeRepository extends CrudRepository<CodeScheme, String> {

    Set<CodeScheme> findByCodeRegistryCodeValueIgnoreCase(final String codeRegistryCodeValue);

    CodeScheme findByCodeRegistryAndCodeValueIgnoreCase(final CodeRegistry codeRegistry,
                                                        final String codeValue);

    CodeScheme findByUriIgnoreCase(final String uri);

    CodeScheme findById(final UUID id);

    Set<CodeScheme> findByCodeRegistry(final CodeRegistry codeRegistry);

    Set<CodeScheme> findAll();

    @Query("select cs from CodeScheme cs " +
        "left join fetch cs.codes cod left join fetch cod.externalReferences er left join fetch cod.members ext " +
        "left join fetch cs.infoDomains ids left join fetch ids.externalReferences idsExtRef left join fetch ids.members idsExt " +
        "left join fetch cs.codeRegistry cr left join fetch cr.organizations org " +
        "where cs.id = ?1")
    CodeScheme findCodeSchemeAndEagerFetchTheChildren(final UUID id);

    @Query(value = "SELECT COUNT(cs) FROM codescheme AS cs WHERE cs.modified >= :modifiedAfter", nativeQuery = true)
    long modifiedAfterCount(@Param("modifiedAfter") final Date modifiedAfter);

    @Query(value = "SELECT COUNT(cs) FROM codescheme AS cs WHERE cs.created >= :createdAfter", nativeQuery = true)
    long createdAfterCount(@Param("createdAfter") final Date createdAfter);

    @Modifying
    @Query(value = "UPDATE CodeScheme AS cs SET cs.contentModified = :timeStamp WHERE cs.id = :codeSchemeId")
    int updateContentModified(@Param("codeSchemeId") final UUID codeSchemeId,
                              @Param("timeStamp") final Date timeStamp);

    @Query(value = "SELECT DISTINCT language FROM code_preflabel WHERE code_id IN (SELECT id FROM code WHERE codescheme_id = :codeSchemeId) " +
        "UNION SELECT DISTINCT language FROM code_description WHERE code_id IN (SELECT id FROM code WHERE codescheme_id = :codeSchemeId) " +
        "UNION SELECT DISTINCT language FROM code_definition WHERE code_id IN (SELECT id FROM code WHERE codescheme_id = :codeSchemeId) " +
        "UNION SELECT DISTINCT language FROM member_preflabel WHERE member_id IN (SELECT id FROM member WHERE extension_id IN (SELECT id FROM extension where parentcodescheme_id = :codeSchemeId)) " +
        "UNION SELECT DISTINCT language FROM extension_preflabel WHERE extension_id IN (SELECT id from extension WHERE parentcodescheme_id = :codeSchemeId) " +
        "UNION SELECT DISTINCT language FROM externalreference_title WHERE externalreference_id IN (SELECT id FROM externalreference WHERE parentcodescheme_id = :codeSchemeId) " +
        "UNION SELECT DISTINCT language FROM externalreference_description WHERE externalreference_id IN (SELECT id FROM externalreference WHERE parentcodescheme_id = :codeSchemeId)", nativeQuery = true)
    Set<String> getUsedLanguagesInContent(@Param("codeSchemeId") final UUID codeSchemeId);
}
