package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.project.shopapp.controller.AuthController;
import com.project.shopapp.model.User;
import com.project.shopapp.model.UserAddress;
import com.project.shopapp.repositories.UserAddressRepository;
import com.project.shopapp.repositories.UserRepository;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:keycloak_phase4;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
        })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KeycloakSecurityIntegrationTest {
    private static final String ISSUER = "http://localhost:8081/realms/shopapp";
    private static final AtomicInteger PHONES = new AtomicInteger(100000000);
    private static final RSAKey KEY;
    private static final HttpServer JWKS;

    static {
        try {
            KEY = new RSAKeyGenerator(2048).keyID("phase4").generate();
            JWKS = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            JWKS.createContext(
                    "/certs",
                    exchange -> {
                        byte[] body =
                                new JWKSet(KEY.toPublicJWK())
                                        .toString()
                                        .getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, body.length);
                        try (var output = exchange.getResponseBody()) {
                            output.write(body);
                        }
                    });
            JWKS.start();
        } catch (Exception error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add(
                "app.security.keycloak.jwk-set-uri",
                () -> "http://127.0.0.1:" + JWKS.getAddress().getPort() + "/certs");
    }

    @AfterAll
    static void stopServer() {
        JWKS.stop(0);
    }

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private UserAddressRepository addresses;
    @Autowired private ApplicationContext context;
    @MockBean private KeycloakAccounts accounts;
    private User owner;
    private User other;

    @Test
    void registrationIsPublicValidatedAndDoesNotIssueLocalCredentials() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        String subject = UUID.randomUUID().toString();
        org.mockito.Mockito.when(accounts.createDisabled(org.mockito.ArgumentMatchers.any()))
                .thenReturn(subject);
        String phone = "0" + PHONES.incrementAndGet();
        mvc.perform(
                        post("/api/v1/auth/register")
                                .contentType("application/json")
                                .content(
                                        """
                                {"fullName":"Registration Test","phoneNumber":"%s",
                                 "password":"test-password","address":"Test address","role":"ADMIN"}
                                """
                                                .formatted(phone)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("role").value("USER"))
                .andExpect(jsonPath("token").doesNotExist())
                .andExpect(jsonPath("password").doesNotExist());
        User saved = users.findByKeycloakUserId(subject).orElseThrow();
        mvc.perform(
                        post("/api/v1/auth/register")
                                .contentType("application/json")
                                .content(
                                        """
                                {"fullName":"Registration Test","phoneNumber":"%s",
                                 "password":"test-password","address":"Test address"}
                                """
                                                .formatted(phone)))
                .andExpect(status().isConflict());
    }

    @BeforeEach
    void profiles() {
        String suffix = UUID.randomUUID().toString();
        owner =
                users.save(
                        User.builder()
                                .keycloakUserId("owner-" + suffix)
                                .phoneNumber("0" + PHONES.incrementAndGet())
                                .fullName("Owner")
                                .address("HCM")
                                .build());
        other =
                users.save(
                        User.builder()
                                .keycloakUserId("other-" + suffix)
                                .phoneNumber("0" + PHONES.incrementAndGet())
                                .fullName("Other")
                                .address("HN")
                                .build());
    }

    @Test
    void verifiesSignatureAndUsesSubjectAndTokenRoleForProfile() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(claims("USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(owner.getId()))
                .andExpect(jsonPath("$.role").value("USER"));
        assertFalse(context.getBeansOfType(AuthController.class).isEmpty());
    }

    @Test
    void onlyTokenAdminCanUseAdminApi() throws Exception {
        mvc.perform(get("/api/v1/admin/orders").header("Authorization", bearer(claims("USER"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/orders").header("Authorization", bearer(claims("ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidIssuerAudienceExpiryMissingSubjectAndMissingExpiry() throws Exception {
        for (JWTClaimsSet.Builder invalid :
                List.of(
                        claims("USER").issuer("https://wrong.example/realm"),
                        claims("USER").audience("different-api"),
                        claims("USER").audience((String) null),
                        claims("USER").expirationTime(Date.from(Instant.now().minusSeconds(600))),
                        claims("USER").notBeforeTime(Date.from(Instant.now().plusSeconds(600))),
                        claims("USER").subject(null),
                        claims("USER").subject(" "),
                        claims("USER").expirationTime(null))) {
            mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(invalid)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void rejectsForgedSignatureAndUnauthenticatedRequest() throws Exception {
        RSAKey attacker = new RSAKeyGenerator(2048).keyID(KEY.getKeyID()).generate();
        mvc.perform(
                        get("/api/v1/auth/me")
                                .header(
                                        "Authorization",
                                        "Bearer " + signed(claims("ADMIN"), attacker)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void doesNotLinkUnknownSubjectByMatchingPhoneOrUsername() throws Exception {
        mvc.perform(
                        get("/api/v1/auth/me")
                                .header(
                                        "Authorization",
                                        bearer(claims("USER").subject(owner.getPhoneNumber()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ignoresUnrelatedAndPrefixedRolesAndBlocksLegacyEndpoints() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(claims("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
        for (String path : List.of("login", "refresh", "logout")) {
            mvc.perform(
                            post("/api/v1/auth/" + path)
                                    .header("Authorization", bearer(claims("ADMIN"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void ignoresTokenPhoneAndDoesNotDeleteAnotherUsersAddress() throws Exception {
        UserAddress address =
                addresses.save(
                        UserAddress.builder()
                                .user(other)
                                .recipientName("Other")
                                .phoneNumber("0900000000")
                                .shippingAddress("HN")
                                .build());
        mvc.perform(
                        delete("/api/v1/addresses/" + address.getId())
                                .header("Authorization", bearer(claims("USER"))))
                .andExpect(status().isNotFound());
        assertTrue(addresses.existsById(address.getId()));
        UserAddress mine =
                addresses.save(
                        UserAddress.builder()
                                .user(owner)
                                .recipientName("Owner")
                                .phoneNumber("0900000000")
                                .shippingAddress("HCM")
                                .build());
        mvc.perform(
                        delete("/api/v1/addresses/" + mine.getId())
                                .header("Authorization", bearer(claims("USER"))))
                .andExpect(status().isNoContent());
        assertFalse(addresses.existsById(mine.getId()));
    }

    private JWTClaimsSet.Builder claims(String role) {
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(owner.getKeycloakUserId())
                .audience("shopapp-api")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .claim("preferred_username", other.getPhoneNumber())
                .claim("phone_number", other.getPhoneNumber())
                .claim("realm_access", Map.of("roles", List.of(role)));
    }

    private String bearer(JWTClaimsSet.Builder claims) throws JOSEException {
        return "Bearer " + signed(claims, KEY);
    }

    private String signed(JWTClaimsSet.Builder claims, RSAKey key) throws JOSEException {
        SignedJWT jwt =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
                        claims.build());
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
}
