package com.fiap.autohub.autohub_charges_api.infrastructure.persistence.entities; // Ajuste o pacote

import com.fiap.autohub.autohub_charges_api.domain.entities.ChargeStatus;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entidade mapeada para a tabela DynamoDB de Cobranças.
 */
@DynamoDbBean
public class ChargeDynamoDbEntity {

    private String chargeId; // Chave de Partição (PK)
    private UUID saleId;     // Chave de Partição do GSI
    private UUID vehicleId;
    private BigDecimal amount;
    private ChargeStatus status;
    private String paymentCode;
    private Map<String, String> gatewayDetails;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime paidAt;
    private OffsetDateTime expiresAt;
    private String failureReason;

    // Chave de Partição
    @DynamoDbPartitionKey
    @DynamoDbAttribute("charge_id")
    public String getChargeId() {
        return chargeId;
    }

    public void setChargeId(String chargeId) {
        this.chargeId = chargeId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "saleId-index")
    @DynamoDbAttribute("sale_id")
    public UUID getSaleId() {
        return saleId;
    }

    public void setSaleId(UUID saleId) {
        this.saleId = saleId;
    }

    @DynamoDbAttribute("vehicle_id")
    public UUID getVehicleId() {
        return saleId;
    }

    public void setVehicleId(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    @DynamoDbAttribute("amount")
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @DynamoDbAttribute("status")
    public ChargeStatus getStatus() {
        return status;
    }

    public void setStatus(ChargeStatus status) {
        this.status = status;
    }

    @DynamoDbAttribute("payment_code")
    public String getPaymentCode() {
        return paymentCode;
    }

    public void setPaymentCode(String paymentCode) {
        this.paymentCode = paymentCode;
    }

    @DynamoDbAttribute("gateway_details")
    public Map<String, String> getGatewayDetails() {
        return gatewayDetails;
    }

    public void setGatewayDetails(Map<String, String> gatewayDetails) {
        this.gatewayDetails = gatewayDetails;
    }

    @DynamoDbAttribute("created_at")
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDbAttribute("updated_at")
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @DynamoDbAttribute("paid_at")
    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(OffsetDateTime paidAt) {
        this.paidAt = paidAt;
    }

    @DynamoDbAttribute("expires_at")
    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    @DynamoDbAttribute("failure_reason")
    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
