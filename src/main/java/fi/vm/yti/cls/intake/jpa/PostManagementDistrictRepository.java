package fi.vm.yti.cls.intake.jpa;

import fi.vm.yti.cls.common.model.PostManagementDistrict;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface PostManagementDistrictRepository extends CrudRepository<PostManagementDistrict, String> {

    PostManagementDistrict findByCodeValue(final String codeValue);

    List<PostManagementDistrict> findAll();

}
