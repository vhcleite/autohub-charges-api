package com.fiap.autohub.autohub_charges_api.domain.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Evento: Cobrança Expirada (Publicado pela Charges API ou Timeout Lambda).
 */
public record ChargeExpiredEvent(
        @JsonProperty("event_id") UUID eventId,
        @JsonProperty("event_type") String eventType, // "ChargeExpired"
        @JsonProperty("timestamp") OffsetDateTime timestamp,
        @JsonProperty("source") String source, // "charges-api" ou "timeout-lambda"
        @JsonProperty("data") EventData data
) {
    public ChargeExpiredEvent(UUID saleId, String chargeId) {
        this(
                UUID.randomUUID(),
                "ChargeExpired",
                OffsetDateTime.now(ZoneOffset.UTC),
                "charges-api", // Ou a fonte real
                new EventData(saleId, chargeId)
        );
    }

    public record EventData(
            @JsonProperty("sale_id") UUID saleId,
            @JsonProperty("charge_id") String chargeId
    ) {
    }
}