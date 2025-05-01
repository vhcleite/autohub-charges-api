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
import software.amazon.awssdk.services.sns.SnsClient;

import java.net.URI;

@Configuration
public class SnsConfig {

    private static final Logger log = LoggerFactory.getLogger(SnsConfig.class); // Logger

    @Bean
    @Profile("!local & !test")
    public SnsClient snsClientProd(@Value("${aws.region}") String awsRegion) {
        log.info("Creating SnsClient for production/dev in region: {}", awsRegion);
        return SnsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    @Profile("local")
    public SnsClient snsClientLocal(
            @Value("${aws.region}") String awsRegion,
            @Value("${aws.localstack.endpoint}") String localstackEndpoint, // Mantém endpoint global do localstack
            @Value("${aws.credentials.accessKey}") String accessKey,
            @Value("${aws.credentials.secretKey}") String secretKey) {
        log.info("Creating SnsClient for local profile pointing to: {}", localstackEndpoint);
        return SnsClient.builder()
                .region(Region.of(awsRegion))
                .endpointOverride(URI.create(localstackEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
