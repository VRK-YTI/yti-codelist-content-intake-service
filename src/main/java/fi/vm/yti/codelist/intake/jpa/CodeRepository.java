package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeScheme;

@Repository
@Transactional
public interface CodeRepository extends CrudRepository<Code, String> {

    Code findByCodeSchemeAndCodeValueAndStatus(final CodeScheme codeScheme, final String codeValue, final String status);

    Code findByCodeSchemeAndId(final CodeScheme codeScheme, final UUID codeId);

    Code findByCodeSchemeAndCodeValue(final CodeScheme codeScheme, final String codeValue);

    Code findByCodeSchemeAndCodeValueAndBroaderCodeId(final CodeScheme codeScheme, final String codeValue, final UUID broaderCodeId);

    Code findById(final UUID id);

    Set<Code> findByCodeScheme(final CodeScheme codeScheme);

    @Query(value = "SELECT c FROM Code as c WHERE c.codeScheme.id = :codeSchemeId AND c.broaderCodeId IS NULL ORDER BY c.codeValue DESC")
    Set<Code> findByCodeSchemeIdAndBroaderCodeIdIsNull(@Param("codeSchemeId") final UUID codeSchemeId);

    Set<Code> findAll();
}
