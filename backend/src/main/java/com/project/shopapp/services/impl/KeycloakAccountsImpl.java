package com.project.shopapp.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.shopapp.components.MessageKeys;
import com.project.shopapp.dtos.request.RegisterRequest;
import com.project.shopapp.exceptions.DuplicateResourceException;
import com.project.shopapp.exceptions.IdentityProviderException;
import com.project.shopapp.services.KeycloakAccounts;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class KeycloakAccountsImpl implements KeycloakAccounts {
    private final RestClient client;
    private final String realm;
    private final String clientId;
    private final String secret;

    public KeycloakAccountsImpl(
            @Value("${app.security.keycloak.admin-url:http://localhost:8081}") String url,
            @Value("${app.security.keycloak.realm:shopapp}") String realm,
            @Value("${app.security.keycloak.registration-client:shopapp-registration}")
                    String clientId,
            @Value("${app.security.keycloak.registration-secret:}") String secret) {
        var factory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
        this.realm = realm;
        this.clientId = clientId;
        this.secret = secret;
    }

    private String serviceAccountToken() {
        if (secret.isBlank()) {
            throw new IdentityProviderException();
        }
        var form = new LinkedMultiValueMap<String, String>();
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
                                Map.of(
                                        "type",
                                        "password",
                                        "value",
                                        request.getPassword(),
                                        "temporary",
                                        false)));
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
