package org.tech_solutions.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.tech_solutions.application.security.auth.AuthEntryPoint;
import org.tech_solutions.application.security.auth.AuthTokenFilter;

import java.util.List;
import java.util.Arrays;

/* ================================================================================
    Configurações relacionadas a segurança e requisições recebidas pelo sistema
================================================================================ */

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthEntryPoint unauthorizedHandler;
    private final AuthTokenFilter tokenFilter;

    @Value("${application.var.cors.allowed-origins:http://localhost:8081,http://localhost:8082,http://127.0.0.1:8081,http://127.0.0.1:8082,http://localhost:19006,http://127.0.0.1:19006}")
    private String allowedOrigins;

    private static final String[] URLS_PUBLICAS = {
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/swagger-resources/**",
        "/api/v1/users/auth",
        "/api/v1/users/register",
        "/api/v1/users/password/forgot",
        "/api/v1/users/password/reset"
    };

    public SecurityConfig(
            AuthEntryPoint unauthorizedHandler,
            AuthTokenFilter tokenFilter
    ) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.tokenFilter = tokenFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {})
                .authorizeHttpRequests(req -> req.requestMatchers(URLS_PUBLICAS)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(
                        tokenFilter,
                        UsernamePasswordAuthenticationFilter.class
                );
        return http.build();

    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
