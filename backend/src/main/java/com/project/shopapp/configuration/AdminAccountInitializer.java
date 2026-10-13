package com.project.shopapp.configuration;

import com.project.shopapp.components.TransactionLog;
import com.project.shopapp.model.Role;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.RoleRepository;
import com.project.shopapp.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class AdminAccountInitializer implements ApplicationRunner {
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ADMIN_NAME = "ShopApp Administrator";
    private static final String SYSTEM_SOURCE = "System";
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String phoneNumber;
    private final String password;

    public AdminAccountInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.phone-number:}") String phoneNumber,
            @Value("${app.admin.password:}") String password) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.phoneNumber = phoneNumber;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (phoneNumber.isBlank()
                || password.isBlank()
                || userRepository.existsByPhoneNumber(phoneNumber)) {
            return;
        }
        Role adminRole =
                roleRepository
                        .findByName(ADMIN_ROLE)
                        .orElseThrow(() -> new IllegalStateException("ADMIN role was not found"));
        User admin =
                User.builder()
                        .fullName(ADMIN_NAME)
                        .phoneNumber(phoneNumber.trim())
                        .address(SYSTEM_SOURCE)
                        .password(passwordEncoder.encode(password))
                        .role(adminRole)
                        .build();
        User saved = userRepository.save(admin);
        TransactionLog.info(log, "Bootstrap administrator created userId={}", saved.getId());
    }
}
