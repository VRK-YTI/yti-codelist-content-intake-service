package fi.vm.yti.codelist.intake.security;

import java.util.Collection;
import java.util.EnumSet;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.model.AbstractIdentifyableCode;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeRegistry;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Extension;
import fi.vm.yti.codelist.intake.model.Member;
import fi.vm.yti.codelist.intake.model.Organization;
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
 * See fi.vm.yti.codelist.intake.integration.AuthorizationManagerForTestsImpl which is the
 * integrationtest-code counterpart for this class.
 */

@Service
@Profile("!automatedtest")
public class AuthorizationManagerImpl implements AuthorizationManager {

    private final AuthenticatedUserProvider userProvider;

    @Inject
    AuthorizationManagerImpl(final AuthenticatedUserProvider userProvider) {
        this.userProvider = userProvider;
    }

    public boolean canBeModifiedByUserInOrganization(final Collection<Organization> organizations) {
        final Collection<UUID> organizationIds = organizations.stream().map(AbstractIdentifyableCode::getId).collect(Collectors.toList());
        final YtiUser user = userProvider.getUser();
        return user.isSuperuser() || user.isInAnyRole(EnumSet.of(ADMIN, CODE_LIST_EDITOR), organizationIds);
    }

    public boolean canExtensionBeDeleted(final Extension extension) {
        final Collection<UUID> organizationIds = extension.getParentCodeScheme().getOrganizations().stream().map(AbstractIdentifyableCode::getId).collect(Collectors.toList());
        final YtiUser user = userProvider.getUser();
        return user.isSuperuser() || (user.isInAnyRole(EnumSet.of(ADMIN, CODE_LIST_EDITOR), organizationIds) && Status.valueOf(extension.getStatus()).ordinal() <= Status.VALID.ordinal());
    }

    public boolean canMemberBeDeleted(final Member member) {
        final Extension extension = member.getExtension();
        final Collection<UUID> organizationIds = extension.getParentCodeScheme().getOrganizations().stream().map(AbstractIdentifyableCode::getId).collect(Collectors.toList());
        final YtiUser user = userProvider.getUser();
        return user.isSuperuser() || (user.isInAnyRole(EnumSet.of(ADMIN, CODE_LIST_EDITOR), organizationIds) && Status.valueOf(extension.getStatus()).ordinal() <= Status.VALID.ordinal());
    }

    public boolean canCodeRegistryBeDeleted(final CodeRegistry codeRegistry) {
        final YtiUser user = userProvider.getUser();
        return user.isSuperuser();
    }

    public boolean canCodeSchemeBeDeleted(final CodeScheme codeScheme) {
        final Collection<UUID> organizationIds = codeScheme.getOrganizations().stream().map(AbstractIdentifyableCode::getId).collect(Collectors.toList());
        final YtiUser user = userProvider.getUser();
        return user.isSuperuser() || (user.isInAnyRole(EnumSet.of(ADMIN, CODE_LIST_EDITOR), organizationIds) && Status.valueOf(codeScheme.getStatus()).ordinal() <= Status.VALID.ordinal());
    }

    public boolean canCodeBeDeleted(final Code code) {
        final CodeScheme codeScheme = code.getCodeScheme();
        final Collection<UUID> organizationIds = codeScheme.getOrganizations().stream().map(AbstractIdentifyableCode::getId).collect(Collectors.toList());
        final YtiUser user = userProvider.getUser();
        return user.isSuperuser() || (user.isInAnyRole(EnumSet.of(ADMIN, CODE_LIST_EDITOR), organizationIds) && Status.valueOf(codeScheme.getStatus()).ordinal() <= Status.VALID.ordinal());
    }

    public boolean isSuperUser() {
        return userProvider.getUser().isSuperuser();
    }

    public UUID getUserId() {
        return userProvider.getUser().getId();
    }
}
