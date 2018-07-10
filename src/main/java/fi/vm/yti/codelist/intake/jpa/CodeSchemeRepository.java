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
            "left join fetch cs.codes cod left join fetch cod.externalReferences er left join fetch cod.extensions ext " +
            "left join fetch cs.dataClassifications clas left join fetch clas.externalReferences clasExtRef left join fetch clas.extensions clasExt " +
            "left join fetch cs.codeRegistry cr left join fetch cr.organizations org " +
            "where cs.id = ?1")
    CodeScheme findCodeSchemeAndEagerFetchTheChildren(final UUID id);

    @Query("select cs from CodeScheme cs " +
            "where cs.variantCodeschemeId = ?1")
    Set<CodeScheme> findAllVariantsFromTheSameMother(final UUID uuidOfTheMotherCodeScheme);
}
