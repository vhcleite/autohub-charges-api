package com.fiap.autohub.autohub_charges_api.domain.exceptions;

/**
 * Exceção para quando uma cobrança não é encontrada.
 */
public class ChargeNotFoundException extends RuntimeException {

    public ChargeNotFoundException(String chargeId) {
        super("Charge not found with id: " + chargeId);
    }

    public ChargeNotFoundException(java.util.UUID saleId) {
        super("Charge not found for sale id: " + saleId);
    }
}