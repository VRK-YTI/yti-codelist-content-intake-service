package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;

@Repository
@Transactional
public interface ExtensionRepository extends CrudRepository<Extension, String> {

    Set<Extension> findAll();

    Page<Extension> findAll(final Pageable pageable);

    Extension findById(final UUID id);

    Set<Extension> findByCodeSchemes(final CodeScheme codeScheme);

    Set<Extension> findByParentCodeScheme(final CodeScheme codeScheme);

    Set<Extension> findByParentCodeSchemeId(final UUID codeSchemeId);

    Set<Extension> findByParentCodeSchemeIdAndPropertyTypeId(final UUID codeSchemeId,
                                                             final UUID propertyTypeId);

    Extension findByParentCodeSchemeAndCodeValueIgnoreCase(final CodeScheme codeScheme,
                                                           final String codeValue);

    Extension findByParentCodeSchemeIdAndCodeValueIgnoreCase(final UUID codeSchemeId,
                                                             final String codeValue);
    @Query("SELECT COUNT(e) FROM Extension as e")
    int getExtensionCount();
}
