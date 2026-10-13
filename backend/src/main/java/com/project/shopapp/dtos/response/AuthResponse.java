package com.project.shopapp.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    @ToString.Exclude private String accessToken;

    private String tokenType;

    private long expiresIn;

    private UserResponse user;
}
