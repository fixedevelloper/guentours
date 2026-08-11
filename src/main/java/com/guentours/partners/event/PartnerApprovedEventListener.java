package com.guentours.partners.event;

import com.guentours.user.service.UserService;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Lives in {@code partners} (the event's own module) rather than {@code user}: a listener always
 * depends on the module publishing the event it reacts to, so keeping it in {@code user} made
 * {@code user} depend on {@code partners} - closing a Modulith cycle with
 * {@code partners -> security -> user}. Depending on {@code UserService} from here instead
 * ({@code partners -> user}) is a dead end for cycles: {@code user} has no other outgoing
 * module dependency once this listener moves out of it.
 */
@Component
public class PartnerApprovedEventListener {

    private final UserService userService;

    public PartnerApprovedEventListener(UserService userService) {
        this.userService = userService;
    }

    @ApplicationModuleListener
    void on(PartnerApprovedEvent event) {
        userService.createPartnerAccount(
                event.partnerId(),
                event.email(),
                event.contactName(),
                event.companyName(),
                event.partnerType()
        );
    }
}
