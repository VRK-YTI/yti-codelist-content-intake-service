package fi.vm.yti.codelist.intake.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.common.model.IndexStatus;

@Repository
public interface IndexStatusRepository extends CrudRepository<IndexStatus, String> {

    @Query(value = "SELECT i FROM IndexStatus as i WHERE i.indexAlias = :aliasName AND i.status = 'successful' ORDER BY i.modified DESC")
    List<IndexStatus> getLatestSuccessfulIndexStatusForIndexAlias(@Param("aliasName") final String aliasName);

    @Query(value = "SELECT i FROM IndexStatus as i WHERE i.indexAlias = :aliasName AND i.status = 'running' ORDER BY i.modified DESC")
    List<IndexStatus> getLatestRunningIndexStatus(@Param("aliasName") final String aliasName);
}
