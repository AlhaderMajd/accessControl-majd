package com.example.accesscontrol.controller;

import com.example.accesscontrol.config.logs;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final logs logs;

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
            @RequestParam(defaultValue = "10") int size) {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.list request actor={} page={} size={} q_len={}", logs.mask(actor), page, size, search == null ? 0 : search.length());

        return ResponseEntity.ok(userService.getUsers(search, page, size));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserDetails(@PathVariable Long id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.details request actor={} userId={}", logs.mask(actor), id);

        return ResponseEntity.ok(userService.getUserDetails(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/credentials")
    public ResponseEntity<AdminUpdateCredentialsResponse> updateUserCredentialsByAdmin(
            @PathVariable Long userId,
            @RequestBody AdminUpdateCredentialsRequest request) {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();

        log.info("users.admin.update_credentials request actor={} userId={} hasEmail={} hasPassword={}",
                logs.mask(actor), userId,
                request != null && StringUtils.hasText(request.getEmail()),
                request != null && StringUtils.hasText(request.getPassword()));

        var resp = userService.updateCredentialsByAdmin(userId, request);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.change_password request actor={}", logs.mask(actor));

        userService.changePassword(request);
        return ResponseEntity.ok(java.util.Map.of("message", "Password updated successfully"));
    }

    @PutMapping("/email")
    public ResponseEntity<?> changeEmail(@RequestBody UpdateEmailRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.change_email request actor={}", logs.mask(actor));

        userService.changeEmail(request);
        return ResponseEntity.ok(java.util.Map.of("message", "Email updated successfully"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/status")
    public ResponseEntity<UpdateUserStatusResponse> updateUserStatus(@RequestBody UpdateUserStatusRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.status request actor={} count={} enable={}",
                logs.mask(actor),
                request == null || request.getUserIds() == null ? 0 : request.getUserIds().size(),
                request == null ? null : request.getEnabled());

        return ResponseEntity.ok(userService.updateUserStatus(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/roles/assign")
    public ResponseEntity<AssignRolesResponse> assignRoles(@RequestBody AssignRolesRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.roles.assign request actor={} users={} roles={}",
                logs.mask(actor),
                request == null || request.getUserIds() == null ? 0 : request.getUserIds().size(),
                request == null || request.getRoleIds() == null ? 0 : request.getRoleIds().size());

        return ResponseEntity.ok(userService.assignRolesToUsers(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/roles/deassign")
    public ResponseEntity<DeassignRolesResponse> deassignRoles(@RequestBody DeassignRolesRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.roles.deassign request actor={} users={} roles={}",
                logs.mask(actor),
                request == null || request.getUserIds() == null ? 0 : request.getUserIds().size(),
                request == null || request.getRoleIds() == null ? 0 : request.getRoleIds().size());

        return ResponseEntity.ok(userService.deassignRolesFromUsers(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/groups/assign")
    public ResponseEntity<AssignUsersToGroupsResponse> assignUsersToGroups(@RequestBody AssignUsersToGroupsRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        int pairsRequested = (request == null || request.getUserIds() == null || request.getGroupIds() == null)
                ? 0 : request.getUserIds().size() * request.getGroupIds().size();

        log.info("users.groups.assign request actor={} users={} groups={} pairs_requested={}",
                logs.mask(actor),
                request == null || request.getUserIds() == null ? 0 : request.getUserIds().size(),
                request == null || request.getGroupIds() == null ? 0 : request.getGroupIds().size(),
                pairsRequested);

        var resp = userService.assignUsersToGroups(request);
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/groups/deassign")
    public ResponseEntity<DeassignUsersFromGroupsResponse> deassignUsersFromGroups(@RequestBody DeassignUsersFromGroupsRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.groups.deassign request actor={} users={} groups={}",
                logs.mask(actor),
                request == null || request.getUserIds() == null ? 0 : request.getUserIds().size(),
                request == null || request.getGroupIds() == null ? 0 : request.getGroupIds().size());

        return ResponseEntity.ok(userService.deassignUsersFromGroups(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public ResponseEntity<DeleteUsersResponse> deleteUsers(@RequestBody DeleteUsersRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("users.delete request actor={} count={}",
                logs.mask(actor),
                request == null || request.getUserIds() == null ? 0 : request.getUserIds().size());

        return ResponseEntity.ok(userService.deleteUsers(request));
    }
}
