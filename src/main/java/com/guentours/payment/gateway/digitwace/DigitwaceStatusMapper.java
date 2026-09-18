package com.guentours.payment.gateway.digitwace;

import com.guentours.payment.gateway.ChargeStatus;

import java.util.Locale;

/**
 * The exact status vocabulary WacePay PayIn returns isn't published on its (client-rendered) API
 * reference pages - matched case-insensitively against values used by comparable wallet/mobile-money
 * gateways (e.g. Flutterwave: "successful"/"failed"/"pending"). An unrecognized value maps to
 * PENDING rather than FAILED: wrongly failing a payment that actually went through needs a manual
 * refund to fix, whereas leaving it PENDING just waits for the next status check to resolve it.
 */
final class DigitwaceStatusMapper {

    private DigitwaceStatusMapper() {}

    static ChargeStatus map(String status) {
        if (status == null) {
            return ChargeStatus.PENDING;
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "success", "successful", "succeeded", "completed", "approved" -> ChargeStatus.SUCCEEDED;
            case "failed", "error", "declined", "cancelled", "canceled", "rejected" -> ChargeStatus.FAILED;
            default -> ChargeStatus.PENDING;
        };
    }
}
