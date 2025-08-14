package com.example.accesscontrol.controller;

import com.example.accesscontrol.dto.common.MessageResponse;
import com.example.accesscontrol.dto.common.PageResponse;
import com.example.accesscontrol.dto.permission.*;
import com.example.accesscontrol.service.PermissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePermissionsResponse createPermissions(@Valid @RequestBody CreatePermissionsRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        int count = request == null || request.getPermissions() == null ? 0 : request.getPermissions().size();
        log.info("permissions.create request actor={} count={}", mask(actor), count);

        return permissionService.createPermissions(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PageResponse<PermissionResponse> getPermissions(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("permissions.list request actor={} page={} size={} q_len={}",
                mask(actor), page, size, search == null ? 0 : search.length());

        return permissionService.getPermissions(search, page, size);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{permissionId}")
    public PermissionResponse getPermissionsDetails(@PathVariable @Min(1) Long permissionId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("permissions.details request actor={} permissionId={}", mask(actor), permissionId);

        return permissionService.getPermissionDetails(permissionId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{permissionId}")
    public UpdatePermissionNameResponse updatePermissionName(
            @PathVariable @Min(1) Long permissionId,
            @Valid @RequestBody UpdatePermissionNameRequest request) {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("permissions.update_name request actor={} permissionId={} new_len={}",
                mask(actor), permissionId, request.getName() == null ? 0 : request.getName().length());

        return permissionService.updatePermissionName(permissionId, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public MessageResponse deletePermissions(@RequestBody List<Long> permissionIds) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("permissions.delete request actor={} count={}",
                mask(actor), permissionIds == null ? 0 : permissionIds.size());

        return permissionService.deletePermissions(permissionIds);
    }

    private String mask(String email) {
        if (email == null || !email.contains("@")) return "unknown";
        String[] p = email.split("@", 2);
        return (p[0].isEmpty() ? "*" : p[0].substring(0,1)) + "***@" + p[1];
    }
}
