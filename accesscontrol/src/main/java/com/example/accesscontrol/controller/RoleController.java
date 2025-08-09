package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.role.*;
import com.example.accesscontrol.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CreateRoleResponse> createRoles(@RequestBody List<CreateRoleRequest> requestList) {
        var response = roleService.createRoles(requestList);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public GetRolesResponse getRoles(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return roleService.getRoles(search, page, size);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{roleId}")
    public RoleWithPermissionsResponse getRoleById(@PathVariable Long roleId) {
        return roleService.getRoleWithPermissions(roleId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{roleId}")
    public UpdateRoleResponse updateRoleName(@PathVariable Long roleId, @Valid @RequestBody UpdateRoleRequest request) {
        return roleService.updateRoleName(roleId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/assign-permissions")
    public Map<String, String> assignPermissions(@RequestBody List<AssignPermissionsToRolesItem> items) {
        String msg = roleService.assignPermissionsToRoles(items);
        return Map.of("message", msg);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/deassign-permissions")
    public Map<String, String> deassignPermissions(@RequestBody List<AssignPermissionsToRolesItem> items) {
        String msg = roleService.deassignPermissionsFromRoles(items);
        return Map.of("message", msg);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/groups/assign-roles")
    public Map<String, String> assignRolesToGroups(@RequestBody List<AssignRolesToGroupsItem> items) {
        String msg = roleService.assignRolesToGroups(items);
        return Map.of("message", msg);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/groups/deassign-roles")
    public Map<String, String> deassignRolesFromGroups(@RequestBody List<AssignRolesToGroupsItem> items) {
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
}
