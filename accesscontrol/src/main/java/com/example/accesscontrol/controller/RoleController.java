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
    public RoleDetailsResponse getRoleById(@PathVariable Long roleId) {
        return roleService.getRoleWithPermissions(roleId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{roleId}")
    public UpdateRoleResponse updateRoleName(@PathVariable Long roleId, @Valid @RequestBody UpdateRoleRequest request) {
        return roleService.updateRoleName(roleId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/assign-permissions")
    public Map<String, String> assignPermissions(@RequestBody List<AssignPermissionsToRolesRequest> items) {
        String msg = roleService.assignPermissionsToRoles(items);
        return Map.of("message", msg);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/deassign-permissions")
    public Map<String, String> deassignPermissions(@RequestBody List<AssignPermissionsToRolesRequest> items) {
        String msg = roleService.deassignPermissionsFromRoles(items);
        return Map.of("message", msg);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/groups/assign-roles")
    public Map<String, String> assignRolesToGroups(@RequestBody List<AssignRolesToGroupsRequest> items) {
        String msg = roleService.assignRolesToGroups(items);
        return Map.of("message", msg);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/groups/deassign-roles")
    public Map<String, String> deassignRolesFromGroups(@RequestBody List<AssignRolesToGroupsRequest> items) {
        String msg = roleService.deassignRolesFromGroups(items);
        return Map.of("message", msg);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public Map<String, String> deleteRoles(@RequestBody Map<String, List<Long>> request) {
        List<Long> roleIds = request.get("roleIds");
        String msg = roleService.deleteRoles(roleIds);
        return Map.of("message", msg);
    }

    private String mask(String email) {
        if (email == null || !email.contains("@")) return "unknown";
        String[] p = email.split("@", 2);
        return (p[0].isEmpty() ? "*" : p[0].substring(0,1)) + "***@" + p[1];
    }
}
