package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.group.AssignRolesToGroupsRequest;
import com.example.accesscontrol.dto.role.*;
import com.example.accesscontrol.service.RoleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
@Slf4j
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CreateRoleResponse> createRoles(@Valid @RequestBody List<CreateRoleRequest> requestList) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("roles.create request actor={} count={}", mask(actor), requestList == null ? 0 : requestList.size());

        var response = roleService.createRoles(requestList);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public GetRolesResponse getRoles(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("roles.list request actor={} page={} size={} q_len={}",
                mask(actor), page, size, search == null ? 0 : search.length());

        return roleService.getRoles(search, page, size);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{roleId}")
    public RoleDetailsResponse getRoleById(@PathVariable @Min(1) Long roleId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("roles.details request actor={} roleId={}", mask(actor), roleId);

        return roleService.getRoleWithPermissions(roleId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{roleId}")
    public UpdateRoleResponse updateRoleName(
            @PathVariable @Min(1) Long roleId,
            @jakarta.validation.Valid @RequestBody UpdateRoleRequest request) {

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("roles.update_name request actor={} roleId={} new_len={}",
                mask(actor), roleId, request.getName() == null ? 0 : request.getName().length());

        return roleService.updateRoleName(roleId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/assign-permissions")
    public Map<String, String> assignPermissions(@jakarta.validation.Valid @RequestBody List<@jakarta.validation.Valid AssignPermissionsToRolesRequest> items) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        int pairCount = (items == null) ? 0 :
                items.stream().mapToInt(i -> i.getPermissionIds() == null ? 0 : i.getPermissionIds().size()).sum();
        log.info("roles.permissions.assign request actor={} role_count={} pair_count={}",
                mask(actor), items == null ? 0 : items.size(), pairCount);

        String msg = roleService.assignPermissionsToRoles(items);
        return java.util.Map.of("message", msg);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/deassign-permissions")
    public Map<String, String> deassignPermissions(@Valid @RequestBody List<@Valid AssignPermissionsToRolesRequest> items) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        int pairCount = (items == null) ? 0 :
                items.stream().mapToInt(i -> i.getPermissionIds() == null ? 0 : i.getPermissionIds().size()).sum();
        log.info("roles.permissions.deassign request actor={} role_count={} pair_count={}",
                mask(actor), items == null ? 0 : items.size(), pairCount);

        String msg = roleService.deassignPermissionsFromRoles(items);
        return Map.of("message", msg);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/groups/assign-roles")
    public Map<String, String> assignRolesToGroups(@Valid @RequestBody List<@Valid AssignRolesToGroupsRequest> items) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();

        int pairCount = (items == null) ? 0
                : items.stream().mapToInt(i -> i.getRoleIds() == null ? 0 : i.getRoleIds().size()).sum();

        log.info("roles.groups.assign request actor={} group_count={} pair_count={}",
                mask(actor), items == null ? 0 : items.size(), pairCount);

        String msg = roleService.assignRolesToGroups(items);
        return Map.of("message", msg);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/groups/deassign-roles")
    public Map<String, String> deassignRolesFromGroups(
            @Valid @RequestBody List<@Valid AssignRolesToGroupsRequest> items) {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();

        int pairCount = (items == null) ? 0
                : items.stream().mapToInt(i -> i.getRoleIds() == null ? 0 : i.getRoleIds().size()).sum();

        log.info("roles.groups.deassign request actor={} group_count={} pair_count={}",
                mask(actor), items == null ? 0 : items.size(), pairCount);

        String msg = roleService.deassignRolesFromGroups(items);
        return Map.of("message", msg);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public Map<String, String> deleteRoles(@RequestBody Map<String, List<Long>> request) {
        List<Long> roleIds = (request == null) ? null : request.get("roleIds");

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("roles.delete request actor={} count={}",
                mask(actor), roleIds == null ? 0 : roleIds.size());

        String msg = roleService.deleteRoles(roleIds);
        return Map.of("message", msg);
    }

    private String mask(String email) {
        if (email == null || !email.contains("@")) return "unknown";
        String[] p = email.split("@", 2);
        return (p[0].isEmpty() ? "*" : p[0].substring(0,1)) + "***@" + p[1];
    }
}
