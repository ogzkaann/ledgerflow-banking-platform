package dev.oguzkaandere.ledgerflow.notification.adapter.in.web;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class NotificationSecurityConfiguration {

    @Bean
    SecurityFilterChain notificationSecurityFilterChain(HttpSecurity http, MeterRegistry meters) throws Exception {
        return http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/liveness", "/actuator/health/readiness")
                        .permitAll()
                        .requestMatchers("/actuator/prometheus", "/actuator/metrics/**", "/actuator/info")
                        .hasRole("ledgerflow-admin")
                        .requestMatchers(HttpMethod.GET, "/api/v1/notifications/**")
                        .hasAnyRole("ledgerflow-operator", "ledgerflow-auditor", "ledgerflow-admin")
                        .anyRequest()
                        .denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(realmRoles()))
                        .authenticationEntryPoint((request, response, exception) -> {
                            meters.counter("security.authentication.failures", "service", "notification-service")
                                    .increment();
                            problem(response, 401, "Unauthorized");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            meters.counter("security.authorization.denials", "service", "notification-service")
                                    .increment();
                            problem(response, 403, "Forbidden");
                        }))
                .headers(Customizer.withDefaults())
                .build();
    }

    private static Converter<Jwt, AbstractAuthenticationToken> realmRoles() {
        return jwt -> new JwtAuthenticationToken(jwt, authorities(jwt), jwt.getSubject());
    }

    private static Collection<SimpleGrantedAuthority> authorities(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (!(realmAccess instanceof Map<?, ?> access) || !(access.get("roles") instanceof List<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(role -> role.startsWith("ledgerflow-"))
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    private static void problem(HttpServletResponse response, int status, String title) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status + "}");
    }
}
