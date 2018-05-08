package fi.vm.yti.codelist.intake.dao.impl;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import fi.vm.yti.codelist.intake.dao.ExtensionSchemeDao;
import fi.vm.yti.codelist.intake.jpa.ExtensionSchemeRepository;
import fi.vm.yti.codelist.intake.log.EntityChangeLogger;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;

@Component
public class ExtensionSchemeDaoImpl implements ExtensionSchemeDao {

    private final EntityChangeLogger entityChangeLogger;
    private final ExtensionSchemeRepository extensionSchemeRepository;

    @Inject
    public ExtensionSchemeDaoImpl(final EntityChangeLogger entityChangeLogger,
                                  final ExtensionSchemeRepository extensionSchemeRepository) {
        this.entityChangeLogger = entityChangeLogger;
        this.extensionSchemeRepository = extensionSchemeRepository;
    }

    public void delete(final ExtensionScheme extensionScheme) {
        entityChangeLogger.logExtensionSchemeChange(extensionScheme);
        extensionSchemeRepository.delete(extensionScheme);
    }

    public void save(final Set<ExtensionScheme> extensionSchemes) {
        extensionSchemeRepository.save(extensionSchemes);
        extensionSchemes.forEach(entityChangeLogger::logExtensionSchemeChange);
    }

    public Set<ExtensionScheme> findAll() {
        return extensionSchemeRepository.findAll();
    }
}
