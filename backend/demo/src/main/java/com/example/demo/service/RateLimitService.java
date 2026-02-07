package com.example.demo.service;

import com.example.demo.entity.QRCode;
import com.example.demo.exception.PaymentException;
import com.example.demo.repository.qr.QRCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final QRCodeRepository qrCodeRepository;
    
    private static final int MAX_QR_CODES_PER_MINUTE = 5;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 1;

    /**
     * Check if user can create a new QR code based on rate limit
     * Note: Since QR codes are in a separate database, we can't query by customerId.
     * This method now checks by paymentId, which should be called per payment.
     * @param paymentId Payment ID
     * @throws PaymentException if rate limit exceeded
     */
    public void checkRateLimit(Long paymentId) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(RATE_LIMIT_WINDOW_MINUTES);
        
        // Count QR codes created in the last minute for this payment
        // Note: In a real microservices setup, rate limiting would be handled by the payment service
        // or a dedicated rate limiting service that has access to both databases
        List<QRCode> recentQRCodes = qrCodeRepository.findByPaymentId(paymentId)
                .stream()
                .filter(qr -> qr.getCreatedAt().isAfter(oneMinuteAgo))
                .toList();
        
        if (recentQRCodes.size() >= MAX_QR_CODES_PER_MINUTE) {
            throw new PaymentException(
                String.format("Rate limit exceeded. Maximum %d QR codes allowed per minute. Please try again later.", 
                    MAX_QR_CODES_PER_MINUTE)
            );
        }
        
        log.debug("Rate limit check passed for paymentId: {}. QR codes in last minute: {}", paymentId, recentQRCodes.size());
    }
}
