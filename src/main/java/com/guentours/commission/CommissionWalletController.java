package com.guentours.commission;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only read access to the commission wallet (see {@code /api/admin/**} in SecurityConfig). */
@RestController
@RequestMapping("/api/admin/commission")
public class CommissionWalletController {

    private final CommissionWalletService walletService;

    public CommissionWalletController(CommissionWalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/wallet")
    public ResponseEntity<CommissionWalletBalanceResponse> wallet() {
        return ResponseEntity.ok(new CommissionWalletBalanceResponse(
                walletService.totalBalance(), walletService.entryCount()));
    }
}
