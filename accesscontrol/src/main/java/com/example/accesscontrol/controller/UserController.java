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
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CreateUsersResponse> createUsers(@RequestBody CreateUsersRequest request) {
        var response = userService.createUsers(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<GetUsersResponse> getUsers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (page < 0 || size <= 0) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(userService.getUsers(search, page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserDetails(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserDetails(id));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(java.util.Map.of("message", "Password updated successfully"));
    }

    @PutMapping("/email")
    public ResponseEntity<?> changeEmail(@RequestBody UpdateEmailRequest request) {
        userService.changeEmail(request);
        return ResponseEntity.ok(java.util.Map.of("message", "Email updated successfully"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/status")
    public ResponseEntity<UpdateUserStatusResponse> updateUserStatus(@RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(userService.updateUserStatus(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/roles/assign")
    public ResponseEntity<AssignRolesResponse> assignRoles(@RequestBody AssignRolesRequest request) {
        return ResponseEntity.ok(userService.assignRolesToUsers(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/roles/deassign")
    public ResponseEntity<DeassignRolesResponse> deassignRoles(@RequestBody DeassignRolesRequest request) {
        return ResponseEntity.ok(userService.deassignRolesFromUsers(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/groups/assign")
    public ResponseEntity<AssignUsersToGroupsResponse> assignUsersToGroups(@RequestBody AssignUsersToGroupsRequest request) {
        return ResponseEntity.ok(userService.assignUsersToGroups(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/groups/deassign")
    public ResponseEntity<DeassignUsersFromGroupsResponse> deassignUsersFromGroups(@RequestBody DeassignUsersFromGroupsRequest request) {
        return ResponseEntity.ok(userService.deassignUsersFromGroups(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public ResponseEntity<DeleteUsersResponse> deleteUsers(@RequestBody DeleteUsersRequest request) {
        return ResponseEntity.ok(userService.deleteUsers(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/credentials")
    public ResponseEntity<AdminUpdateCredentialsResponse> updateUserCredentialsByAdmin(
            @PathVariable Long userId, @RequestBody AdminUpdateCredentialsRequest request) {
        return ResponseEntity.ok(userService.updateCredentialsByAdmin(userId, request));
    }
}
