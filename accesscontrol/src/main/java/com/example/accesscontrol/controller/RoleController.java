package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.*;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.accesscontrol.dto.AssignPermissionsToRolesItem;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<?> assignPermissionsToRoles(@Valid @RequestBody List<AssignPermissionsToRolesItem> items) {
        try {
            AssignPermissionsToRolesResponse response = roleService.assignPermissionsToRoles(items);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to assign permissions"));
        }
    }
}
