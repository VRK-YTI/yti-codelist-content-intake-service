package fi.vm.yti.codelist.intake.integration;

import fi.vm.yti.codelist.common.model.Organization;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.util.Collection;


@Component
@Profile("test")
public class AuthorizationManagerForTestsImpl implements AuthorizationManager {

    public boolean canBeModifiedByUserInOrganization(final Collection<Organization> organizations ) {
        return true;
    }

    public boolean isSuperUser() {
        return true;
    }
}
