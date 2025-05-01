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
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
public class SqsConfig {

    private static final Logger log = LoggerFactory.getLogger(SqsConfig.class); // Logger

    // --- Cliente Assíncrono (Usado pelo Spring Cloud AWS SQS Listener/Function) ---
    @Bean
    @Profile("!local & !test")
    public SqsAsyncClient sqsAsyncClientProd(@Value("${aws.region}") String awsRegion) {
        log.info("Creating SqsAsyncClient for production/dev in region: {}", awsRegion);
        return SqsAsyncClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    @Profile("local")
    public SqsAsyncClient sqsAsyncClientLocal(
            @Value("${aws.region}") String awsRegion,
            @Value("${aws.localstack.endpoint}") String localstackEndpoint, // Mantém endpoint global do localstack
            @Value("${aws.credentials.accessKey}") String accessKey,
            @Value("${aws.credentials.secretKey}") String secretKey) {
        log.info("Creating SqsAsyncClient for local profile pointing to: {}", localstackEndpoint);
        return SqsAsyncClient.builder()
                .region(Region.of(awsRegion))
                .endpointOverride(URI.create(localstackEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    @Bean
    @Profile("!local & !test")
    public SqsClient sqsClientProd(@Value("${aws.region}") String awsRegion) {
        log.info("Creating SqsClient (sync) for production/dev in region: {}", awsRegion);
        return SqsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    @Profile("local")
    public SqsClient sqsClientLocal(
            @Value("${aws.region}") String awsRegion,
            @Value("${aws.localstack.endpoint}") String localstackEndpoint, // Mantém endpoint global do localstack
            @Value("${aws.credentials.accessKey}") String accessKey,
            @Value("${aws.credentials.secretKey}") String secretKey) {
        log.info("Creating SqsClient (sync) for local profile pointing to: {}", localstackEndpoint);
        return SqsClient.builder()
                .region(Region.of(awsRegion))
                .endpointOverride(URI.create(localstackEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
