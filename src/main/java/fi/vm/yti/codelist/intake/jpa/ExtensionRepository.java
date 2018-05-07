package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.Extension;

@Repository
@Transactional
public interface ExtensionRepository extends CrudRepository<Extension, String> {

    Set<Extension> findAll();
}
