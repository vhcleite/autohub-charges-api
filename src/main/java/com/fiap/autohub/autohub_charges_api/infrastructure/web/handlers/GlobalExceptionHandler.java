package com.fiap.autohub.autohub_charges_api.infrastructure.web.handlers; // Pacote sugerido

import com.fiap.autohub.autohub_charges_api.domain.exceptions.ChargeNotFoundException;
import com.fiap.autohub.autohub_charges_api.domain.exceptions.PaymentGatewayException;
import com.fiap.autohub.autohub_charges_api.infrastructure.web.dtos.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

/**
 * Controller Advice para tratamento global de exceções na API de Cobranças.
 * Captura exceções específicas e genéricas, retornando um ErrorResponse padronizado.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handler para ChargeNotFoundException (Recurso não encontrado).
     *
     * @param ex      A exceção capturada.
     * @param request A requisição HTTP.
     * @return ResponseEntity com status 404 Not Found.
     */
    @ExceptionHandler(ChargeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChargeNotFoundException(ChargeNotFoundException ex, HttpServletRequest request) {
        log.warn("Charge not found exception: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handler para PaymentGatewayException (Erro ao interagir com gateway).
     *
     * @param ex      A exceção capturada.
     * @param request A requisição HTTP.
     * @return ResponseEntity com status 502 Bad Gateway ou 500 Internal Server Error.
     */
    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ErrorResponse> handlePaymentGatewayException(PaymentGatewayException ex, HttpServletRequest request) {
        log.error("Payment gateway exception: {}", ex.getMessage(), ex); // Logar com stack trace
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_GATEWAY.value(), // 502 indica erro no upstream/gateway
                HttpStatus.BAD_GATEWAY.getReasonPhrase(),
                "Error communicating with payment gateway: " + ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
    }

    /**
     * Handler para erros de validação de DTOs (@Valid).
     *
     * @param ex      A exceção MethodArgumentNotValidException capturada.
     * @param request A requisição HTTP.
     * @return ResponseEntity com status 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        // Coleta todas as mensagens de erro de validação
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation error: {}", errors);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed: " + errors,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handler genérico para outras exceções não tratadas especificamente.
     *
     * @param ex      A exceção capturada.
     * @param request A requisição HTTP.
     * @return ResponseEntity com status 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex); // Logar com stack trace
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred: " + ex.getMessage(), // Evitar expor detalhes internos em produção
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
