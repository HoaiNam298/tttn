package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.project.shopapp.model.User;
import com.project.shopapp.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class UserIdentityIntegrationTest {
    @Autowired private UserRepository users;

    @Test
    void savesKeycloakProfileWithoutLocalPasswordOrRole() {
        User saved = users.saveAndFlush(profile("subject-one", "0900000001"));

        assertEquals(
                saved.getId(), users.findByKeycloakUserId("subject-one").orElseThrow().getId());
        assertFalse(users.findByKeycloakUserId("different-subject").isPresent());
    }

    @Test
    void preventsTwoProfilesFromClaimingTheSameKeycloakIdentity() {
        users.saveAndFlush(profile("same-subject", "0900000001"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> users.saveAndFlush(profile("same-subject", "0900000002")));
    }

    @Test
    void rejectsMissingKeycloakIdentity() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> users.saveAndFlush(profile(null, "0900000001")));
    }

    private User profile(String subject, String phoneNumber) {
        return User.builder()
                .keycloakUserId(subject)
                .fullName("Identity test")
                .phoneNumber(phoneNumber)
                .address("Test address")
                .build();
    }
}
