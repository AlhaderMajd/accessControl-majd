package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.group.AssignRolesToGroupsRequest;
import com.example.accesscontrol.dto.role.*;
import com.example.accesscontrol.dto.permission.PermissionResponse;
import com.example.accesscontrol.exception.DuplicateResourceException;
import com.example.accesscontrol.exception.GlobalExceptionHandler;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RoleControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private RoleService roleService;

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    @Nested
    @DisplayName("POST /api/roles (create roles)")
    class CreateRolesTests {

        @Test
        @DisplayName("201 Created on valid list")
        void create_ok_201() throws Exception {
            var body = List.of(
                    new CreateRoleRequest() {{ setName("ADMIN"); setPermissionIds(List.of(1L)); }},
                    new CreateRoleRequest() {{ setName("MEMBER"); setPermissionIds(List.of()); }}
            );

            var resp = CreateRoleResponse.builder()
                    .message("Roles created successfully")
                    .created(List.of("ADMIN", "MEMBER"))
                    .build();

            when(roleService.createRoles(anyList())).thenReturn(resp);

            mockMvc.perform(post("/api/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Roles created successfully"))
                    .andExpect(jsonPath("$.created[0]").value("ADMIN"));

            ArgumentCaptor<List<CreateRoleRequest>> cap = ArgumentCaptor.forClass(List.class);
            verify(roleService).createRoles(cap.capture());
            assertThat(cap.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("400 Bad Request when payload has invalid item (@NotBlank)")
        void create_validation_400() throws Exception {
            var body = List.of(new CreateRoleRequest() {{ setName(""); }});

            mockMvc.perform(post("/api/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(roleService);
        }

        @Test
        @DisplayName("409 Conflict when duplicate role names")
        void create_duplicate_409() throws Exception {
            var body = List.of(new CreateRoleRequest() {{ setName("ADMIN"); }});
            when(roleService.createRoles(anyList()))
                    .thenThrow(new DuplicateResourceException("Some role names already exist: [ADMIN]"));

            mockMvc.perform(post("/api/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Some role names already exist: [ADMIN]"));
        }

        @Test
        @DisplayName("500 Internal Server Error on unexpected service error")
        void create_unexpected_500() throws Exception {
            var body = List.of(new CreateRoleRequest() {{ setName("OPS"); }});
            when(roleService.createRoles(anyList())).thenThrow(new RuntimeException("boom"));

            mockMvc.perform(post("/api/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error", containsString("Something went wrong")));
        }
    }

    @Nested
    @DisplayName("GET /api/roles (list with search & pagination)")
    class ListRolesTests {

        @Test
        @DisplayName("200 OK with page payload")
        void list_ok_200() throws Exception {
            var page = GetRolesResponse.builder()
                    .roles(List.of(RoleResponse.builder().id(1L).name("ADMIN").build()))
                    .page(0)
                    .total(2L)
                    .build();

            when(roleService.getRoles(eq("A"), eq(0), eq(1))).thenReturn(page);

            mockMvc.perform(get("/api/roles")
                            .param("search", "A")
                            .param("page", "0")
                            .param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roles[0].name").value("ADMIN"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.total").value(2));

            verify(roleService).getRoles("A", 0, 1);
        }

        @Test
        @DisplayName("400 Bad Request on invalid page/size")
        void list_badPagination_400() throws Exception {
            mockMvc.perform(get("/api/roles")
                            .param("search", "")
                            .param("page", "-1")
                            .param("size", "0"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(roleService);
        }
    }

    @Nested
    @DisplayName("GET /api/roles/{roleId} (details)")
    class DetailsTests {

        @Test
        @DisplayName("200 OK returns role with permissions")
        void details_ok_200() throws Exception {
            var details = RoleDetailsResponse.builder()
                    .id(10L).name("ADMIN")
                    .permissions(List.of(
                            PermissionResponse.builder().id(1L).name("USER_READ").build()
                    ))
                    .build();

            when(roleService.getRoleWithPermissions(10L)).thenReturn(details);

            mockMvc.perform(get("/api/roles/{roleId}", 10))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.name").value("ADMIN"))
                    .andExpect(jsonPath("$.permissions[0].name").value("USER_READ"));

            verify(roleService).getRoleWithPermissions(10L);
        }

        @Test
        @DisplayName("400 Bad Request when roleId < 1")
        void details_badId_400() throws Exception {
            mockMvc.perform(get("/api/roles/{roleId}", 0))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(roleService);
        }

        @Test
        @DisplayName("404 Not Found when service throws ResourceNotFoundException")
        void details_notFound_404() throws Exception {
            when(roleService.getRoleWithPermissions(999L))
                    .thenThrow(new ResourceNotFoundException("Role not found"));

            mockMvc.perform(get("/api/roles/{roleId}", 999))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Role not found"));
        }
    }

    @Nested
    @DisplayName("PUT /api/roles/{roleId} (update name)")
    class UpdateNameTests {

        @Test
        @DisplayName("200 OK on valid update")
        void update_ok_200() throws Exception {
            var req = new UpdateRoleRequest();
            req.setName("BASIC_MEMBER");

            var resp = UpdateRoleResponse.builder()
                    .message("Role name updated successfully")
                    .build();

            when(roleService.updateRoleName(eq(11L), any(UpdateRoleRequest.class)))
                    .thenReturn(resp);

            mockMvc.perform(put("/api/roles/{roleId}", 11)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Role name updated successfully"));

            ArgumentCaptor<UpdateRoleRequest> cap = ArgumentCaptor.forClass(UpdateRoleRequest.class);
            verify(roleService).updateRoleName(eq(11L), cap.capture());
            assertThat(cap.getValue().getName()).isEqualTo("BASIC_MEMBER");
        }

        @Test
        @DisplayName("400 Bad Request when roleId < 1")
        void update_badId_400() throws Exception {
            var req = new UpdateRoleRequest(); req.setName("X");
            mockMvc.perform(put("/api/roles/{roleId}", 0)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(roleService);
        }

        @Test
        @DisplayName("400 Bad Request when body invalid (@NotBlank)")
        void update_badBody_400() throws Exception {
            var req = new UpdateRoleRequest(); req.setName("");

            mockMvc.perform(put("/api/roles/{roleId}", 12)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(roleService);
        }

        @Test
        @DisplayName("404 Not Found when service throws ResourceNotFoundException")
        void update_notFound_404() throws Exception {
            var req = new UpdateRoleRequest(); req.setName("NEW");
            when(roleService.updateRoleName(eq(13L), any(UpdateRoleRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Role not found"));

            mockMvc.perform(put("/api/roles/{roleId}", 13)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Role not found"));
        }

        @Test
        @DisplayName("409 Conflict when duplicate role name")
        void update_duplicate_409() throws Exception {
            var req = new UpdateRoleRequest(); req.setName("ADMIN");
            when(roleService.updateRoleName(eq(14L), any(UpdateRoleRequest.class)))
                    .thenThrow(new DuplicateResourceException("Role name already exists"));

            mockMvc.perform(put("/api/roles/{roleId}", 14)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Role name already exists"));
        }
    }

    @Nested
    @DisplayName("POST /api/roles/assign-permissions")
    class AssignPermissionsTests {

        @Test
        @DisplayName("200 OK on valid assignment")
        void assign_ok_200() throws Exception {
            var items = List.of(
                    new AssignPermissionsToRolesRequest() {{
                        setRoleId(11L); setPermissionIds(List.of(1L, 2L));
                    }}
            );

            when(roleService.assignPermissionsToRoles(anyList()))
                    .thenReturn("Permissions assigned successfully. Total assignments: 2");

            mockMvc.perform(post("/api/roles/assign-permissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(items)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("Permissions assigned successfully")));
        }

        @Test
        @DisplayName("400 Bad Request when payload invalid (@NotNull/@NotEmpty)")
        void assign_validation_400() throws Exception {
            // use HashMap because Map.of(..) rejects nulls and would NPE before sending the request
            Map<String, Object> bad = new HashMap<>();
            bad.put("roleId", null);
            bad.put("permissionIds", List.of(1));

            var items = List.of(bad);

            mockMvc.perform(post("/api/roles/assign-permissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(items)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(roleService);
        }

        @Test
        @DisplayName("404 Not Found when service throws ResourceNotFoundException")
        void assign_notFound_404() throws Exception {
            var items = List.of(
                    new AssignPermissionsToRolesRequest() {{
                        setRoleId(99L); setPermissionIds(List.of(1L));
                    }}
            );
            when(roleService.assignPermissionsToRoles(anyList()))
                    .thenThrow(new ResourceNotFoundException("Some roles not found"));

            mockMvc.perform(post("/api/roles/assign-permissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(items)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Some roles not found"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/roles/deassign-permissions")
    class DeassignPermissionsTests {

        @Test
        @DisplayName("200 OK on valid deassignment")
        void deassign_ok_200() throws Exception {
            var items = List.of(
                    new AssignPermissionsToRolesRequest() {{
                        setRoleId(11L); setPermissionIds(List.of(1L));
                    }}
            );

            when(roleService.deassignPermissionsFromRoles(anyList()))
                    .thenReturn("Permissions removed successfully");

            mockMvc.perform(delete("/api/roles/deassign-permissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(items)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Permissions removed successfully"));
        }

        @Test
        @DisplayName("400 Bad Request on validation error")
        void deassign_validation_400() throws Exception {
            var items = List.of(
                    Map.of("roleId", 11, "permissionIds", List.of())
            );

            mockMvc.perform(delete("/api/roles/deassign-permissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(items)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(roleService);
        }

        @Test
        @DisplayName("404 Not Found when service throws ResourceNotFoundException")
        void deassign_notFound_404() throws Exception {
            var items = List.of(
                    new AssignPermissionsToRolesRequest() {{
                        setRoleId(123L); setPermissionIds(List.of(1L));
                    }}
            );
            when(roleService.deassignPermissionsFromRoles(anyList()))
                    .thenThrow(new ResourceNotFoundException("Role not found"));

            mockMvc.perform(delete("/api/roles/deassign-permissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(items)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Role not found"));
        }
    }

    @Nested
    @DisplayName("POST /api/roles/groups/assign-roles")
    class AssignRolesToGroupsTests {

        @Test
        @DisplayName("200 OK on valid assignment")
        void assignRoles_ok_200() throws Exception {
            var items = List.of(
                    new AssignRolesToGroupsRequest() {{
                        setGroupId(5L); setRoleIds(List.of(10L, 11L));
                    }}
            );

            when(roleService.assignRolesToGroups(anyList()))
                    .thenReturn("Roles assigned to groups successfully. Inserted: 2");

            mockMvc.perform(post("/api/roles/groups/assign-roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(items)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("assigned to groups")));
        }

        @Test
        @DisplayName("400 Bad Request when payload invalid")
        void assignRoles_validation_400() throws Exception {
            Map<String, Object> bad = new HashMap<>();
            bad.put("groupId", null);           // null triggers @NotNull
            bad.put("roleIds", List.of(1));     // just to shape the payload

            var items = List.of(bad);

            mockMvc.perform(post("/api/roles/groups/assign-roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(items)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(roleService);
        }

        @Test
        @DisplayName("404 Not Found when service throws ResourceNotFoundException")
        void assignRoles_notFound_404() throws Exception {
            var items = List.of(
                    new AssignRolesToGroupsRequest() {{ setGroupId(999L); setRoleIds(List.of(1L)); }}
            );
            when(roleService.assignRolesToGroups(anyList()))
                    .thenThrow(new ResourceNotFoundException("Group not found"));

            mockMvc.perform(post("/api/roles/groups/assign-roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(items)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Group not found"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/roles/groups/deassign-roles")
    class DeassignRolesFromGroupsTests {

        @Test
        @DisplayName("200 OK on valid deassignment")
        void deassignRoles_ok_200() throws Exception {
            var items = List.of(
                    new AssignRolesToGroupsRequest() {{ setGroupId(5L); setRoleIds(List.of(10L)); }}
            );

            when(roleService.deassignRolesFromGroups(anyList())).thenReturn("Roles deassigned from groups successfully");

            mockMvc.perform(delete("/api/roles/groups/deassign-roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(items)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Roles deassigned from groups successfully"));
        }

        @Test
        @DisplayName("400 Bad Request when payload invalid")
        void deassignRoles_validation_400() throws Exception {
            var items = List.of(
                    Map.of("groupId", 5, "roleIds", List.of())
            );

            mockMvc.perform(delete("/api/roles/groups/deassign-roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(items)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(roleService);
        }

        @Test
        @DisplayName("404 Not Found when service throws ResourceNotFoundException")
        void deassignRoles_notFound_404() throws Exception {
            var items = List.of(
                    new AssignRolesToGroupsRequest() {{ setGroupId(1000L); setRoleIds(List.of(1L)); }}
            );
            when(roleService.deassignRolesFromGroups(anyList()))
                    .thenThrow(new ResourceNotFoundException("Group not found"));

            mockMvc.perform(delete("/api/roles/groups/deassign-roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(items)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Group not found"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/roles (batch)")
    class DeleteRolesTests {

        @Test
        @DisplayName("200 OK on valid delete")
        void delete_ok_200() throws Exception {
            var body = Map.of("roleIds", List.of(10L, 11L));
            when(roleService.deleteRoles(eq(List.of(10L, 11L))))
                    .thenReturn("Roles deleted successfully");

            mockMvc.perform(delete("/api/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Roles deleted successfully"));

            verify(roleService).deleteRoles(eq(List.of(10L, 11L)));
        }

        @Test
        @DisplayName("400 Bad Request when service throws IllegalArgumentException")
        void delete_illegalArg_400() throws Exception {
            var body = Map.of("roleIds", List.of());
            when(roleService.deleteRoles(anyList()))
                    .thenThrow(new IllegalArgumentException("No role IDs provided"));

            mockMvc.perform(delete("/api/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("No role IDs provided"));
        }

        @Test
        @DisplayName("404 Not Found when service throws ResourceNotFoundException")
        void delete_notFound_404() throws Exception {
            var body = Map.of("roleIds", List.of(999L));
            when(roleService.deleteRoles(eq(List.of(999L))))
                    .thenThrow(new ResourceNotFoundException("One or more role IDs do not exist"));

            mockMvc.perform(delete("/api/roles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("One or more role IDs do not exist"));
        }
    }
}
