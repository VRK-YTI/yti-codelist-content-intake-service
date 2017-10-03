package fi.vm.yti.codelist.intake.jpa;

import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;

@Repository
@Transactional
public interface CodeSchemeRepository extends CrudRepository<CodeScheme, String> {

    CodeScheme findByCodeRegistryAndCodeValue(final CodeRegistry codeRegistry, final String codeValue);

    CodeScheme findByCodeRegistryAndCodeValueAndStatus(final CodeRegistry codeRegistry, final String codeValue, final String status);

    CodeScheme findById(final String id);

    Set<CodeScheme> findByCodeRegistry(final CodeRegistry codeRegistry);

    Set<CodeScheme> findAll();

}