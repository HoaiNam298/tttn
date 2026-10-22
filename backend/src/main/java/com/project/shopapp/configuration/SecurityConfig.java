package com.project.shopapp.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.shopapp.components.I18nMessages;
import com.project.shopapp.components.KeycloakAuthenticationConverter;
import com.project.shopapp.components.MessageKeys;
import com.project.shopapp.exceptions.ApiError;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@Slf4j
public class SecurityConfig {
    private static final List<String> ALLOWED_METHODS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private static final List<String> ALLOWED_HEADERS =
            List.of("Authorization", "Content-Type", "Accept-Language");
    private final JwtDecoder decoder;
    private final KeycloakAuthenticationConverter converter;
    private final List<String> allowedOrigins;
    private final I18nMessages messages;
    private final ShopAppLocaleResolver localeResolver;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            JwtDecoder decoder,
            KeycloakAuthenticationConverter converter,
            I18nMessages messages,
            ShopAppLocaleResolver localeResolver,
            ObjectMapper objectMapper,
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.decoder = decoder;
        this.converter = converter;
        this.messages = messages;
        this.localeResolver = localeResolver;
        this.objectMapper = objectMapper;
        this.allowedOrigins =
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .toList();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.sessionManagement(
                session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.exceptionHandling(
                errors ->
                        errors.authenticationEntryPoint(this::sendUnauthorized)
                                .accessDeniedHandler(this::sendForbidden));
        http.authorizeHttpRequests(
                auth ->
                        auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register")
                                .permitAll()
                                .requestMatchers(
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/refresh",
                                        "/api/v1/auth/logout")
                                .denyAll()
                                .requestMatchers("/api/v1/health", "/actuator/health")
                                .permitAll()
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/categories/**",
                                        "/api/v1/products/**",
                                        "/api/v1/media/**",
                                        "/api/v1/payments/methods",
                                        "/api/v1/payments/vnpay/ipn",
                                        "/api/v1/payments/vnpay/return")
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/v1/products/*/reviews")
                                .hasAnyRole("USER", "ADMIN")
                                .requestMatchers(
                                        "/api/v1/categories/**",
                                        "/api/v1/products/**",
                                        "/api/v1/admin/**")
                                .hasRole("ADMIN")
                                .anyRequest()
                                .hasAnyRole("USER", "ADMIN"));
        http.oauth2ResourceServer(
                resource ->
                        resource.jwt(
                                        jwt ->
                                                jwt.decoder(decoder)
                                                        .jwtAuthenticationConverter(converter))
                                .authenticationEntryPoint(this::sendUnauthorized)
                                .accessDeniedHandler(this::sendForbidden));
        return http.build();
    }

    private void sendUnauthorized(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException {
        writeSecurityError(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                MessageKeys.SECURITY_AUTHENTICATION_REQUIRED);
        log.debug("Access rejected reason=authentication_required");
    }

    private void sendForbidden(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception)
            throws IOException {
        log.warn("Access rejected reason=insufficient_permission");
        writeSecurityError(
                request, response, HttpStatus.FORBIDDEN, MessageKeys.SECURITY_ACCESS_DENIED);
    }

    private void writeSecurityError(
            HttpServletRequest request, HttpServletResponse response, HttpStatus status, String key)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.addHeader("Vary", "Accept-Language");
        String message = messages.getForLocale(localeResolver.resolveLocale(request), key);
        objectMapper.writeValue(
                response.getOutputStream(), ApiError.of(status, key, message, Map.of()));
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setExposedHeaders(List.of("X-Request-ID"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
