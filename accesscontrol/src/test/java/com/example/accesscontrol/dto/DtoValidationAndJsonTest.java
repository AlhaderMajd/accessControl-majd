package com.example.accesscontrol.dto;

import com.example.accesscontrol.dto.auth.AuthRequest;
import com.example.accesscontrol.dto.auth.AuthResponse;
import com.example.accesscontrol.dto.common.MessageResponse;
import com.example.accesscontrol.dto.role.CreateRoleRequest;
import com.example.accesscontrol.dto.permission.UpdatePermissionNameRequest;
import com.example.accesscontrol.dto.user.assignRolesToUser.AssignRolesRequest;
import com.example.accesscontrol.dto.user.createUsers.CreateUserRequest;
import com.example.accesscontrol.dto.user.createUsers.CreateUsersRequest;
import com.example.accesscontrol.dto.user.getUsers.GetUsersResponse;
import com.example.accesscontrol.dto.user.getUsers.UserSummaryResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DtoValidationAndJsonTest {

    private static ValidatorFactory factory;
    private static Validator validator;
    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void setupValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void authRequest_validation_violationsOnInvalid() {
        AuthRequest dto = new AuthRequest();
        dto.setEmail("not-an-email");
        dto.setPassword(" "); // blank

        Set<ConstraintViolation<AuthRequest>> violations = validator.validate(dto);
        // Should have violations for email format and not blank password
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void createUserRequest_builder_getters_and_validation() {
        CreateUserRequest dto = CreateUserRequest.builder()
                .email("john.doe@example.com")
                .password("secret1")
                .enabled(true)
                .build();

        assertEquals("john.doe@example.com", dto.getEmail());
        assertTrue(dto.isEnabled());

        // Now break validation
        dto.setPassword("123"); // too short
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(dto);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void createUsersRequest_notEmpty_and_nestedValidations() {
        // Empty list should violate @NotEmpty
        CreateUsersRequest empty = CreateUsersRequest.builder().users(new ArrayList<>()).build();
        Set<ConstraintViolation<CreateUsersRequest>> v1 = validator.validate(empty);
        assertTrue(v1.stream().anyMatch(v -> v.getPropertyPath().toString().equals("users")));

        // Nested invalid email should be caught via @Valid
        CreateUserRequest badUser = CreateUserRequest.builder()
                .email("bad")
                .password("secret1")
                .build();
        CreateUsersRequest nested = CreateUsersRequest.builder().users(List.of(badUser)).build();
        Set<ConstraintViolation<CreateUsersRequest>> v2 = validator.validate(nested);
        assertFalse(v2.isEmpty());
        // Ensure violation path mentions users
        assertTrue(v2.stream().anyMatch(v -> v.getPropertyPath().toString().startsWith("users")));
    }

    @Test
    void assignRolesRequest_notEmpty_and_elementNotNull() {
        AssignRolesRequest dto = new AssignRolesRequest();
        dto.setUserIds(new java.util.ArrayList<>(java.util.Arrays.asList(1L, null))); // use null-permitting list to trigger @NotNull element constraint
        dto.setRoleIds(List.of()); // empty list should violate @NotEmpty

        Set<ConstraintViolation<AssignRolesRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        // at least one violation for userIds element and one for roleIds list
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().startsWith("userIds")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("roleIds")));
    }

    @Test
    void messageResponse_builder_and_equalsHashCode() {
        MessageResponse a = MessageResponse.builder().message("ok").build();
        MessageResponse b = MessageResponse.builder().message("ok").build();
        MessageResponse c = MessageResponse.builder().message("different").build();

        assertEquals(a, b);
        assertNotEquals(a, c);
        assertEquals("ok", a.getMessage());
    }

    @Test
    void getUsersResponse_lombok_data_equality_and_accessors() {
        UserSummaryResponse u1 = UserSummaryResponse.builder().id(1L).email("a@b.com").enabled(true).build();
        UserSummaryResponse u2 = UserSummaryResponse.builder().id(2L).email("c@d.com").enabled(false).build();

        GetUsersResponse r1 = new GetUsersResponse(List.of(u1, u2), 0, 2L);
        GetUsersResponse r2 = new GetUsersResponse(List.of(u1, u2), 0, 2L);

        assertEquals(r1, r2);
        assertEquals(2, r1.getUsers().size());
        assertEquals(0, r1.getPage());
        assertEquals(2L, r1.getTotal());
    }

    @Test
    void authResponse_json_serialization_and_deserialization() throws Exception {
        AuthResponse resp = AuthResponse.builder()
                .token("T")
                .userId(42L)
                .roles(List.of("ADMIN", "USER"))
                .build();

        String json = mapper.writeValueAsString(resp);
        Map<String, Object> asMap = mapper.readValue(json, new TypeReference<>() {});
        assertTrue(asMap.containsKey("token"));
        assertTrue(asMap.containsKey("userId"));
        assertTrue(asMap.containsKey("roles"));
        assertEquals("42", asMap.get("userId").toString());

        // round trip
        AuthResponse back = mapper.readValue(json, AuthResponse.class);
        assertEquals(resp, back);
    }

    @Test
    void createRoleRequest_validation() {
        CreateRoleRequest dto = new CreateRoleRequest();
        dto.setName(" "); // blank
        dto.setPermissionIds(null); // optional allowed
        Set<ConstraintViolation<CreateRoleRequest>> violations = validator.validate(dto);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));

        dto.setName("role-name");
        violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void updatePermissionNameRequest_validation() {
        UpdatePermissionNameRequest dto = new UpdatePermissionNameRequest();
        dto.setName("");
        Set<ConstraintViolation<UpdatePermissionNameRequest>> v1 = validator.validate(dto);
        assertFalse(v1.isEmpty());

        dto.setName("a".repeat(101)); // exceed max 100
        Set<ConstraintViolation<UpdatePermissionNameRequest>> v2 = validator.validate(dto);
        assertFalse(v2.isEmpty());

        dto.setName("Valid Name");
        Set<ConstraintViolation<UpdatePermissionNameRequest>> v3 = validator.validate(dto);
        assertTrue(v3.isEmpty());
    }
}
