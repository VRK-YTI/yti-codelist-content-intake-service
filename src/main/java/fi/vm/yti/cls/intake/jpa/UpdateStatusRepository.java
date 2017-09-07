package fi.vm.yti.cls.intake.jpa;

import fi.vm.yti.cls.common.model.UpdateStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UpdateStatusRepository extends CrudRepository<UpdateStatus, String> {

    @Query(value = "SELECT u FROM UpdateStatus as u WHERE u.dataType = :dataType AND u.status = 'successful' ORDER BY u.modified DESC")
    List<UpdateStatus> getLatestSuccessfulUpdatesForType(@Param("dataType") final String dataType);

}
