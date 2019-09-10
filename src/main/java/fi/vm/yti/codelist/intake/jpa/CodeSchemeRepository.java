package fi.vm.yti.codelist.intake.jpa;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
