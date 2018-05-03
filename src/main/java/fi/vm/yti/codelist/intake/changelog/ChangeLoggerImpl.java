package fi.vm.yti.codelist.intake.changelog;

import java.util.UUID;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.intake.jpa.CommitRepository;
import fi.vm.yti.codelist.intake.jpa.EditedEntryRepository;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Commit;
import fi.vm.yti.codelist.intake.model.EditedEntity;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;

@Service
public class ChangeLoggerImpl implements ChangeLogger {

    final AuthorizationManager authorizationManager;
    final Tracer tracer;
    final CommitRepository commitRepository;
    final EditedEntryRepository editedEntryRepository;

    public ChangeLoggerImpl(final AuthorizationManager authorizationManager,
                            final Tracer tracer,
                            final CommitRepository commitRepository,
                            final EditedEntryRepository editedEntryRepository) {
        this.authorizationManager = authorizationManager;
        this.tracer = tracer;
        this.commitRepository = commitRepository;
        this.editedEntryRepository = editedEntryRepository;
    }

    public void logCodeSchemeChange(final CodeScheme codeScheme) {
        final EditedEntity editedEntity = new EditedEntity(createCommit());
        editedEntity.setCodeScheme(codeScheme);
        editedEntryRepository.save(editedEntity);
    }

    public void logCodeChange(final Code code) {
        final EditedEntity editedEntity = new EditedEntity(createCommit());
        editedEntity.setCode(code);
        editedEntryRepository.save(editedEntity);
    }

    public void logExternalReferenceChange(final ExternalReference externalReference) {
        final EditedEntity editedEntity = new EditedEntity(createCommit());
        editedEntity.setExternalReference(externalReference);
        editedEntryRepository.save(editedEntity);
    }

    private Commit createCommit() {
        String traceId = getTraceId();
        Commit commit = null;
        if (traceId != null) {
            commit = commitRepository.findById(traceId);
        }
        if (commit == null) {
            if (traceId == null) {
                traceId = UUID.randomUUID().toString();
            }
            commit = new Commit(traceId, authorizationManager.getUserId());
            commitRepository.save(commit);
        }
        return commit;
    }

    private String getTraceId() {
        final Span span = tracer.getCurrentSpan();
        if (span != null) {
            return span.traceIdString();
        }
        return null;
    }
}
