package com.guentours.provider;

import com.guentours.shared.exception.BusinessException;

/** Raised by {@link TravelProviderClient#verifyFlightPrice}/{@code verifyHotelPrice} when the provider
 *  reports the quoted seats/rooms are no longer available. */
public class OfferExpiredException extends BusinessException {

    public OfferExpiredException(String message) {
        super(message);
    }
}
