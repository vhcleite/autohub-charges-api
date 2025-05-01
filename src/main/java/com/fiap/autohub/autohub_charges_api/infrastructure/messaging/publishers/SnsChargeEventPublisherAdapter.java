package com.fiap.autohub.autohub_charges_api.infrastructure.messaging.publishers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.autohub.autohub_charges_api.domain.events.*;
import com.fiap.autohub.autohub_charges_api.domain.ports.out.ChargeEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.HashMap;
import java.util.Map;

/**
 * Adaptador que implementa a porta ChargeEventPublisherPort usando AWS SNS.
 * Publica eventos relacionados a cobranças no tópico SNS principal.
 */
@Component
public class SnsChargeEventPublisherAdapter implements ChargeEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(SnsChargeEventPublisherAdapter.class);

    private final SnsClient snsClient; // Injetado pela SnsConfig
    private final ObjectMapper objectMapper;
    private final String topicArn;

    public SnsChargeEventPublisherAdapter(SnsClient snsClient,
                                          ObjectMapper objectMapper,
                                          @Value("${sns.topic.main-event-bus-arn}") String topicArn) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.topicArn = topicArn;
    }

    // Implementações para cada tipo de evento a ser publicado
    @Override
    public void publishChargeCreated(ChargeCreatedEvent event) {
        publishEvent(event, event.eventType());
    }

    @Override
    public void publishChargeCreationFailed(ChargeCreationFailedEvent event) {
        publishEvent(event, event.eventType());
    }

    @Override
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        publishEvent(event, event.eventType());
    }

    @Override
    public void publishPaymentFailed(PaymentFailedEvent event) {
        publishEvent(event, event.eventType());
    }

    @Override
    public void publishChargeExpired(ChargeExpiredEvent event) {
        publishEvent(event, event.eventType());
    }

    private void publishEvent(Object eventPayload, String eventType) {
        if (topicArn == null || topicArn.isBlank() || topicArn.startsWith("${")) {
            log.error("SNS Topic ARN is not configured or not resolved correctly: '{}'. Cannot publish event type '{}'", topicArn, eventType);
            return;
        }
        try {
            String messageBody = objectMapper.writeValueAsString(eventPayload);
            log.info("Publishing event type '{}' to SNS topic {}: {}", eventType, topicArn, messageBody);

            Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
            messageAttributes.put("eventType", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(eventType)
                    .build());

            PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(messageBody)
                    .messageAttributes(messageAttributes)
                    .build();

            snsClient.publish(publishRequest);
            log.info("Event type '{}' published successfully to topic {}.", eventType, topicArn);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event payload for type {}: {}", eventType, eventPayload, e);
        } catch (SnsException e) {
            log.error("Failed to publish event type '{}' to SNS topic {}: {}", eventType, topicArn, e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error publishing event type '{}' to SNS topic {}: {}", eventType, topicArn, e.getMessage(), e);
        }
    }
}
