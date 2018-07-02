package fi.vm.yti.codelist.intake.security;

import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.ExtensionScheme;
import fi.vm.yti.codelist.intake.model.Organization;

import java.util.Collection;
import java.util.UUID;

public interface AuthorizationManager {

    boolean canBeModifiedByUserInOrganization(final Collection<Organization> organizations);

    boolean canExtensionSchemeBeDeleted(final ExtensionScheme extensionScheme);

    boolean canExtensionBeDeleted(final Extension extension);

    boolean canCodeRegistryBeDeleted(final CodeRegistry codeRegistry);

    boolean canCodeSchemeBeDeleted(final CodeScheme codeScheme);

    boolean canCodeBeDeleted(final Code codeScheme);

    boolean isSuperUser();

    UUID getUserId();

}
