package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;

@Repository
@Transactional
public interface CodeSchemeRepository extends CrudRepository<CodeScheme, String> {

    CodeScheme findByCodeRegistryCodeValueAndCodeValueIgnoreCase(final String codeRegistryCodeValue, final String codeValue);

    CodeScheme findByCodeRegistryAndCodeValueIgnoreCase(final CodeRegistry codeRegistry, final String codeValue);

    CodeScheme findByUriIgnoreCase(final String uri);

    CodeScheme findById(final UUID id);

    Set<CodeScheme> findByCodeRegistry(final CodeRegistry codeRegistry);

    Set<CodeScheme> findAll();
}
