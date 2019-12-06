package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ExternalReference;

@Repository
@Transactional
public interface ExternalReferenceRepository extends CrudRepository<ExternalReference, String> {

    ExternalReference findByGlobalTrueAndHref(final String href);

    Set<ExternalReference> findByParentCodeSchemeId(final UUID codeSchemeId);

    ExternalReference findByParentCodeSchemeIdAndHref(final UUID parentCodeSchemeId,
                                                      final String href);

    ExternalReference findByParentCodeSchemeIdAndId(final UUID parentCodeSchemeId,
                                                    final UUID id);

    ExternalReference findById(final UUID id);

    ExternalReference findByIdAndParentCodeScheme(final UUID id,
                                                  final CodeScheme codeScheme);

    Set<ExternalReference> findAll();
}
