package fi.vm.yti.codelist.intake.jpa;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.EditedEntity;

@Repository
@Transactional
public interface EditedEntryRepository extends CrudRepository<EditedEntity, String> {

}
