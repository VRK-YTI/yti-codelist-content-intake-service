package fi.vm.yti.codelist.intake.jpa;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.intake.model.Commit;

@Repository
@Transactional
public interface CommitRepository extends CrudRepository<Commit, String> {

    Commit findByTraceId(@Param("traceId") final String traceId);
}
