package fi.vm.yti.cls.intake.jpa;

import fi.vm.yti.cls.common.model.HealthCareDistrict;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealthCareDistrictRepository extends CrudRepository<HealthCareDistrict, String> {

    HealthCareDistrict findByCodeValue(final String codeValue);

    List<HealthCareDistrict> findAll();

}
