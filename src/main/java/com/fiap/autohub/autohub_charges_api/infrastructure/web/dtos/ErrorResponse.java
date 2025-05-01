package com.fiap.autohub.autohub_charges_api.infrastructure.web.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO padrão para respostas de erro da API.
 */
@Schema(description = "Estrutura padrão para respostas de erro")
public record ErrorResponse(
        @Schema(description = "Código de status HTTP", example = "404")
        int status,

        @Schema(description = "Tipo do erro", example = "Not Found")
        String error,

        @Schema(description = "Mensagem detalhada do erro", example = "Charge not found with id: ch_123")
        String message,

        @Schema(description = "Caminho da requisição que causou o erro", example = "/charges/ch_123")
        String path
) {
}