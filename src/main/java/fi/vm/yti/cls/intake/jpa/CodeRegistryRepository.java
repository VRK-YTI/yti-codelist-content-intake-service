package fi.vm.yti.cls.intake.jpa;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.cls.common.model.CodeRegistry;

@Repository
public interface CodeRegistryRepository extends CrudRepository<CodeRegistry, String> {

    CodeRegistry findByCodeValue(final String codeValue);

    CodeRegistry findById(final String id);

    List<CodeRegistry> findAll();

}
