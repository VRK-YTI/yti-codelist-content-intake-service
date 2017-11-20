package fi.vm.yti.codelist.intake.security;

import fi.vm.yti.security.AuthenticatedUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationManager {

    private final AuthenticatedUserProvider userProvider;

    @Autowired
    AuthorizationManager(AuthenticatedUserProvider userProvider) {
        this.userProvider = userProvider;
    }
}
