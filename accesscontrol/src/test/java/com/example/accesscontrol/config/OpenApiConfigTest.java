package com.example.accesscontrol.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    @Test
    void accessControlOpenAPI_containsExpectedInfoAndSecurity() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.accessControlOpenAPI();

        assertNotNull(openAPI, "OpenAPI bean should not be null");

        Info info = openAPI.getInfo();
        assertNotNull(info, "OpenAPI info should be present");
        assertEquals("Access Control API", info.getTitle());
        assertEquals("RBAC, Users, Roles, Groups, Permissions", info.getDescription());
        assertEquals("v1", info.getVersion());

        assertNotNull(openAPI.getSecurity());
        assertTrue(openAPI.getSecurity().stream()
                .anyMatch(sr -> sr.containsKey("bearerAuth")), "Security requirements should include bearerAuth");

        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        assertNotNull(scheme, "bearerAuth security scheme should be defined");
        assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
        assertEquals("bearer", scheme.getScheme());
        assertEquals("JWT", scheme.getBearerFormat());

        boolean requirementMatches = openAPI.getSecurity().stream()
                .map(SecurityRequirement::keySet)
                .anyMatch(keys -> keys.contains("bearerAuth"));
        assertTrue(requirementMatches, "Security requirement should refer to bearerAuth scheme");
    }
}
