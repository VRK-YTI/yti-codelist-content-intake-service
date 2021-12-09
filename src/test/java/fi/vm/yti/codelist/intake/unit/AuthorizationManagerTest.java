package fi.vm.yti.codelist.intake.unit;

import fi.vm.yti.codelist.common.model.Status;
import fi.vm.yti.codelist.intake.model.Code;
import fi.vm.yti.codelist.intake.model.CodeScheme;
import fi.vm.yti.codelist.intake.model.Organization;
import fi.vm.yti.codelist.intake.security.AuthorizationManagerImpl;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthorizationManagerTest {

    @Mock
    AuthenticatedUserProvider provider;

    @InjectMocks
    AuthorizationManagerImpl authorizationManager;

    private static UUID userOrganization = UUID.fromString("d18fe638-d7d7-486d-90b4-73f564328fc4");

    @Test
    public void testSuperuserAccess() {
        when(provider.getUser()).thenReturn(getUser(true));
        assertTrue(authorizationManager.canBeModifiedByUserInOrganization(getOrganizations(userOrganization)));
    }

    @Test
    public void testAccessCodeScheme() {
        when(provider.getUser()).thenReturn(getUser(false, Role.CODE_LIST_EDITOR));

        CodeScheme codeScheme = new CodeScheme();
        codeScheme.setOrganizations(getOrganizations(userOrganization));
        codeScheme.setStatus(Status.DRAFT.name());

        assertTrue(authorizationManager.canCodeSchemeBeDeleted(codeScheme));
    }

    private static YtiUser getUser(boolean superUser, Role... roles) {
        HashMap<UUID, Set<Role>> rolesInOrganization = new HashMap<>();
        rolesInOrganization.put(userOrganization, getRoles(roles));
        return new YtiUser("test@localhost", "Unit", "Test", UUID.randomUUID(), superUser, false, null, null, rolesInOrganization, null, null);
    }

    private static Set<Organization> getOrganizations(UUID... ids) {
        return Arrays.stream(ids).map(id -> {
            Organization o = new Organization();
            o.setId(id);
            return o;
        }).collect(Collectors.toSet());
    }

    private static Set<Role> getRoles(Role... roles) {
        HashSet<Role> userRoles = new HashSet<>();
        userRoles.addAll(Arrays.asList(roles));
        return userRoles;
    }
}
