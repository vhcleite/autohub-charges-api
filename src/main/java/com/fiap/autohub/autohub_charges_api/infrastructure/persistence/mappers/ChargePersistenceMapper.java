package com.fiap.autohub.autohub_charges_api.infrastructure.persistence.mappers; // Ajuste o pacote

import com.fiap.autohub.autohub_charges_api.domain.entities.Charge;
import com.fiap.autohub.autohub_charges_api.infrastructure.persistence.entities.ChargeDynamoDbEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper entre a Entidade de Domínio Charge e a Entidade DynamoDB ChargeDynamoDbEntity.
 */
@Mapper(componentModel = "spring")
public interface ChargePersistenceMapper {

    @Mapping(source = "gatewayDetails", target = "gatewayDetails", qualifiedByName = "mapObjectToStringMap")
    ChargeDynamoDbEntity toDynamoDbEntity(Charge charge);

    @Mapping(source = "gatewayDetails", target = "gatewayDetails", qualifiedByName = "mapStringToObjectMap")
    Charge toDomainEntity(ChargeDynamoDbEntity entity);

    @Named("mapObjectToStringMap")
    default Map<String, String> mapObjectToStringMap(Map<String, Object> objectMap) {
        if (objectMap == null) {
            return null;
        }
        return objectMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null) // Evita NPE se valor for null
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue())));
    }

    @Named("mapStringToObjectMap")
    default Map<String, Object> mapStringToObjectMap(Map<String, String> stringMap) {
        if (stringMap == null) {
            return null;
        }
        return stringMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
