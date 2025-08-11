package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.user.assignRolesToUser.AssignRolesRequest;
import com.example.accesscontrol.dto.user.assignRolesToUser.AssignRolesResponse;
import com.example.accesscontrol.dto.user.assignUsersToGroup.AssignUsersToGroupsRequest;
import com.example.accesscontrol.dto.user.assignUsersToGroup.AssignUsersToGroupsResponse;
import com.example.accesscontrol.dto.user.createUsers.CreateUsersRequest;
import com.example.accesscontrol.dto.user.createUsers.CreateUsersResponse;
import com.example.accesscontrol.dto.user.deassignUsersFromGroups.DeassignUsersFromGroupsRequest;
import com.example.accesscontrol.dto.user.deassignUsersFromGroups.DeassignUsersFromGroupsResponse;
import com.example.accesscontrol.dto.user.deassignUsersFromUsers.DeassignRolesRequest;
import com.example.accesscontrol.dto.user.deassignUsersFromUsers.DeassignRolesResponse;
import com.example.accesscontrol.dto.user.deleteUsers.DeleteUsersRequest;
import com.example.accesscontrol.dto.user.deleteUsers.DeleteUsersResponse;
import com.example.accesscontrol.dto.user.getUsers.GetUsersResponse;
import com.example.accesscontrol.dto.user.getUsers.UserResponse;
import com.example.accesscontrol.dto.user.updateCredentials.AdminUpdateCredentialsRequest;
import com.example.accesscontrol.dto.user.updateCredentials.AdminUpdateCredentialsResponse;
import com.example.accesscontrol.dto.user.updateUserStatus.UpdateUserStatusRequest;
import com.example.accesscontrol.dto.user.updateUserStatus.UpdateUserStatusResponse;
import com.example.accesscontrol.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void createUsers_returns201() throws Exception {
        CreateUsersResponse resp = new CreateUsersResponse();
        resp.setCreatedUserIds(List.of(1L, 2L));
        resp.setAssignedRoles(List.of("MEMBER"));
        when(userService.createUsers(any(CreateUsersRequest.class))).thenReturn(resp);

        String body = "{\"users\":[{\"email\":\"a@b.com\",\"password\":\"pass\"}]}";

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdUserIds", contains(1,2)))
                .andExpect(jsonPath("$.assignedRoles[0]", is("MEMBER")));
    }

    @Test
    void getUsers_invalidParams_returns400() throws Exception {
        mockMvc.perform(get("/api/users").param("page", "-1").param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUsers_valid_returnsOkWithItems() throws Exception {
        GetUsersResponse resp = new GetUsersResponse(List.of(new com.example.accesscontrol.dto.user.getUsers.UserSummaryResponse(1L, "x@y.com", true)), 0, 1);
        when(userService.getUsers("", 0, 10)).thenReturn(resp);
        mockMvc.perform(get("/api/users").param("search", "").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].email", is("x@y.com")));
    }

    @Test
    void getUserDetails_returnsOkBody() throws Exception {
        UserResponse ur = UserResponse.builder().id(5L).email("u@e.com").enabled(true).roles(List.of("USER")).groups(List.of()).build();
        when(userService.getUserDetails(5L)).thenReturn(ur);
        mockMvc.perform(get("/api/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("u@e.com")));
    }

    @Test
    void changePassword_returnsOkMessage() throws Exception {
        String body = "{\"oldPassword\":\"a\",\"newPassword\":\"b\"}";
        mockMvc.perform(put("/api/users/change-password").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Password updated successfully")));
    }

    @Test
    void changeEmail_returnsOkMessage() throws Exception {
        String body = "{\"newEmail\":\"new@e.com\"}";
        mockMvc.perform(put("/api/users/email").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Email updated successfully")));
    }

    @Test
    void updateUserStatus_returnsOk() throws Exception {
        UpdateUserStatusResponse resp = new UpdateUserStatusResponse();
        resp.setUpdatedCount(1);
        when(userService.updateUserStatus(any(UpdateUserStatusRequest.class))).thenReturn(resp);
        String body = "{\"userIds\":[1],\"enabled\":true}";
        mockMvc.perform(put("/api/users/status").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount", is(1)));
    }

    @Test
    void assignRoles_returnsOk() throws Exception {
        AssignRolesResponse resp = new AssignRolesResponse();
        resp.setAssignedCount(2);
        when(userService.assignRolesToUsers(any(AssignRolesRequest.class))).thenReturn(resp);

        String body = "{\"userIds\":[1,2],\"roleIds\":[3]}";

        mockMvc.perform(post("/api/users/roles/assign").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedCount", is(2)));
    }

    @Test
    void deassignRoles_returnsOk() throws Exception {
        DeassignRolesResponse resp = DeassignRolesResponse.builder()
                .removedCount(1)
                .build();
        when(userService.deassignRolesFromUsers(any(DeassignRolesRequest.class))).thenReturn(resp);
        String body = "{\"userIds\":[1],\"roleIds\":[2]}";
        mockMvc.perform(delete("/api/users/roles/deassign").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.removedCount", is(1)));
    }

    @Test
    void assignUsersToGroups_returnsOk() throws Exception {
        AssignUsersToGroupsResponse resp = AssignUsersToGroupsResponse.builder()
                .assignedCount(3)
                .build();
        when(userService.assignUsersToGroups(any(AssignUsersToGroupsRequest.class))).thenReturn(resp);
        String body = "{\"userIds\":[1,2,3],\"groupIds\":[10]}";
        mockMvc.perform(post("/api/users/groups/assign").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedCount", is(3)));
    }

    @Test
    void deassignUsersFromGroups_returnsOk() throws Exception {
        DeassignUsersFromGroupsResponse resp = DeassignUsersFromGroupsResponse.builder()
                .removedCount(2)
                .build();
        when(userService.deassignUsersFromGroups(any(DeassignUsersFromGroupsRequest.class))).thenReturn(resp);
        String body = "{\"userIds\":[1,2],\"groupIds\":[5]}";
        mockMvc.perform(delete("/api/users/groups/deassign").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.removedCount", is(2)));
    }

    @Test
    void deleteUsers_returnsOk() throws Exception {
        DeleteUsersResponse resp = DeleteUsersResponse.builder()
                .deletedCount(2)
                .build();
        when(userService.deleteUsers(any(DeleteUsersRequest.class))).thenReturn(resp);
        String body = "{\"userIds\":[7,8]}";
        mockMvc.perform(delete("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount", is(2)));
    }

    @Test
    void updateUserCredentialsByAdmin_returnsOk() throws Exception {
        AdminUpdateCredentialsResponse resp = AdminUpdateCredentialsResponse.builder()
                .passwordUpdated(true)
                .build();
        when(userService.updateCredentialsByAdmin(any(Long.class), any(AdminUpdateCredentialsRequest.class))).thenReturn(resp);
        String body = "{\"newPassword\":\"P@ssw0rd\"}";
        mockMvc.perform(patch("/api/users/42/credentials").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordUpdated", is(true)));
    }

    @Test
    void getUsers_sizeZero_returns400_andSkipsService() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getUsers(any(), anyInt(), anyInt());
    }
}
