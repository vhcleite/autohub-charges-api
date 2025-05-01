package com.fiap.autohub.autohub_charges_api.domain.entities;

/**
 * Enum representando os possíveis status de uma Cobrança.
 */
public enum ChargeStatus {
    PENDING,  // Cobrança criada, aguardando pagamento
    PAID,     // Pagamento confirmado
    FAILED,   // Falha no pagamento ou na criação da cobrança no gateway
    EXPIRED,  // Cobrança expirou antes do pagamento
    ERROR     // Estado de erro inesperado
}
