package fi.vm.yti.codelist.intake.dao;

import java.util.Set;

import fi.vm.yti.codelist.intake.model.Extension;

public interface ExtensionDao {

    void delete(final Extension extension);

    void save(final Set<Extension> extensions);

    Set<Extension> findAll();
}
