package com.kafsys.identity.config;

import com.kafsys.identity.entity.Role;
import com.kafsys.identity.entity.User;
import com.kafsys.identity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAdminUser();
        seedOperatorUser();
        seedCustomerUsers();
    }

    private void seedAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(new User(
                    "admin",
                    "admin@kafsys.com",
                    passwordEncoder.encode("Admin@Kafsys1"),
                    Role.ROLE_ADMIN
            ));
            log.info("Seeded admin user");
        }
    }

    private void seedOperatorUser() {
        if (!userRepository.existsByUsername("operator")) {
            userRepository.save(new User(
                    "operator",
                    "operator@kafsys.com",
                    passwordEncoder.encode("Operator@Kafsys1"),
                    Role.ROLE_OPERATOR
            ));
            log.info("Seeded operator user");
        }
    }

    private void seedCustomerUsers() {
        String[][] customers = {
                {"alice.chen", "alice.chen@example.com", "Alice@Kafsys1"},
                {"bob.taylor", "bob.taylor@example.com", "Bob@Kafsys1"},
                {"carol.smith", "carol.smith@example.com", "Carol@Kafsys1"}
        };

        for (String[] c : customers) {
            if (!userRepository.existsByUsername(c[0])) {
                userRepository.save(new User(c[0], c[1], passwordEncoder.encode(c[2]), Role.ROLE_CUSTOMER));
            }
        }
        log.info("Seeded customer users");
    }
}
