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

    // Diagnostic second admin (see the "seulement le compte admin" prod login bug) - fixed
    // credentials, not env-configurable like the main admin above, specifically so it exists
    // identically in every environment without needing new VPS env vars. If this account logs in
    // fine in prod while admin@guentours.com doesn't, the bug is isolated to that specific DB row/
    // account rather than to admin login/AdminLayout in general. Safe to remove once diagnosed.
    private static final String DIAGNOSTIC_ADMIN_EMAIL = "admin2@guentours.com";
    private static final String DIAGNOSTIC_ADMIN_PASSWORD = "ChangeMe123!";
    private static final String DIAGNOSTIC_ADMIN_FULL_NAME = "Admin Diagnostic";

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (!enabled) {
            return;
        }
        seedOne(email, password, fullName);
        seedOne(DIAGNOSTIC_ADMIN_EMAIL, DIAGNOSTIC_ADMIN_PASSWORD, DIAGNOSTIC_ADMIN_FULL_NAME);
    }

    private void seedOne(String email, String password, String fullName) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        User admin = new User(email, passwordEncoder.encode(password), fullName, Role.ADMIN, null);
        admin.setMustChangePassword(true);
        userRepository.save(admin);
        log.warn("Seeded default super-admin account ({}) - change its password before going to production", email);
    }
}
