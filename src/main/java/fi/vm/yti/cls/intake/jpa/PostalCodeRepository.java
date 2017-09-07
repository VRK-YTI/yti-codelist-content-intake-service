package fi.vm.yti.cls.intake.jpa;

import fi.vm.yti.cls.common.model.PostalCode;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface PostalCodeRepository extends CrudRepository<PostalCode, String> {

    PostalCode findByCodeValue(final String codeValue);

    List<PostalCode> findAll();

}
