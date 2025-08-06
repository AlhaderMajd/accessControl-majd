package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.*;
import com.example.accesscontrol.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
