package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ExternalReference;

@Repository
public interface ExternalReferenceRepository extends CrudRepository<ExternalReference, String> {

    Set<ExternalReference> findByParentCodeScheme(final CodeScheme codeScheme);

    Set<ExternalReference> findByParentCodeSchemeId(final UUID codeSchemeId);

    ExternalReference findById(final UUID id);

    ExternalReference findByIdAndParentCodeScheme(final UUID id, final CodeScheme codeScheme);

    Set<ExternalReference> findAll();
}
