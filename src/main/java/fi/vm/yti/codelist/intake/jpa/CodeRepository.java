package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;

@Repository
@Transactional
public interface CodeRepository extends CrudRepository<Code, String> {

    Code findByCodeSchemeAndCodeValueIgnoreCase(final CodeScheme codeScheme, final String codeValue);

    Code findByCodeSchemeAndCodeValueAndBroaderCodeId(final CodeScheme codeScheme, final String codeValue, final UUID broaderCodeId);

    Code findById(final UUID id);

    Set<Code> findByCodeScheme(final CodeScheme codeScheme);

    Set<Code> findByCodeSchemeId(final UUID codeSchemeId);

    Set<Code> findByCodeSchemeIdAndBroaderCodeIdIsNull(final UUID codeSchemeId);

    Set<Code> findByBroaderCodeId(final UUID broaderCodeId);

    Set<Code> findAll();
}
