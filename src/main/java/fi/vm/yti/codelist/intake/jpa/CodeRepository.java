package fi.vm.yti.codelist.intake.jpa;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;

@Repository
@Transactional
public interface CodeRepository extends CrudRepository<Code, String> {

    Code findByUriIgnoreCase(final String uri);

    Code findByCodeSchemeCodeValueIgnoreCaseAndCodeValueIgnoreCase(final String codeSchemeCodeValue, final String codeValue);

    Code findByCodeSchemeCodeValueIgnoreCaseAndCodeValueIgnoreCaseAndBroaderCodeId(final String codeSchemeCodeValue, final String codeValue, final UUID broaderCodeId);

    Code findById(final UUID id);

    @Query(value = "SELECT c.order FROM Code as c WHERE c.codeScheme = :codeScheme ORDER BY c.order DESC")
    List<Integer> getInMaxOrder(@Param("codeScheme") final CodeScheme codeScheme);

    Set<Code> findByCodeScheme(final CodeScheme codeScheme);

    Set<Code> findByCodeSchemeId(final UUID codeSchemeId);

    Set<Code> findByCodeSchemeIdAndBroaderCodeIdIsNull(final UUID codeSchemeId);

    Set<Code> findByBroaderCodeId(final UUID broaderCodeId);

    Set<Code> findAll();
}
