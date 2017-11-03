package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.common.model.ExternalReference;

@Repository
public interface ExternalReferenceRepository extends CrudRepository<ExternalReference, String> {

    ExternalReference findById(final String id);

    Set<ExternalReference> findAll();
}
