package fi.vm.yti.codelist.intake.update;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.intake.jpa.UpdateStatusRepository;
import fi.vm.yti.codelist.intake.model.UpdateStatus;

@Component
public class UpdateManager {

    public static final String UPDATE_SUCCESSFUL = "successful";
    public static final String UPDATE_FAILED = "failed";
    public static final String UPDATE_CANCELED = "canceled";
    public static final String UPDATE_RUNNING = "running";
    private final UpdateStatusRepository updateStatusRepository;

    @Inject
    public UpdateManager(final UpdateStatusRepository updateStatusRepository) {
        this.updateStatusRepository = updateStatusRepository;
    }

    public boolean shouldUpdateData(final String dataType, final String identifier, final String version) {
        final List<UpdateStatus> list = updateStatusRepository.getLatestSuccessfulUpdatesForType(dataType, identifier);
        if (!list.isEmpty()) {
            final UpdateStatus status = list.get(0);
            return !status.getVersion().equals(version);
        }
        return true;
    }

    public UpdateStatus createStatus(final String dataType,
                                     final String identifier,
                                     final String source,
                                     final String version,
                                     final String status) {
        final UpdateStatus updateStatus = new UpdateStatus();
        final UUID uuid = UUID.randomUUID();
        updateStatus.setId(uuid);
        updateStatus.setDataType(dataType);
        updateStatus.setSource(source);
        updateStatus.setVersion(version);
        updateStatus.setIdentifier(identifier);
        updateStatus.setStatus(status);
        updateStatus.setModified(new Date(System.currentTimeMillis()));
        updateStatusRepository.save(updateStatus);
        return updateStatus;
    }

    public void updateFailedStatus(final UpdateStatus updateStatus) {
        updateStatus.setStatus(UPDATE_FAILED);
        updateStatus.setModified(new Date(System.currentTimeMillis()));
        updateStatusRepository.save(updateStatus);
    }

    public void updateSuccessStatus(final UpdateStatus updateStatus) {
        updateStatus.setStatus(UPDATE_SUCCESSFUL);
        updateStatus.setModified(new Date(System.currentTimeMillis()));
        updateStatusRepository.save(updateStatus);
    }

    public UpdateStatus updateStatus(final UpdateStatus updateStatus,
                                     final String status) {
        updateStatus.setStatus(status);
        updateStatus.setModified(new Date(System.currentTimeMillis()));
        updateStatusRepository.save(updateStatus);
        return updateStatus;
    }
}
