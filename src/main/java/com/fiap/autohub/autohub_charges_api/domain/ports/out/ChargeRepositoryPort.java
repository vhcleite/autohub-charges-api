package com.fiap.autohub.autohub_charges_api.domain.ports.out;

import com.fiap.autohub.autohub_charges_api.domain.entities.Charge;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para persistência de Cobranças (ex: DynamoDB).
 */
public interface ChargeRepositoryPort {

    Charge save(Charge charge);

    Optional<Charge> findById(String chargeId);

    Optional<Charge> findBySaleId(UUID saleId);

    /**
     * Deleta uma cobrança pelo seu ID.
     *
     * @param chargeId O ID da cobrança a ser deletada.
     */
    void deleteById(String chargeId);
}
