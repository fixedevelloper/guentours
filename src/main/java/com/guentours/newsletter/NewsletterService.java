package com.guentours.newsletter;

import com.guentours.newsletter.domain.NewsletterSubscriber;
import com.guentours.newsletter.domain.NewsletterSubscriberRepository;
import com.guentours.newsletter.event.NewsletterSubscribedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NewsletterService {

    private final NewsletterSubscriberRepository repository;
    private final ApplicationEventPublisher events;

    public NewsletterService(NewsletterSubscriberRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    /** Idempotent: subscribing an already-known email is a silent no-op - no error, no duplicate row,
     *  no second confirmation email. */
    @Transactional
    public void subscribe(String email, String source) {
        if (repository.findByEmailIgnoreCase(email).isPresent()) {
            return;
        }
        repository.save(new NewsletterSubscriber(email, source));
        events.publishEvent(new NewsletterSubscribedEvent(email, source));
    }
}
