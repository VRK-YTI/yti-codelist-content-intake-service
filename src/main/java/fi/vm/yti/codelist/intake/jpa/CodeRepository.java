package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;

@Repository
@Transactional
public interface CodeRepository extends PagingAndSortingRepository<Code, String> {

    Code findByUriIgnoreCase(final String uri);

    Code findByCodeSchemeAndOrder(final CodeScheme codeScheme,
                                  final Integer order);

    Code findByCodeSchemeAndCodeValueIgnoreCase(final CodeScheme codeScheme,
                                                final String codeValue);

    Code findByCodeSchemeAndCodeValueIgnoreCaseAndBroaderCodeId(final CodeScheme codeScheme,
                                                                final String codeValue,
                                                                final UUID broaderCodeId);

    Code findById(final UUID id);

    @Query(value = "SELECT c.flatorder FROM code as c WHERE c.codescheme_id = :codeSchemeId ORDER BY c.flatorder DESC LIMIT 1", nativeQuery = true)
    Integer getCodeMaxOrder(@Param("codeSchemeId") final UUID codeSchemeId);

    Set<Code> findByCodeScheme(final CodeScheme codeScheme);

    Set<Code> findByCodeSchemeId(final UUID codeSchemeId);

    Set<Code> findByCodeSchemeIdAndBroaderCodeIdIsNull(final UUID codeSchemeId);

    Set<Code> findByBroaderCodeId(final UUID broaderCodeId);

    Set<Code> findAll();

    Page<Code> findAll(final Pageable pageable);

    @Query("SELECT COUNT(c) FROM Code as c")
    int getCodeCount();
}
