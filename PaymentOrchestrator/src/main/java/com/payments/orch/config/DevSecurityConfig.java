package com.payments.orch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Dev-only security override. Active ONLY under the "dev" profile.
 *
 * <p>The shared {@code DefaultSecurityConfig} (picked up via the
 * {@code com.commons} component scan) is annotated {@code @Profile("!dev")},
 * so under dev this permit-all chain is the only filter chain in the context.
 * (Two "any request" chains is a hard startup error on Spring Security 6.4+.)
 * Under any other profile this class is not loaded and the default
 * OAuth2 resource-server chain applies as before.</p>
 *
 * <p>No anonymous authorities are needed here: this service has no
 * {@code @PreAuthorize} checks, so {@code permitAll()} is sufficient.</p>
 */
@Configuration
@Profile("dev")
public class DevSecurityConfig {

    @Bean
    public SecurityFilterChain devPermitAllFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(cs -> cs.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
