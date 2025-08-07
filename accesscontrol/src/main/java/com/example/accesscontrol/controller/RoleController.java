package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.*;
import com.example.accesscontrol.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.accesscontrol.dto.AssignPermissionsToRolesItem;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<CreateRoleResponse> createRoles(@RequestBody List<CreateRoleRequest> requestList) {
        CreateRoleResponse response = roleService.createRoles(requestList);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public GetRolesResponse getRoles(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return roleService.getRoles(search, page, size);
    }

    @GetMapping("/{roleId}")
    public RoleWithPermissionsResponse getRoleById(@PathVariable Long roleId) {
        return roleService.getRoleWithPermissions(roleId);
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<UpdateRoleResponse> updateRoleName(
            @PathVariable Long roleId,
            @Valid @RequestBody UpdateRoleRequest request) {
        UpdateRoleResponse response = roleService.updateRoleName(roleId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assign-permissions")
    public ResponseEntity<?> assignPermissions(@RequestBody List<AssignPermissionsToRolesItem> items) {
        try {
            String message = roleService.assignPermissionsToRoles(items);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to assign permissions"));
        }
    }


    @DeleteMapping("/deassign-permissions")
    public ResponseEntity<?> deassignPermissions(@RequestBody List<AssignPermissionsToRolesItem> items) {
        try {
            String message = roleService.deassignPermissionsFromRoles(items);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to remove permissions"));
        }
    }

    @PostMapping("/groups/assign-roles")
    public ResponseEntity<?> assignRolesToGroups(@RequestBody List<AssignRolesToGroupsItem> items) {
        try {
            String message = roleService.assignRolesToGroups(items);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to assign roles to groups"));
        }
    }

    @DeleteMapping("/groups/deassign-roles")
    public ResponseEntity<?> deassignRolesFromGroups(@RequestBody List<AssignRolesToGroupsItem> items) {
        try {
            String message = roleService.deassignRolesFromGroups(items);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to deassign roles from groups"));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteRoles(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> roleIds = request.get("roleIds");

            if (roleIds == null || roleIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "No role IDs provided"));
            }

            String message = roleService.deleteRoles(roleIds);
            return ResponseEntity.ok(Map.of("message", message));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to delete roles"));
        }
    }
}
