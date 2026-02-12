package com.example.paymentservice.service.outbox;

/**
 * Contract for outbox event consumers following the event consumer pattern.
 * Each implementation handles exactly one event type: deserialize payload and publish (e.g. to Kafka).
 * <p>
 * Register new event types by adding a new {@link org.springframework.stereotype.Component}
 * that implements this interface; the processor will discover it via dependency injection.
 */
public interface OutboxEventConsumer {

    /**
     * The event type this consumer handles (e.g. "PaymentCreatedEvent").
     * Must match the type stored in the outbox table.
     */
    String getEventType();

    /**
     * Consume the outbox event: deserialize {@code eventData} and publish with trace context.
     *
     * @param eventData   JSON payload from the outbox record
     * @param traceparent traceparent header for distributed tracing (may be null)
     */
    void consume(String eventData, String traceparent) throws Exception;
}
