package com.fiap.autohub.autohub_charges_api.infrastructure.adapters.out; // Ajuste o pacote

import com.fiap.autohub.autohub_charges_api.domain.ports.out.PaymentGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação MOCK da porta do Gateway de Pagamento para desenvolvimento/teste local.
 * Simula a criação e cancelamento de uma cobrança.
 */
@Component
public class MockPaymentGatewayAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGatewayAdapter.class);

    @Override
    public Optional<PaymentGatewayResponse> createCharge(UUID saleId, BigDecimal amount) {
        log.info("[MOCK] Simulating charge creation for saleId: {} and amount: {}", saleId, amount);

        String mockChargeId = "MOCK-CHARGE-" + UUID.randomUUID().toString().substring(0, 8);
        String mockPaymentCode = "PIX-COPIA-E-COLA-MOCK-" + mockChargeId;
        OffsetDateTime mockExpiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(15);

        log.info("[MOCK] Simulated charge created with ID: {}, PaymentCode: {}, ExpiresAt: {}",
                mockChargeId, mockPaymentCode, mockExpiresAt);

        PaymentGatewayResponse response = new PaymentGatewayResponse(
                mockChargeId,
                mockPaymentCode,
                mockExpiresAt
        );

        return Optional.of(response);

        // Para simular falha:
        // log.error("[MOCK] Simulating charge creation failure for saleId: {}", saleId);
        // return Optional.empty();
    }

    @Override
    public boolean cancelCharge(String gatewayChargeId) { // <<< IMPLEMENTAÇÃO DO NOVO MÉTODO
        log.warn("[MOCK] Simulating charge cancellation attempt for gatewayChargeId: {}", gatewayChargeId);
        log.info("[MOCK] Charge {} considered cancelled/compensated.", gatewayChargeId);
        return true;
    }
}
