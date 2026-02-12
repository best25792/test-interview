package com.example.paymentservice.domain.model;

import com.example.paymentservice.entity.OutboxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventOutbox {
    private Long id;
    private String eventType;
    private String eventData;
    private OutboxStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private Integer retryCount ;
    private String errorMessage;
    private String traceparent;
}
