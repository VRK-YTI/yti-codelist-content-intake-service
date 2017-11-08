package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;

@Repository
@Transactional
public interface CodeSchemeRepository extends CrudRepository<CodeScheme, String> {

    CodeScheme findByCodeRegistryAndId(final CodeRegistry codeRegistry, final UUID id);

    CodeScheme findByCodeRegistryAndCodeValue(final CodeRegistry codeRegistry, final String codeValue);

    CodeScheme findByCodeValueAndStatusAndCodeRegistry(final String codeValue, final String status, final CodeRegistry codeRegistry);

    CodeScheme findById(final UUID id);

    Set<CodeScheme> findByCodeRegistry(final CodeRegistry codeRegistry);

    Set<CodeScheme> findAll();
}
