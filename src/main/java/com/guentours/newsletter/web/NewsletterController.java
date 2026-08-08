package com.guentours.newsletter.web;

import com.guentours.newsletter.NewsletterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/newsletter")
public class NewsletterController {

    private final NewsletterService newsletterService;

    public NewsletterController(NewsletterService newsletterService) {
        this.newsletterService = newsletterService;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@Valid @RequestBody NewsletterSubscribeRequest request) {
        newsletterService.subscribe(request.email().trim().toLowerCase(), request.source());
        return ResponseEntity.ok().build();
    }
}
