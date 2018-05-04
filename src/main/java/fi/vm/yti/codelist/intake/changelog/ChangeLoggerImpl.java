package fi.vm.yti.codelist.intake.changelog;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.intake.jpa.CommitRepository;
import fi.vm.yti.codelist.intake.jpa.EditedEntityRepository;
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
    final EditedEntityRepository editedEntityRepository;

    public ChangeLoggerImpl(final AuthorizationManager authorizationManager,
                            final Tracer tracer,
                            final CommitRepository commitRepository,
                            final EditedEntityRepository editedEntityRepository) {
        this.authorizationManager = authorizationManager;
        this.tracer = tracer;
        this.commitRepository = commitRepository;
        this.editedEntityRepository = editedEntityRepository;
    }

    public void logCodeSchemeChange(final CodeScheme codeScheme) {
        final EditedEntity editedEntity = new EditedEntity(createCommit());
        editedEntity.setCodeScheme(codeScheme);
        editedEntityRepository.save(editedEntity);
    }

    public void logCodeChange(final Code code) {
        final EditedEntity editedEntity = new EditedEntity(createCommit());
        editedEntity.setCode(code);
        editedEntityRepository.save(editedEntity);
    }

    public void logExternalReferenceChange(final ExternalReference externalReference) {
        final EditedEntity editedEntity = new EditedEntity(createCommit());
        editedEntity.setExternalReference(externalReference);
        editedEntityRepository.save(editedEntity);
    }

    private Commit createCommit() {
        final String traceId = getTraceId();
        Commit commit = null;
        if (traceId != null) {
            commit = commitRepository.findByTraceId(traceId);
        }
        if (commit == null) {
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
