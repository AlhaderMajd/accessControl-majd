package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.common.MessageResponse;
import com.example.accesscontrol.dto.common.PageResponse;
import com.example.accesscontrol.dto.permission.*;
import com.example.accesscontrol.entity.Permission;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.exception.DuplicateResourceException;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    @Transactional
    public CreatePermissionsResponse createPermissions(CreatePermissionsRequest request) {
        if (request == null || request.getPermissions() == null || request.getPermissions().isEmpty()) {
            throw new IllegalArgumentException("Permission list must not be empty");
        }

        List<String> names = request.getPermissions().stream()
                .map(s -> s == null ? "" : s.trim())
                .filter(s -> !s.isBlank())
                .toList();
        if (names.isEmpty()) throw new IllegalArgumentException("Permission list must not be empty");

        var dupPayload = names.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        s -> s.toLowerCase(java.util.Locale.ROOT),
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        if (!dupPayload.isEmpty()) {
            throw new DuplicateResourceException("Duplicate permission names in request: " + dupPayload);
        }

        Set<String> existing = permissionRepository
                .findByNameInIgnoreCase(names.stream().map(s -> s.toLowerCase(Locale.ROOT)).toList())
                .stream().map(Permission::getName).collect(java.util.stream.Collectors.toSet());
        if (!existing.isEmpty()) {
            throw new DuplicateResourceException("Permissions already exist: " + existing);
        }

        List<Permission> saved;
        try {
            saved = permissionRepository.saveAll(
                    names.stream().map(n -> Permission.builder().name(n).build()).toList()
            );
        } catch (DataIntegrityViolationException e) {
            var nowExisting = permissionRepository
                    .findByNameInIgnoreCase(names.stream().map(s -> s.toLowerCase(Locale.ROOT)).toList())
                    .stream().map(Permission::getName).collect(java.util.stream.Collectors.toSet());
            throw new DuplicateResourceException("Permissions already exist: " + nowExisting);
        }

        List<PermissionResponse> items = saved.stream()
                .sorted(java.util.Comparator.comparing(Permission::getName, java.text.Collator.getInstance())
                        .thenComparing(Permission::getId))
                .map(p -> PermissionResponse.builder().id(p.getId()).name(p.getName()).build())
                .toList();

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("permissions.create success actor={} created={}", mask(actor), saved.size());

        return CreatePermissionsResponse.builder()
                .message("Permissions created successfully")
                .createdCount(saved.size())
                .items(items)
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<PermissionResponse> getPermissions(String search, int page, int size) {
        final String q = (search == null ? "" : search.trim());
        final int pageSafe = Math.max(0, page);
        final int sizeSafe = Math.min(Math.max(1, size), 100);
        final Pageable pageable = PageRequest.of(pageSafe, sizeSafe, Sort.by(Sort.Direction.DESC, "id"));

        Page<Permission> pg = permissionRepository.findByNameContainingIgnoreCase(q, pageable);

        List<PermissionResponse> items = pg.getContent().stream()
                .map(p -> PermissionResponse.builder().id(p.getId()).name(p.getName()).build())
                .toList();

        return PageResponse.<PermissionResponse>builder()
                .items(items)
                .page(pageSafe)
                .size(sizeSafe)
                .total(pg.getTotalElements())
                .build();
    }

    @Transactional(readOnly = true)
    public PermissionResponse getPermissionDetails(Long permissionId) {
        Permission p = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        return PermissionResponse.builder().id(p.getId()).name(p.getName()).build();
    }

    @Transactional
    public UpdatePermissionNameResponse updatePermissionName(Long permissionId, UpdatePermissionNameRequest request) {
        String newName = (request == null || request.getName() == null) ? null : request.getName().trim();
        if (newName == null || newName.isEmpty() || newName.length() > 100) {
            throw new IllegalArgumentException("Permission name is required");
        }

        Permission p = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));

        String old = p.getName();

        if (old != null && old.equalsIgnoreCase(newName)) {
            log.info("permissions.update_name no_change permissionId={} name='{}'", permissionId, old);
            return UpdatePermissionNameResponse.builder()
                    .message("Permission updated successfully")
                    .id(p.getId())
                    .oldName(old)
                    .newName(old)
                    .build();
        }

        var dup = permissionRepository
                .findByNameInIgnoreCase(List.of(newName.toLowerCase(Locale.ROOT)))
                .stream().filter(existing -> !existing.getId().equals(permissionId)).findFirst();
        if (dup.isPresent()) {
            throw new DuplicateResourceException("Permission name already exists");
        }

        p.setName(newName);
        try {
            permissionRepository.save(p);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("Permission name already exists");
        }

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("permissions.update_name success actor={} permissionId={} old='{}' new='{}'",
                mask(actor), permissionId, old, newName);

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

        var ids = permissionIds.stream().filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("No valid permission IDs provided");
        }

        var existing = permissionRepository.findAllById(ids);
        if (existing.size() != ids.size()) {
            var found = existing.stream().map(Permission::getId).collect(java.util.stream.Collectors.toSet());
            var missing = ids.stream().filter(id -> !found.contains(id)).toList();
            throw new ResourceNotFoundException("Some permissions not found: " + missing);
        }

        try {
            for (Permission perm : existing) {
                for (Role r : new ArrayList<>(perm.getRoles())) {
                    r.getPermissions().remove(perm);
                }
            }
            permissionRepository.deleteAllInBatch(existing);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Cannot delete permissions due to existing references: " +
                    (ex.getMostSpecificCause() == null ? ex.getMessage() : ex.getMostSpecificCause().getMessage()));
        }

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("permissions.delete success actor={} deleted={}", mask(actor), ids.size());

        return MessageResponse.builder().message("Permissions deleted successfully").build();
    }

    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        return permissionRepository.findByRoleId(roleId);
    }

    @Transactional(readOnly = true)
    public List<Long> getExistingPermissionIds(List<Long> ids) {
        return permissionRepository.findAllById(ids).stream().map(Permission::getId).toList();
    }

    private String mask(String email) {
        if (email == null || !email.contains("@")) return "unknown";
        String[] p = email.split("@", 2);
        return (p[0].isEmpty() ? "*" : p[0].substring(0,1)) + "***@" + p[1];
    }
}
