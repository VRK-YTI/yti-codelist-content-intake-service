package fi.vm.yti.codelist.intake.security;

import fi.vm.yti.codelist.intake.model.Organization;

import java.util.Collection;

public interface AuthorizationManager {
    boolean canBeModifiedByUserInOrganization(final Collection<Organization> organizations );
    boolean isSuperUser();
}
