package com.project.shopapp.components;

import com.project.shopapp.repositories.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class KeycloakAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UserRepository users;

    public KeycloakAuthenticationConverter(UserRepository users) {
        this.users = users;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        var user =
                users.findByKeycloakUserId(jwt.getSubject())
                        .orElseThrow(
                                () ->
                                        new OAuth2AuthenticationException(
                                                new OAuth2Error(
                                                        "invalid_token",
                                                        "No linked ShopApp profile",
                                                        null)));
        Object access = jwt.getClaim("realm_access");
        Object roles = access instanceof Map<?, ?> map ? map.get("roles") : null;
        var authorities =
                roles instanceof Collection<?> values
                        ? values.stream()
                                .filter(role -> role instanceof String)
                                .filter(role -> List.of("USER", "ADMIN").contains(role))
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .toList()
                        : List.<SimpleGrantedAuthority>of();
        // Compatibility boundary for domain services that still accept the local
        // unique phone number. It is resolved from verified sub, never token phone/username.
        // The JWT principal retains sub; no fallback to a matching local phone exists.
        return new JwtAuthenticationToken(jwt, authorities, user.getPhoneNumber());
    }
}
