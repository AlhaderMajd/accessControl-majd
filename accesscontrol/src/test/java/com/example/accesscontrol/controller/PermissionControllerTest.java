package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.common.MessageResponse;
import com.example.accesscontrol.dto.common.PageResponse;
import com.example.accesscontrol.dto.permission.*;
import com.example.accesscontrol.service.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PermissionService permissionService;

    @Test
    void list_returnsPage() throws Exception {
        PermissionResponse p = PermissionResponse.builder().id(1L).name("READ").build();
        PageResponse<PermissionResponse> page = PageResponse.<PermissionResponse>builder()
                .items(List.of(p)).page(0).size(10).total(1).build();
        when(permissionService.getPermissions("", 0, 10)).thenReturn(page);

        mockMvc.perform(get("/api/permissions").param("search", "").param("page", "0").param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].name").value("READ"));
    }

    @Test
    void create_returns201() throws Exception {
        CreatePermissionsResponse resp = CreatePermissionsResponse.builder().message("ok").createdCount(1).build();
        when(permissionService.createPermissions(any(CreatePermissionsRequest.class))).thenReturn(resp);
        String body = "{\"items\":[{\"name\":\"READ\"}]}";
        mockMvc.perform(post("/api/permissions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.createdCount").value(1));
    }

    @Test
    void details_returnsOk() throws Exception {
        PermissionResponse p = PermissionResponse.builder().id(9L).name("EXEC").build();
        when(permissionService.getPermissionDetails(9L)).thenReturn(p);
        mockMvc.perform(get("/api/permissions/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("EXEC"));
    }

    @Test
    void updateName_returnsOk() throws Exception {
        UpdatePermissionNameResponse resp = UpdatePermissionNameResponse.builder().message("updated").build();
        when(permissionService.updatePermissionName(eq(3L), any(UpdatePermissionNameRequest.class))).thenReturn(resp);
        String body = "{\"name\":\"RUN\"}";
        mockMvc.perform(put("/api/permissions/3").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("updated"));
    }

    @Test
    void delete_returnsOk() throws Exception {
        MessageResponse resp = MessageResponse.builder().message("deleted").build();
        when(permissionService.deletePermissions(any())).thenReturn(resp);
        String body = "[1,2]";
        mockMvc.perform(delete("/api/permissions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("deleted"));
    }
}
