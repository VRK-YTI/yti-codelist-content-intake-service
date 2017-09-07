package fi.vm.yti.cls.intake.jpa;

import fi.vm.yti.cls.common.model.StreetNumber;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StreetNumberRepository extends CrudRepository<StreetNumber, String> {

    StreetNumber findById(final String id);

    List<StreetNumber> findAll();

}
