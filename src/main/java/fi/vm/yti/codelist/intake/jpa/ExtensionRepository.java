package fi.vm.yti.codelist.intake.jpa;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;

@Repository
@Transactional
public interface ExtensionRepository extends CrudRepository<Extension, String> {

    Set<Extension> findAll();

    Set<Extension> findByCodeId(final UUID id);

    Extension findByExtensionSchemeAndOrder(final ExtensionScheme extensionScheme,
                                            final Integer order);

    @Query(value = "SELECT e.order FROM Extension as e WHERE e.extensionScheme = :extensionScheme ORDER BY e.order DESC")
    List<Integer> getInMaxOrder(@Param("extensionScheme") final ExtensionScheme extensionScheme);

    Set<Extension> findByExtensionId(final UUID id);

    Set<Extension> findByExtensionSchemeId(final UUID id);

    Extension findById(final UUID id);

    Extension findByExtensionSchemeAndId(final ExtensionScheme extensionScheme,
                                         final UUID id);

    Extension findByExtensionSchemeAndCodeCodeValueIgnoreCase(final ExtensionScheme extensionScheme,
                                                              final String codeCodeValue);

    Extension findByExtensionSchemeAndCodeUriIgnoreCase(final ExtensionScheme extensionScheme,
                                                        final String codeCodeValue);
}
