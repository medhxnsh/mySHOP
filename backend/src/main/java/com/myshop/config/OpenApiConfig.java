package com.myshop.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenApiConfig — Configures the Swagger UI and OpenAPI specification.
 *
 * WHY OPENAPI?
 * OpenAPI (formerly Swagger) is a standard for documenting REST APIs.
 * SpringDoc automatically reads your @RestController, @RequestMapping,
 * @RequestBody, @ApiResponse annotations and generates:
 * 1. A /v3/api-docs JSON endpoint (machine-readable spec)
 * 2. A /swagger-ui.html UI (human-readable interactive docs)
 *
 * Accessible at: http://localhost:8080/swagger-ui.html
 *
 * WHY CONFIGURE SECURITY SCHEME HERE?
 * We need to tell Swagger UI that our endpoints require a Bearer JWT.
 * This lets developers click "Authorize" in Swagger UI and enter their
 * token, so they can test protected endpoints directly from the browser.
 */
@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI myShopOpenAPI() {
                // The name of the security scheme — referenced in @SecurityRequirement
                final String securitySchemeName = "bearerAuth";

                return new OpenAPI()
                                .info(new Info()
                                                .title("myShop API")
                                                .description("E-commerce platform REST API")
                                                .version("1.0.0")
                                                .license(new License()
                                                                .name("MIT")))
                                // Declare global security: all endpoints require Bearer JWT by default.
                                // Individual endpoints can override this with @SecurityRequirements({})
                                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                                .components(new Components()
                                                .addSecuritySchemes(securitySchemeName,
                                                                new SecurityScheme()
                                                                                .name(securitySchemeName)
                                                                                // HTTP scheme — uses the Authorization
                                                                                // header
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                // Bearer scheme — value is: "Bearer
                                                                                // <token>"
                                                                                .scheme("bearer")
                                                                                // Format hint for documentation only
                                                                                .bearerFormat("JWT")));
        }
}
