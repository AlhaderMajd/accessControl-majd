package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.group.AssignRolesToGroupsRequest;
import com.example.accesscontrol.dto.permission.PermissionResponse;
import com.example.accesscontrol.dto.role.*;
import com.example.accesscontrol.entity.*;
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
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Role not found"));
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
        if (request.getName() == null || request.getName().isBlank())
            throw new IllegalArgumentException("Invalid role name");
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        role.setName(request.getName());
        roleRepository.save(role);
        return UpdateRoleResponse.builder().message("Role name updated successfully").build();
    }

    @Transactional
    public String assignPermissionsToRoles(List<AssignPermissionsToRolesRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Requests cannot be empty");
        }

        List<Long> allRoleIds = requests.stream()
                .map(AssignPermissionsToRolesRequest::getRoleId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();

        List<Long> allPermissionIds = requests.stream()
                .flatMap(r -> r.getPermissionIds() != null ? r.getPermissionIds().stream() : java.util.stream.Stream.<Long>empty())
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();

        List<Long> existingRoleIds = roleRepository.findAllById(allRoleIds)
                .stream().map(Role::getId).toList();

        List<Long> existingPermissionIds = permissionService.getExistingPermissionIds(allPermissionIds);

        if (existingRoleIds.isEmpty() || existingPermissionIds.isEmpty()) {
            return "No permissions assigned (0). No valid roles or permissions found.";
        }

        int assigned = rolePermissionService.assignPermissionsToRoles(existingRoleIds, existingPermissionIds);
        return "Permissions assigned successfully. Total assignments: " + assigned;
    }


    @Transactional
    public String deassignPermissionsFromRoles(List<AssignPermissionsToRolesRequest> items) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Invalid or empty input");
        Set<Long> roleIds = items.stream().map(AssignPermissionsToRolesRequest::getRoleId).collect(Collectors.toSet());
        Set<Long> permissionIds = items.stream().flatMap(i -> i.getPermissionIds().stream()).collect(Collectors.toSet());
        int n = rolePermissionService.deassignPermissionsFromRoles(new ArrayList<>(roleIds), new ArrayList<>(permissionIds));
        return (n > 0) ? "Permissions removed successfully" : "No permissions were removed";
    }

    @Transactional
    public String assignRolesToGroups(List<AssignRolesToGroupsRequest> items) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Invalid or empty input");
        Set<Long> groupIds = items.stream().map(AssignRolesToGroupsRequest::getGroupId).collect(Collectors.toSet());
        Set<Long> roleIds = items.stream().flatMap(i -> i.getRoleIds().stream()).collect(Collectors.toSet());
        var validGroupIds = groupRoleService.getExistingGroupIds(new ArrayList<>(groupIds));
        var validRoleIds = getByIdsOrThrow(new ArrayList<>(roleIds)).stream().map(Role::getId).toList();
        int inserted = groupRoleService.assignRolesToGroups(validGroupIds, validRoleIds);
        return "Roles assigned to groups successfully. Inserted: " + inserted;
    }

    @Transactional
    public String deassignRolesFromGroups(List<AssignRolesToGroupsRequest> items) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Invalid or empty input");
        var groupIds = items.stream().map(AssignRolesToGroupsRequest::getGroupId).distinct().toList();
        var roleIds = items.stream().flatMap(i -> i.getRoleIds().stream()).distinct().toList();
        groupRoleService.deassignRolesFromGroups(groupIds, roleIds);
        return "Roles deassigned from groups successfully";
    }

    @Transactional
    public String deleteRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) throw new IllegalArgumentException("No role IDs provided");
        List<Long> existingIds = roleRepository.findAllById(roleIds).stream().map(Role::getId).toList();
        if (existingIds.size() != roleIds.size()) throw new NoSuchElementException("One or more role IDs do not exist");
        rolePermissionService.deleteByRoleIds(roleIds);
        groupRoleService.deleteByRoleIds(roleIds);
        userRoleService.deleteByRoleIds(roleIds);
        roleRepository.deleteAllById(roleIds);
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
