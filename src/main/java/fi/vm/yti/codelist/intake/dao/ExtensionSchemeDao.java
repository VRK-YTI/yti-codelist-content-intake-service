package fi.vm.yti.codelist.intake.dao;

import java.util.Set;

import fi.vm.yti.codelist.intake.model.ExtensionScheme;

public interface ExtensionSchemeDao {

    void delete(final ExtensionScheme extensionScheme);

    void save(final Set<ExtensionScheme> extensionSchemes);

    Set<ExtensionScheme> findAll();
}
