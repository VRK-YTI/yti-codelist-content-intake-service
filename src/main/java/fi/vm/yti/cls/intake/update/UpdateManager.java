package fi.vm.yti.cls.intake.update;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import fi.vm.yti.cls.common.model.UpdateStatus;
import fi.vm.yti.cls.intake.jpa.UpdateStatusRepository;


/**
 * Class that is responsible for update gatekeeper and status handling and bookkeeping.
 */
@Component
public class UpdateManager {

    public static final String UPDATE_SUCCESSFUL = "successful";

    public static final String UPDATE_FAILED = "failed";

    public static final String UPDATE_CANCELED = "canceled";

    public static final String UPDATE_RUNNING = "running";

    private UpdateStatusRepository m_updateStatusRepository;


    @Inject
    public UpdateManager(final UpdateStatusRepository updateStatusRepository) {

        m_updateStatusRepository = updateStatusRepository;

    }


    public boolean shouldInitialize(final String dataType) {

        final List<UpdateStatus> list = m_updateStatusRepository.getLatestSuccessfulUpdatesForType(dataType);

        if (!list.isEmpty()) {
            return false;
        }

        return true;

    }


    public boolean shouldUpdateData(final String dataType, final String version) {

        final List<UpdateStatus> list = m_updateStatusRepository.getLatestSuccessfulUpdatesForType(dataType);

        // Initial data (null) or yet updated processed update, version does not match.
        if (!list.isEmpty()) {

            final UpdateStatus status = list.get(0);

            if (status.getVersion().equals(version)) {
                return false;
            }

        }

        return true;

    }


    public String getNextUpdateVersion(final String dataType) {

        final List<UpdateStatus> list = m_updateStatusRepository.getLatestSuccessfulUpdatesForType(dataType);

        if (!list.isEmpty()) {
            final UpdateStatus status = list.get(0);
            return status.getNextVersion();
        }

        return null;

    }


    public UpdateStatus createStatus(final String dataType,
                                     final String source,
                                     final String version,
                                     final String status) {

        return createStatus(dataType, source, version, null, status);

    }


    public UpdateStatus createStatus(final String dataType,
                                     final String source,
                                     final String version,
                                     final String nextVersion,
                                     final String status) {

        final UpdateStatus updateStatus = new UpdateStatus();
        updateStatus.setId(UUID.randomUUID().toString());
        updateStatus.setDataType(dataType);
        updateStatus.setSource(source);
        updateStatus.setVersion(version);
        updateStatus.setNextVersion(nextVersion);
        updateStatus.setStatus(status);
        updateStatus.setCreated(new Date(System.currentTimeMillis()));

        m_updateStatusRepository.save(updateStatus);

        return updateStatus;

    }


    public void updateFailedStatus(final UpdateStatus updateStatus) {

        updateStatus.setStatus(UPDATE_FAILED);
        updateStatus.setModified(new Date(System.currentTimeMillis()));

        m_updateStatusRepository.save(updateStatus);

    }


    public void updateSuccessStatus(final UpdateStatus updateStatus) {

        updateSuccessStatus(updateStatus, null);

    }


    private void updateSuccessStatus(final UpdateStatus updateStatus,
                                     final String nextVersion) {

        updateStatus.setStatus(UPDATE_SUCCESSFUL);
        if (nextVersion != null) {
            updateStatus.setNextVersion(nextVersion);
        }
        updateStatus.setModified(new Date(System.currentTimeMillis()));

        m_updateStatusRepository.save(updateStatus);

    }


    public UpdateStatus updateStatus(final UpdateStatus updateStatus,
                                     final String status) {

        updateStatus.setStatus(status);
        updateStatus.setModified(new Date(System.currentTimeMillis()));

        m_updateStatusRepository.save(updateStatus);

        return updateStatus;

    }

}
