package fi.vm.yti.codelist.intake.jpa;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeScheme;

@Repository
@Transactional
public interface CodeRepository extends CrudRepository<Code, String> {

    Code findByCodeSchemeAndCodeValueAndStatus(final CodeScheme codeScheme, final String codeValue, final String status);

    Code findByCodeSchemeAndCodeValue(final CodeScheme codeScheme, final String codeValue);

    Code findById(final String id);

    Set<Code> findByCodeScheme(final CodeScheme codeScheme);

    Set<Code> findAll();

}
