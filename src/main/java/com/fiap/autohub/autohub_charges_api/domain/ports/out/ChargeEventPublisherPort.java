package com.fiap.autohub.autohub_charges_api.domain.ports.out;

import com.fiap.autohub.autohub_charges_api.domain.events.*;

/**
 * Porta de saída para publicação de eventos relacionados a Cobranças.
 */
public interface ChargeEventPublisherPort {

    void publishChargeCreated(ChargeCreatedEvent event);

    void publishChargeCreationFailed(ChargeCreationFailedEvent event);

    void publishPaymentCompleted(PaymentCompletedEvent event);

    void publishPaymentFailed(PaymentFailedEvent event);

    void publishChargeExpired(ChargeExpiredEvent event);
}
