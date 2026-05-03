package com.flowboard.config;

import com.flowboard.service.CustomUserDetailsService;
import com.flowboard.service.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.DispatcherType;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;

    @Value("${app.cors.allowed-origins:http://localhost:*,http://127.0.0.1:*}")
    private String allowedOrigins;

    @Bean
    public JWTAuthenticationFilter jwtAuthenticationFilter() {
        return new JWTAuthenticationFilter(jwtService, userDetailsService);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:"))
                .frameOptions(frame -> frame.sameOrigin())
            )
            .authorizeHttpRequests(authz -> authz
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/error", "/api/v1/error").permitAll()
                .requestMatchers("/health", "/api/v1/health").permitAll()
                .requestMatchers("/auth/**", "/api/v1/auth/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        
        // Parse and convert all origins to regex patterns
        String[] originPatterns = allowedOrigins.split(",");
        java.util.List<String> regexPatterns = new java.util.ArrayList<>();
        
        logger.info("CORS Configuration: Processing {} origin patterns from: {}", originPatterns.length, allowedOrigins);
        
        for (String origin : originPatterns) {
            String trimmed = origin.trim();
            String pattern;
            
            if (trimmed.contains("*")) {
                // Convert wildcard pattern to regex
                pattern = trimmed;
                pattern = pattern.replace("*", "\u0001");  // Use placeholder for *
                pattern = pattern.replaceAll("[.+^${}|()\\[\\]\\\\]", "\\\\$0");  // Escape regex chars
                pattern = pattern.replace("\u0001", ".*");  // Convert placeholder to regex
                logger.debug("CORS: Converted wildcard '{}' to regex pattern '{}'", trimmed, pattern);
            } else {
                // Exact origin - escape as literal regex
                pattern = "^" + trimmed.replaceAll("[.+^${}|()\\[\\]\\\\]", "\\\\$0") + "$";
                logger.debug("CORS: Converted exact origin '{}' to regex pattern '{}'", trimmed, pattern);
            }
            regexPatterns.add(pattern);
        }
        
        // Use setAllowedOriginPatterns for all origins (works with or without credentials)
        cors.setAllowedOriginPatterns(regexPatterns);
        cors.setAllowCredentials(true);
        logger.info("CORS: Configured {} origin patterns with credentials", regexPatterns.size());
        
        cors.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cors.setAllowedHeaders(java.util.Arrays.asList(
            org.springframework.http.HttpHeaders.AUTHORIZATION,
            org.springframework.http.HttpHeaders.CONTENT_TYPE,
            "X-Amz-Date",
            "X-Api-Key",
            "X-Amz-Security-Token",
            "X-Amzn-Trace-Id"
        ));
        cors.setExposedHeaders(java.util.Arrays.asList(org.springframework.http.HttpHeaders.AUTHORIZATION));
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
