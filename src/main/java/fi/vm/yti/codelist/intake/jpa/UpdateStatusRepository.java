package fi.vm.yti.codelist.intake.jpa;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fi.vm.yti.codelist.common.model.UpdateStatus;

@Repository
@Transactional
public interface UpdateStatusRepository extends CrudRepository<UpdateStatus, String> {

    @Query(value = "SELECT u FROM UpdateStatus as u WHERE u.dataType = :dataType AND u.status = 'successful' ORDER BY u.modified DESC")
    List<UpdateStatus> getLatestSuccessfulUpdatesForType(@Param("dataType") final String dataType);

}
