package com.guentours.user;

import com.guentours.user.domain.Role;
import com.guentours.user.domain.User;
import com.guentours.user.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a default super-admin account on first boot, since there is otherwise no way into the
 * admin dashboard on a fresh install - promoting further accounts to ADMIN stays a manual role
 * update (see {@link User#promoteToAdmin()}). Idempotent: skips seeding once an account already
 * exists at the configured email, so this runs safely on every startup.
 */
@Component
class AdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String email;
    private final String password;
    private final String fullName;

    AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
                @Value("${app.admin.seed-enabled:true}") boolean enabled,
                @Value("${app.admin.email:admin@guentours.com}") String email,
                @Value("${app.admin.password:ChangeMe123!}") String password,
                @Value("${app.admin.full-name:Super Admin}") String fullName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (!enabled || userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        User admin = new User(email,passwordEncoder.encode(password), fullName, Role.ADMIN,null);
        admin.isMustChangePassword();
        userRepository.save(admin);
        log.warn("Seeded default super-admin account ({}) - change its password before going to production", email);
    }
}
