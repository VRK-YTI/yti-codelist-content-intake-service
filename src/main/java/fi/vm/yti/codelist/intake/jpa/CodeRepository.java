package fi.vm.yti.codelist.intake.jpa;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;

@Repository
@Transactional
public interface CodeRepository extends PagingAndSortingRepository<Code, String> {

    Code findByUriIgnoreCase(final String uri);

    Code findByCodeSchemeAndOrder(final CodeScheme codeScheme,
                                  final Integer order);

    Set<Code> findByCodeSchemeAndStatus(final CodeScheme subCodeScheme,
                                        final String status);

    Code findByCodeSchemeAndCodeValueIgnoreCase(final CodeScheme codeScheme,
                                                final String codeValue);

    Code findByCodeSchemeAndCodeValueIgnoreCaseAndBroaderCodeId(final CodeScheme codeScheme,
                                                                final String codeValue,
                                                                final UUID broaderCodeId);

    Code findById(final UUID id);

    @Query(value = "SELECT c.flatorder FROM code AS c WHERE c.codescheme_id = :codeSchemeId ORDER BY c.flatorder DESC LIMIT 1", nativeQuery = true)
    Integer getCodeMaxOrder(@Param("codeSchemeId") final UUID codeSchemeId);

    Set<Code> findBySubCodeScheme(final CodeScheme subCodeScheme);

    Set<Code> findByCodeSchemeId(final UUID codeSchemeId);

    Set<Code> findByCodeSchemeIdAndBroaderCodeIdIsNull(final UUID codeSchemeId);

    Set<Code> findByBroaderCodeId(final UUID broaderCodeId);

    Set<Code> findAll();

    Page<Code> findAll(final Pageable pageable);

    @Query("SELECT COUNT(c) FROM Code as c")
    int getCodeCount();

    @Query(value = "SELECT COUNT(c) FROM code AS c WHERE c.modified >= :modifiedAfter", nativeQuery = true)
    long modifiedAfterCount(@Param("modifiedAfter") final Date modifiedAfter);

    @Query(value = "SELECT COUNT(c) FROM code AS c WHERE c.created >= :createdAfter", nativeQuery = true)
    long createdAfterCount(@Param("createdAfter") final Date createdAfter);
}
