package com.guentours.user.service;

import com.guentours.notification.service.PartnerWelcomeNotifier;
import com.guentours.shared.exception.BusinessException;
import com.guentours.shared.exception.NotFoundException;

import com.guentours.user.domain.Role;
import com.guentours.user.domain.User;
import com.guentours.user.domain.UserRepository;
import com.guentours.user.event.UserAutoProvisionedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
public class UserService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final TemporaryPasswordCache temporaryPasswordCache;
    private final PartnerWelcomeNotifier notifier;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       ApplicationEventPublisher events, TemporaryPasswordCache temporaryPasswordCache,
                       PartnerWelcomeNotifier notifier) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.temporaryPasswordCache = temporaryPasswordCache;
        this.notifier = notifier;
    }

    @Transactional
    public User registerNewUser(String email, String fullName, String phone, String rawPassword) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("An account already exists for " + email);
        }
        User user = new User(email,passwordEncoder.encode(rawPassword), fullName, Role.CUSTOMER, null);
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
            User user = new User(email, passwordEncoder.encode(temporaryPassword), fullName, Role.CUSTOMER,null);
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

    @Transactional
    public User createPartnerAccount(String partnerId, String email, String contactName,
                                     String companyName, String partnerTypeName) {
        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            // Rejeu d'événement (ex: retry Spring Modulith après échec d'envoi mail) — idempotent.
            return existing.get();
        }

        //String tempPassword = generateTempPassword();
        String tempPassword = "123456789";
        Role role = PartnerRoleMapper.fromPartnerType(partnerTypeName);

        User user = new User(email, passwordEncoder.encode(tempPassword), contactName, role, partnerId);
        user.setMustChangePassword(true);

        User saved = userRepository.save(user);

        notifier.sendPartnerWelcomeEmail(email, contactName, companyName, tempPassword);

        return saved;
    }

    private String generateTempPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}