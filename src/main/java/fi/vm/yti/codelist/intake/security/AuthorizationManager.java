package fi.vm.yti.codelist.intake.security;

import static fi.vm.yti.security.Role.ADMIN;
import static fi.vm.yti.security.Role.CODE_LIST_EDITOR;

import java.util.Collection;
import java.util.EnumSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fi.vm.yti.codelist.common.model.Organization;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;

@Service
public class AuthorizationManager {
    
    private final AuthenticatedUserProvider userProvider;
    
    @Autowired
    AuthorizationManager(AuthenticatedUserProvider userProvider) {
        this.userProvider = userProvider;
    }

    public boolean canBeModifiedByUserInOrganization(Collection<Organization> organizations ) {
        Collection<UUID> organizationIds = organizations.stream().map(organization -> organization.getId()).collect(Collectors.toList());
        YtiUser user = userProvider.getUser();
        return user.isSuperuser() || user.isInAnyRole(EnumSet.of(ADMIN, CODE_LIST_EDITOR), organizationIds);
    }
}
