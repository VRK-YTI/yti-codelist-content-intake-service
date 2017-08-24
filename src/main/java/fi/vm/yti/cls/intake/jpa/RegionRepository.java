package fi.vm.yti.cls.intake.jpa;

import fi.vm.yti.cls.common.model.Region;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface RegionRepository extends CrudRepository<Region, String> {

    Region findByCode(final String code);

    List<Region> findAll();

}
