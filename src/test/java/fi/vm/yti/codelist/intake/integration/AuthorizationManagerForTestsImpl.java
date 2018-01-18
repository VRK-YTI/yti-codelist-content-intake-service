package fi.vm.yti.codelist.intake.integration;

import fi.vm.yti.codelist.common.model.Organization;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.util.Collection;

/**
 * This class is used in integration tests to replace the production-code-version of
 * AuthorizationManager because we want to be able to test the content-intake-api without
 * needing to provide authorization during the tests. Because this class is loaded via the
 * "test"-profile, the answer to any auth-related question raised in the API checks will
 * be "YES" and thus the tests are allowed to test the functionality without hindrance.
 *
 * Of course to test the actual authorization-related functionality separate tests are needed.
 *
 * @see fi.vm.yti.codelist.intake.security.AuthorizationManagerImpl which is the
 * production-code counterpart for this class.
 */

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
