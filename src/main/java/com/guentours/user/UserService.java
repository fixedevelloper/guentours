package com.guentours.user;

import com.guentours.shared.exception.BusinessException;
import com.guentours.shared.exception.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final TemporaryPasswordCache temporaryPasswordCache;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        ApplicationEventPublisher events, TemporaryPasswordCache temporaryPasswordCache) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.temporaryPasswordCache = temporaryPasswordCache;
    }

    @Transactional
    public User registerNewUser(String email, String fullName, String phone, String rawPassword) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("An account already exists for " + email);
        }
        User user = new User(email, fullName, phone, passwordEncoder.encode(rawPassword), false);
        return userRepository.save(user);
    }

    /**
     * Called from the checkout flow: returns the existing account for the given email,
     * or transparently creates one with a generated password and emails the credentials.
     */
    @Transactional
    public User findOrCreateForCheckout(String email, String fullName, String phone) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            String temporaryPassword = PasswordGenerator.generate();
            User user = new User(email, fullName, phone, passwordEncoder.encode(temporaryPassword), true);
            User saved = userRepository.save(user);
            temporaryPasswordCache.store(saved.getId(), temporaryPassword);
            events.publishEvent(new UserAutoProvisionedEvent(saved.getId()));
            return saved;
        });
    }

    public User getById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    public User getByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));
    }

    /** Single-use: the notification module calls this once to email the credentials, then it's gone. */
    public Optional<String> consumeTemporaryPassword(String userId) {
        return temporaryPasswordCache.consume(userId);
    }
}
