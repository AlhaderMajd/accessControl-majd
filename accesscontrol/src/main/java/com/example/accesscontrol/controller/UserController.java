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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CreateUsersResponse> createUsers(@Valid @RequestBody CreateUsersRequest request) {
        var response = userService.createUsers(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<GetUsersResponse> getUsers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.list request actor={} page={} size={} q_len={}", mask(actor), page, size, search == null ? 0 : search.length());

        return ResponseEntity.ok(userService.getUsers(search, page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserDetails(
            @PathVariable @Min(1) Long id) {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.details request actor={} userId={}", mask(actor), id);

        return ResponseEntity.ok(userService.getUserDetails(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/credentials")
    public ResponseEntity<AdminUpdateCredentialsResponse> updateUserCredentialsByAdmin(
            @PathVariable @Min(1) Long userId,
            @Valid @RequestBody AdminUpdateCredentialsRequest request) {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();

        log.info("users.admin.update_credentials request actor={} userId={} hasEmail={} hasPassword={}",
                mask(actor), userId,
                StringUtils.hasText(request.getEmail()),
                StringUtils.hasText(request.getPassword()));

        var resp = userService.updateCredentialsByAdmin(userId, request);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.change_password request actor={}", mask(actor));

        userService.changePassword(request);
        return ResponseEntity.ok(java.util.Map.of("message", "Password updated successfully"));
    }

    @PutMapping("/email")
    public ResponseEntity<?> changeEmail(@Valid @RequestBody UpdateEmailRequest request) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.change_email request actor={}", mask(actor));

        userService.changeEmail(request);
        return ResponseEntity.ok(java.util.Map.of("message", "Email updated successfully"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/status")
    public ResponseEntity<UpdateUserStatusResponse> updateUserStatus(
            @Valid @RequestBody UpdateUserStatusRequest request) {

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.status request actor={} count={} enable={}",
                mask(actor),
                request.getUserIds() == null ? 0 : request.getUserIds().size(),
                request.getEnabled());

        return ResponseEntity.ok(userService.updateUserStatus(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/roles/assign")
    public ResponseEntity<AssignRolesResponse> assignRoles(@Valid @RequestBody AssignRolesRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.roles.assign request actor={} users={} roles={}",
                mask(actor),
                request.getUserIds() == null ? 0 : request.getUserIds().size(),
                request.getRoleIds() == null ? 0 : request.getRoleIds().size());

        return ResponseEntity.ok(userService.assignRolesToUsers(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/roles/deassign")
    public ResponseEntity<DeassignRolesResponse> deassignRoles(@Valid @RequestBody DeassignRolesRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.roles.deassign request actor={} users={} roles={}",
                mask(actor),
                request.getUserIds() == null ? 0 : request.getUserIds().size(),
                request.getRoleIds() == null ? 0 : request.getRoleIds().size());

        return ResponseEntity.ok(userService.deassignRolesFromUsers(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/groups/assign")
    public ResponseEntity<AssignUsersToGroupsResponse> assignUsersToGroups(
            @Valid @RequestBody AssignUsersToGroupsRequest request) {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();

        int pairsRequested = (request == null) ? 0
                : (request.getUserIds() == null || request.getGroupIds() == null)
                ? 0
                : request.getUserIds().size() * request.getGroupIds().size();

        log.info("users.groups.assign request actor={} users={} groups={} pairs_requested={}",
                mask(actor),
                request == null || request.getUserIds() == null ? 0 : request.getUserIds().size(),
                request == null || request.getGroupIds() == null ? 0 : request.getGroupIds().size(),
                pairsRequested);

        var resp = userService.assignUsersToGroups(request);
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/groups/deassign")
    public ResponseEntity<DeassignUsersFromGroupsResponse> deassignUsersFromGroups(
            @Valid @RequestBody DeassignUsersFromGroupsRequest request) {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.groups.deassign request actor={} users={} groups={}",
                mask(actor),
                request.getUserIds() == null ? 0 : request.getUserIds().size(),
                request.getGroupIds() == null ? 0 : request.getGroupIds().size());

        return ResponseEntity.ok(userService.deassignUsersFromGroups(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public ResponseEntity<DeleteUsersResponse> deleteUsers(@Valid @RequestBody DeleteUsersRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.delete request actor={} count={}",
                mask(actor),
                request.getUserIds() == null ? 0 : request.getUserIds().size());

        return ResponseEntity.ok(userService.deleteUsers(request));
    }

    private String mask(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) return "unknown";
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        String head = local.isEmpty() ? "*" : local.substring(0, 1);
        return head + "***@" + domain;
    }
}
