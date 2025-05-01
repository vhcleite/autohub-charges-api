package com.fiap.autohub.autohub_charges_api.application.config; // Verifique o pacote

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do OpenAPI (Swagger) para a API de Cobranças.
 * Define informações gerais da API e o esquema de segurança JWT Bearer.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "AutoHub Charges API",
                version = "v1",
                description = "API responsável pelo gerenciamento de cobranças e callbacks de pagamento."
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT Authorization header using the Bearer scheme. Example: \"Authorization: Bearer {token}\"",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
