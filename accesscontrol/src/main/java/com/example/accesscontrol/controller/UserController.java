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
import com.example.accesscontrol.dto.user.updateUserInfo.ChangePasswordRequest;
import com.example.accesscontrol.dto.user.updateUserInfo.UpdateEmailRequest;
import com.example.accesscontrol.dto.user.updateUserStatus.UpdateUserStatusRequest;
import com.example.accesscontrol.dto.user.updateUserStatus.UpdateUserStatusResponse;
import com.example.accesscontrol.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Create users (bulk) and auto-assign MEMBER role")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CreateUsersResponse createUsers(@Valid @RequestBody CreateUsersRequest request) {
        return userService.createUsers(request);
    }

    @Operation(summary = "Get users with search and pagination")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('ADMIN','AUTHOR')")
    public GetUsersResponse getUsers(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        return userService.getUsers(search, page, size);
    }

    @Operation(summary = "Get user details with roles and groups")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('ADMIN','AUTHOR')")
    public UserResponse getUserDetails(@PathVariable("id") Long id) {
        return userService.getUserDetails(id);
    }

    @Operation(summary = "Admin updates a user's email and/or password")
    @PutMapping("/{id}/admin-credentials")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public AdminUpdateCredentialsResponse updateCredentialsByAdmin(
            @PathVariable("id") Long userId,
            @Valid @RequestBody AdminUpdateCredentialsRequest request
    ) {
        return userService.updateCredentialsByAdmin(userId, request);
    }

    @Operation(summary = "Authenticated user changes own password")
    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
    }

    @Operation(summary = "Authenticated user changes own email")
    @PostMapping("/change-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void changeEmail(@Valid @RequestBody UpdateEmailRequest request) {
        userService.changeEmail(request);
    }

    @Operation(summary = "Enable/disable users (bulk)")
    @PutMapping("/status")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public UpdateUserStatusResponse updateUserStatus(@Valid @RequestBody UpdateUserStatusRequest request) {
        return userService.updateUserStatus(request);
    }

    @Operation(summary = "Assign roles to users (bulk, additive)")
    @PostMapping("/assign-roles")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public AssignRolesResponse assignRolesToUsers(@Valid @RequestBody AssignRolesRequest request) {
        return userService.assignRolesToUsers(request);
    }

    @Operation(summary = "Deassign roles from users (bulk)")
    @PostMapping("/deassign-roles")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public DeassignRolesResponse deassignRolesFromUsers(@Valid @RequestBody DeassignRolesRequest request) {
        return userService.deassignRolesFromUsers(request);
    }

    @Operation(summary = "Assign users to groups (bulk, additive)")
    @PostMapping("/assign-groups")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public AssignUsersToGroupsResponse assignUsersToGroups(@Valid @RequestBody AssignUsersToGroupsRequest request) {
        return userService.assignUsersToGroups(request);
    }

    @Operation(summary = "Deassign users from groups (bulk)")
    @PostMapping("/deassign-groups")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public DeassignUsersFromGroupsResponse deassignUsersFromGroups(@Valid @RequestBody DeassignUsersFromGroupsRequest request) {
        return userService.deassignUsersFromGroups(request);
    }

    @Operation(summary = "Delete users (bulk)")
    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public DeleteUsersResponse deleteUsers(@Valid @RequestBody DeleteUsersRequest request) {
        return userService.deleteUsers(request);
    }
}
