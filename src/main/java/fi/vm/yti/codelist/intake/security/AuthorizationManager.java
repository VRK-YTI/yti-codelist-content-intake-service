package fi.vm.yti.codelist.intake.security;

import fi.vm.yti.codelist.common.model.Organization;

import java.util.Collection;

public interface AuthorizationManager {
    public boolean canBeModifiedByUserInOrganization(final Collection<Organization> organizations );
    public boolean isSuperUser();
}
