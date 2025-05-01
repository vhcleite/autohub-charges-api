package com.fiap.autohub.autohub_charges_api.infrastructure.web.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * DTO para a requisição de callback simulado do gateway de pagamento.
 */
@Schema(description = "Payload esperado no callback simulado do gateway de pagamento")
public record CallbackRequestDto(
        @Schema(description = "Novo status da cobrança (ex: PAID, FAILED)", example = "PAID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Status cannot be blank")
        String status,

        @Schema(description = "Detalhes adicionais do gateway (opcional)", example = "{\"gateway_message\":\"Payment approved\"}")
        Map<String, Object> details // Usando Object para flexibilidade
) {
}