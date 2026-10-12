package com.queueless.config;

import com.queueless.security.JwtAccessDeniedHandler;
import com.queueless.security.JwtAuthenticationEntryPoint;
import com.queueless.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security filter chain configuration for stateless JWT authentication and role-based access control.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint unauthorizedHandler,
                          JwtAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.unauthorizedHandler = unauthorizedHandler;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — unnecessary for stateless REST APIs using JWT tokens
            .csrf(csrf -> csrf.disable())

            // Configure custom JSON error responses for 401 and 403
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(unauthorizedHandler)
                .accessDeniedHandler(accessDeniedHandler)
            )

            // Stateless session management (no HTTP session cookies)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Role-based authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public infrastructure & auth endpoints
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/error").permitAll()

                // Authenticated user profile
                .requestMatchers("/api/auth/me").authenticated()

                // Business operations (Only Business Owners can register and manage businesses)
                .requestMatchers(HttpMethod.POST, "/api/businesses").hasRole("BUSINESS_OWNER")
                .requestMatchers(HttpMethod.GET, "/api/businesses").hasRole("BUSINESS_OWNER")
                .requestMatchers(HttpMethod.PUT, "/api/businesses/**").hasRole("BUSINESS_OWNER")
                .requestMatchers(HttpMethod.GET, "/api/businesses/**").authenticated()

                // Service operations (Only Business Owners can configure services)
                .requestMatchers(HttpMethod.POST, "/api/businesses/*/services").hasRole("BUSINESS_OWNER")
                .requestMatchers(HttpMethod.PUT, "/api/services/**").hasRole("BUSINESS_OWNER")
                .requestMatchers(HttpMethod.GET, "/api/businesses/*/services").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/services/**").authenticated()

                // Queue Management (Owners and Staff can open/close queues & manage customers)
                .requestMatchers(HttpMethod.POST, "/api/services/*/queues").hasAnyRole("BUSINESS_OWNER", "STAFF")
                .requestMatchers(HttpMethod.PUT, "/api/queues/*/close").hasAnyRole("BUSINESS_OWNER", "STAFF")
                .requestMatchers(HttpMethod.PUT, "/api/queues/*/entries/call-next").hasAnyRole("BUSINESS_OWNER", "STAFF")
                .requestMatchers(HttpMethod.PUT, "/api/queue-entries/*/start-service").hasAnyRole("BUSINESS_OWNER", "STAFF")
                .requestMatchers(HttpMethod.PUT, "/api/queue-entries/*/complete").hasAnyRole("BUSINESS_OWNER", "STAFF")
                .requestMatchers(HttpMethod.PUT, "/api/queue-entries/*/skip").hasAnyRole("BUSINESS_OWNER", "STAFF")
                .requestMatchers(HttpMethod.GET, "/api/queues/*/entries").hasAnyRole("BUSINESS_OWNER", "STAFF")

                // Customer Queue Operations (Customers join and view their positions)
                .requestMatchers(HttpMethod.POST, "/api/queues/*/entries").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/api/queues/*/entries/my").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.PUT, "/api/queue-entries/*/cancel").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/queues/*").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/queue-entries/*/estimate").authenticated()

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )

            // Add JWT filter before Spring Security's standard authentication filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // Allow H2 console iframe display
            .headers(headers ->
                headers.frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    /**
     * AuthenticationManager bean: coordinates authentication providers to verify credentials.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Password encoder bean — used to hash and verify passwords using BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

