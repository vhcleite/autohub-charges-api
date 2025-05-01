package com.fiap.autohub.autohub_charges_api.infrastructure.messaging.consumers;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.autohub.autohub_charges_api.domain.events.VehicleReservedEvent;
import com.fiap.autohub.autohub_charges_api.domain.ports.in.ChargeServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Componente responsável por processar os eventos SQS recebidos pela Charges API.
 * Contém a lógica de roteamento baseada no eventType.
 * É chamado pelo @Bean Consumer<SQSEvent> na classe principal da aplicação.
 */
@Component
public class ChargeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ChargeEventConsumer.class);

    private final ChargeServicePort chargeService;
    private final ObjectMapper objectMapper;

    public ChargeEventConsumer(ChargeServicePort chargeService, ObjectMapper objectMapper) {
        this.chargeService = chargeService;
        this.objectMapper = objectMapper;
    }

    /**
     * Método principal que processa um lote de mensagens SQS.
     * É invocado pelo bean Consumer<SQSEvent> do Spring Cloud Function.
     *
     * @param sqsEvent O evento SQS recebido da Lambda.
     */
    public void consumeEvent(SQSEvent sqsEvent) {
        if (sqsEvent == null || sqsEvent.getRecords() == null) {
            log.warn("Received null or empty SQSEvent.");
            return;
        }
        log.info("Processing SQS event with {} record(s)", sqsEvent.getRecords().size());
        List<SQSEvent.SQSMessage> messages = sqsEvent.getRecords();

        for (SQSEvent.SQSMessage message : messages) {
            String messageId = message.getMessageId();
            String messageBody = message.getBody();
            log.debug("Processing message ID: {}, Body: {}", messageId, messageBody);

            try {
                // Tenta ler o tipo de evento do corpo JSON
                JsonNode rootNode = objectMapper.readTree(messageBody);
                String eventType = rootNode.path("event_type").asText(null);

                // Verifica se é uma mensagem de timeout (que tem formato diferente)
                if (eventType == null && rootNode.has("chargeId") && rootNode.has("saleId")) {
                    log.info("Detected potential timeout message (Message ID: {})", messageId);
                    handleTimeoutMessage(rootNode.path("saleId").asText(), rootNode.path("chargeId").asText());
                }
                // Verifica se é um evento de negócio conhecido
                else if (eventType != null) {
                    log.info("Routing event (Message ID: {}) based on eventType: {}", messageId, eventType);
                    routeBusinessEvent(eventType, messageBody, messageId);
                }
                // Se não for nenhum dos formatos esperados
                else {
                    log.error("Received message (ID: {}) with unknown format or missing 'eventType': {}", messageId, messageBody);
                    throw new IllegalArgumentException("Unknown message format or missing eventType for message ID: " + messageId);
                }

            } catch (JsonProcessingException e) {
                log.error("Failed to parse message body (Message ID: {}): {}", messageId, messageBody, e);
                throw new RuntimeException("Message parsing failed for message ID: " + messageId, e);
            } catch (Exception e) {
                log.error("Failed to process message (Message ID: {}): {}", messageId, messageBody, e);
                throw new RuntimeException("Message processing failed for message ID: " + messageId, e);
            }
        }
        log.info("Finished processing batch of {} message(s).", messages.size());
    }

    /**
     * Roteia eventos de negócio baseados no eventType.
     */
    private void routeBusinessEvent(String eventType, String messageBody, String messageId) throws JsonProcessingException {
        switch (eventType) {
            case "VehicleReserved":
                VehicleReservedEvent vre = objectMapper.readValue(messageBody, VehicleReservedEvent.class);
                chargeService.processVehicleReservation(vre);
                break;
            default:
                log.warn("Received unhandled business eventType '{}' for message ID: {}", eventType, messageId);
                break;
        }
        log.debug("Finished processing business event message ID: {} for eventType: {}", messageId, eventType);
    }

    /**
     * Processa uma mensagem da fila de timeout.
     */
    private void handleTimeoutMessage(String saleIdStr, String chargeId) {
        if (saleIdStr == null || chargeId == null) {
            log.error("Received invalid timeout message: saleId or chargeId is null.");
            throw new IllegalArgumentException("Invalid timeout message content.");
        }
        try {
            UUID saleId = UUID.fromString(saleIdStr);
            chargeService.handleChargeTimeout(saleId, chargeId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for saleId in timeout message: {}", saleIdStr, e);
            throw new RuntimeException("Invalid saleId format in timeout message", e);
        }
    }
}
