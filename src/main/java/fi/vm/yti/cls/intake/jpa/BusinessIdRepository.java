package fi.vm.yti.cls.intake.jpa;

import fi.vm.yti.cls.common.model.BusinessId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessIdRepository extends CrudRepository<BusinessId, String> {

    BusinessId findByCodeValue(final String codeValue);

    List<BusinessId> findAll();


}
