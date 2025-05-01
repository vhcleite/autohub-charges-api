package com.fiap.autohub.autohub_charges_api.infrastructure.persistence.repositories;

import com.fiap.autohub.autohub_charges_api.domain.entities.Charge;
import com.fiap.autohub.autohub_charges_api.domain.ports.out.ChargeRepositoryPort;
import com.fiap.autohub.autohub_charges_api.infrastructure.persistence.entities.ChargeDynamoDbEntity;
import com.fiap.autohub.autohub_charges_api.infrastructure.persistence.mappers.ChargePersistenceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of ChargeRepositoryPort using DynamoDB Enhanced Client.
 * Located in the persistence repositories package.
 */
@Repository
public class DynamoDbChargeRepositoryAdapter implements ChargeRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbChargeRepositoryAdapter.class);
    private static final String SALE_ID_INDEX_NAME = "saleId-index"; // GSI Name

    private final DynamoDbTable<ChargeDynamoDbEntity> chargeTable;
    private final DynamoDbIndex<ChargeDynamoDbEntity> saleIdIndex;
    private final ChargePersistenceMapper mapper;

    public DynamoDbChargeRepositoryAdapter(DynamoDbEnhancedClient enhancedClient,
                                           ChargePersistenceMapper mapper,
                                           @Value("${aws.dynamodb.table-name.charges}") String tableName) {
        this.mapper = mapper;
        this.chargeTable = enhancedClient.table(tableName, TableSchema.fromBean(ChargeDynamoDbEntity.class));
        this.saleIdIndex = chargeTable.index(SALE_ID_INDEX_NAME);
        log.info("DynamoDbChargeRepositoryAdapter configured for table: {}", tableName);
    }

    @Override
    public Charge save(Charge charge) {
        log.debug("Saving charge with chargeId: {} for saleId: {}", charge.getChargeId(), charge.getSaleId());
        try {
            ChargeDynamoDbEntity entity = mapper.toDynamoDbEntity(charge);
            chargeTable.putItem(entity);
            log.info("Charge {} saved successfully to DynamoDB. vehicleId: {}", entity.getChargeId(), entity.getVehicleId());
            return mapper.toDomainEntity(entity);
        } catch (DynamoDbException e) {
            log.error("DynamoDB error saving chargeId {}: {}", charge.getChargeId(), e.getMessage(), e);
            throw new RuntimeException("Failed to save charge to DynamoDB", e);
        } catch (Exception e) {
            log.error("Unexpected error saving chargeId {}: {}", charge.getChargeId(), e.getMessage(), e);
            throw new RuntimeException("Unexpected error during charge save", e);
        }
    }

    @Override
    public Optional<Charge> findById(String chargeId) {
        log.debug("Finding charge by ID: {}", chargeId);
        try {
            ChargeDynamoDbEntity entity = chargeTable.getItem(Key.builder().partitionValue(chargeId).build());
            return Optional.ofNullable(entity).map(mapper::toDomainEntity);
        } catch (DynamoDbException e) {
            log.error("DynamoDB error finding charge by ID {}: {}", chargeId, e.getMessage(), e);
            throw new RuntimeException("Failed to find charge by ID from DynamoDB", e);
        } catch (Exception e) {
            log.error("Unexpected error finding charge by ID {}: {}", chargeId, e.getMessage(), e);
            throw new RuntimeException("Unexpected error during charge findById", e);
        }
    }

    @Override
    public Optional<Charge> findBySaleId(UUID saleId) {
        log.debug("Finding charge by Sale ID using GSI '{}': {}", SALE_ID_INDEX_NAME, saleId);
        try {
            QueryConditional queryConditional = QueryConditional
                    .keyEqualTo(k -> k.partitionValue(saleId.toString()));

            QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                    .queryConditional(queryConditional)
                    .limit(1) // We only expect one charge per sale
                    .build();

            // Query the GSI - this returns an SdkIterable<Page<ChargeDynamoDbEntity>>
            Iterator<Page<ChargeDynamoDbEntity>> pageIterator = saleIdIndex.query(queryRequest).iterator();

            if (pageIterator.hasNext()) {
                Page<ChargeDynamoDbEntity> firstPage = pageIterator.next();
                if (!firstPage.items().isEmpty()) {
                    ChargeDynamoDbEntity entity = firstPage.items().get(0); // Get the first item from the first page
                    if (pageIterator.hasNext() || firstPage.items().size() > 1) {
                        log.warn("Found multiple charges for saleId {} in GSI '{}'. Returning the first one.", saleId, SALE_ID_INDEX_NAME);
                    }
                    return Optional.of(mapper.toDomainEntity(entity));
                }
            }
            // No items found
            return Optional.empty();

        } catch (DynamoDbException e) {
            log.error("DynamoDB error finding charge by Sale ID {} using GSI '{}': {}", saleId, SALE_ID_INDEX_NAME, e.getMessage(), e);
            throw new RuntimeException("Failed to find charge by Sale ID from DynamoDB", e);
        } catch (Exception e) {
            log.error("Unexpected error finding charge by Sale ID {}: {}", saleId, e.getMessage(), e);
            throw new RuntimeException("Unexpected error during charge findBySaleId", e);
        }
    }

    @Override
    public void deleteById(String chargeId) {
        log.warn("Attempting to delete charge with ID: {} (Compensation)", chargeId);
        try {
            ChargeDynamoDbEntity deletedItem = chargeTable.deleteItem(Key.builder().partitionValue(chargeId).build());
            if (deletedItem != null) {
                log.info("Successfully deleted charge with ID: {} from DynamoDB.", chargeId);
            } else {
                log.warn("Charge with ID {} not found for deletion (compensation).", chargeId);
            }
        } catch (DynamoDbException e) {
            log.error("CRITICAL: DynamoDB error deleting charge {} during compensation: {}", chargeId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("CRITICAL: Unexpected error deleting charge {} during compensation: {}", chargeId, e.getMessage(), e);
        }
    }
}
