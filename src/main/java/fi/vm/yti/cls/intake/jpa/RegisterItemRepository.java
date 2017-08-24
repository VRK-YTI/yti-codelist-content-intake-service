package fi.vm.yti.cls.intake.jpa;

import fi.vm.yti.cls.common.model.RegisterItem;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface RegisterItemRepository extends CrudRepository<RegisterItem, String> {

    RegisterItem findByRegisterAndCode(final String register, final String code);

    RegisterItem findByCode(final String code);

    List<RegisterItem> findByRegister(final String register);

    List<RegisterItem> findAll();

}
