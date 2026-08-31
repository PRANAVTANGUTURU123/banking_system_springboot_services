package com.eft.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Dev-only security override. Active ONLY under the "dev" profile.
 *
 * <p>This service does not use commons-security; without this config,
 * spring-boot-starter-security's auto-configured chain would require
 * authentication for everything (including actuator) with a generated
 * password. No OAuth2 issuer is configured yet (see the TODO in
 * application.yml), so there is nothing to validate tokens against.
 * Under dev this permit-all chain is the only chain in the context.</p>
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
