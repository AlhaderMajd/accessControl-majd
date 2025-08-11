package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.common.MessageResponse;
import com.example.accesscontrol.dto.common.PageResponse;
import com.example.accesscontrol.dto.permission.*;
import com.example.accesscontrol.entity.Permission;
import com.example.accesscontrol.exception.DuplicateResourceException;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionService rolePermissionService;

    @Transactional
    public CreatePermissionsResponse createPermissions(CreatePermissionsRequest request) {
        if (request == null || request.getPermissions() == null || request.getPermissions().isEmpty()) {
            throw new DuplicateResourceException("Permission list must not be empty");
        }

        List<String> names = request.getPermissions().stream()
                .map(n -> n == null ? "" : n.trim())
                .filter(n -> !n.isBlank())
                .distinct()
                .toList();
        if (names.isEmpty()) throw new DuplicateResourceException("Permission list must not be empty");

        Set<String> existing = permissionRepository.findByNameInIgnoreCase(names).stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());
        if (!existing.isEmpty()) throw new DuplicateResourceException("Permissions already exist: " + existing);

        List<Permission> saved = permissionRepository.saveAll(
                names.stream().map(n -> Permission.builder().name(n).build()).toList()
        );

        return CreatePermissionsResponse.builder()
                .message("Permissions created successfully")
                .createdCount(saved.size())
                .items(saved.stream()
                        .map(p -> PermissionResponse.builder().id(p.getId()).name(p.getName()).build())
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<PermissionResponse> getPermissions(String search, int page, int size) {
        if (page < 0 || size <= 0) throw new IllegalArgumentException("Invalid pagination or search parameters");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Permission> pg = permissionRepository.findByNameContainingIgnoreCase(search == null ? "" : search, pageable);

        List<PermissionResponse> items = pg.getContent().stream()
                .map(p -> PermissionResponse.builder().id(p.getId()).name(p.getName()).build())
                .toList();

        return PageResponse.<PermissionResponse>builder()
                .items(items).page(page).size(size).total(pg.getTotalElements()).build();
    }

    @Transactional(readOnly = true)
    public PermissionResponse getPermissionDetails(Long permissionId) {
        Permission p = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        return PermissionResponse.builder().id(p.getId()).name(p.getName()).build();
    }

    @Transactional
    public UpdatePermissionNameResponse updatePermissionName(Long permissionId, UpdatePermissionNameRequest request) {
        String newName = (request == null) ? null : request.getName();
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Permission name is required");
        }
        Permission p = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        String old = p.getName();
        p.setName(newName.trim());
        permissionRepository.save(p);

        return UpdatePermissionNameResponse.builder()
                .message("Permission updated successfully")
                .id(p.getId())
                .oldName(old)
                .newName(p.getName())
                .build();
    }

    @Transactional
    public MessageResponse deletePermissions(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            throw new IllegalArgumentException("No permission IDs provided");
        }
        List<Long> existing = permissionRepository.findAllById(permissionIds).stream()
                .map(Permission::getId).toList();
        if (existing.isEmpty()) throw new ResourceNotFoundException("No matching permissions found");

        rolePermissionService.deleteByPermissionIds(existing);
        permissionRepository.deleteAllById(existing);

        return MessageResponse.builder()
                .message("Permissions deleted successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        return permissionRepository.findByRoleId(roleId);
    }

    @Transactional(readOnly = true)
    public List<Long> getExistingPermissionIds(List<Long> ids) {
        return permissionRepository.findAllById(ids).stream().map(Permission::getId).toList();
    }
}
