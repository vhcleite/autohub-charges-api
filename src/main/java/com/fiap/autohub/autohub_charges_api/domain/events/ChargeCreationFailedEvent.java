package com.fiap.autohub.autohub_charges_api.domain.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Evento: Falha na Criação da Cobrança (Publicado pela Charges API).
 */
public record ChargeCreationFailedEvent(
        @JsonProperty("event_id") UUID eventId,
        @JsonProperty("event_type") String eventType, // "ChargeCreationFailed"
        @JsonProperty("timestamp") OffsetDateTime timestamp,
        @JsonProperty("source") String source, // "charges-api"
        @JsonProperty("data") EventData data
) {
    public ChargeCreationFailedEvent(UUID saleId, UUID vehicleId, String reason) {
        this(
                UUID.randomUUID(),
                "ChargeCreationFailed",
                OffsetDateTime.now(ZoneOffset.UTC),
                "charges-api",
                new EventData(saleId, vehicleId, reason)
        );
    }

    public record EventData(
            @JsonProperty("sale_id") UUID saleId,
            @JsonProperty("vehicle_id") UUID vehicleId,
            @JsonProperty("reason") String reason
    ) {
    }
}