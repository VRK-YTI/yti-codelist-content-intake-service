package fi.vm.yti.cls.intake.jpa;

import fi.vm.yti.cls.common.model.Municipality;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface MunicipalityRepository extends CrudRepository<Municipality, String> {

    Municipality findByCode(final String code);

    List<Municipality> findAll();

}
