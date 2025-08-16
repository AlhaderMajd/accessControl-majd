package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.user.assignRolesToUser.AssignRolesRequest;
import com.example.accesscontrol.dto.user.assignRolesToUser.AssignRolesResponse;
import com.example.accesscontrol.dto.user.assignUsersToGroup.AssignUsersToGroupsRequest;
import com.example.accesscontrol.dto.user.assignUsersToGroup.AssignUsersToGroupsResponse;
import com.example.accesscontrol.dto.user.createUsers.CreateUserRequest;
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
import com.example.accesscontrol.dto.user.getUsers.UserSummaryResponse;
import com.example.accesscontrol.dto.user.updateCredentials.AdminUpdateCredentialsRequest;
import com.example.accesscontrol.dto.user.updateCredentials.AdminUpdateCredentialsResponse;
import com.example.accesscontrol.dto.user.updateUserInfo.ChangePasswordRequest;
import com.example.accesscontrol.dto.user.updateUserInfo.UpdateEmailRequest;
import com.example.accesscontrol.dto.user.updateUserStatus.UpdateUserStatusRequest;
import com.example.accesscontrol.dto.user.updateUserStatus.UpdateUserStatusResponse;
import com.example.accesscontrol.exception.EmailAlreadyUsedException;
import com.example.accesscontrol.exception.GlobalExceptionHandler;
import com.example.accesscontrol.exception.InvalidCredentialsException;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.service.UserService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserService userService;

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    @Nested
    @DisplayName("POST /api/users (create users)")
    class CreateUsersTests {
        @Test
        void create_ok_201() throws Exception {
            var req = CreateUsersRequest.builder()
                    .users(List.of(
                            CreateUserRequest.builder().email("u1@acme.test").password("P@ssw0rd").enabled(true).build(),
                            CreateUserRequest.builder().email("u2@acme.test").password("P@ssw0rd").enabled(false).build()
                    )).build();

            var resp = new CreateUsersResponse(List.of(101L, 102L), List.of("MEMBER"));

            when(userService.createUsers(any(CreateUsersRequest.class))).thenReturn(resp);

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.createdUserIds[0]").value(101))
                    .andExpect(jsonPath("$.assignedRoles[0]").value("MEMBER"));

            ArgumentCaptor<CreateUsersRequest> cap = ArgumentCaptor.forClass(CreateUsersRequest.class);
            verify(userService).createUsers(cap.capture());
            assertThat(cap.getValue().getUsers()).hasSize(2);
        }

        @Test
        void create_illegalArg_400() throws Exception {
            // Triggers Bean Validation (@NotEmpty on users), so service is never called.
            var req = CreateUsersRequest.builder().users(List.of()).build();

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        void create_emailDup_409() throws Exception {
            var req = CreateUsersRequest.builder()
                    .users(List.of(CreateUserRequest.builder()
                            .email("dup@acme.test").password("P@ssw0rd").enabled(true).build()))
                    .build();

            when(userService.createUsers(any(CreateUsersRequest.class)))
                    .thenThrow(new EmailAlreadyUsedException("Some emails already in use: [dup@acme.test]"));

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Some emails already in use: [dup@acme.test]"));
        }
    }

    @Nested
    @DisplayName("GET /api/users (list)")
    class ListUsersTests {
        @Test
        void list_ok_200() throws Exception {
            var page = new GetUsersResponse(
                    List.of(UserSummaryResponse.builder().id(1L).email("u@x").enabled(true).build()),
                    0, 2L);

            when(userService.getUsers(eq("x"), eq(0), eq(1))).thenReturn(page);

            mockMvc.perform(get("/api/users")
                            .param("search", "x")
                            .param("page", "0")
                            .param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users[0].email").value("u@x"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.total").value(2));

            verify(userService).getUsers("x", 0, 1);
        }

        @Test
        void list_badPagination_400() throws Exception {
            mockMvc.perform(get("/api/users")
                            .param("search", "")
                            .param("page", "-1")
                            .param("size", "0"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("GET /api/users/{id} (details)")
    class DetailsTests {
        @Test
        void details_ok_200() throws Exception {
            var resp = UserResponse.builder()
                    .id(10L).email("u@acme.test").enabled(true)
                    .roles(List.of("MEMBER")).groups(List.of("Engineering"))
                    .build();

            when(userService.getUserDetails(10L)).thenReturn(resp);

            mockMvc.perform(get("/api/users/{id}", 10))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("u@acme.test"))
                    .andExpect(jsonPath("$.roles[0]").value("MEMBER"))
                    .andExpect(jsonPath("$.groups[0]").value("Engineering"));

            verify(userService).getUserDetails(10L);
        }

        @Test
        void details_badId_400() throws Exception {
            // @Min(1) on path variable triggers ConstraintViolation -> 400 by GlobalExceptionHandler
            mockMvc.perform(get("/api/users/{id}", 0))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        void details_notFound_404() throws Exception {
            when(userService.getUserDetails(999L))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(get("/api/users/{id}", 999))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("User not found"));
        }
    }

    @Nested
    @DisplayName("PUT /api/users/change-password")
    class ChangePasswordTests {
        @Test
        void change_ok_200() throws Exception {
            // Ensure both passwords satisfy validation (@Size >= 6)
            var req = new ChangePasswordRequest("Old#123", "New#12345");

            doNothing().when(userService).changePassword(any(ChangePasswordRequest.class));

            mockMvc.perform(put("/api/users/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password updated successfully"));

            ArgumentCaptor<ChangePasswordRequest> cap = ArgumentCaptor.forClass(ChangePasswordRequest.class);
            verify(userService).changePassword(cap.capture());
            assertThat(cap.getValue().getNewPassword()).isEqualTo("New#12345");
        }

        @Test
        void change_invalidOld_401() throws Exception {
            // Pass validation to reach service
            var req = new ChangePasswordRequest("wrong1", "New#12345");
            doThrow(new InvalidCredentialsException("Old password is incorrect"))
                    .when(userService).changePassword(any(ChangePasswordRequest.class));

            mockMvc.perform(put("/api/users/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Old password is incorrect"));
        }

        @Test
        void change_badNew_400() throws Exception {
            // New password passes @Size but service rejects by policy
            var req = new ChangePasswordRequest("Old#123", "weakpass");
            doThrow(new IllegalArgumentException("Password must meet security requirements"))
                    .when(userService).changePassword(any(ChangePasswordRequest.class));

            mockMvc.perform(put("/api/users/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Password must meet security requirements"));
        }
    }

    @Nested
    @DisplayName("PUT /api/users/email")
    class ChangeEmailTests {
        @Test
        void email_ok_200() throws Exception {
            var req = new UpdateEmailRequest("new@acme.test");
            doNothing().when(userService).changeEmail(any(UpdateEmailRequest.class));

            mockMvc.perform(put("/api/users/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Email updated successfully"));

            verify(userService).changeEmail(any(UpdateEmailRequest.class));
        }

        @Test
        void email_badFormat_400() throws Exception {
            // Fails @Email validation before service
            var req = new UpdateEmailRequest("bad");

            mockMvc.perform(put("/api/users/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        void email_dup_409() throws Exception {
            var req = new UpdateEmailRequest("dup@acme.test");
            doThrow(new EmailAlreadyUsedException("Email already taken"))
                    .when(userService).changeEmail(any(UpdateEmailRequest.class));

            mockMvc.perform(put("/api/users/email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Email already taken"));
        }
    }

    @Nested
    @DisplayName("PUT /api/users/status")
    class UpdateStatusTests {
        @Test
        void status_ok_200() throws Exception {
            var req = new UpdateUserStatusRequest(List.of(1L, 2L), true);
            var resp = UpdateUserStatusResponse.builder()
                    .message("User status updated successfully").updatedCount(2).build();

            when(userService.updateUserStatus(any(UpdateUserStatusRequest.class))).thenReturn(resp);

            mockMvc.perform(put("/api/users/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User status updated successfully"))
                    .andExpect(jsonPath("$.updatedCount").value(2));
        }

        @Test
        void status_illegal_400() throws Exception {
            // Trigger validation (empty list / null flag) -> 400 before service
            var req = new UpdateUserStatusRequest(List.of(), null);

            mockMvc.perform(put("/api/users/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        void status_notFound_404() throws Exception {
            var req = new UpdateUserStatusRequest(List.of(999L), true);
            when(userService.updateUserStatus(any(UpdateUserStatusRequest.class)))
                    .thenThrow(new ResourceNotFoundException("No users found to update"));

            mockMvc.perform(put("/api/users/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("No users found to update"));
        }
    }

    @Nested
    @DisplayName("POST /api/users/roles/assign")
    class AssignRolesTests {
        @Test
        void assign_ok_200() throws Exception {
            var req = new AssignRolesRequest(List.of(1L, 2L), List.of(10L));
            var resp = AssignRolesResponse.builder().message("Roles assigned successfully").assignedCount(2).build();

            when(userService.assignRolesToUsers(any(AssignRolesRequest.class))).thenReturn(resp);

            mockMvc.perform(post("/api/users/roles/assign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Roles assigned successfully"))
                    .andExpect(jsonPath("$.assignedCount").value(2));
        }

        @Test
        void assign_illegal_400() throws Exception {
            // Invalid payload (empty lists) -> Bean Validation 400
            var req = new AssignRolesRequest(List.of(), List.of());

            mockMvc.perform(post("/api/users/roles/assign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        void assign_notFound_404() throws Exception {
            var req = new AssignRolesRequest(List.of(1L), List.of(999L));
            when(userService.assignRolesToUsers(any(AssignRolesRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Some roles not found"));

            mockMvc.perform(post("/api/users/roles/assign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Some roles not found"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/users/roles/deassign")
    class DeassignRolesTests {
        @Test
        void deassign_ok_200() throws Exception {
            var req = new DeassignRolesRequest();
            req.setUserIds(List.of(1L));
            req.setRoleIds(List.of(10L));

            var resp = DeassignRolesResponse.builder().message("Roles deassigned successfully").removedCount(1).build();

            when(userService.deassignRolesFromUsers(any(DeassignRolesRequest.class))).thenReturn(resp);

            mockMvc.perform(delete("/api/users/roles/deassign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Roles deassigned successfully"))
                    .andExpect(jsonPath("$.removedCount").value(1));
        }

        @Test
        void deassign_notFound_404() throws Exception {
            var req = new DeassignRolesRequest();
            req.setUserIds(List.of(999L));
            req.setRoleIds(List.of(10L));

            when(userService.deassignRolesFromUsers(any(DeassignRolesRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Some users not found"));

            mockMvc.perform(delete("/api/users/roles/deassign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Some users not found"));
        }
    }

    @Nested
    @DisplayName("POST /api/users/groups/assign")
    class AssignUsersToGroupsTests {
        @Test
        void assign_ok_200() throws Exception {
            var req = new AssignUsersToGroupsRequest();
            req.setUserIds(List.of(1L, 2L));
            req.setGroupIds(List.of(5L, 6L));

            var resp = AssignUsersToGroupsResponse.builder()
                    .message("Users assigned to groups successfully").assignedCount(3).build();

            when(userService.assignUsersToGroups(any(AssignUsersToGroupsRequest.class))).thenReturn(resp);

            mockMvc.perform(post("/api/users/groups/assign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Users assigned to groups successfully"))
                    .andExpect(jsonPath("$.assignedCount").value(3));
        }
    }

    @Nested
    @DisplayName("DELETE /api/users/groups/deassign")
    class DeassignUsersFromGroupsTests {
        @Test
        void deassign_ok_200() throws Exception {
            var req = new DeassignUsersFromGroupsRequest();
            req.setUserIds(List.of(1L));
            req.setGroupIds(List.of(5L));

            var resp = DeassignUsersFromGroupsResponse.builder()
                    .message("Users deassigned from groups successfully").removedCount(1).build();

            when(userService.deassignUsersFromGroups(any(DeassignUsersFromGroupsRequest.class))).thenReturn(resp);

            mockMvc.perform(delete("/api/users/groups/deassign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Users deassigned from groups successfully"))
                    .andExpect(jsonPath("$.removedCount").value(1));
        }

        @Test
        void deassign_illegal_400() throws Exception {
            // Invalid payload -> Bean Validation 400
            var req = new DeassignUsersFromGroupsRequest();
            req.setUserIds(List.of());
            req.setGroupIds(List.of());

            mockMvc.perform(delete("/api/users/groups/deassign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        void deassign_usersNotFound_404() throws Exception {
            var req = new DeassignUsersFromGroupsRequest();
            req.setUserIds(List.of(999L));
            req.setGroupIds(List.of(5L));

            when(userService.deassignUsersFromGroups(any(DeassignUsersFromGroupsRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Some users not found"));

            mockMvc.perform(delete("/api/users/groups/deassign")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Some users not found"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/users (batch)")
    class DeleteUsersTests {
        @Test
        void delete_ok_200() throws Exception {
            var req = new DeleteUsersRequest();
            req.setUserIds(List.of(10L, 11L));

            var resp = DeleteUsersResponse.builder().message("Users deleted successfully").deletedCount(2).build();

            when(userService.deleteUsers(any(DeleteUsersRequest.class))).thenReturn(resp);

            mockMvc.perform(delete("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Users deleted successfully"))
                    .andExpect(jsonPath("$.deletedCount").value(2));
        }

        @Test
        void delete_illegal_400() throws Exception {
            // Invalid per @NotEmpty on userIds -> 400 before service
            var req = new DeleteUsersRequest();
            req.setUserIds(List.of());

            mockMvc.perform(delete("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }

        @Test
        void delete_notFound_404() throws Exception {
            var req = new DeleteUsersRequest();
            req.setUserIds(List.of(999L));

            when(userService.deleteUsers(any(DeleteUsersRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Some users not found"));

            mockMvc.perform(delete("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Some users not found"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/{userId}/credentials")
    class UpdateCredentialsByAdminTests {
        @Test
        void update_ok_200() throws Exception {
            var req = new AdminUpdateCredentialsRequest();
            req.setEmail("new@acme.test");
            req.setPassword("New#12345");

            var resp = AdminUpdateCredentialsResponse.builder()
                    .message("Credentials updated successfully")
                    .id(10L).emailUpdated(true).passwordUpdated(true)
                    .build();

            when(userService.updateCredentialsByAdmin(eq(10L), any(AdminUpdateCredentialsRequest.class)))
                    .thenReturn(resp);

            mockMvc.perform(patch("/api/users/{userId}/credentials", 10)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Credentials updated successfully"))
                    .andExpect(jsonPath("$.emailUpdated").value(true))
                    .andExpect(jsonPath("$.passwordUpdated").value(true));
        }

        @Test
        void update_illegal_400() throws Exception {
            // Empty request: let service enforce business rule (at least one provided)
            var req = new AdminUpdateCredentialsRequest();

            when(userService.updateCredentialsByAdmin(eq(11L), any(AdminUpdateCredentialsRequest.class)))
                    .thenThrow(new IllegalArgumentException("At least one of email or password must be provided"));

            mockMvc.perform(patch("/api/users/{userId}/credentials", 11)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("At least one of email or password must be provided"));
        }

        @Test
        void update_dupEmail_409() throws Exception {
            var req = new AdminUpdateCredentialsRequest();
            req.setEmail("dup@acme.test");

            when(userService.updateCredentialsByAdmin(eq(12L), any(AdminUpdateCredentialsRequest.class)))
                    .thenThrow(new EmailAlreadyUsedException("Email already in use"));

            mockMvc.perform(patch("/api/users/{userId}/credentials", 12)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Email already in use"));
        }

        @Test
        void update_notFound_404() throws Exception {
            var req = new AdminUpdateCredentialsRequest();
            req.setPassword("New#12345");

            when(userService.updateCredentialsByAdmin(eq(999L), any(AdminUpdateCredentialsRequest.class)))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            mockMvc.perform(patch("/api/users/{userId}/credentials", 999)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("User not found"));
        }
    }
}
