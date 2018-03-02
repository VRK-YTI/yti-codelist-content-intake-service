package fi.vm.yti.codelist.intake.security;

import java.util.Collection;
import java.util.EnumSet;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.common.model.Organization;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import static fi.vm.yti.security.Role.ADMIN;
import static fi.vm.yti.security.Role.CODE_LIST_EDITOR;

/**
 * This class is annotated with the "!test"-profile because during integration tests
 * there is another class (also implementing the AuthorizationManager interface) being
 * used instead of this as the AuthorizationManager (which always says "YES you are
 * authorized") in order to let us test the API without auth-related hassle.
 * <p>
 * The existence of that other class (which has been marked @Profile="test") requires that
 * this class is marked with @Profile="!test" - otherwise this class is trying to get
 * loaded during the tests as well and an error ensues.
 *
 * @see fi.vm.yti.codelist.intake.integration.AuthorizationManagerForTestsImpl which is the
 * integrationtest-code counterpart for this class.
 */

@Service
@Profile("!test")
public class AuthorizationManagerImpl implements AuthorizationManager {

    private final AuthenticatedUserProvider userProvider;

    @Inject
    AuthorizationManagerImpl(final AuthenticatedUserProvider userProvider) {
        this.userProvider = userProvider;
    }

    public boolean canBeModifiedByUserInOrganization(final Collection<Organization> organizations) {
        final Collection<UUID> organizationIds = organizations.stream().map(organization -> organization.getId()).collect(Collectors.toList());
        final YtiUser user = userProvider.getUser();
        return user.isSuperuser() || user.isInAnyRole(EnumSet.of(ADMIN, CODE_LIST_EDITOR), organizationIds);
    }

    public boolean isSuperUser() {
        return userProvider.getUser().isSuperuser();
    }
}
