package com.fiap.autohub.autohub_charges_api.infrastructure.web.dtos;

import com.fiap.autohub.autohub_charges_api.domain.entities.ChargeStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO (Data Transfer Object) para representar a resposta de uma Cobrança.
 */
@Schema(description = "Representa os detalhes de uma cobrança")
public record ChargeResponseDto(
        @Schema(description = "ID único da cobrança (gerado pelo gateway ou API)", example = "ch_mock_123abc")
        String chargeId,

        @Schema(description = "ID da venda associada a esta cobrança", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
        UUID saleId,

        @Schema(description = "Valor da cobrança", example = "55000.90")
        BigDecimal amount,

        @Schema(description = "Status atual da cobrança", example = "PENDING")
        ChargeStatus status,

        @Schema(description = "Código ou link para pagamento (ex: PIX Copia e Cola, URL)", example = "PIX-MOCK-123")
        String paymentCode,

        @Schema(description = "Detalhes adicionais retornados pelo gateway de pagamento")
        Map<String, Object> gatewayDetails, // Usando Object para flexibilidade no DTO

        @Schema(description = "Data e hora de criação da cobrança")
        OffsetDateTime createdAt,

        @Schema(description = "Data e hora da última atualização da cobrança")
        OffsetDateTime updatedAt,

        @Schema(description = "Data e hora em que a cobrança foi paga (se aplicável)")
        OffsetDateTime paidAt,

        @Schema(description = "Data e hora em que a cobrança expira (se aplicável)")
        OffsetDateTime expiresAt,

        @Schema(description = "Motivo da falha (se aplicável)", example = "Saldo insuficiente")
        String failureReason
) {
}