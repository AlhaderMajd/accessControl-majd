package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.role.*;
import com.example.accesscontrol.service.RoleService;
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

@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleService roleService;

    @Test
    void getRoles_returnsItems() throws Exception {
        RoleResponse r1 = new RoleResponse();
        r1.setId(1L);
        r1.setName("ADMIN");
        GetRolesResponse resp = GetRolesResponse.builder().roles(List.of(r1)).page(0).total(1).build();
        when(roleService.getRoles("", 0, 10)).thenReturn(resp);

        mockMvc.perform(get("/api/roles").param("search", "").param("page", "0").param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasSize(1)))
                .andExpect(jsonPath("$.roles[0].name").value("ADMIN"));
    }

    @Test
    void createRoles_returns201() throws Exception {
        CreateRoleResponse cr = CreateRoleResponse.builder().message("ok").created(List.of("ADMIN","USER")).build();
        when(roleService.createRoles(any())).thenReturn(cr);
        String body = "[{\"name\":\"NEW\"}]";
        mockMvc.perform(post("/api/roles").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created", hasSize(2)))
                .andExpect(jsonPath("$.message").value("ok"));
    }

    @Test
    void getRoleById_returnsDetails() throws Exception {
        com.example.accesscontrol.dto.permission.PermissionResponse p = com.example.accesscontrol.dto.permission.PermissionResponse.builder().id(1L).name("P1").build();
                RoleDetailsResponse rd = RoleDetailsResponse.builder().id(5L).name("MANAGER").permissions(List.of(p)).build();
        when(roleService.getRoleWithPermissions(5L)).thenReturn(rd);
        mockMvc.perform(get("/api/roles/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("MANAGER"));
    }

    @Test
    void updateRoleName_returnsUpdated() throws Exception {
        UpdateRoleResponse ur = UpdateRoleResponse.builder().message("updated").build();
        when(roleService.updateRoleName(eq(7L), any(UpdateRoleRequest.class))).thenReturn(ur);
        String body = "{\"name\":\"LEAD\"}";
        mockMvc.perform(put("/api/roles/7").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("updated"));
    }

    @Test
    void assignPermissions_returnsMessage() throws Exception {
        when(roleService.assignPermissionsToRoles(any())).thenReturn("ok");
        String body = "[{\"roleId\":1,\"permissionIds\":[1,2]}]";
        mockMvc.perform(post("/api/roles/assign-permissions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("ok"));
    }

    @Test
    void deassignPermissions_returnsMessage() throws Exception {
        when(roleService.deassignPermissionsFromRoles(any())).thenReturn("done");
        String body = "[{\"roleId\":1,\"permissionIds\":[1]}]";
        mockMvc.perform(delete("/api/roles/deassign-permissions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("done"));
    }

    @Test
    void assignRolesToGroups_returnsMessage() throws Exception {
        when(roleService.assignRolesToGroups(any())).thenReturn("assigned");
        String body = "[{\"groupId\":1,\"roleIds\":[2]}]";
        mockMvc.perform(post("/api/roles/groups/assign-roles").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("assigned"));
    }

    @Test
    void deassignRolesFromGroups_returnsMessage() throws Exception {
        when(roleService.deassignRolesFromGroups(any())).thenReturn("removed");
        String body = "[{\"groupId\":1,\"roleIds\":[2]}]";
        mockMvc.perform(delete("/api/roles/groups/deassign-roles").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("removed"));
    }

    @Test
    void deleteRoles_returnsMessage() throws Exception {
        when(roleService.deleteRoles(any())).thenReturn("deleted");
        String body = "{\"roleIds\":[1,2]}";
        mockMvc.perform(delete("/api/roles").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("deleted"));
    }
}
