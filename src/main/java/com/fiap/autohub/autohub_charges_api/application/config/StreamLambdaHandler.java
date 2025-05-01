package com.fiap.autohub.autohub_charges_api.application.config; // Verifique se este é o pacote correto

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fiap.autohub.autohub_charges_api.AutohubChargesApiApplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Handler da AWS Lambda para processar requisições proxy do HTTP API Gateway v2.
 * Utiliza a biblioteca aws-serverless-java-container para inicializar o Spring Boot.
 */
public class StreamLambdaHandler implements RequestStreamHandler {
    private static SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> handler;

    static {
        try {
            handler = SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(AutohubChargesApiApplication.class);
        } catch (ContainerInitializationException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not initialize Spring Boot application for HTTP handler", e);
        }
    }

    /**
     * Lida com o stream de requisição de entrada da Lambda.
     * Delega o processamento para o handler do aws-serverless-java-container.
     *
     * @param inputStream  O stream de entrada contendo o payload do evento Lambda (evento API Gateway).
     * @param outputStream O stream de saída para escrever o payload da resposta Lambda (resposta API Gateway).
     * @param context      O contexto de execução da AWS Lambda.
     * @throws IOException Se ocorrer um erro de I/O.
     */
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
}