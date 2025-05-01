package com.fiap.autohub.autohub_charges_api; // Ajuste o pacote se for diferente

import com.fiap.autohub.autohub_charges_api.domain.ports.out.ChargeEventPublisherPort;
import com.fiap.autohub.autohub_charges_api.domain.ports.out.NotificationServicePort;
import com.fiap.autohub.autohub_charges_api.infrastructure.persistence.repositories.DynamoDbChargeRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Teste básico para verificar se o contexto da aplicação Charges API carrega.
 */
@SpringBootTest
@ActiveProfiles("test") // Garante que application-test.yml seja usado
class AutohubChargesApiApplicationTests {

    // Mocka os clientes AWS SDK e os Adapters/Ports necessários para que o contexto suba
    // sem precisar de credenciais ou endpoints reais/localstack neste teste.
    @MockBean
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @MockBean
    private SnsClient snsClient;

    @MockBean
    private SqsAsyncClient sqsAsyncClient;

    @MockBean
    private SqsClient sqsClient;

    @MockBean
    private DynamoDbChargeRepositoryAdapter dynamoDbChargeRepositoryAdapter;

    @MockBean
    private ChargeEventPublisherPort chargeEventPublisherPort;

    @MockBean // <<< ADICIONADO MOCK PARA A PORTA DE NOTIFICAÇÃO
    private NotificationServicePort notificationServicePort;

    @Test
    void contextLoads() {
        // Este teste agora deve passar, pois todas as dependências externas estão mockadas.
        System.out.println("Charges API Application Context loaded successfully!");
    }

}
