package com.fiap.autohub.autohub_charges_api.infrastructure.web.controllers;

import com.fiap.autohub.autohub_charges_api.domain.entities.Charge;
import com.fiap.autohub.autohub_charges_api.domain.events.VehicleReservedEvent;
import com.fiap.autohub.autohub_charges_api.domain.exceptions.ChargeNotFoundException;
import com.fiap.autohub.autohub_charges_api.domain.ports.in.ChargeServicePort;
import com.fiap.autohub.autohub_charges_api.infrastructure.web.dtos.CallbackRequestDto;
import com.fiap.autohub.autohub_charges_api.infrastructure.web.dtos.ChargeResponseDto;
import com.fiap.autohub.autohub_charges_api.infrastructure.web.dtos.ErrorResponse;
import com.fiap.autohub.autohub_charges_api.infrastructure.web.mappers.ChargeDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Controller REST para a API de Cobranças.
 * Inclui o endpoint de callback do gateway e endpoints de teste/consulta.
 */
@RestController
@RequestMapping("/charges")
@Tag(name = "Charges Management", description = "Endpoints para gerenciamento de cobranças e callbacks")
public class ChargeController {

    private static final Logger log = LoggerFactory.getLogger(ChargeController.class);

    private final ChargeServicePort chargeService;
    private final ChargeDtoMapper mapper;

    public ChargeController(ChargeServicePort chargeService, ChargeDtoMapper mapper) {
        this.chargeService = chargeService;
        this.mapper = mapper;
    }

    @PutMapping("/callback/{chargeId}")
    @Operation(summary = "Recebe callback do Gateway de Pagamento (Simulado)", description = "Endpoint para simular o recebimento de notificações de status do gateway de pagamento.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Callback processado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChargeResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (ex: status inválido no payload)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cobrança não encontrada para o chargeId fornecido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ChargeResponseDto> handlePaymentCallback(
            @Parameter(description = "ID da cobrança retornado pelo gateway", required = true, example = "MOCK-CHARGE-XYZ")
            @PathVariable String chargeId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Payload do callback contendo o novo status", required = true,
                    content = @Content(schema = @Schema(implementation = CallbackRequestDto.class)))
            @Valid @RequestBody CallbackRequestDto callbackRequest) {

        log.info("Received payment callback for chargeId: {} with status: {}", chargeId, callbackRequest.status());
        Charge updatedCharge = chargeService.handlePaymentCallback(chargeId, callbackRequest.status(), callbackRequest.details());
        return ResponseEntity.ok(mapper.toResponseDto(updatedCharge));
    }

    @GetMapping("/sale/{saleId}")
    @Operation(summary = "Busca cobrança por ID da Venda", description = "Retorna os detalhes da cobrança associada a um ID de venda específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cobrança encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChargeResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Nenhuma cobrança encontrada para este ID de venda", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ChargeResponseDto> getChargeBySaleId(
            @Parameter(description = "ID da venda (UUID)", required = true, example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
            @PathVariable UUID saleId) {
        log.info("Received request to get charge by saleId: {}", saleId);
        Charge charge = chargeService.findChargeBySaleId(saleId);
        return ResponseEntity.ok(mapper.toResponseDto(charge));
    }

    // --- Endpoints de Teste Local ---
    // Estes endpoints chamam os mesmos métodos de serviço que os consumers SQS,
    @PostMapping("/test/process-reservation")
    @Operation(summary = "[TESTE LOCAL] Simula o recebimento do evento VehicleReserved", description = "Cria uma cobrança como se o evento VehicleReserved tivesse sido recebido via SQS.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Processamento simulado iniciado (verifique os logs e o evento ChargeCreated publicado no LocalStack SNS)"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "500", description = "Erro interno")
    })
    public ResponseEntity<String> testProcessVehicleReservation(@RequestBody VehicleReservedEvent.EventData testData) {
        log.warn("[TEST ENDPOINT] Simulating VehicleReserved event processing for saleId: {}", testData.saleId());
        // Cria o objeto de evento completo para passar ao serviço
        VehicleReservedEvent simulatedEvent = new VehicleReservedEvent(
                UUID.randomUUID(), "VehicleReserved", OffsetDateTime.now(), "test-endpoint", testData
        );
        try {
            chargeService.processVehicleReservation(simulatedEvent);
            return ResponseEntity.ok("Simulated VehicleReserved processing initiated for saleId: " + testData.saleId());
        } catch (Exception e) {
            log.error("[TEST ENDPOINT] Error simulating VehicleReserved processing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/test/process-timeout")
    @Operation(summary = "[TESTE LOCAL] Simula o recebimento de uma mensagem de timeout", description = "Verifica se uma cobrança expirou como se a mensagem tivesse chegado via SQS.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Processamento de timeout simulado concluído (verifique logs e evento ChargeExpired no LocalStack SNS)"),
            @ApiResponse(responseCode = "404", description = "Cobrança não encontrada"),
            @ApiResponse(responseCode = "500", description = "Erro interno")
    })
    public ResponseEntity<String> testProcessTimeout(@RequestParam UUID saleId, @RequestParam String chargeId) {
        log.warn("[TEST ENDPOINT] Simulating timeout check for saleId: {}, chargeId: {}", saleId, chargeId);
        try {
            chargeService.handleChargeTimeout(saleId, chargeId);
            return ResponseEntity.ok("Simulated timeout check completed for chargeId: " + chargeId);
        } catch (ChargeNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("[TEST ENDPOINT] Error simulating timeout processing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/ping")
    public String ping() {
        log.info("Ping endpoint invoked!");
        return "pong-1";
    }
}