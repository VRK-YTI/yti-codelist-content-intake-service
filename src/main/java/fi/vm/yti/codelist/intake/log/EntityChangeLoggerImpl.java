package fi.vm.yti.codelist.intake.log;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.intake.jpa.CommitRepository;
import fi.vm.yti.codelist.intake.jpa.EditedEntityRepository;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Commit;
import fi.vm.yti.codelist.intake.model.EditedEntity;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;
import fi.vm.yti.codelist.intake.model.ExternalReference;
import fi.vm.yti.codelist.intake.model.PropertyType;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;

@Service
public class EntityChangeLoggerImpl implements EntityChangeLogger {

    final AuthorizationManager authorizationManager;
    final Tracer tracer;
    final CommitRepository commitRepository;
    final EditedEntityRepository editedEntityRepository;
    final EntityPayloadLogger entityPayloadLogger;

    public EntityChangeLoggerImpl(final AuthorizationManager authorizationManager,
                                  final Tracer tracer,
                                  final CommitRepository commitRepository,
                                  final EditedEntityRepository editedEntityRepository,
                                  final EntityPayloadLogger entityPayloadLogger) {
        this.authorizationManager = authorizationManager;
        this.tracer = tracer;
        this.commitRepository = commitRepository;
        this.editedEntityRepository = editedEntityRepository;
        this.entityPayloadLogger = entityPayloadLogger;
    }

    public void logCodeRegistryChange(final CodeRegistry codeRegistry) {
        entityPayloadLogger.logCodeRegistry(codeRegistry);
        final EditedEntity editedEntity = new EditedEntity(createCommit());
        editedEntity.setCodeRegistry(codeRegistry);
        editedEntityRepository.save(editedEntity);
    }

    public void logCodeSchemeChange(final CodeScheme codeScheme) {
        entityPayloadLogger.logCodeScheme(codeScheme);
        final EditedEntity editedEntity = new EditedEntity(createCommit());
        editedEntity.setCodeScheme(codeScheme);
        editedEntityRepository.save(editedEntity);
    }

    public void logCodeChange(final Code code) {
        entityPayloadLogger.logCode(code);
        final EditedEntity editedEntity = new EditedEntity(createCommit());
        editedEntity.setCode(code);
        editedEntityRepository.save(editedEntity);
    }

    public void logExternalReferenceChange(final ExternalReference externalReference) {
        entityPayloadLogger.logExternalReference(externalReference);
        final EditedEntity editedEntity = new EditedEntity(createCommit());
        editedEntity.setExternalReference(externalReference);
        editedEntityRepository.save(editedEntity);
    }

    public void logPropertyTypeChange(final PropertyType propertyType) {
        entityPayloadLogger.logPropertyType(propertyType);
        final EditedEntity editedEntity = new EditedEntity(createCommit());
        editedEntity.setPropertyType(propertyType);
        editedEntityRepository.save(editedEntity);
    }

    public void logExtensionSchemeChange(final ExtensionScheme extensionScheme) {
        entityPayloadLogger.logExtensionScheme(extensionScheme);
        final EditedEntity editedEntity = new EditedEntity(createCommit());
        editedEntity.setExtensionScheme(extensionScheme);
        editedEntityRepository.save(editedEntity);
    }


    public void logExtensionChange(final Extension extension) {
        entityPayloadLogger.logExtension(extension);
        final EditedEntity editedEntity = new EditedEntity(createCommit());
        editedEntity.setExtension(extension);
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
