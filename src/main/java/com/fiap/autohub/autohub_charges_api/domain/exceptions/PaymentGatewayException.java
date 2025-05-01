package com.fiap.autohub.autohub_charges_api.domain.exceptions;

/**
 * Exceção para falha ao interagir com o gateway de pagamento.
 */
public class PaymentGatewayException extends RuntimeException {
    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
