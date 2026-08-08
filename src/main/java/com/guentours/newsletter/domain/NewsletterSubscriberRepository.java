package com.guentours.newsletter.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, String> {

    Optional<NewsletterSubscriber> findByEmailIgnoreCase(String email);
}
