package fi.vm.yti.cls.intake.jpa;

import fi.vm.yti.cls.common.model.CodeRegistry;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeRegistryRepository extends CrudRepository<CodeRegistry, String> {

    CodeRegistry findByCodeValue(final String codeValue);

    List<CodeRegistry> findAll();

}
