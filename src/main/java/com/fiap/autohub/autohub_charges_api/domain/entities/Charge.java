package com.fiap.autohub.autohub_charges_api.domain.entities;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de domínio para Cobrança. Classe mutável.
 */
public class Charge {

    private String chargeId; // Gerado pelo gateway ou internamente (String para flexibilidade)
    private UUID saleId;
    private UUID vehicleId;
    private BigDecimal amount;
    private ChargeStatus status;
    private String paymentCode; // Ex: PIX Copia e Cola, URL de pagamento
    private Map<String, Object> gatewayDetails; // Detalhes flexíveis do gateway
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime paidAt;
    private OffsetDateTime expiresAt;
    private String failureReason;

    // Construtor padrão
    public Charge() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.updatedAt = this.createdAt;
    }

    // Construtor para criação inicial
    public Charge(UUID saleId, UUID vehicleId, BigDecimal amount) {
        this();
        this.saleId = saleId;
        this.vehicleId = vehicleId;
        this.amount = amount;
        this.status = ChargeStatus.PENDING;
    }

    // Construtor completo (para mapeamento da persistência)
    public Charge(String chargeId, UUID saleId, UUID vehicleId, BigDecimal amount, ChargeStatus status,
                  String paymentCode, Map<String, Object> gatewayDetails,
                  OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime paidAt,
                  OffsetDateTime expiresAt, String failureReason) {
        this.chargeId = chargeId;
        this.vehicleId = vehicleId;
        this.saleId = saleId;
        this.amount = amount;
        this.status = status;
        this.paymentCode = paymentCode;
        this.gatewayDetails = gatewayDetails;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.paidAt = paidAt;
        this.expiresAt = expiresAt;
        this.failureReason = failureReason;
    }

    // --- Getters ---
    public String getChargeId() {
        return chargeId;
    }

    public UUID getSaleId() {
        return saleId;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public ChargeStatus getStatus() {
        return status;
    }

    public String getPaymentCode() {
        return paymentCode;
    }

    public Map<String, Object> getGatewayDetails() {
        return gatewayDetails;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    // --- Setters (campos que podem mudar) ---
    public void setChargeId(String chargeId) {
        this.chargeId = chargeId;
    } // Se gerado depois

    public void setSaleId(UUID saleId) {
        this.saleId = saleId;
    }

    public void setVehicleId(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    } // Geralmente definido no construtor

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    } // Para mapper

    public void setStatus(ChargeStatus status) {
        this.status = status;
    }

    public void setPaymentCode(String paymentCode) {
        this.paymentCode = paymentCode;
    }

    public void setGatewayDetails(Map<String, Object> gatewayDetails) {
        this.gatewayDetails = gatewayDetails;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setPaidAt(OffsetDateTime paidAt) {
        this.paidAt = paidAt;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
        // Ao setar falha, geralmente o status também muda
        if (failureReason != null && this.status != ChargeStatus.FAILED && this.status != ChargeStatus.EXPIRED) {
            this.setStatus(ChargeStatus.FAILED);
        } else {
            this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Charge charge = (Charge) o;
        return Objects.equals(chargeId, charge.chargeId) && Objects.equals(saleId, charge.saleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chargeId, saleId);
    }
}
