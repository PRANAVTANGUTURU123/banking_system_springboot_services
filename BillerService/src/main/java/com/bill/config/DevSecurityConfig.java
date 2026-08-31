package com.bill.config;

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
 * <p>The shared {@code DefaultSecurityConfig} (picked up via the
 * {@code com.commons} component scan) is annotated {@code @Profile("!dev")},
 * so under dev this permit-all chain is the only filter chain in the context.
 * Under any other profile this class is not loaded and the default
 * OAuth2 resource-server chain applies as before.</p>
 *
 * <p>The anonymous user is granted the SCOPE_* authorities this service's
 * {@code @PreAuthorize} checks reference (inert in dev while method security
 * comes from the shared config, but kept as insurance). Note: endpoints that
 * read {@code CurrentUser.customerId()} still fail for anonymous callers —
 * only JWT principals carry a customer id. The registry lookup
 * {@code GET /api/v1/billers/{refNum}/active} does not use it and works
 * anonymously.</p>
 */
@Configuration
@Profile("dev")
public class DevSecurityConfig {

    private static final List<String> DEV_AUTHORITIES = List.of(
            "ROLE_ANONYMOUS",
            "SCOPE_fdx:bill.read",
            "SCOPE_fdx:bill.write"
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
