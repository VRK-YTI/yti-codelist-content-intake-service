package fi.vm.yti.codelist.intake.jpa;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.yti.codelist.intake.model.EditedEntity;

@Repository
@Transactional
public interface EditedEntityRepository extends CrudRepository<EditedEntity, String> {

}
