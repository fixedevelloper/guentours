package com.guentours.user.event;

import com.guentours.partners.event.PartnerApprovedEvent;
import com.guentours.user.service.UserService;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

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
                event.partnerType().name()
        );
    }
}
