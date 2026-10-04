package com.project.shopapp.configuration;

import com.project.shopapp.model.Role;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.RoleRepository;
import com.project.shopapp.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminAccountInitializer implements ApplicationRunner {
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
                        .findByName("ADMIN")
                        .orElseThrow(() -> new IllegalStateException("ADMIN role was not found"));
        userRepository.save(
                new User(
                        "ShopApp Administrator",
                        phoneNumber.trim(),
                        "System",
                        passwordEncoder.encode(password),
                        null,
                        adminRole));
    }
}
