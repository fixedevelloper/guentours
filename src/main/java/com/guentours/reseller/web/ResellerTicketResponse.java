package com.guentours.reseller.web;

import java.util.List;

public record ResellerTicketResponse(
        String bookingId,
        String status,
        String pnrCode,
        List<String> eTicketNumbers
) {}