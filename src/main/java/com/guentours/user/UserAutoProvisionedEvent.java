package com.guentours.user;

/**
 * Published exactly once, right after a new account is created transparently during
 * checkout for an email address that had never booked before. Deliberately carries
 * only the user id - the generated temporary password is never put on the event bus
 * (Spring Modulith durably persists event payloads in the {@code event_publication}
 * table, which is the wrong place for a plaintext credential) and must instead be
 * read once via {@link UserService#consumeTemporaryPassword(String)}.
 */
public record UserAutoProvisionedEvent(String userId) {
}
