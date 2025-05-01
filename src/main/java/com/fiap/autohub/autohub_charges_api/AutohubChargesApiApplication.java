package com.fiap.autohub.autohub_charges_api; // Ajuste o pacote se necessário

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fiap.autohub.autohub_charges_api.infrastructure.messaging.consumers.ChargeEventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.util.function.Consumer;

@SpringBootApplication
public class AutohubChargesApiApplication {

    private static final Logger log = LoggerFactory.getLogger(AutohubChargesApiApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AutohubChargesApiApplication.class, args);
    }

    /**
     * Bean funcional que será encontrado pelo Spring Cloud Function (FunctionInvoker)
     * quando o perfil 'sqs' estiver ativo.
     */
    @Bean
    @Profile("sqs")
    public Consumer<SQSEvent> chargeEventsConsumer(ChargeEventConsumer consumerLogic) {
        log.info("Creating chargeEventsConsumer bean for SQS profile.");
        return consumerLogic::consumeEvent;
    }
}
