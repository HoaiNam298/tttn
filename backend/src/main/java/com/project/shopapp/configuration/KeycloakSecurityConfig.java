package com.project.shopapp.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class KeycloakSecurityConfig {
    @Bean
    JwtDecoder keycloakJwtDecoder(
            @Value("${app.security.keycloak.issuer-uri}") String issuer,
            @Value("${app.security.keycloak.jwk-set-uri}") String jwks,
            @Value("${app.security.keycloak.audience}") String audience) {
        var decoder =
                NimbusJwtDecoder.withJwkSetUri(jwks).jwsAlgorithm(SignatureAlgorithm.RS256).build();
        decoder.setJwtValidator(validators(issuer, audience));
        return decoder;
    }

    public static OAuth2TokenValidator<Jwt> validators(String issuer, String audience) {
        OAuth2TokenValidator<Jwt> claims =
                jwt -> {
                    if (jwt.getSubject() == null
                            || jwt.getSubject().isBlank()
                            || jwt.getExpiresAt() == null
                            || jwt.getAudience() == null
                            || !jwt.getAudience().contains(audience)) {
                        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
                    }
                    return OAuth2TokenValidatorResult.success();
                };
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), claims);
    }
}
