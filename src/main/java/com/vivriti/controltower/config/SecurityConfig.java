package com.vivriti.controltower.config;

import com.vivriti.controltower.common.CorrelationIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Spring Security configuration with HTTP Basic authentication and role-based access.
 *
 * <p>Users:
 * <ul>
 *   <li>operator / operator123 → ROLE_OPERATOR</li>
 *   <li>approver / approver123 → ROLE_APPROVER</li>
 * </ul>
 *
 * <p>Maker-Checker segregation is enforced at the service layer by {@code MakerCheckerGuard}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails operator = User.builder()
                .username("operator")
                .password(encoder.encode("operator123"))
                .roles("OPERATOR")
                .build();

        UserDetails approver = User.builder()
                .username("approver")
                .password(encoder.encode("approver123"))
                .roles("APPROVER")
                .build();

        return new InMemoryUserDetailsManager(operator, approver);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    CorrelationIdFilter correlationIdFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(correlationIdFilter, BasicAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Static web resources & dashboard — public
                .requestMatchers("/", "/index.html", "/static/**", "/css/**", "/js/**", "/favicon.ico", "/dashboard/**").permitAll()
                // OpenAPI & Swagger — public
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // H2 console — public (dev only)
                .requestMatchers("/h2-console/**").permitAll()
                // Health/actuator — public
                .requestMatchers("/actuator/**").permitAll()

                // Generator, Quality Report & Reset — either role
                .requestMatchers(HttpMethod.POST, "/api/v1/generate", "/api/v1/reset").hasAnyRole("OPERATOR", "APPROVER")
                .requestMatchers(HttpMethod.GET, "/api/v1/quality-report").hasAnyRole("OPERATOR", "APPROVER")

                // Ingestion — either role
                .requestMatchers(HttpMethod.POST, "/api/v1/ingest/**").hasAnyRole("OPERATOR", "APPROVER")

                // Reconciliation — either role can trigger, both can read
                .requestMatchers(HttpMethod.POST, "/api/v1/reconcile").hasAnyRole("OPERATOR", "APPROVER")
                .requestMatchers(HttpMethod.GET, "/api/v1/match-decisions/**").hasAnyRole("OPERATOR", "APPROVER")

                // Exceptions — operator can update/override via PUT or POST, both can read
                .requestMatchers(HttpMethod.GET, "/api/v1/exceptions/**").hasAnyRole("OPERATOR", "APPROVER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/exceptions/**").hasRole("OPERATOR")
                .requestMatchers(HttpMethod.POST, "/api/v1/exceptions/**").hasRole("OPERATOR")

                // Close — both can evaluate, only approver can approve, both can read
                .requestMatchers(HttpMethod.POST, "/api/v1/close/evaluate").hasAnyRole("OPERATOR", "APPROVER")
                .requestMatchers(HttpMethod.POST, "/api/v1/close/approve").hasRole("APPROVER")
                .requestMatchers(HttpMethod.GET, "/api/v1/close/**").hasAnyRole("OPERATOR", "APPROVER")

                // Audit — both can read
                .requestMatchers(HttpMethod.GET, "/api/v1/audit/**").hasAnyRole("OPERATOR", "APPROVER")

                // Evaluator — either role, supports both GET and POST
                .requestMatchers("/api/v1/evaluate").hasAnyRole("OPERATOR", "APPROVER")

                // Default — authenticated
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
