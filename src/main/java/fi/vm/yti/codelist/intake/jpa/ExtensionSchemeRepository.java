package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;

@Repository
@Transactional
public interface ExtensionSchemeRepository extends CrudRepository<ExtensionScheme, String> {

    Set<ExtensionScheme> findAll();

    ExtensionScheme findById(final UUID id);

    Set<ExtensionScheme> findByParentCodeScheme(final CodeScheme codeScheme);

    Set<ExtensionScheme> findByParentCodeSchemeId(final UUID codeSchemeId);

    ExtensionScheme findByParentCodeSchemeAndCodeValue(final CodeScheme codeScheme,
                                                       final String codeValue);

    ExtensionScheme findByParentCodeSchemeIdAndCodeValue(final UUID codeSchemeId,
                                                         final String codeValue);
}
