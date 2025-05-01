# Sub Hackathon - Autohub Charges API

API responsável pelo gerenciamento do ciclo de vida das cobranças (criação, atualização de status via callback,
tratamento de timeouts) e pela interação com o gateway de pagamento (simulado) na plataforma AutoHub.

## Integrantes:

- Victor Leite - RM354905

![Java 21](https://img.shields.io/badge/java-v21-blue)

## Visão Geral

Esta API é um componente central na saga de venda de veículos. Ela é responsável por:

- Receber a notificação de que um veículo foi reservado (VehicleReservedEvent).
- Interagir com um gateway de pagamento (atualmente um mock) para criar uma cobrança associada à venda.
- Publicar o evento ChargeCreated com os detalhes da cobrança (ex: código PIX, link de pagamento).
- Agendar uma verificação de timeout para a cobrança.
- Receber callbacks (webhooks simulados) do gateway de pagamento para atualizar o status da cobrança (PAID, FAILED).
- Publicar eventos de resultado do pagamento (PaymentCompleted, PaymentFailed).
- Processar mensagens de timeout para verificar e marcar cobranças como expiradas, publicando ChargeExpired.
- Publicar ChargeCreationFailed caso ocorra erro na interação inicial com o gateway ou ao salvar o estado.

## Arquitetura

A API segue a Arquitetura Hexagonal, separando o domínio de negócio da infraestrutura.

- Domínio: Contém as entidades (Charge, ChargeStatus), eventos, exceções e as interfaces das portas (entrada:
  ChargeServicePort; saída: ChargeRepositoryPort, ChargeEventPublisherPort, PaymentGatewayPort,
  NotificationServicePort).
- Aplicação: Contém a implementação da lógica de negócio (ChargeServiceImpl).
- Infraestrutura: Contém os adaptadores:
    - Entrada: Controller REST (ChargeController) para callbacks e testes; Consumidor SQS (ChargeEventConsumer) para
      eventos de negócio e timeouts.
    - Saída: Adaptador de persistência para DynamoDB (DynamoDbChargeRepositoryAdapter), Adaptador de publicação para
      SNS (SnsChargeEventPublisherAdapter), Adaptador mock para o Gateway de Pagamento (MockPaymentGatewayAdapter),
      Adaptador mock para Notificações (MockNotificationServiceAdapter).
- Deployment: A aplicação é empacotada como um "fat JAR" e deployada em duas funções AWS Lambda distintas:
    - Lambda HTTP: Acionada pelo API Gateway, usa StreamLambdaHandler (aws-serverless-java-container). Responsável pelos
      endpoints REST (callback, testes, consultas futuras).
    - Lambda SQS: Acionada por Event Source Mappings de filas SQS, usa FunctionInvoker (
      spring-cloud-function-adapter-aws).
      Responsável por processar eventos (VehicleReserved) e mensagens de timeout.

## Tecnologias

- Linguagem: Java 21
- Framework: Spring Boot 3.4.4
- Build: Maven
- Base de Dados: AWS DynamoDB
- Mensageria: AWS SNS, AWS SQS (via Spring Cloud AWS / Spring Cloud Function)
- Infraestrutura: AWS Lambda, API Gateway, Terraform
- Testes: JUnit 5, Mockito, Testcontainers (LocalStack)
- Documentação: Springdoc OpenAPI (Swagger UI)
- Outros: MapStruct

## Ficheiros de Configuração

- application.yml: Configurações base, defaults para ambiente local, placeholders.
- application-prod.yml: Configurações comuns de produção (ex: nível de log, região AWS lida de env var). Define as
  propriedades AWS para ler das variáveis de ambiente correspondentes.
- application-http.yml: Ativado com perfil http. Exclui auto-configurações SQS/Function desnecessárias para a Lambda
  HTTP.
- application-sqs.yml: Ativado com perfil sqs. Define web-application-type: none, exclui auto-configurações
  Web/Security/Swagger, define spring.cloud.function.definition.
- application-local.yml: Ativado com perfil local. Aponta para endpoints do LocalStack, define credenciais dummy, nome
  da tabela DynamoDB local, nomes/URLs das filas/tópico locais.
- application-test.yml: Ativado com perfil test. Exclui auto-configurações desnecessárias para testes (SecretsManager,
  JPA, Flyway), define propriedades dummy para AWS/Segurança.

## Como Executar API localmente

### Iniciar localstack com serviços da AWS usando docker-compose

```
docker-compose up -d
```

A partir desse momento subirá um docker com todos os serviços AWS necessários para a API.

Execute a api. Lembrando que os endpoints estão protegidos pelo spring security então será necessário passar o token JWT
gerado no user pool especificado no application.ym

## Como Visualizar o Swagger

Este projeto conta com Swagger para especificação e documentação da API. Para visualizar, basta executar localmente o
projeto e então acessar o link
abaixo:

[Link Swagger](http://localhost:8080/swagger-ui/index.html)