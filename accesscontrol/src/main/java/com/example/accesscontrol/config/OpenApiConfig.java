package com.example.accesscontrol.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI accessControlOpenAPI() {
        final String bearerScheme = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Access Control API")
                        .description("RBAC, Users, Roles, Groups, Permissions")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(bearerScheme))
                .components(new Components()
                        .addSecuritySchemes(bearerScheme, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
