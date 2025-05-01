package com.fiap.autohub.autohub_charges_api.infrastructure.adapters.out; // Ajuste o pacote se necessário

import com.fiap.autohub.autohub_charges_api.domain.entities.Charge;
import com.fiap.autohub.autohub_charges_api.domain.ports.out.NotificationServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementação MOCK da porta de serviço de notificação.
 * Usada em ambientes locais e de teste para evitar envios reais de e-mail/SMS.
 * Apenas regista um log indicando que a notificação seria enviada.
 */
@Component
public class MockNotificationServiceAdapter implements NotificationServicePort {

    private static final Logger log = LoggerFactory.getLogger(MockNotificationServiceAdapter.class);

    /**
     * Simula o envio de instruções de pagamento.
     * Em um ambiente real, este método chamaria um serviço como AWS SES ou SendGrid.
     *
     * @param charge     A cobrança criada contendo os detalhes necessários.
     * @param buyerEmail O e-mail do comprador.
     */
    @Override
    public void sendPaymentInstructions(Charge charge, String buyerEmail) {
        log.info("[MOCK] >>> Sending payment instructions to email '{}' for chargeId '{}' with payment code '{}'",
                buyerEmail, charge.getChargeId(), charge.getPaymentCode());
    }
}
