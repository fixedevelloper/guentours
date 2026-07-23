package com.guentours.notification.service;

public interface PartnerWelcomeNotifier {
    void sendPartnerWelcomeEmail(String email, String contactName, String companyName, String tempPassword);
}
