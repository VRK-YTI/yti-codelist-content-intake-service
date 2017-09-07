package fi.vm.yti.cls.intake.jpa;

import fi.vm.yti.cls.common.model.MagistrateServiceUnit;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MagistrateServiceUnitRepository extends CrudRepository<MagistrateServiceUnit, String> {

    MagistrateServiceUnit findByCodeValue(final String codeValue);

    List<MagistrateServiceUnit> findAll();


}
