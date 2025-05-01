package com.fiap.autohub.autohub_charges_api.domain.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Evento: Pagamento Concluído (Publicado pela Charges API).
 */
public record PaymentCompletedEvent(
        @JsonProperty("event_id") UUID eventId,
        @JsonProperty("event_type") String eventType, // "PaymentCompleted"
        @JsonProperty("timestamp") OffsetDateTime timestamp,
        @JsonProperty("source") String source, // "charges-api"
        @JsonProperty("data") EventData data
) {
    public PaymentCompletedEvent(UUID saleId, UUID vehicleId, String chargeId, OffsetDateTime paidAt) {
        this(
                UUID.randomUUID(),
                "PaymentCompleted",
                OffsetDateTime.now(ZoneOffset.UTC),
                "charges-api",
                new EventData(saleId, vehicleId, chargeId, paidAt)
        );
    }

    public record EventData(
            @JsonProperty("sale_id") UUID saleId,
            @JsonProperty("vehicle_id") UUID vehicleId,
            @JsonProperty("charge_id") String chargeId,
            @JsonProperty("paid_at") OffsetDateTime paidAt
    ) {
    }
}