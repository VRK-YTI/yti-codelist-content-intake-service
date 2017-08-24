package fi.vm.yti.cls.intake.jpa;

import fi.vm.yti.cls.common.model.BusinessServiceSubRegion;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface BusinessServiceSubRegionRepository extends CrudRepository<BusinessServiceSubRegion, String> {

    BusinessServiceSubRegion findByCode(final String code);

    List<BusinessServiceSubRegion> findAll();

}
