package fi.vm.yti.codelist.intake.security;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import fi.vm.yti.security.config.FakeUserLogin;
import fi.vm.yti.security.config.FakeUserLoginProvider;
import fi.vm.yti.security.config.SecurityBaseConfig;

@Configuration
@Import(SecurityBaseConfig.class)
public class SecurityConfig {

    @Nullable
    private final String fakeLoginMail;
    @Nullable
    private final String fakeLoginFirstName;
    @Nullable
    private final String fakeLoginLastName;

    SecurityConfig(@Value("${fake.login.mail:}") @Nullable String fakeLoginMail,
                   @Value("${fake.login.firstName:}") @Nullable String fakeLoginFirstName,
                   @Value("${fake.login.lastName:}") @Nullable String fakeLoginLastName) {

        this.fakeLoginMail = fakeLoginMail;
        this.fakeLoginFirstName = fakeLoginFirstName;
        this.fakeLoginLastName = fakeLoginLastName;
    }

    @Bean
    @ConditionalOnProperty("fake.login.mail")
    FakeUserLoginProvider fakeUserLoginProvider() {
        return () -> new FakeUserLogin(fakeLoginMail, fakeLoginFirstName, fakeLoginLastName);
    }
}
