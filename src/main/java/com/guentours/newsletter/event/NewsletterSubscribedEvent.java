package com.guentours.newsletter.event;

/** Fired once per newly-subscribed email (never re-fired on a repeat signup of the same address). */
public record NewsletterSubscribedEvent(String email, String source) {
}
