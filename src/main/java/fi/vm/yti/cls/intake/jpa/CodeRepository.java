package fi.vm.yti.cls.intake.jpa;

import fi.vm.yti.cls.common.model.Code;
import fi.vm.yti.cls.common.model.CodeScheme;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeRepository extends CrudRepository<Code, String> {

    Code findByCodeSchemeAndCodeValue(final CodeScheme codeScheme, final String codeValue);

    List<Code> findByCodeScheme(final CodeScheme codeScheme);

    List<Code> findAll();

}
