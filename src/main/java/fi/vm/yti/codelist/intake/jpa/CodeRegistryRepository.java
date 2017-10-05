package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.common.model.CodeRegistry;

@Repository
@Transactional
public interface CodeRegistryRepository extends CrudRepository<CodeRegistry, String> {

    CodeRegistry findByCodeValue(final String codeValue);

    CodeRegistry findById(final String id);

    Set<CodeRegistry> findAll();

}
