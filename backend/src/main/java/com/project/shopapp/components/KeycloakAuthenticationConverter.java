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
        // Resolve the business identity only from verified sub. Keep the JWT principal
        // and the local phone-based name used by domain services; never trust token phone.
        return new ShopAppAuthenticationToken(jwt, authorities, user);
    }
}
