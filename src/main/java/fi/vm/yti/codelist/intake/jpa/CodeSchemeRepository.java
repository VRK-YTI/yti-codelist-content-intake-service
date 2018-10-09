package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;

@Repository
@Transactional
public interface CodeSchemeRepository extends CrudRepository<CodeScheme, String> {

    Set<CodeScheme> findByCodeRegistryCodeValueIgnoreCase(final String codeRegistryCodeValue);

    CodeScheme findByCodeRegistryAndCodeValueIgnoreCase(final CodeRegistry codeRegistry, final String codeValue);

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

    @Query("select cs from CodeScheme cs " +
            "where cs.prevCodeschemeId = ?1")
    CodeScheme findByPrevCodeschemeId(final UUID codeschemeId);
}
