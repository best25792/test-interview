package com.example.walletservice.controller;

import com.example.walletservice.dto.request.CreateHoldRequest;
import com.example.walletservice.entity.Hold;
import com.example.walletservice.service.HoldService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/holds")
@RequiredArgsConstructor
public class HoldController {

    private final HoldService holdService;

    @PostMapping
    public ResponseEntity<Hold> createHold(@RequestBody CreateHoldRequest request) {
        Hold hold = holdService.createHold(
                request.getUserId(),
                request.getPaymentId(),
                request.getAmount(),
                request.getReason()
        );
        return new ResponseEntity<>(hold, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/capture")
    public ResponseEntity<Void> captureHold(@PathVariable Long id, @RequestBody CaptureHoldRequest request) {
        holdService.captureHold(id, request.getReason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<Void> releaseHold(@PathVariable Long id, @RequestBody ReleaseHoldRequest request) {
        holdService.releaseHold(id, request.getReason());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<Hold> getHoldByPaymentId(@PathVariable Long paymentId) {
        Hold hold = holdService.getHoldByPaymentId(paymentId);
        return ResponseEntity.ok(hold);
    }

    @GetMapping("/users/{userId}/available-balance")
    public ResponseEntity<AvailableBalanceResponse> getAvailableBalance(@PathVariable Long userId) {
        BigDecimal availableBalance = holdService.getAvailableBalance(userId);
        return ResponseEntity.ok(new AvailableBalanceResponse(userId, availableBalance));
    }

    @Data
    static class CreateHoldRequest {
        private Long userId;
        private Long paymentId;
        private BigDecimal amount;
        private String reason;
    }

    @Data
    static class CaptureHoldRequest {
        private String reason;
    }

    @Data
    static class ReleaseHoldRequest {
        private String reason;
    }

    record AvailableBalanceResponse(Long userId, BigDecimal availableBalance) {}
}
