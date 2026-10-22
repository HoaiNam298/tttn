package com.project.shopapp.components;

import com.project.shopapp.dtos.response.UserResponse;
import com.project.shopapp.model.User;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class ShopAppAuthenticationToken extends JwtAuthenticationToken {
    private final UserResponse profile;

    public ShopAppAuthenticationToken(
            Jwt jwt, Collection<? extends GrantedAuthority> authorities, User user) {
        super(jwt, authorities, user.getPhoneNumber());
        boolean admin =
                authorities.stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        this.profile = UserResponse.from(user, admin ? "ADMIN" : "USER");
    }

    public UserResponse getProfile() {
        return profile;
    }
}
