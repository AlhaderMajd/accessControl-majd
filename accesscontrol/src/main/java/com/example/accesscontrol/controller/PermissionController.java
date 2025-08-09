package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.common.MessageResponse;
import com.example.accesscontrol.dto.common.PageResponse;
import com.example.accesscontrol.dto.permission.*;
import com.example.accesscontrol.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePermissionsResponse create(@RequestBody CreatePermissionsRequest request) {
        return permissionService.createPermissions(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PageResponse<PermissionResponse> list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return permissionService.getPermissions(search, page, size);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{permissionId}")
    public PermissionResponse details(@PathVariable Long permissionId) {
        return permissionService.getPermissionDetails(permissionId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{permissionId}")
    public UpdatePermissionNameResponse updateName(
            @PathVariable Long permissionId,
            @RequestBody UpdatePermissionNameRequest request
    ) {
        return permissionService.updatePermissionName(permissionId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public MessageResponse delete(@RequestBody List<Long> permissionIds) {
        return permissionService.deletePermissions(permissionIds);
    }
}
