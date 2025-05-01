package com.fiap.autohub.autohub_charges_api.domain.ports.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída que abstrai a interação com o Gateway de Pagamento (real ou mock).
 */
public interface PaymentGatewayPort {

    /**
     * Tenta criar uma nova cobrança no gateway.
     *
     * @param saleId ID da venda associada.
     * @param amount Valor a ser cobrado.
     * @return Um Optional contendo os detalhes da cobrança criada (incluindo ID do gateway e código de pagamento)
     * se for bem-sucedido, ou vazio em caso de falha na criação.
     */
    Optional<PaymentGatewayResponse> createCharge(UUID saleId, BigDecimal amount);

    /**
     * Tenta cancelar uma cobrança existente no gateway.
     * Útil para compensação se o processo falhar após a criação da cobrança.
     *
     * @param gatewayChargeId O ID da cobrança no gateway a ser cancelada.
     * @return true se o cancelamento foi bem-sucedido (ou se o gateway não suporta/não precisa de cancelamento explícito), false caso contrário.
     */
    boolean cancelCharge(String gatewayChargeId); // <<< NOVO MÉTODO

    /**
     * Record para encapsular a resposta da criação da cobrança no gateway.
     */
    record PaymentGatewayResponse(
            String gatewayChargeId, // ID gerado pelo gateway
            String paymentCode,     // Código/Link para o usuário pagar
            java.time.OffsetDateTime expiresAt // Timestamp de expiração
            // Adicionar outros campos relevantes retornados pelo gateway, se necessário
    ) {
    }
}
