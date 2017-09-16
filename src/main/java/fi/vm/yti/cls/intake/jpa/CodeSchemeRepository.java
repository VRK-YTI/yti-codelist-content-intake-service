package fi.vm.yti.cls.intake.jpa;

import fi.vm.yti.cls.common.model.CodeRegistry;
import fi.vm.yti.cls.common.model.CodeScheme;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeSchemeRepository extends CrudRepository<CodeScheme, String> {

    CodeScheme findByCodeRegistryAndCodeValue(final CodeRegistry codeRegistry, final String codeValue);

    CodeScheme findByCodeRegistryAndCodeValueAndStatus(final CodeRegistry codeRegistry, final String codeValue, final String status);

    CodeScheme findById(final String id);

    List<CodeScheme> findByCodeRegistry(final CodeRegistry codeRegistry);

    List<CodeScheme> findAll();

}
