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

@Repository
@Transactional
public interface CodeRegistryRepository extends CrudRepository<CodeRegistry, String> {

    CodeRegistry findByCodeValueIgnoreCase(final String codeValue);

    CodeRegistry findById(final UUID id);

    Set<CodeRegistry> findAll();

    @Query(value = "SELECT COUNT(cr) FROM coderegistry AS cr WHERE cr.modified >= :modifiedAfter", nativeQuery = true)
    long modifiedAfterCount(@Param("modifiedAfter") final Date modifiedAfter);

    @Query(value = "SELECT COUNT(cr) FROM coderegistry AS cr WHERE cr.created >= :createdAfter", nativeQuery = true)
    long createdAfterCount(@Param("createdAfter") final Date createdAfter);
}
