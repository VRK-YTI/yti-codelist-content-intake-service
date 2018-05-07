package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.ExtensionScheme;

@Repository
@Transactional
public interface ExtensionSchemeRepository extends CrudRepository<ExtensionScheme, String> {

    Set<ExtensionScheme> findAll();
}
