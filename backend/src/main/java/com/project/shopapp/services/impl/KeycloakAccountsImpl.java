package com.project.shopapp.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.shopapp.components.MessageKeys;
import com.project.shopapp.dtos.request.KeycloakCredentialForm;
import com.project.shopapp.dtos.request.KeycloakPasswordCredential;
import com.project.shopapp.dtos.request.RegisterRequest;
import com.project.shopapp.exceptions.DuplicateResourceException;
import com.project.shopapp.exceptions.IdentityProviderException;
import com.project.shopapp.exceptions.LocalizedArgumentException;
import com.project.shopapp.services.KeycloakAccounts;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class KeycloakAccountsImpl implements KeycloakAccounts {
    private final RestClient client;
    private final String realm;
    private final String clientId;
    private final String secret;
    private final JwtDecoder decoder;
    private final String webClientId;

    public KeycloakAccountsImpl(
            @Value("${app.security.keycloak.admin-url:http://localhost:8081}") String url,
            @Value("${app.security.keycloak.realm:shopapp}") String realm,
            @Value("${app.security.keycloak.registration-client:shopapp-registration}")
                    String clientId,
            @Value("${app.security.keycloak.registration-secret:}") String secret,
            @Value("${app.security.keycloak.web-client:shopapp-web}") String webClientId,
            JwtDecoder decoder) {
        var factory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
        this.realm = realm;
        this.clientId = clientId;
        this.secret = secret;
        this.webClientId = webClientId;
        this.decoder = decoder;
    }

    private String serviceAccountToken() {
        if (secret.isBlank()) {
            throw new IdentityProviderException();
        }
        var form = new KeycloakCredentialForm();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", secret);
        JsonNode result =
                client.post()
                        .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form)
                        .retrieve()
                        .body(JsonNode.class);
        if (result == null || !result.path("access_token").isTextual()) {
            throw new IdentityProviderException();
        }
        return result.path("access_token").asText();
    }

    @Override
    public String createDisabled(RegisterRequest request) {
        try {
            var response =
                    client.post()
                            .uri("/admin/realms/{realm}/users", realm)
                            .headers(headers -> headers.setBearerAuth(serviceAccountToken()))
                            .body(accountPayload(request))
                            .retrieve()
                            .toBodilessEntity();
            var location = response.getHeaders().getLocation();
            if (response.getStatusCode().value() != 201 || location == null) {
                throw new IdentityProviderException();
            }
            String path = location.getPath();
            String subject = path.substring(path.lastIndexOf('/') + 1);
            java.util.UUID.fromString(subject);
            return subject;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 409) {
                throw new DuplicateResourceException(MessageKeys.PHONE_ALREADY_REGISTERED);
            }
            throw new IdentityProviderException();
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new IdentityProviderException();
        }
    }

    private Map<String, Object> accountPayload(RegisterRequest request) {
        return Map.of(
                "username", request.getPhoneNumber(),
                "enabled", false,
                "firstName", request.getFullName(),
                "credentials",
                        List.of(
                                new KeycloakPasswordCredential(
                                        "password", request.getPassword(), false)));
    }

    @Override
    public void updateName(String subject, String fullName) {
        try {
            client.put()
                    .uri("/admin/realms/{realm}/users/{subject}", realm, subject)
                    .headers(headers -> headers.setBearerAuth(serviceAccountToken()))
                    .body(Map.of("firstName", fullName))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new IdentityProviderException();
        }
    }

    @Override
    public void verifyPassword(String subject, String username, String password) {
        var form = new KeycloakCredentialForm();
        form.add("grant_type", "password");
        form.add("client_id", webClientId);
        form.add("username", username);
        form.add("password", password);
        JsonNode tokens;
        try {
            tokens =
                    client.post()
                            .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(form)
                            .retrieve()
                            .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            JsonNode error;
            try {
                error = exception.getResponseBodyAs(JsonNode.class);
            } catch (RuntimeException malformedResponse) {
                throw new IdentityProviderException();
            }
            if (exception.getStatusCode().value() == 400
                    && error != null
                    && "invalid_grant".equals(error.path("error").asText())) {
                throw new LocalizedArgumentException(MessageKeys.PASSWORD_CURRENT_INVALID);
            }
            throw new IdentityProviderException();
        } catch (RestClientException exception) {
            throw new IdentityProviderException();
        }
        if (tokens == null || !tokens.path("refresh_token").isTextual()) {
            throw new IdentityProviderException();
        }
        try {
            if (!subject.equals(
                    decoder.decode(tokens.path("access_token").asText()).getSubject())) {
                throw new IdentityProviderException();
            }
        } catch (JwtException exception) {
            throw new IdentityProviderException();
        } finally {
            // A password grant creates a temporary session; do not leave it alive.
            var logout = new KeycloakCredentialForm();
            logout.add("client_id", webClientId);
            logout.add("refresh_token", tokens.path("refresh_token").asText());
            try {
                client.post()
                        .uri("/realms/{realm}/protocol/openid-connect/logout", realm)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(logout)
                        .retrieve()
                        .toBodilessEntity();
            } catch (RestClientException exception) {
                throw new IdentityProviderException();
            }
        }
    }

    @Override
    public void updatePassword(String subject, String password) {
        try {
            client.put()
                    .uri("/admin/realms/{realm}/users/{subject}/reset-password", realm, subject)
                    .headers(headers -> headers.setBearerAuth(serviceAccountToken()))
                    .body(new KeycloakPasswordCredential("password", password, false))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 400) {
                throw new LocalizedArgumentException(MessageKeys.PASSWORD_POLICY);
            }
            throw new IdentityProviderException();
        } catch (RestClientException exception) {
            throw new IdentityProviderException();
        }
    }

    @Override
    public void revokeSessions(String subject) {
        try {
            client.post()
                    .uri("/admin/realms/{realm}/users/{subject}/logout", realm, subject)
                    .headers(headers -> headers.setBearerAuth(serviceAccountToken()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new IdentityProviderException();
        }
    }

    @Override
    public void enable(String subject) {
        try {
            client.put()
                    .uri("/admin/realms/{realm}/users/{subject}", realm, subject)
                    .headers(headers -> headers.setBearerAuth(serviceAccountToken()))
                    .body(Map.of("enabled", true))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new IdentityProviderException();
        }
    }

    @Override
    public void delete(String subject) {
        try {
            client.delete()
                    .uri("/admin/realms/{realm}/users/{subject}", realm, subject)
                    .headers(headers -> headers.setBearerAuth(serviceAccountToken()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) {
                throw new IdentityProviderException();
            }
        } catch (RestClientException exception) {
            throw new IdentityProviderException();
        }
    }
}
