package fi.vm.yti.codelist.intake.dao.impl;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.intake.dao.ExtensionDao;
import fi.vm.yti.codelist.intake.jpa.ExtensionRepository;
import fi.vm.yti.codelist.intake.log.EntityChangeLogger;
import fi.vm.yti.codelist.intake.model.Extension;

@Component
public class ExtensionDaoImpl implements ExtensionDao {

    private final EntityChangeLogger entityChangeLogger;
    private final ExtensionRepository extensionRepository;

    @Inject
    public ExtensionDaoImpl(final EntityChangeLogger entityChangeLogger,
                            final ExtensionRepository extensionRepository) {
        this.entityChangeLogger = entityChangeLogger;
        this.extensionRepository = extensionRepository;
    }

    public void delete(final Extension extension) {
        entityChangeLogger.logExtensionChange(extension);
        extensionRepository.delete(extension);
    }

    public void save(final Set<Extension> extensions) {
        extensionRepository.save(extensions);
        extensions.forEach(entityChangeLogger::logExtensionChange);
    }

    public Set<Extension> findAll() {
        return extensionRepository.findAll();
    }
}
