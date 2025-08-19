package com.example.accesscontrol.service;

import com.example.accesscontrol.config.logs;
import com.example.accesscontrol.dto.group.AssignRolesToGroupsRequest;
import com.example.accesscontrol.dto.permission.PermissionResponse;
import com.example.accesscontrol.dto.role.*;
import com.example.accesscontrol.entity.Group;
import com.example.accesscontrol.entity.Permission;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.User;
import com.example.accesscontrol.exception.DuplicateResourceException;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.RoleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionService permissionService;
    private final logs logs;


    @PersistenceContext
    private EntityManager em;

    @Transactional
    public CreateRoleResponse createRoles(List<CreateRoleRequest> requests) {
        if (requests == null || requests.isEmpty()
                || requests.stream().anyMatch(r -> r.getName() == null || r.getName().isBlank())) {
            throw new IllegalArgumentException("Invalid role data");
        }

        var normalized = requests.stream().map(r -> {
            var name = r.getName().trim();
            var pids = (r.getPermissionIds() == null) ? List.<Long>of()
                    : r.getPermissionIds().stream().filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();
            var nr = new CreateRoleRequest();
            nr.setName(name);
            nr.setPermissionIds(pids);
            return nr;
        }).toList();

        var dupNames = normalized.stream()
                .collect(Collectors.groupingBy(CreateRoleRequest::getName, Collectors.counting()))
                .entrySet().stream().filter(e -> e.getValue() > 1).map(Map.Entry::getKey).toList();
        if (!dupNames.isEmpty()) throw new DuplicateResourceException("Duplicate role names in request: " + dupNames);

        var names = normalized.stream().map(CreateRoleRequest::getName).toList();

        var existingNames = roleRepository.findExistingNames(names);
        if (!existingNames.isEmpty()) throw new DuplicateResourceException("Some role names already exist: " + existingNames);

        var allPermissionIds = normalized.stream().flatMap(r -> r.getPermissionIds().stream()).distinct().toList();
        if (!allPermissionIds.isEmpty()) {
            var existingPermIds = permissionService.getExistingPermissionIds(allPermissionIds);
            if (existingPermIds.size() != allPermissionIds.size()) {
                var missing = new HashSet<>(allPermissionIds);
                missing.removeAll(existingPermIds);
                throw new ResourceNotFoundException("Some permissions not found: " + missing);
            }
        }

        List<Role> savedRoles;
        try {
            savedRoles = roleRepository.saveAll(
                    normalized.stream().map(r -> Role.builder().name(r.getName()).build()).toList()
            );
        } catch (DataIntegrityViolationException e) {
            var nowExisting = roleRepository.findExistingNames(names);
            throw new DuplicateResourceException("Some role names already exist: " + nowExisting);
        }

        Map<String, Role> byName = savedRoles.stream().collect(Collectors.toMap(Role::getName, r -> r));
        for (var req : normalized) {
            if (!req.getPermissionIds().isEmpty()) {
                Role role = byName.get(req.getName());
                var wanted = new HashSet<>(req.getPermissionIds());
                var currentIds = role.getPermissions().stream().map(Permission::getId).collect(Collectors.toSet());

                for (Long pid : wanted) {
                    if (!currentIds.contains(pid)) {
                        role.getPermissions().add(em.getReference(Permission.class, pid));
                    }
                }
            }
        }
        roleRepository.saveAll(savedRoles);

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("roles.create success actor={} created={} with_permissions={}",
                logs.mask(actor), savedRoles.size(), normalized.stream().anyMatch(r -> !r.getPermissionIds().isEmpty()));

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

        var permissions = permissionService.getPermissionsByRoleId(roleId);

        var permDtos = permissions.stream()
                .map(p -> PermissionResponse.builder().id(p.getId()).name(p.getName()).build())
                .sorted(Comparator
                        .comparing(PermissionResponse::getName, Collator.getInstance())
                        .thenComparing(PermissionResponse::getId))
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

        if (newName.equalsIgnoreCase(role.getName())) {
            log.info("roles.update_name no_change roleId={}", roleId);
            return UpdateRoleResponse.builder().message("Role name updated successfully").build();
        }

        roleRepository.findByName(newName).ifPresent(existing -> {
            if (!existing.getId().equals(roleId)) throw new DuplicateResourceException("Role name already exists");
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
                logs.mask(actor), roleId, old, newName);

        return UpdateRoleResponse.builder().message("Role name updated successfully").build();
    }

    @Transactional
    public String assignPermissionsToRoles(List<AssignPermissionsToRolesRequest> requests) {
        if (requests == null || requests.isEmpty()) throw new IllegalArgumentException("Requests cannot be empty");

        Map<Long, Set<Long>> wanted = new LinkedHashMap<>();
        for (var r : requests) {
            if (r == null || r.getRoleId() == null || r.getRoleId() <= 0)
                throw new IllegalArgumentException("Invalid roleId in request");
            if (r.getPermissionIds() == null || r.getPermissionIds().isEmpty())
                throw new IllegalArgumentException("permissionIds must not be empty");

            var perms = r.getPermissionIds().stream().filter(Objects::nonNull).filter(id -> id > 0)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (perms.isEmpty()) throw new IllegalArgumentException("permissionIds must not be empty");
            wanted.computeIfAbsent(r.getRoleId(), k -> new LinkedHashSet<>()).addAll(perms);
        }

        var roleIds = new ArrayList<>(wanted.keySet());
        var roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            var found = roles.stream().map(Role::getId).collect(Collectors.toSet());
            var missing = roleIds.stream().filter(id -> !found.contains(id)).toList();
            throw new ResourceNotFoundException("Some roles not found: " + missing);
        }

        var allPermissionIds = wanted.values().stream().flatMap(Set::stream).distinct().toList();
        var existingPermIds = permissionService.getExistingPermissionIds(allPermissionIds);
        if (existingPermIds.size() != allPermissionIds.size()) {
            var miss = new HashSet<>(allPermissionIds);
            miss.removeAll(existingPermIds);
            throw new ResourceNotFoundException("Some permissions not found: " + miss);
        }

        int assigned = 0;
        Map<Long, Role> byId = roles.stream().collect(Collectors.toMap(Role::getId, r -> r));
        for (var e : wanted.entrySet()) {
            Role role = byId.get(e.getKey());
            var currentIds = role.getPermissions().stream().map(Permission::getId).collect(Collectors.toSet());
            for (Long pid : e.getValue()) {
                if (!currentIds.contains(pid)) {
                    role.getPermissions().add(em.getReference(Permission.class, pid));
                    assigned++;
                }
            }
        }
        roleRepository.saveAll(roles);

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        int pairCount = wanted.values().stream().mapToInt(Set::size).sum();
        log.info("roles.permissions.assign success actor={} roles={} pairs_requested={} assigned={}",
                logs.mask(actor), wanted.size(), pairCount, assigned);

        return "Permissions assigned successfully. Total assignments: " + assigned;
    }

    @Transactional
    public String deassignPermissionsFromRoles(List<AssignPermissionsToRolesRequest> items) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Invalid or empty input");

        Map<Long, Set<Long>> wanted = new LinkedHashMap<>();
        for (var it : items) {
            if (it == null || it.getRoleId() == null || it.getRoleId() <= 0)
                throw new IllegalArgumentException("Invalid roleId in request");
            if (it.getPermissionIds() == null || it.getPermissionIds().isEmpty())
                throw new IllegalArgumentException("permissionIds must not be empty");

            var perms = it.getPermissionIds().stream().filter(Objects::nonNull).filter(id -> id > 0)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (perms.isEmpty()) throw new IllegalArgumentException("permissionIds must not be empty");
            wanted.computeIfAbsent(it.getRoleId(), k -> new LinkedHashSet<>()).addAll(perms);
        }

        var permissionIds = wanted.values().stream().flatMap(Set::stream).distinct().toList();
        var existingPermIds = permissionService.getExistingPermissionIds(permissionIds);
        if (existingPermIds.size() != permissionIds.size()) {
            var miss = new HashSet<>(permissionIds);
            miss.removeAll(existingPermIds);
            throw new ResourceNotFoundException("Some permissions not found: " + miss);
        }

        var roles = roleRepository.findAllById(new ArrayList<>(wanted.keySet()));
        if (roles.size() != wanted.keySet().size()) {
            var found = roles.stream().map(Role::getId).collect(Collectors.toSet());
            var missing = wanted.keySet().stream().filter(id -> !found.contains(id)).toList();
            throw new ResourceNotFoundException("Some roles not found: " + missing);
        }

        int removed = 0;
        Map<Long, Role> byId = roles.stream().collect(Collectors.toMap(Role::getId, r -> r));
        for (var e : wanted.entrySet()) {
            Role role = byId.get(e.getKey());
            Set<Long> toRemoveIds = e.getValue();
            var toRemove = role.getPermissions().stream()
                    .filter(p -> toRemoveIds.contains(p.getId()))
                    .toList();
            removed += toRemove.size();
            role.getPermissions().removeAll(toRemove);
        }
        roleRepository.saveAll(roles);

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        int pairCount = wanted.values().stream().mapToInt(Set::size).sum();
        log.info("roles.permissions.deassign success actor={} roles={} pairs_requested={} removed={}",
                logs.mask(actor), wanted.size(), pairCount, removed);

        return removed > 0 ? "Permissions removed successfully" : "No permissions were removed";
    }

    @Transactional
    public String assignRolesToGroups(List<AssignRolesToGroupsRequest> items) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Invalid or empty input");

        Map<Long, Set<Long>> wanted = new LinkedHashMap<>();
        for (var it : items) {
            if (it == null || it.getGroupId() == null || it.getGroupId() <= 0)
                throw new IllegalArgumentException("Invalid groupId in request");
            if (it.getRoleIds() == null || it.getRoleIds().isEmpty())
                throw new IllegalArgumentException("roleIds must not be empty");
            var norm = it.getRoleIds().stream().filter(Objects::nonNull).filter(id -> id > 0)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (norm.isEmpty()) throw new IllegalArgumentException("roleIds must not be empty");
            wanted.computeIfAbsent(it.getGroupId(), k -> new LinkedHashSet<>()).addAll(norm);
        }

        var roleIds = wanted.values().stream().flatMap(Set::stream).distinct().toList();
        var roles = getByIdsOrThrow(roleIds);

        int inserted = 0;
        for (var e : wanted.entrySet()) {
            Long groupId = e.getKey();
            for (Role r : roles) {
                if (e.getValue().contains(r.getId())) {
                    boolean already = r.getGroups().stream().anyMatch(g -> Objects.equals(g.getId(), groupId));
                    if (!already) {
                        r.getGroups().add(em.getReference(Group.class, groupId));
                        inserted++;
                    }
                }
            }
        }
        roleRepository.saveAll(roles);

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        int pairCount = wanted.values().stream().mapToInt(Set::size).sum();
        log.info("roles.groups.assign success actor={} groups={} pairs_requested={} inserted={}",
                logs.mask(actor), wanted.size(), pairCount, inserted);

        return "Roles assigned to groups successfully. Inserted: " + inserted;
    }

    @Transactional
    public String deassignRolesFromGroups(List<AssignRolesToGroupsRequest> items) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Invalid or empty input");

        Map<Long, Set<Long>> wanted = new LinkedHashMap<>();
        for (var it : items) {
            if (it == null || it.getGroupId() == null || it.getGroupId() <= 0)
                throw new IllegalArgumentException("Invalid groupId in request");
            if (it.getRoleIds() == null || it.getRoleIds().isEmpty())
                throw new IllegalArgumentException("roleIds must not be empty");
            var norm = it.getRoleIds().stream().filter(Objects::nonNull).filter(id -> id > 0)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (norm.isEmpty()) throw new IllegalArgumentException("roleIds must not be empty");
            wanted.computeIfAbsent(it.getGroupId(), k -> new LinkedHashSet<>()).addAll(norm);
        }

        var roles = getByIdsOrThrow(wanted.values().stream().flatMap(Set::stream).distinct().toList());

        int removed = 0;
        for (var e : wanted.entrySet()) {
            Long groupId = e.getKey();
            for (Role r : roles) {
                if (e.getValue().contains(r.getId())) {
                    var toRemove = r.getGroups().stream()
                            .filter(g -> Objects.equals(g.getId(), groupId))
                            .toList();
                    removed += toRemove.size();
                    r.getGroups().removeAll(toRemove);
                }
            }
        }
        roleRepository.saveAll(roles);

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        int pairCount = wanted.values().stream().mapToInt(Set::size).sum();
        log.info("roles.groups.deassign success actor={} groups={} pairs_requested={} removed={}",
                logs.mask(actor), wanted.size(), pairCount, removed);

        return (removed > 0) ? "Roles deassigned from groups successfully" : "No roles were deassigned from groups";
    }

    @Transactional
    public String deleteRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) throw new IllegalArgumentException("No role IDs provided");
        var ids = roleIds.stream().filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();
        if (ids.isEmpty()) throw new IllegalArgumentException("No valid role IDs provided");

        var roles = roleRepository.findAllById(ids);
        if (roles.size() != ids.size()) {
            var found = roles.stream().map(Role::getId).collect(Collectors.toSet());
            var missing = ids.stream().filter(id -> !found.contains(id)).toList();
            throw new ResourceNotFoundException("One or more role IDs do not exist: " + missing);
        }

        try {
            for (Role r : roles) {
                for (User u : new ArrayList<>(r.getUsers())) {
                    u.getRoles().remove(r);
                }
                r.getGroups().clear();
                r.getPermissions().clear();
            }
            roleRepository.deleteAllInBatch(roles);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Cannot delete roles due to existing references: " +
                    (ex.getMostSpecificCause() == null ? ex.getMessage() : ex.getMostSpecificCause().getMessage()));
        }

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor = (auth == null) ? "unknown" : auth.getName();
        log.info("roles.delete success actor={} deleted={}", logs.mask(actor), ids.size());

        return "Roles deleted successfully";
    }

    @Transactional
    public Role getOrCreateRole(String roleName) {
        return roleRepository.findByName(roleName).orElseGet(() -> {
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
}
