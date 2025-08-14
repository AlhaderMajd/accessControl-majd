package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.group.AssignRolesToGroupsRequest;
import com.example.accesscontrol.dto.permission.PermissionResponse;
import com.example.accesscontrol.dto.role.*;
import com.example.accesscontrol.entity.*;
import com.example.accesscontrol.exception.DuplicateResourceException;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionService permissionService;
    private final RolePermissionService rolePermissionService;
    private final GroupRoleService groupRoleService;
    private final UserRoleService userRoleService;

    @Transactional
    public Role getOrCreateRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    try {
                        return roleRepository.save(Role.builder().name(roleName).build());
                    } catch (DataIntegrityViolationException e) {
                        return roleRepository.findByName(roleName).orElseThrow(() -> e);
                    }
                });
    }

    @Transactional(readOnly = true)
    public List<Role> getByIdsOrThrow(List<Long> ids) {
        List<Role> roles = roleRepository.findAllById(ids);
        if (roles.size() != ids.size()) throw new ResourceNotFoundException("Some roles not found");
        return roles;
    }

    @Transactional(readOnly = true)
    public List<Long> getExistingIds(List<Long> ids) {
        return roleRepository.findAllById(ids).stream().map(Role::getId).toList();
    }

    @Transactional
    public CreateRoleResponse createRoles(List<CreateRoleRequest> requests) {
        if (requests == null || requests.isEmpty()
                || requests.stream().anyMatch(r -> r.getName() == null || r.getName().isBlank())) {
            throw new IllegalArgumentException("Invalid role data");
        }

        var normalized = requests.stream().map(r -> {
            var name = r.getName().trim();
            var pids = (r.getPermissionIds() == null) ? List.<Long>of()
                    : r.getPermissionIds().stream()
                    .filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();
            var nr = new CreateRoleRequest();
            nr.setName(name);
            nr.setPermissionIds(pids);
            return nr;
        }).toList();

        var dupNames = normalized.stream()
                .collect(java.util.stream.Collectors.groupingBy(CreateRoleRequest::getName, java.util.stream.Collectors.counting()))
                .entrySet().stream().filter(e -> e.getValue() > 1).map(Map.Entry::getKey).toList();
        if (!dupNames.isEmpty()) {
            throw new com.example.accesscontrol.exception.DuplicateResourceException("Duplicate role names in request: " + dupNames);
        }

        var names = normalized.stream().map(CreateRoleRequest::getName).toList();
        var existingNames = roleRepository.findExistingNames(names);
        if (!existingNames.isEmpty()) {
            throw new com.example.accesscontrol.exception.DuplicateResourceException("Some role names already exist: " + existingNames);
        }

        var allPermissionIds = normalized.stream()
                .flatMap(r -> r.getPermissionIds().stream())
                .distinct().toList();
        if (!allPermissionIds.isEmpty()) {
            var existingPermIds = permissionService.getExistingPermissionIds(allPermissionIds);
            if (existingPermIds.size() != allPermissionIds.size()) {
                var missing = new java.util.HashSet<>(allPermissionIds);
                missing.removeAll(existingPermIds);
                throw new com.example.accesscontrol.exception.ResourceNotFoundException("Some permissions not found: " + missing);
            }
        }

        var toPersist = normalized.stream()
                .map(r -> Role.builder().name(r.getName()).build())
                .toList();

        List<Role> savedRoles;
        try {
            savedRoles = roleRepository.saveAll(toPersist);
        } catch (DataIntegrityViolationException e) {
            var nowExisting = roleRepository.findExistingNames(names);
            throw new com.example.accesscontrol.exception.DuplicateResourceException("Some role names already exist: " + nowExisting);
        }

        var nameToId = savedRoles.stream()
                .collect(java.util.stream.Collectors.toMap(Role::getName, Role::getId));

        List<RolePermission> rp = new java.util.ArrayList<>();
        for (var req : normalized) {
            if (!req.getPermissionIds().isEmpty()) {
                Long roleId = nameToId.get(req.getName());
                for (Long pid : req.getPermissionIds()) {
                    RolePermission link = new RolePermission();
                    link.setRole(Role.builder().id(roleId).build());
                    link.setPermission(Permission.builder().id(pid).build());
                    rp.add(link);
                }
            }
        }
        if (!rp.isEmpty()) {
            rolePermissionService.saveAll(rp);
        }

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("roles.create success actor={} created={} with_permissions={}",
                mask(actor), savedRoles.size(), !rp.isEmpty());

        return CreateRoleResponse.builder()
                .message("Roles created successfully")
                .created(savedRoles.stream().map(Role::getName).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public GetRolesResponse getRoles(String search, int page, int size) {
        final String q = (search == null ? "" : search.trim());
        final int pageSafe = Math.max(0, page);
        final int sizeSafe = Math.min(Math.max(1, size), 100);
        final Pageable pageable = PageRequest.of(pageSafe, sizeSafe, Sort.by(Sort.Direction.DESC, "id"));

        Page<Role> rolePage = roleRepository.findByNameContainingIgnoreCase(q, pageable);

        var roles = rolePage.getContent().stream()
                .map(r -> RoleResponse.builder().id(r.getId()).name(r.getName()).build())
                .toList();

        return GetRolesResponse.builder()
                .roles(roles)
                .page(pageSafe)
                .total(rolePage.getTotalElements())
                .build();
    }

    @Transactional(readOnly = true)
    public RoleDetailsResponse getRoleWithPermissions(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        List<Permission> permissions = permissionService.getPermissionsByRoleId(roleId);

        var permDtos = permissions.stream()
                .map(p -> PermissionResponse.builder().id(p.getId()).name(p.getName()).build())
                .toList();

        return RoleDetailsResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .permissions(permDtos)
                .build();
    }

    @Transactional
    public UpdateRoleResponse updateRoleName(Long roleId, UpdateRoleRequest request) {
        String newName = request.getName() == null ? null : request.getName().trim();
        if (newName == null || newName.isEmpty() || newName.length() > 100) {
            throw new IllegalArgumentException("Invalid role name");
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (newName.equals(role.getName())) {
            log.info("roles.update_name no_change roleId={}", roleId);
            return UpdateRoleResponse.builder()
                    .message("Role name updated successfully")
                    .build();
        }

        roleRepository.findByName(newName).ifPresent(existing -> {
            if (!existing.getId().equals(roleId)) {
                throw new DuplicateResourceException("Role name already exists");
            }
        });

        String old = role.getName();
        role.setName(newName);
        try {
            roleRepository.save(role);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException("Role name already exists");
        }

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("roles.update_name success actor={} roleId={} old='{}' new='{}'",
                mask(actor), roleId, old, newName);

        return UpdateRoleResponse.builder()
                .message("Role name updated successfully")
                .build();
    }

    @Transactional
    public String assignPermissionsToRoles(List<AssignPermissionsToRolesRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Requests cannot be empty");
        }

        Map<Long, Set<Long>> wanted = new java.util.LinkedHashMap<>();
        for (var r : requests) {
            if (r == null || r.getRoleId() == null || r.getRoleId() <= 0)
                throw new IllegalArgumentException("Invalid roleId in request");
            if (r.getPermissionIds() == null || r.getPermissionIds().isEmpty())
                throw new IllegalArgumentException("permissionIds must not be empty");

            var perms = r.getPermissionIds().stream()
                    .filter(Objects::nonNull).filter(id -> id > 0)
                    .collect(java.util.stream.Collectors.toSet());
            if (perms.isEmpty())
                throw new IllegalArgumentException("permissionIds must not be empty");

            wanted.computeIfAbsent(r.getRoleId(), k -> new java.util.LinkedHashSet<>()).addAll(perms);
        }
        if (wanted.isEmpty()) throw new IllegalArgumentException("Nothing to assign");

        var roleIds = new java.util.ArrayList<>(wanted.keySet());
        var roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            var found = roles.stream().map(Role::getId).collect(java.util.stream.Collectors.toSet());
            var missing = roleIds.stream().filter(id -> !found.contains(id)).toList();
            throw new com.example.accesscontrol.exception.ResourceNotFoundException("Some roles not found: " + missing);
        }

        var allPermissionIds = wanted.values().stream().flatMap(Set::stream).distinct().toList();
        var existingPermIds = permissionService.getExistingPermissionIds(allPermissionIds);
        if (existingPermIds.size() != allPermissionIds.size()) {
            var missing = new java.util.HashSet<>(allPermissionIds);
            missing.removeAll(existingPermIds);
            throw new com.example.accesscontrol.exception.ResourceNotFoundException("Some permissions not found: " + missing);
        }

        int assigned = rolePermissionService.assignRolePermissionPairs(wanted);

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        int pairCount = wanted.values().stream().mapToInt(Set::size).sum();
        log.info("roles.permissions.assign success actor={} roles={} pairs_requested={} assigned={}",
                mask(actor), wanted.size(), pairCount, assigned);

        return "Permissions assigned successfully. Total assignments: " + assigned;
    }


    @Transactional
    public String deassignPermissionsFromRoles(List<AssignPermissionsToRolesRequest> items) {
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Invalid or empty input");

        Map<Long, Set<Long>> wanted = new java.util.LinkedHashMap<>();
        for (var it : items) {
            if (it == null || it.getRoleId() == null || it.getRoleId() <= 0)
                throw new IllegalArgumentException("Invalid roleId in request");
            if (it.getPermissionIds() == null || it.getPermissionIds().isEmpty())
                throw new IllegalArgumentException("permissionIds must not be empty");

            var perms = it.getPermissionIds().stream()
                    .filter(Objects::nonNull).filter(id -> id > 0)
                    .collect(java.util.stream.Collectors.toSet());
            if (perms.isEmpty())
                throw new IllegalArgumentException("permissionIds must not be empty");

            wanted.computeIfAbsent(it.getRoleId(), k -> new java.util.LinkedHashSet<>()).addAll(perms);
        }
        if (wanted.isEmpty()) throw new IllegalArgumentException("Nothing to deassign");

        var roleIds = new java.util.ArrayList<>(wanted.keySet());
        var roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            var found = roles.stream().map(Role::getId).collect(java.util.stream.Collectors.toSet());
            var missing = roleIds.stream().filter(id -> !found.contains(id)).toList();
            throw new com.example.accesscontrol.exception.ResourceNotFoundException("Some roles not found: " + missing);
        }

        var allPermIds = wanted.values().stream().flatMap(Set::stream).distinct().toList();
        var existingPermIds = permissionService.getExistingPermissionIds(allPermIds);
        if (existingPermIds.size() != allPermIds.size()) {
            var missing = new java.util.HashSet<>(allPermIds);
            missing.removeAll(existingPermIds);
            throw new com.example.accesscontrol.exception.ResourceNotFoundException("Some permissions not found: " + missing);
        }

        int removed = rolePermissionService.deleteRolePermissionPairs(wanted);

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        int pairCount = wanted.values().stream().mapToInt(Set::size).sum();
        log.info("roles.permissions.deassign success actor={} roles={} pairs_requested={} removed={}",
                mask(actor), wanted.size(), pairCount, removed);

        return removed > 0 ? "Permissions removed successfully" : "No permissions were removed";
    }

    @Transactional
    public String assignRolesToGroups(List<AssignRolesToGroupsRequest> items) {
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Invalid or empty input");

        Map<Long, Set<Long>> wanted = new java.util.LinkedHashMap<>();
        for (var it : items) {
            if (it == null || it.getGroupId() == null || it.getGroupId() <= 0)
                throw new IllegalArgumentException("Invalid groupId in request");
            if (it.getRoleIds() == null || it.getRoleIds().isEmpty())
                throw new IllegalArgumentException("roleIds must not be empty");

            var roles = it.getRoleIds().stream()
                    .filter(Objects::nonNull).filter(id -> id > 0)
                    .collect(java.util.stream.Collectors.toSet());
            if (roles.isEmpty())
                throw new IllegalArgumentException("roleIds must not be empty");

            wanted.computeIfAbsent(it.getGroupId(), k -> new java.util.LinkedHashSet<>()).addAll(roles);
        }
        if (wanted.isEmpty()) throw new IllegalArgumentException("Nothing to assign");

        var groupIds = new java.util.ArrayList<>(wanted.keySet());
        var existingGroupIds = groupRoleService.getExistingGroupIds(groupIds);
        if (existingGroupIds.size() != groupIds.size()) {
            var found = new java.util.HashSet<>(existingGroupIds);
            var missing = groupIds.stream().filter(id -> !found.contains(id)).toList();
            throw new com.example.accesscontrol.exception.ResourceNotFoundException("Some groups not found: " + missing);
        }

        var roleIds = wanted.values().stream().flatMap(Set::stream).distinct().toList();
        getByIdsOrThrow(roleIds);

        int inserted = groupRoleService.assignGroupRolePairs(wanted);

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        int pairCount = wanted.values().stream().mapToInt(Set::size).sum();
        log.info("roles.groups.assign success actor={} groups={} pairs_requested={} inserted={}",
                mask(actor), wanted.size(), pairCount, inserted);

        return "Roles assigned to groups successfully. Inserted: " + inserted;
    }

    @Transactional
    public String deassignRolesFromGroups(List<AssignRolesToGroupsRequest> items) {
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Invalid or empty input");

        Map<Long, Set<Long>> wanted = new java.util.LinkedHashMap<>();
        for (var it : items) {
            if (it == null || it.getGroupId() == null || it.getGroupId() <= 0)
                throw new IllegalArgumentException("Invalid groupId in request");
            if (it.getRoleIds() == null || it.getRoleIds().isEmpty())
                throw new IllegalArgumentException("roleIds must not be empty");

            var normRoleIds = it.getRoleIds().stream()
                    .filter(Objects::nonNull).filter(id -> id > 0)
                    .collect(java.util.stream.Collectors.toSet());
            if (normRoleIds.isEmpty())
                throw new IllegalArgumentException("roleIds must not be empty");

            wanted.computeIfAbsent(it.getGroupId(), k -> new java.util.LinkedHashSet<>()).addAll(normRoleIds);
        }
        if (wanted.isEmpty()) throw new IllegalArgumentException("Nothing to deassign");

        var groupIds = new java.util.ArrayList<>(wanted.keySet());
        var existingGroupIds = groupRoleService.getExistingGroupIds(groupIds);
        if (existingGroupIds.size() != groupIds.size()) {
            var found = new java.util.HashSet<>(existingGroupIds);
            var missing = groupIds.stream().filter(id -> !found.contains(id)).toList();
            throw new com.example.accesscontrol.exception.ResourceNotFoundException("Some groups not found: " + missing);
        }

        var roleIds = wanted.values().stream().flatMap(Set::stream).distinct().toList();
        getByIdsOrThrow(roleIds);

        int removed = groupRoleService.deleteGroupRolePairs(wanted);

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        int pairCount = wanted.values().stream().mapToInt(Set::size).sum();
        log.info("roles.groups.deassign success actor={} groups={} pairs_requested={} removed={}",
                mask(actor), wanted.size(), pairCount, removed);

        return (removed > 0) ? "Roles deassigned from groups successfully"
                : "No roles were deassigned from groups";
    }

    @Transactional
    public String deleteRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty())
            throw new IllegalArgumentException("No role IDs provided");

        var ids = roleIds.stream()
                .filter(Objects::nonNull).filter(id -> id > 0)
                .distinct().toList();
        if (ids.isEmpty())
            throw new IllegalArgumentException("No valid role IDs provided");

        var existing = roleRepository.findAllById(ids);
        if (existing.size() != ids.size()) {
            var found = existing.stream().map(Role::getId).collect(java.util.stream.Collectors.toSet());
            var missing = ids.stream().filter(id -> !found.contains(id)).toList();
            throw new com.example.accesscontrol.exception.ResourceNotFoundException("One or more role IDs do not exist: " + missing);
        }

        try {
            rolePermissionService.deleteByRoleIds(ids);
            groupRoleService.deleteByRoleIds(ids);
            userRoleService.deleteByRoleIds(ids);

            roleRepository.deleteAllByIdInBatch(ids);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Cannot delete roles due to existing references: " +
                    (ex.getMostSpecificCause() == null ? ex.getMessage() : ex.getMostSpecificCause().getMessage()));
        }

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("roles.delete success actor={} deleted={}", mask(actor), ids.size());

        return "Roles deleted successfully";
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getRoleSummariesByIds(List<Long> roleIds) {
        return roleRepository.findAllById(roleIds).stream()
                .map(r -> RoleResponse.builder().id(r.getId()).name(r.getName()).build())
                .toList();
    }

    private String mask(String email) {
        if (email == null || !email.contains("@")) return "unknown";
        String[] p = email.split("@", 2);
        return (p[0].isEmpty() ? "*" : p[0].substring(0,1)) + "***@" + p[1];
    }
}
