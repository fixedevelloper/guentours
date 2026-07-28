package com.guentours.payment.gateway.flutterwave;

class UnsupportedMobileMoneyCountryException extends RuntimeException {
    UnsupportedMobileMoneyCountryException(String iso2) {
        super("Mobile money non disponible pour le pays : " + iso2);
    }
}