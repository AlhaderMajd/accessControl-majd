package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.common.MessageResponse;
import com.example.accesscontrol.dto.common.PageResponse;
import com.example.accesscontrol.dto.permission.*;
import com.example.accesscontrol.exception.DuplicateResourceException;
import com.example.accesscontrol.exception.GlobalExceptionHandler;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.service.PermissionService;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(PermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PermissionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private PermissionService permissionService;

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    @Nested
    @DisplayName("POST /api/permissions (create)")
    class CreatePermissionsTests {

        @Test
        @DisplayName("201 Created on valid payload")
        void create_ok_201() throws Exception {
            var req = new CreatePermissionsRequest();
            req.setPermissions(List.of("USER_READ", "USER_WRITE"));

            var resp = CreatePermissionsResponse.builder()
                    .message("Permissions created successfully")
                    .createdCount(2)
                    .items(List.of(
                            PermissionResponse.builder().id(10L).name("USER_READ").build(),
                            PermissionResponse.builder().id(11L).name("USER_WRITE").build()
                    ))
                    .build();

            when(permissionService.createPermissions(any(CreatePermissionsRequest.class))).thenReturn(resp);

            mockMvc.perform(post("/api/permissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.createdCount").value(2))
                    .andExpect(jsonPath("$.items[0].name").value("USER_READ"));

            ArgumentCaptor<CreatePermissionsRequest> cap = ArgumentCaptor.forClass(CreatePermissionsRequest.class);
            verify(permissionService).createPermissions(cap.capture());
            assertThat(cap.getValue().getPermissions()).containsExactly("USER_READ", "USER_WRITE");
        }

        @Test
        @DisplayName("400 Bad Request when DTO validation fails (@NotEmpty)")
        void create_validation_400() throws Exception {
            var req = new CreatePermissionsRequest();
            req.setPermissions(List.of());

            mockMvc.perform(post("/api/permissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(permissionService);
        }

        @Test
        @DisplayName("409 Conflict when service detects duplicates")
        void create_duplicate_409() throws Exception {
            var req = new CreatePermissionsRequest();
            req.setPermissions(List.of("USER_READ", "USER_READ"));

            when(permissionService.createPermissions(any(CreatePermissionsRequest.class)))
                    .thenThrow(new DuplicateResourceException("Permissions already exist: [USER_READ]"));

            mockMvc.perform(post("/api/permissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Permissions already exist: [USER_READ]"));
        }

        @Test
        @DisplayName("500 Internal Server Error on unexpected service error")
        void create_unexpected_500() throws Exception {
            var req = new CreatePermissionsRequest();
            req.setPermissions(List.of("USER_EXPORT"));

            when(permissionService.createPermissions(any(CreatePermissionsRequest.class)))
                    .thenThrow(new RuntimeException("boom"));

            mockMvc.perform(post("/api/permissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error", containsString("Something went wrong")));
        }
    }

    @Nested
    @DisplayName("GET /api/permissions (list with search & pagination)")
    class ListPermissionsTests {

        @Test
        @DisplayName("200 OK with page payload")
        void list_ok_200() throws Exception {
            var page = PageResponse.<PermissionResponse>builder()
                    .items(List.of(PermissionResponse.builder().id(1L).name("USER_READ").build()))
                    .page(0)
                    .size(1)
                    .total(2L)
                    .build();

            when(permissionService.getPermissions(eq("USER"), eq(0), eq(1))).thenReturn(page);

            mockMvc.perform(get("/api/permissions")
                            .param("search", "USER")
                            .param("page", "0")
                            .param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].name").value("USER_READ"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(1))
                    .andExpect(jsonPath("$.total").value(2));

            verify(permissionService).getPermissions("USER", 0, 1);
        }

        @Test
        @DisplayName("400 Bad Request on invalid page/size (controller validation)")
        void list_badPagination_400() throws Exception {
            mockMvc.perform(get("/api/permissions")
                            .param("search", "")
                            .param("page", "-1")  // @Min(0)
                            .param("size", "0"))  // @Min(1)
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(permissionService);
        }
    }

    @Nested
    @DisplayName("GET /api/permissions/{permissionId} (details)")
    class DetailsTests {

        @Test
        @DisplayName("200 OK returns PermissionResponse")
        void details_ok_200() throws Exception {
            var resp = PermissionResponse.builder().id(10L).name("USER_EXPORT").build();

            when(permissionService.getPermissionDetails(10L)).thenReturn(resp);

            mockMvc.perform(get("/api/permissions/{permissionId}", 10))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.name").value("USER_EXPORT"));

            verify(permissionService).getPermissionDetails(10L);
        }

        @Test
        @DisplayName("400 Bad Request when permissionId < 1")
        void details_badId_400() throws Exception {
            mockMvc.perform(get("/api/permissions/{permissionId}", 0))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(permissionService);
        }

        @Test
        @DisplayName("404 Not Found when service throws ResourceNotFoundException")
        void details_notFound_404() throws Exception {
            when(permissionService.getPermissionDetails(999L))
                    .thenThrow(new ResourceNotFoundException("Permission not found"));

            mockMvc.perform(get("/api/permissions/{permissionId}", 999))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Permission not found"));
        }
    }

    @Nested
    @DisplayName("PUT /api/permissions/{permissionId} (update name)")
    class UpdateNameTests {

        @Test
        @DisplayName("200 OK on valid update")
        void update_ok_200() throws Exception {
            var req = new UpdatePermissionNameRequest();
            req.setName("USER_READ_ALL");

            var resp = UpdatePermissionNameResponse.builder()
                    .message("Permission updated successfully")
                    .id(10L)
                    .oldName("USER_READ")
                    .newName("USER_READ_ALL")
                    .build();

            when(permissionService.updatePermissionName(eq(10L), any(UpdatePermissionNameRequest.class)))
                    .thenReturn(resp);

            mockMvc.perform(put("/api/permissions/{permissionId}", 10)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Permission updated successfully"))
                    .andExpect(jsonPath("$.newName").value("USER_READ_ALL"));

            ArgumentCaptor<UpdatePermissionNameRequest> cap = ArgumentCaptor.forClass(UpdatePermissionNameRequest.class);
            verify(permissionService).updatePermissionName(eq(10L), cap.capture());
            assertThat(cap.getValue().getName()).isEqualTo("USER_READ_ALL");
        }

        @Test
        @DisplayName("400 Bad Request when permissionId < 1")
        void update_badId_400() throws Exception {
            var req = new UpdatePermissionNameRequest();
            req.setName("X");

            mockMvc.perform(put("/api/permissions/{permissionId}", 0)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(permissionService);
        }

        @Test
        @DisplayName("400 Bad Request when body invalid (@NotBlank)")
        void update_badBody_400() throws Exception {
            var req = new UpdatePermissionNameRequest();
            req.setName("");

            mockMvc.perform(put("/api/permissions/{permissionId}", 12)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(permissionService);
        }

        @Test
        @DisplayName("404 Not Found when service throws ResourceNotFoundException")
        void update_notFound_404() throws Exception {
            var req = new UpdatePermissionNameRequest();
            req.setName("NEW");

            when(permissionService.updatePermissionName(eq(13L), any(UpdatePermissionNameRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Permission not found"));

            mockMvc.perform(put("/api/permissions/{permissionId}", 13)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Permission not found"));
        }

        @Test
        @DisplayName("409 Conflict when service throws DuplicateResourceException")
        void update_duplicate_409() throws Exception {
            var req = new UpdatePermissionNameRequest();
            req.setName("USER_READ"); // duplicate target

            when(permissionService.updatePermissionName(eq(14L), any(UpdatePermissionNameRequest.class)))
                    .thenThrow(new DuplicateResourceException("Permission name already exists"));

            mockMvc.perform(put("/api/permissions/{permissionId}", 14)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Permission name already exists"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/permissions (batch)")
    class DeletePermissionsTests {

        @Test
        @DisplayName("200 OK on valid delete")
        void delete_ok_200() throws Exception {
            var ids = List.of(10L, 11L);
            var resp = MessageResponse.builder().message("Permissions deleted successfully").build();

            when(permissionService.deletePermissions(eq(ids))).thenReturn(resp);

            mockMvc.perform(delete("/api/permissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(ids)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Permissions deleted successfully"));

            verify(permissionService).deletePermissions(eq(ids));
        }

        @Test
        @DisplayName("400 Bad Request when service throws IllegalArgumentException")
        void delete_illegalArg_400() throws Exception {
            var ids = List.of();

            when(permissionService.deletePermissions(anyList()))
                    .thenThrow(new IllegalArgumentException("No permission IDs provided"));

            mockMvc.perform(delete("/api/permissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(ids)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("No permission IDs provided"));
        }

        @Test
        @DisplayName("404 Not Found when service throws ResourceNotFoundException")
        void delete_notFound_404() throws Exception {
            var ids = List.of(999L);

            when(permissionService.deletePermissions(eq(ids)))
                    .thenThrow(new ResourceNotFoundException("No matching permissions found"));

            mockMvc.perform(delete("/api/permissions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(ids)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("No matching permissions found"));
        }
    }
}
