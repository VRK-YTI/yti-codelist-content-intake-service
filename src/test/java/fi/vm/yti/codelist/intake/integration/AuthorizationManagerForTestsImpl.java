package fi.vm.yti.codelist.intake.integration;

import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.Member;
import fi.vm.yti.codelist.intake.model.Organization;
import fi.vm.yti.codelist.intake.security.AuthorizationManager;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.util.Collection;
import java.util.UUID;

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
@Profile("automatedtest")
public class AuthorizationManagerForTestsImpl implements AuthorizationManager {

    public boolean canBeModifiedByUserInOrganization(final Collection<Organization> organizations ) {
        return true;
    }

    @Override
    public boolean canExtensionBeDeleted(final Extension extension) {
        return true;
    }

    @Override
    public boolean canMemberBeDeleted(final Member member) {
        return true;
    }

    @Override
    public boolean canCodeRegistryBeDeleted(final CodeRegistry codeRegistry) {
        return true;
    }

    @Override
    public boolean canCodeSchemeBeDeleted(final CodeScheme codeScheme) {
        return true;
    }

    @Override
    public boolean canCodeBeDeleted(final Code codeScheme) {
        return true;
    }

    public boolean isSuperUser() {
        return true;
    }

    public UUID getUserId() { return UUID.randomUUID(); }
}
