#!/bin/bash

# Script para inicializar recursos AWS no LocalStack para a Charges API.

echo "########### Criando recursos DynamoDB/SNS/SQS no LocalStack para Charges API ###########"

# Definições
REGION="us-east-1"
SNS_TOPIC_NAME="AutoHubBusinessEventsTopic-local"
CHARGES_VEHICLE_RESERVED_QUEUE_NAME="ChargesApi_VehicleReserved_Queue-local"
CHARGES_VEHICLE_RESERVED_DLQ_NAME="ChargesApi_VehicleReserved_DLQ-local"
CHARGE_TIMEOUT_QUEUE_NAME="AutoHubChargeTimeoutQueue-local"
CHARGE_TIMEOUT_DLQ_NAME="AutoHubChargeTimeoutQueue_DLQ-local" # Nome ligeiramente diferente para DLQ
DYNAMODB_TABLE_NAME="AutoHubCharges-local"
SALE_ID_GSI_NAME="saleId-index"

# Função para esperar o LocalStack ficar pronto (opcional, mas útil)
wait_for_localstack() {
  echo "Aguardando LocalStack ficar pronto..."
  # Use `awslocal` para verificar o status de um serviço, como SQS
  until awslocal sqs list-queues > /dev/null 2>&1; do
    echo -n "."
    sleep 1
  done
  echo " LocalStack pronto!"
}

# Espera o LocalStack
wait_for_localstack

# 1. Criar Tabela DynamoDB
echo "--- Criando Tabela DynamoDB: ${DYNAMODB_TABLE_NAME} ---"
awslocal dynamodb create-table \
    --table-name ${DYNAMODB_TABLE_NAME} \
    --attribute-definitions \
        AttributeName=charge_id,AttributeType=S \
        AttributeName=sale_id,AttributeType=S \
    --key-schema \
        AttributeName=charge_id,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5 \
    --global-secondary-indexes \
        "[
            {
                \"IndexName\": \"${SALE_ID_GSI_NAME}\",
                \"KeySchema\": [
                    {\"AttributeName\": \"sale_id\",\"KeyType\": \"HASH\"}
                ],
                \"Projection\": {
                    \"ProjectionType\": \"ALL\"
                },
                \"ProvisionedThroughput\": {
                    \"ReadCapacityUnits\": 5,
                    \"WriteCapacityUnits\": 5
                }
            }
        ]" \
    --region ${REGION}
echo "Tabela DynamoDB ${DYNAMODB_TABLE_NAME} criada."

# 2. Criar Tópico SNS (ignora erro se já existir)
echo "--- Criando/Verificando Tópico SNS: ${SNS_TOPIC_NAME} ---"
awslocal sns create-topic --name ${SNS_TOPIC_NAME} --region ${REGION} || echo "Tópico SNS ${SNS_TOPIC_NAME} já existe ou erro ignorado."
SNS_TOPIC_ARN=$(awslocal sns list-topics --query "Topics[?ends_with(TopicArn, ':${SNS_TOPIC_NAME}')].TopicArn" --output text --region ${REGION})
echo "SNS Topic ARN: ${SNS_TOPIC_ARN}"

# 3. Criar Fila de Timeout e sua DLQ
echo "--- Criando DLQ de Timeout: ${CHARGE_TIMEOUT_DLQ_NAME} ---"
awslocal sqs create-queue --queue-name ${CHARGE_TIMEOUT_DLQ_NAME} --region ${REGION}
TIMEOUT_DLQ_URL=$(awslocal sqs get-queue-url --queue-name ${CHARGE_TIMEOUT_DLQ_NAME} --query QueueUrl --output text --region ${REGION})
TIMEOUT_DLQ_ARN=$(awslocal sqs get-queue-attributes --queue-url ${TIMEOUT_DLQ_URL} --attribute-names QueueArn --query Attributes.QueueArn --output text --region ${REGION})
echo "Timeout DLQ ARN: ${TIMEOUT_DLQ_ARN}"

echo "--- Criando Fila de Timeout Principal: ${CHARGE_TIMEOUT_QUEUE_NAME} ---"
awslocal sqs create-queue --queue-name ${CHARGE_TIMEOUT_QUEUE_NAME} --region ${REGION} \
  --attributes '{
    "RedrivePolicy": "{\"deadLetterTargetArn\":\"'"${TIMEOUT_DLQ_ARN}"'\",\"maxReceiveCount\":\"3\"}",
    "VisibilityTimeout": "90" # Timeout deve ser >= ao timeout da Lambda de timeout (se houver) ou da Charges SQS Lambda
  }'
TIMEOUT_QUEUE_URL=$(awslocal sqs get-queue-url --queue-name ${CHARGE_TIMEOUT_QUEUE_NAME} --query QueueUrl --output text --region ${REGION})
# TIMEOUT_QUEUE_ARN=$(awslocal sqs get-queue-attributes --queue-url ${TIMEOUT_QUEUE_URL} --attribute-names QueueArn --query Attributes.QueueArn --output text --region ${REGION})
echo "Timeout Queue URL: ${TIMEOUT_QUEUE_URL}"

# 4. Criar Fila VehicleReserved e sua DLQ
echo "--- Criando DLQ VehicleReserved: ${CHARGES_VEHICLE_RESERVED_DLQ_NAME} ---"
awslocal sqs create-queue --queue-name ${CHARGES_VEHICLE_RESERVED_DLQ_NAME} --region ${REGION}
VR_DLQ_URL=$(awslocal sqs get-queue-url --queue-name ${CHARGES_VEHICLE_RESERVED_DLQ_NAME} --query QueueUrl --output text --region ${REGION})
VR_DLQ_ARN=$(awslocal sqs get-queue-attributes --queue-url ${VR_DLQ_URL} --attribute-names QueueArn --query Attributes.QueueArn --output text --region ${REGION})
echo "VehicleReserved DLQ ARN: ${VR_DLQ_ARN}"

echo "--- Criando Fila Principal VehicleReserved: ${CHARGES_VEHICLE_RESERVED_QUEUE_NAME} ---"
awslocal sqs create-queue --queue-name ${CHARGES_VEHICLE_RESERVED_QUEUE_NAME} --region ${REGION} \
  --attributes '{
    "RedrivePolicy": "{\"deadLetterTargetArn\":\"'"${VR_DLQ_ARN}"'\",\"maxReceiveCount\":\"3\"}",
    "VisibilityTimeout": "130" # Exemplo: >= ao timeout da Lambda SQS da Charges API
  }'
VR_QUEUE_URL=$(awslocal sqs get-queue-url --queue-name ${CHARGES_VEHICLE_RESERVED_QUEUE_NAME} --query QueueUrl --output text --region ${REGION})
VR_QUEUE_ARN=$(awslocal sqs get-queue-attributes --queue-url ${VR_QUEUE_URL} --attribute-names QueueArn --query Attributes.QueueArn --output text --region ${REGION})
echo "VehicleReserved Queue ARN: ${VR_QUEUE_ARN}"

# 5. Criar Assinatura SNS -> SQS para VehicleReserved
echo "--- Criando Assinatura SNS -> SQS (${SNS_TOPIC_NAME} -> ${CHARGES_VEHICLE_RESERVED_QUEUE_NAME}) ---"
awslocal sns subscribe \
  --topic-arn ${SNS_TOPIC_ARN} \
  --protocol sqs \
  --notification-endpoint ${VR_QUEUE_ARN} \
  --attributes '{ "RawMessageDelivery": "true", "FilterPolicy": "{\"eventType\": [\"VehicleReserved\"]}" }' \
  --region ${REGION}

echo "########### Criação de recursos concluída. ###########"

