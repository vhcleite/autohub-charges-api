package com.fiap.autohub.autohub_charges_api.domain.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Evento: Cobrança Criada (Publicado pela Charges API).
 */
public record ChargeCreatedEvent(
        @JsonProperty("event_id") UUID eventId,
        @JsonProperty("event_type") String eventType, // "ChargeCreated"
        @JsonProperty("timestamp") OffsetDateTime timestamp,
        @JsonProperty("source") String source, // "charges-api"
        @JsonProperty("data") EventData data
) {
    public ChargeCreatedEvent(UUID saleId, UUID vehicleId, String chargeId, String paymentCode, BigDecimal amount, OffsetDateTime expiresAt) {
        this(
                UUID.randomUUID(),
                "ChargeCreated",
                OffsetDateTime.now(ZoneOffset.UTC),
                "charges-api",
                new EventData(saleId, vehicleId, chargeId, paymentCode, amount, expiresAt)
        );
    }

    public record EventData(
            @JsonProperty("sale_id") UUID saleId,
            @JsonProperty("vehicle_id") UUID vehicleId,
            @JsonProperty("charge_id") String chargeId,
            @JsonProperty("payment_code") String paymentCode, // Ex: PIX Code, URL
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("expires_at") OffsetDateTime expiresAt
    ) {
    }
}