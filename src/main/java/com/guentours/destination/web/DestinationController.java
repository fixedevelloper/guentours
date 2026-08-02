package com.guentours.destination.web;

import com.guentours.destination.FeaturedDestinationResponse;
import com.guentours.destination.FeaturedDestinationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public read-only endpoint backing the homepage's "popular destinations" section. */
@RestController
@RequestMapping("/api/destinations")
public class DestinationController {

    private final FeaturedDestinationService service;

    public DestinationController(FeaturedDestinationService service) {
        this.service = service;
    }

    @GetMapping("/featured")
    public List<FeaturedDestinationResponse> getFeatured() {
        return service.getActive().stream().map(FeaturedDestinationResponse::from).toList();
    }
}
