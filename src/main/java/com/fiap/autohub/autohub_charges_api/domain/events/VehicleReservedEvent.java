// Pacote: com.fiap.autohub.autohub_charges_api.domain.events

package com.fiap.autohub.autohub_charges_api.domain.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Evento consumido: Veículo Reservado.
 * PRECISA incluir o preço para a API de Cobranças funcionar sem chamadas síncronas.
 */
public record VehicleReservedEvent(
        @JsonProperty("event_id") UUID eventId,
        @JsonProperty("event_type") String eventType, // "VehicleReserved"
        @JsonProperty("timestamp") OffsetDateTime timestamp,
        @JsonProperty("source") String source, // "vehicles-api"
        @JsonProperty("data") EventData data
) {
    /**
     * Dados específicos para o evento VehicleReserved.
     * IMPORTANTE: Incluir o preço aqui.
     */
    public record EventData(
            @JsonProperty("sale_id") UUID saleId,
            @JsonProperty("vehicle_id") UUID vehicleId,
            @JsonProperty("price") BigDecimal price
    ) {
    }

    // Métodos utilitários
    public UUID getSaleId() {
        return (data != null) ? data.saleId() : null;
    }

    public UUID getVehicleId() {
        return (data != null) ? data.vehicleId() : null;
    }

    public BigDecimal getPrice() {
        return (data != null) ? data.price() : null;
    }
}
