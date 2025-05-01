package com.fiap.autohub.autohub_charges_api.application.services;

import com.fiap.autohub.autohub_charges_api.domain.entities.Charge;
import com.fiap.autohub.autohub_charges_api.domain.entities.ChargeStatus;
import com.fiap.autohub.autohub_charges_api.domain.events.*;
import com.fiap.autohub.autohub_charges_api.domain.exceptions.ChargeNotFoundException;
import com.fiap.autohub.autohub_charges_api.domain.exceptions.PaymentGatewayException;
import com.fiap.autohub.autohub_charges_api.domain.ports.in.ChargeServicePort;
import com.fiap.autohub.autohub_charges_api.domain.ports.out.ChargeEventPublisherPort;
import com.fiap.autohub.autohub_charges_api.domain.ports.out.ChargeRepositoryPort;
import com.fiap.autohub.autohub_charges_api.domain.ports.out.NotificationServicePort;
import com.fiap.autohub.autohub_charges_api.domain.ports.out.PaymentGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Service
public class ChargeServiceImpl implements ChargeServicePort {

    private static final Logger log = LoggerFactory.getLogger(ChargeServiceImpl.class);

    private final ChargeRepositoryPort chargeRepository;
    private final PaymentGatewayPort paymentGateway; // Mock ou real
    private final ChargeEventPublisherPort eventPublisher;
    private final NotificationServicePort notificationService;
    private final SqsClient sqsClient;
    private final String timeoutQueueUrl;

    public ChargeServiceImpl(ChargeRepositoryPort chargeRepository,
                             PaymentGatewayPort paymentGateway,
                             ChargeEventPublisherPort eventPublisher,
                             NotificationServicePort notificationService,
                             SqsClient sqsClient,
                             @Value("${sqs.queue.charge-timeout-url}") String timeoutQueueUrl) {
        this.chargeRepository = chargeRepository;
        this.paymentGateway = paymentGateway;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
        this.sqsClient = sqsClient;
        this.timeoutQueueUrl = timeoutQueueUrl;
    }

    @Override
    public void processVehicleReservation(VehicleReservedEvent event) {
        if (event == null || event.data() == null || event.getSaleId() == null || event.getPrice() == null || event.getVehicleId() == null) {
            log.error("Received invalid VehicleReservedEvent: {}", event);
            return;
        }

        UUID saleId = event.getSaleId();
        log.info("Processing VehicleReservedEvent for saleId: {} and vehicle_id: {}", saleId, event.getVehicleId());

        PaymentGatewayPort.PaymentGatewayResponse gatewayResponse;
        try {
            gatewayResponse = paymentGateway.createCharge(saleId, event.getPrice())
                    .orElseThrow(() -> new PaymentGatewayException("Payment gateway returned empty response for saleId: " + saleId));
        } catch (Exception e) {
            log.error("Failed to create charge in payment gateway for saleId: {}", saleId, e);
            eventPublisher.publishChargeCreationFailed(new ChargeCreationFailedEvent(saleId, event.getVehicleId(), "Gateway error: " + e.getMessage()));
            return;
        }

        // Cobrança criada no gateway, agora tentamos salvar localmente
        Charge charge = new Charge(saleId, event.getVehicleId(), event.getPrice());
        charge.setChargeId(gatewayResponse.gatewayChargeId());
        charge.setPaymentCode(gatewayResponse.paymentCode());
        charge.setExpiresAt(gatewayResponse.expiresAt());

        Charge savedCharge;
        try {
            savedCharge = chargeRepository.save(charge);
            log.info("Charge created and saved with chargeId: {} {}", savedCharge.getChargeId(), savedCharge);
        } catch (Exception e) {
            log.error("Failed to save charge {} for saleId {} to repository after successful gateway creation.", charge.getChargeId(), saleId, e);
            log.warn("Attempting compensation 1: Cancelling charge {} in gateway.", charge.getChargeId());
            try {
                boolean cancelled = paymentGateway.cancelCharge(charge.getChargeId());
                if (cancelled) {
                    log.info("Charge {} successfully cancelled in gateway (compensation 1).", charge.getChargeId());
                } else {
                    log.error("CRITICAL: Failed to cancel charge {} in gateway during compensation 1.", charge.getChargeId());
                }
            } catch (Exception cancelEx) {
                log.error("CRITICAL: Error during gateway cancellation for charge {} during compensation 1.", charge.getChargeId(), cancelEx);
            }
            eventPublisher.publishChargeCreationFailed(new ChargeCreationFailedEvent(saleId, event.getVehicleId(), "Failed to save charge state after gateway creation. Gateway charge cancelled: " + charge.getChargeId()));
            return;
        }

        try {
            ChargeCreatedEvent chargeCreatedEvent = new ChargeCreatedEvent(
                    savedCharge.getSaleId(),
                    savedCharge.getVehicleId(),
                    savedCharge.getChargeId(),
                    savedCharge.getPaymentCode(),
                    savedCharge.getAmount(),
                    savedCharge.getExpiresAt()
            );
            eventPublisher.publishChargeCreated(chargeCreatedEvent);
            log.info("Published ChargeCreatedEvent for charge {} (sale {})", savedCharge.getChargeId(), saleId);
        } catch (Exception e) {
            log.error("Failed to publish ChargeCreatedEvent for charge {} (sale {}). Charge WAS saved. Attempting compensation...", savedCharge.getChargeId(), saleId, e);
            try {
                chargeRepository.deleteById(savedCharge.getChargeId());
                log.info("Successfully deleted charge {} from repository (compensation 2).", savedCharge.getChargeId());
            } catch (Exception deleteEx) {
                log.error("CRITICAL: Failed to delete charge {} from repository during compensation 2.", savedCharge.getChargeId(), deleteEx);
            }
            log.warn("Attempting compensation 2: Cancelling charge {} in gateway.", savedCharge.getChargeId());
            try {
                boolean cancelled = paymentGateway.cancelCharge(savedCharge.getChargeId());
                if (cancelled) {
                    log.info("Charge {} successfully cancelled in gateway (compensation 2).", savedCharge.getChargeId());
                } else {
                    log.error("CRITICAL: Failed to cancel charge {} in gateway during compensation 2.", savedCharge.getChargeId());
                }
            } catch (Exception cancelEx) {
                log.error("CRITICAL: Error during gateway cancellation for charge {} during compensation 2.", savedCharge.getChargeId(), cancelEx);
            }
            eventPublisher.publishChargeCreationFailed(new ChargeCreationFailedEvent(saleId, event.getVehicleId(), "Failed to publish ChargeCreatedEvent after saving charge state. Compensations attempted for charge: " + savedCharge.getChargeId()));
            return;
        }

        // Agendar timeout.
        try {
            scheduleTimeoutCheck(savedCharge);
        } catch (Exception e) {
            log.error("Failed to schedule timeout check for charge {} (sale {}). Charge was saved and event published.", savedCharge.getChargeId(), saleId, e);
        }
    }

    private void scheduleTimeoutCheck(Charge charge) {
        if (charge.getExpiresAt() == null) {
            log.warn("Charge {} for sale {} does not have an expiration time. Cannot schedule timeout check.",
                    charge.getChargeId(), charge.getSaleId());
            return;
        }

        Duration delay = Duration.between(OffsetDateTime.now(ZoneOffset.UTC), charge.getExpiresAt());
        int delaySeconds = (int) delay.getSeconds();

        if (delaySeconds <= 0) {
            log.warn("Charge {} expiration time is in the past or immediate. Not scheduling timeout check.", charge.getChargeId());
            return;
        }
        if (delaySeconds > 900) {
            log.warn("Charge {} expiration time is beyond SQS max delay (900s). Setting delay to 900s.", charge.getChargeId());
            delaySeconds = 900;
        }

        String messageBody = String.format("{\"saleId\":\"%s\", \"chargeId\":\"%s\"}", charge.getSaleId(), charge.getChargeId());

        try {
            log.info("Scheduling timeout check for charge {} (sale {}) with delay {} seconds to queue {}",
                    charge.getChargeId(), charge.getSaleId(), delaySeconds, timeoutQueueUrl);

            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(timeoutQueueUrl)
                    .messageBody(messageBody)
                    .delaySeconds(delaySeconds)
                    .build();

            sqsClient.sendMessage(sendMsgRequest);
            log.info("Timeout check message sent successfully for charge {}", charge.getChargeId());

        } catch (SqsException e) {
            log.error("Failed to send timeout check message for charge {} to SQS queue {}: {}",
                    charge.getChargeId(), timeoutQueueUrl, e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending timeout check message for charge {}: {}", charge.getChargeId(), e.getMessage(), e);
        }
    }


    @Override
    public Charge handlePaymentCallback(String chargeId, String paymentStatus, Map<String, Object> details) {
        log.info("Handling payment callback for chargeId: {} with status: {}", chargeId, paymentStatus);
        Charge charge = findChargeByIdOrThrow(chargeId);

        if (charge.getStatus() == ChargeStatus.PENDING) {
            boolean success = "PAID".equalsIgnoreCase(paymentStatus);
            Charge savedCharge;
            if (success) {
                charge.setStatus(ChargeStatus.PAID);
                charge.setPaidAt(OffsetDateTime.now(ZoneOffset.UTC));
                charge.setUpdatedAt(charge.getPaidAt());
                charge.setGatewayDetails(details);
                try {
                    savedCharge = chargeRepository.save(charge);
                    log.info("Charge {} status updated to PAID. {}", chargeId, savedCharge);
                } catch (Exception e) {
                    log.error("Failed to save PAID status for charge {} to repository.", chargeId, e);
                    throw new RuntimeException("Failed to update charge status after successful payment confirmation", e);
                }
                try {
                    eventPublisher.publishPaymentCompleted(new PaymentCompletedEvent(
                            savedCharge.getSaleId(), savedCharge.getVehicleId(), savedCharge.getChargeId(), savedCharge.getPaidAt()
                    ));
                } catch (Exception e) {
                    log.error("Failed to publish PaymentCompletedEvent for charge {}. Charge status WAS updated to PAID.", chargeId, e);
                    throw new RuntimeException("Failed to publish payment completed event after successful payment confirmation", e);
                }
                return savedCharge;
            } else {
                String reason = "Gateway reported failure";
                if (details != null && details.containsKey("failure_reason")) {
                    reason = details.get("failure_reason").toString();
                }
                charge.setFailureReason(reason);
                charge.setGatewayDetails(details);
                try {
                    savedCharge = chargeRepository.save(charge);
                    log.warn("Charge {} status updated to FAILED. Reason: {}", chargeId, reason);
                } catch (Exception e) {
                    log.error("Failed to save FAILED status for charge {} to repository.", chargeId, e);
                    throw new RuntimeException("Failed to update charge status after payment failure confirmation", e);
                }
                try {
                    eventPublisher.publishPaymentFailed(new PaymentFailedEvent(
                            savedCharge.getSaleId(), savedCharge.getVehicleId(), savedCharge.getChargeId(), reason
                    ));
                } catch (Exception e) {
                    log.error("Failed to publish PaymentFailedEvent for charge {}. Charge status WAS updated to FAILED.", chargeId, e);
                    throw new RuntimeException("Failed to publish payment failed event after successful payment confirmation", e);
                }
                return savedCharge;
            }
        } else {
            log.warn("Ignoring payment callback for charge {} because its status is already {}", chargeId, charge.getStatus());
            return charge;
        }
    }

    @Override
    public void handleChargeTimeout(UUID saleId, String chargeId) {
        log.warn("Handling charge timeout check for saleId: {}, chargeId: {}", saleId, chargeId);
        try {
            Charge charge = findChargeByIdOrThrow(chargeId);

            if (!charge.getSaleId().equals(saleId)) {
                log.error("Timeout message mismatch: chargeId {} does not belong to saleId {}. Ignoring.", chargeId, saleId);
                return;
            }

            if (charge.getStatus() == ChargeStatus.PENDING) {
                charge.setStatus(ChargeStatus.EXPIRED);
                charge.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                charge.setFailureReason("Charge expired before payment.");
                Charge savedCharge;
                try {
                    savedCharge = chargeRepository.save(charge);
                    log.info("Charge {} status updated to EXPIRED.", chargeId);
                } catch (Exception e) {
                    log.error("Failed to save EXPIRED status for charge {} to repository.", chargeId, e);
                    throw new RuntimeException("Failed to update charge status after timeout", e);
                }
                try {
                    eventPublisher.publishChargeExpired(new ChargeExpiredEvent(saleId, chargeId));
                } catch (Exception e) {
                    log.error("Failed to publish ChargeExpiredEvent for charge {}. Charge status WAS updated to EXPIRED.", chargeId, e);
                }
            } else {
                log.info("Ignoring timeout for charge {} because its status is already {}.", chargeId, charge.getStatus());
            }
        } catch (ChargeNotFoundException e) {
            log.warn("Charge {} not found during timeout check for saleId {}. It might have been deleted or never existed.", chargeId, saleId);
        } catch (Exception e) {
            log.error("Unexpected error handling timeout for charge {} (sale {}): {}", chargeId, saleId, e.getMessage(), e);
            throw new RuntimeException("Unexpected error handling charge timeout", e);
        }
    }

    @Override
    public Charge findChargeById(String chargeId) {
        log.debug("Finding charge by ID: {}", chargeId);
        return findChargeByIdOrThrow(chargeId);
    }

    @Override
    public Charge findChargeBySaleId(UUID saleId) {
        log.debug("Finding charge by Sale ID: {}", saleId);
        return chargeRepository.findBySaleId(saleId)
                .orElseThrow(() -> {
                    log.warn("Charge not found for sale ID: {}", saleId);
                    return new ChargeNotFoundException(saleId);
                });
    }

    private Charge findChargeByIdOrThrow(String chargeId) {
        return chargeRepository.findById(chargeId)
                .orElseThrow(() -> {
                    log.error("Charge not found with ID: {}", chargeId);
                    return new ChargeNotFoundException(chargeId);
                });
    }
}
