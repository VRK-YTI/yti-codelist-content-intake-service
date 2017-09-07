package fi.vm.yti.cls.intake.jpa;

import fi.vm.yti.cls.common.model.Magistrate;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MagistrateRepository extends CrudRepository<Magistrate, String> {

    Magistrate findByCodeValue(final String codeValue);

    List<Magistrate> findAll();

}
