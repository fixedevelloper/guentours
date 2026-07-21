package com.guentours.notification;


public interface PartnerWelcomeNotifier {
    void sendPartnerWelcomeEmail(String email, String contactName, String companyName, String tempPassword);
}
