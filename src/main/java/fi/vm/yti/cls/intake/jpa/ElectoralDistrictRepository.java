package fi.vm.yti.cls.intake.jpa;

import fi.vm.yti.cls.common.model.ElectoralDistrict;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElectoralDistrictRepository extends CrudRepository<ElectoralDistrict, String> {

    ElectoralDistrict findByCodeValue(final String codeValue);

    List<ElectoralDistrict> findAll();

}
