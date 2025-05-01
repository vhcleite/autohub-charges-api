package com.fiap.autohub.autohub_charges_api.domain.ports.out; // Ajuste o pacote se necessário

import com.fiap.autohub.autohub_charges_api.domain.entities.Charge;

/**
 * Porta de saída para abstrair o envio de notificações (ex: e-mail).
 * A implementação real será feita posteriormente.
 */
public interface NotificationServicePort {

    /**
     * Envia as instruções de pagamento para o comprador.
     *
     * @param charge     A cobrança criada contendo os detalhes necessários.
     * @param buyerEmail O e-mail do comprador (idealmente vindo do evento).
     */
    void sendPaymentInstructions(Charge charge, String buyerEmail); // Assumindo que o email virá de algum lugar

}
