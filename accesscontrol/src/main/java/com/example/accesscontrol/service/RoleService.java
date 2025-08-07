package com.example.accesscontrol.service;

import org.apache.commons.lang3.tuple.Pair; // ✅ Correct one
import com.example.accesscontrol.dto.*;
import com.example.accesscontrol.entity.*;
import com.example.accesscontrol.exception.DuplicateResourceException;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionService permissionService;
    private final RolePermissionService rolePermissionService;
    private final GroupRoleService groupRoleService;
    private final GroupService groupService;

    public Role getOrCreateRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(roleName);
                    return roleRepository.save(newRole);
                });
    }

    public Role getByIdOrThrow(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    public List<Role> getByIdsOrThrow(List<Long> ids) {
        List<Role> roles = roleRepository.findAllById(ids);
        if (roles.size() != ids.size()) {
            throw new ResourceNotFoundException("Some roles not found");
        }
        return roles;
    }

    public CreateRoleResponse createRoles(List<CreateRoleRequest> requests) {
        if (requests == null || requests.isEmpty() || requests.stream().anyMatch(r -> r.getName() == null || r.getName().isBlank())) {
            throw new IllegalArgumentException("Invalid role data");
        }

        List<String> names = requests.stream().map(CreateRoleRequest::getName).toList();
        List<String> existingNames = roleRepository.findExistingNames(names);

        if (!existingNames.isEmpty()) {
            throw new DuplicateResourceException("Some role names already exist: " + existingNames);
        }

        List<Role> roles = requests.stream()
                .map(req -> Role.builder().name(req.getName()).build())
                .collect(Collectors.toList());

        roleRepository.saveAll(roles);

        Map<String, Long> nameToId = roles.stream()
                .collect(Collectors.toMap(Role::getName, Role::getId));

        List<RolePermission> rolePermissions = new ArrayList<>();
        for (CreateRoleRequest req : requests) {
            if (req.getPermissionIds() != null && !req.getPermissionIds().isEmpty()) {
                Long roleId = nameToId.get(req.getName());
                for (Long permissionId : req.getPermissionIds()) {
                    rolePermissions.add(new RolePermission(roleId, permissionId));
                }
            }
        }

        try {
            rolePermissionService.saveAll(rolePermissions);
        } catch (Exception e) {
            throw new RuntimeException("Failed to assign permissions");
        }

        return CreateRoleResponse.builder()
                .message("Roles created successfully")
                .created(names)
                .build();
    }

    public GetRolesResponse getRoles(String search, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Invalid pagination or search parameters");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Role> rolePage = roleRepository.findByNameContainingIgnoreCase(search, pageable);

        List<RoleResponse> roles = rolePage.getContent().stream()
                .map(role -> RoleResponse.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .build())
                .toList();

        return GetRolesResponse.builder()
                .roles(roles)
                .page(page)
                .total(rolePage.getTotalElements())
                .build();
    }

    public RoleWithPermissionsResponse getRoleWithPermissions(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        List<Permission> permissions = permissionService.getPermissionsByRoleId(roleId);

        List<PermissionResponse> permissionResponses = permissions.stream()
                .map(p -> PermissionResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .build())
                .toList();

        return RoleWithPermissionsResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .permissions(permissionResponses)
                .build();
    }

    public UpdateRoleResponse updateRoleName(Long roleId, UpdateRoleRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Invalid role name");
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        role.setName(request.getName());
        try {
            roleRepository.save(role);
        } catch (Exception e) {
            throw new RuntimeException("Could not update role name");
        }

        return UpdateRoleResponse.builder()
                .message("Role name updated successfully")
                .build();
    }
    public String assignPermissionsToRoles(List<AssignPermissionsToRolesItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Invalid or empty input");
        }

        Set<Long> roleIds = items.stream().map(AssignPermissionsToRolesItem::getRoleId).collect(Collectors.toSet());
        Set<Long> permissionIds = items.stream()
                .flatMap(i -> i.getPermissionIds().stream())
                .collect(Collectors.toSet());

        List<Long> existingRoleIds = roleRepository.findAllById(roleIds).stream()
                .map(Role::getId).toList();
        List<Long> existingPermissionIds = permissionService.getExistingPermissionIds(permissionIds.stream().toList());

        if (existingRoleIds.size() != roleIds.size() || existingPermissionIds.size() != permissionIds.size()) {
            throw new NoSuchElementException("One or more role or permission IDs are invalid");
        }

        int inserted = rolePermissionService.assignPermissionsToRoles(existingRoleIds, existingPermissionIds);

        if (inserted == 0) {
            return "No new permissions were assigned (all already exist)";
        }

        return "Permissions assigned successfully";
    }


    public String deassignPermissionsFromRoles(List<AssignPermissionsToRolesItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Invalid or empty input");
        }

        Set<Long> roleIds = items.stream().map(AssignPermissionsToRolesItem::getRoleId).collect(Collectors.toSet());
        Set<Long> permissionIds = items.stream()
                .flatMap(i -> i.getPermissionIds().stream())
                .collect(Collectors.toSet());

        List<Long> existingRoleIds = roleRepository.findAllById(roleIds)
                .stream()
                .map(Role::getId)
                .toList();

        List<Long> existingPermissionIds = permissionService.getExistingPermissionIds(permissionIds.stream().toList());

        if (existingRoleIds.size() != roleIds.size() || existingPermissionIds.size() != permissionIds.size()) {
            throw new NoSuchElementException("One or more role or permission IDs are invalid");
        }

        int deletedCount = rolePermissionService.deassignPermissionsFromRoles(existingRoleIds, existingPermissionIds);
        if (deletedCount == 0) {
            throw new IllegalStateException("Failed to remove permissions");
        }

        int deleted = rolePermissionService.deassignPermissionsFromRoles(existingRoleIds, existingPermissionIds);

        if ( deleted > 0) {
            return "No new permissions were deassigned";
        }
        return "Permissions removed successfully";
    }

    @Transactional
    public String assignRolesToGroups(List<AssignRolesToGroupsItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Invalid or empty input");
        }

        Set<Long> groupIds = items.stream().map(AssignRolesToGroupsItem::getGroupId).collect(Collectors.toSet());
        Set<Long> roleIds = items.stream().flatMap(i -> i.getRoleIds().stream()).collect(Collectors.toSet());

        // Validate input
        List<Long> validGroupIds = groupService.getByIdsOrThrow(groupIds.stream().toList())
                .stream().map(Group::getId).toList();
        List<Long> validRoleIds = getByIdsOrThrow(roleIds.stream().toList())
                .stream().map(Role::getId).toList();

        // Flatten input into group-role pairs
        Set<Pair<Long, Long>> requestedPairs = new HashSet<>();
        for (AssignRolesToGroupsItem item : items) {
            Long groupId = item.getGroupId();
            for (Long roleId : item.getRoleIds()) {
                requestedPairs.add(Pair.of(groupId, roleId));
            }
        }

        // Use GroupRoleService instead of direct repository access
        Set<Pair<Long, Long>> existingPairs = groupRoleService.getAllGroupRolePairs();

        Set<Pair<Long, Long>> toInsert = requestedPairs.stream()
                .filter(pair -> !existingPairs.contains(pair))
                .collect(Collectors.toSet());

        // Create entities to insert
        List<GroupRole> newEntities = toInsert.stream().map(pair -> {
            GroupRole gr = new GroupRole();
            gr.setGroupId(pair.getLeft());
            gr.setRoleId(pair.getRight());
            return gr;
        }).toList();

        groupRoleService.saveAll(newEntities); // ✅ delegate save

        return "Roles assigned to groups successfully. Inserted: " + newEntities.size();
    }



    public String deassignRolesFromGroups(List<AssignRolesToGroupsItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Invalid or empty input");
        }

        Set<Long> groupIds = items.stream().map(AssignRolesToGroupsItem::getGroupId).collect(Collectors.toSet());
        Set<Long> roleIds = items.stream().flatMap(i -> i.getRoleIds().stream()).collect(Collectors.toSet());

        groupService.getByIdsOrThrow(new ArrayList<>(groupIds));
        List<Long> validRoleIds = getByIdsOrThrow(new ArrayList<>(roleIds)).stream().map(Role::getId).toList();

        groupRoleService.deassignRolesFromGroups(new ArrayList<>(groupIds), validRoleIds);
        return "Roles deassigned from groups successfully";
    }


}
