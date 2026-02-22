package com.myshop.config;

import com.myshop.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.myshop.security.UserDetailsServiceImpl;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.Customizer;
import java.util.List;

/**
 * SecurityConfig — THE heart of Spring Security.
 *
 * @EnableWebSecurity: Enables Spring Security's web security support.
 * @EnableMethodSecurity: Enables @PreAuthorize, @PostAuthorize on methods.
 *
 *                        KEY CONCEPTS:
 *
 *                        1. STATELESS SESSION (JWT approach):
 *                        Traditional apps use HTTP sessions stored on the
 *                        server.
 *                        JWT apps are stateless — the token carries identity,
 *                        no server-side session needed.
 *                        SessionCreationPolicy.STATELESS = never create/use
 *                        HTTP sessions.
 *                        Benefit: horizontal scaling (any server can handle any
 *                        request).
 *
 *                        2. CSRF DISABLED:
 *                        CSRF attacks exploit the browser automatically sending
 *                        session cookies.
 *                        With JWT (not cookies), CSRF is not applicable —
 *                        clients explicitly set the
 *                        Authorization header, which browsers won't do
 *                        automatically on cross-origin requests.
 *                        → Safe to disable CSRF for stateless JWT APIs.
 *
 *                        3. ROUTE AUTHORIZATION:
 *                        requestMatchers defines which URLs require what level
 *                        of access:
 *                        - permitAll(): no auth needed (public endpoints and
 *                        Swagger)
 *                        - hasRole("ADMIN"): only users with ROLE_ADMIN
 *                        - authenticated(): any logged-in user
 *                        - anyRequest().authenticated(): everything else needs
 *                        auth
 *
 *                        4. FILTER ORDER:
 *                        JwtAuthenticationFilter runs BEFORE
 *                        UsernamePasswordAuthenticationFilter.
 *                        This ensures our JWT check happens before Spring tries
 *                        form-based auth.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Add Security Headers
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)))

                // Disable CSRF — not needed for stateless JWT APIs
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless session — no HTTP session ever created
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Route-level authorization rules
                .authorizeHttpRequests(auth -> auth

                        // Public: Auth endpoints — anyone can register/login
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // Authenticated users can manage reviews (Must precede broader
                        // /api/v1/products/** rules)
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/*/reviews").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/*/reviews/eligibility").authenticated()

                        // Public: Product & Category reads
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()

                        // ADMIN only: Product writes
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")

                        // ADMIN only: Category writes
                        .requestMatchers(HttpMethod.POST, "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasRole("ADMIN")

                        // ADMIN only: All admin endpoints
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Public: Actuator health (for Docker healthcheck and monitoring)
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // Public: Swagger UI + OpenAPI spec
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**")
                        .permitAll()

                        // Everything else requires authentication
                        .anyRequest().authenticated())

                // Set the authentication provider (knows how to verify username+password)
                .authenticationProvider(authenticationProvider())

                // Add our JWT filter BEFORE Spring's default
                // UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * DaoAuthenticationProvider — Spring Security's auth provider that uses
     * a UserDetailsService + PasswordEncoder to verify credentials.
     *
     * When AuthenticationManager.authenticate() is called (in AuthService.login):
     * 1. Provider calls userDetailsService.loadUserByUsername(email)
     * 2. Calls passwordEncoder.matches(rawPassword, storedHash)
     * 3. If match → returns authenticated token
     * 4. If not → throws BadCredentialsException → 401
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * BCryptPasswordEncoder — the industry standard for password hashing.
     *
     * WHY NOT MD5 or SHA-256 for passwords?
     * MD5/SHA are FAST hash functions → attacker can try billions per second.
     * BCrypt is intentionally SLOW (cost factor = 2^10 = 1024 rounds).
     * Each BCrypt check takes ~100ms → attacker can only try ~10/sec.
     *
     * The password_hash in our DB (from V2__seed_data.sql) was created with
     * cost=10. BCryptPasswordEncoder defaults to cost=10. They match.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager — used in AuthService to validate login credentials.
     * Spring Boot auto-configures this, we just expose it as a bean.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000" // dev
        // TODO Phase 7: Add Azure frontend URL e.g.
        // "https://myshop-frontend.azurewebsites.net"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-ID"));
        configuration.setExposedHeaders(List.of("X-Cache", "Retry-After"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
