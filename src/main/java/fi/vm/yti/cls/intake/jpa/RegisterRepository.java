package fi.vm.yti.cls.intake.jpa;

import fi.vm.yti.cls.common.model.Register;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegisterRepository extends CrudRepository<Register, String> {

    Register findByCode(final String code);

    List<Register> findAll();

}
