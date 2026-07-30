package com.guentours.commission;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin-only management of the shareholders every earned commission is split across (see
 * {@code /api/admin/**} in SecurityConfig). Shareholders are a company equity split, unrelated to
 * any user/reseller/partner account.
 */
@RestController
@RequestMapping("/api/admin/commission/shareholders")
public class ShareholderController {

    private final ShareholderService shareholderService;

    public ShareholderController(ShareholderService shareholderService) {
        this.shareholderService = shareholderService;
    }

    @GetMapping
    public ResponseEntity<List<ShareholderResponse>> list() {
        var response = shareholderService.findAll().stream()
                .map(s -> ShareholderResponse.of(s, shareholderService.balanceFor(s.getId())))
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ShareholderResponse> create(@RequestBody ShareholderRequest request) {
        Shareholder shareholder = shareholderService.create(request.name(), request.percentage());
        return ResponseEntity.ok(ShareholderResponse.of(shareholder, shareholderService.balanceFor(shareholder.getId())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShareholderResponse> update(@PathVariable String id, @RequestBody ShareholderRequest request) {
        Shareholder shareholder = shareholderService.update(id, request.name(), request.percentage(), request.active());
        return ResponseEntity.ok(ShareholderResponse.of(shareholder, shareholderService.balanceFor(shareholder.getId())));
    }
}
