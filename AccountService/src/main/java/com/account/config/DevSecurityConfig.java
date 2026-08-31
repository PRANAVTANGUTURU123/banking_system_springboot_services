package com.account.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Dev-only security override. Active ONLY under the "dev" profile.
 *
 * <p>The shared {@code DefaultSecurityConfig} (imported in
 * {@code AccountServiceApplication}) is annotated {@code @Profile("!dev")},
 * so under dev this permit-all chain is the only filter chain in the context.
 * (Two "any request" chains is a hard startup error on Spring Security 6.4+.)
 * Under any other profile this class is not loaded and the default
 * OAuth2 resource-server chain applies as before.</p>
 *
 * <p>The anonymous user is additionally granted the SCOPE_* authorities the
 * controllers' {@code @PreAuthorize} checks expect. With DefaultSecurityConfig
 * absent in dev, {@code @EnableMethodSecurity} is off and those checks are
 * inert anyway — the grants are kept so this config keeps working if method
 * security is ever enabled independently of the shared config.</p>
 */
@Configuration
@Profile("dev")
public class DevSecurityConfig {

    // Every SCOPE_* authority referenced by @PreAuthorize in this service's controllers
    private static final List<String> DEV_AUTHORITIES = List.of(
            "ROLE_ANONYMOUS",
            "SCOPE_fdx:accounts.read",
            "SCOPE_fdx:accounts.write",
            "SCOPE_admin:accounts",
            "SCOPE_admin:accounts.read",
            "SCOPE_admin:accounts.write",
            "SCOPE_fdx:transactions.read",
            "SCOPE_fdx:transactions.write"
    );

    @Bean
    public SecurityFilterChain devPermitAllFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(cs -> cs.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .anonymous(anon -> anon.authorities(
                    AuthorityUtils.createAuthorityList(DEV_AUTHORITIES.toArray(String[]::new))
            ));
        return http.build();
    }
}
