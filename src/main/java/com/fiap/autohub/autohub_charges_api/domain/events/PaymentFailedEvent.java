package com.fiap.autohub.autohub_charges_api.domain.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Evento: Falha no Pagamento (Publicado pela Charges API).
 */
public record PaymentFailedEvent(
        @JsonProperty("event_id") UUID eventId,
        @JsonProperty("event_type") String eventType, // "PaymentFailed"
        @JsonProperty("timestamp") OffsetDateTime timestamp,
        @JsonProperty("source") String source, // "charges-api"
        @JsonProperty("data") EventData data
) {
    public PaymentFailedEvent(UUID saleId, UUID vehicleId, String chargeId, String reason) {
        this(
                UUID.randomUUID(),
                "PaymentFailed",
                OffsetDateTime.now(ZoneOffset.UTC),
                "charges-api",
                new EventData(saleId, vehicleId, chargeId, reason)
        );
    }

    public record EventData(
            @JsonProperty("sale_id") UUID saleId,
            @JsonProperty("vehicle_id") UUID vehicleId,
            @JsonProperty("charge_id") String chargeId,
            @JsonProperty("reason") String reason
    ) {
    }
}