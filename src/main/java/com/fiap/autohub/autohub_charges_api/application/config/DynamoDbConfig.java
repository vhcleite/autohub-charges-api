package com.fiap.autohub.autohub_charges_api.application.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

/**
 * Configuração para o cliente DynamoDB, seguindo o padrão de beans por perfil.
 */
@Configuration
public class DynamoDbConfig {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbConfig.class);

    private final String awsRegion;

    public DynamoDbConfig(@Value("${aws.region}") String awsRegion) {
        this.awsRegion = awsRegion;
    }

    /**
     * Bean para o DynamoDB Enhanced Client em produção/dev.
     * Cria o cliente base DynamoDbClient internamente usando DefaultCredentialsProvider.
     *
     * @return DynamoDbEnhancedClient configurado para produção/dev.
     */
    @Bean
    @Profile("!local & !test")
    public DynamoDbEnhancedClient dynamoDbEnhancedClientProd() {
        log.info("Creating DynamoDbEnhancedClient for production/dev in region: {}", awsRegion);
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(this.awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create()) // Usa Role da Lambda
                .build();

        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    /**
     * Bean para o DynamoDB Enhanced Client no ambiente local.
     * Cria o cliente base DynamoDbClient internamente apontando para LocalStack/Endpoint customizado.
     *
     * @param dynamoDbEndpoint Endpoint do DynamoDB local (ex: LocalStack).
     * @param accessKey        Chave de acesso dummy para LocalStack.
     * @param secretKey        Chave secreta dummy para LocalStack.
     * @return DynamoDbEnhancedClient configurado para local.
     */
    @Bean
    @Profile("local")
    public DynamoDbEnhancedClient dynamoDbEnhancedClientLocal(
            @Value("${aws.dynamodb.endpoint}") String dynamoDbEndpoint,
            @Value("${aws.credentials.accessKey}") String accessKey,
            @Value("${aws.credentials.secretKey}") String secretKey
    ) {
        log.info("Creating DynamoDbEnhancedClient for local profile pointing to: {}", dynamoDbEndpoint);
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(this.awsRegion))
                .endpointOverride(URI.create(dynamoDbEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();

        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}