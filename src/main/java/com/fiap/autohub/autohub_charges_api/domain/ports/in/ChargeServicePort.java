package com.fiap.autohub.autohub_charges_api.domain.ports.in;

import com.fiap.autohub.autohub_charges_api.domain.entities.Charge;
import com.fiap.autohub.autohub_charges_api.domain.events.VehicleReservedEvent; // Evento consumido

import java.util.Map;
import java.util.UUID;

/**
 * Porta de entrada para os casos de uso da API de Cobranças.
 */
public interface ChargeServicePort {

    /**
     * Processa o evento de veículo reservado para criar uma nova cobrança.
     * @param event Evento contendo dados da venda e do veículo.
     */
    void processVehicleReservation(VehicleReservedEvent event);

    /**
     * Processa o callback do gateway de pagamento.
     * @param chargeId ID da cobrança sendo atualizada.
     * @param paymentStatus Status recebido do gateway (ex: "PAID", "FAILED").
     * @param details Detalhes adicionais do gateway (opcional).
     * @return A entidade Charge atualizada.
     */
    Charge handlePaymentCallback(String chargeId, String paymentStatus, Map<String, Object> details);

    /**
     * Processa o evento de timeout para uma cobrança.
     * Verifica se a cobrança ainda está pendente e a marca como expirada se for o caso.
     * @param saleId ID da venda associada.
     * @param chargeId ID da cobrança a ser verificada.
     */
    void handleChargeTimeout(UUID saleId, String chargeId);

    /**
     * Busca uma cobrança pelo seu ID.
     * @param chargeId O ID da cobrança.
     * @return A entidade Charge.
     * @throws ChargeNotFoundException se a cobrança não for encontrada.
     */
    Charge findChargeById(String chargeId);

    /**
     * Busca uma cobrança pelo ID da venda.
     * @param saleId O ID da venda.
     * @return A entidade Charge associada.
     * @throws ChargeNotFoundException se a cobrança não for encontrada.
     */
    Charge findChargeBySaleId(UUID saleId);

}
