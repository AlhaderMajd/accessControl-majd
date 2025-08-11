package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.group.AssignRolesToGroupsRequest;
import com.example.accesscontrol.dto.permission.PermissionResponse;
import com.example.accesscontrol.dto.role.*;
import com.example.accesscontrol.entity.*;
import com.example.accesscontrol.exception.DuplicateResourceException;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.RoleRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
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
    private final UserRoleService userRoleService;

    @Transactional
    public Role getOrCreateRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
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
                || requests.stream().anyMatch(r -> r.getName() == null || r.getName().isBlank()))
            throw new IllegalArgumentException("Invalid role data");

        List<String> names = requests.stream().map(CreateRoleRequest::getName).toList();
        List<String> existingNames = roleRepository.findExistingNames(names);
        if (!existingNames.isEmpty())
            throw new DuplicateResourceException("Some role names already exist: " + existingNames);

        var roles = requests.stream()
                .map(req -> Role.builder().name(req.getName()).build())
                .toList();
        roleRepository.saveAll(roles);

        Map<String, Long> nameToId = roles.stream().collect(Collectors.toMap(Role::getName, Role::getId));
        List<RolePermission> rp = new ArrayList<>();
        for (CreateRoleRequest req : requests) {
            if (req.getPermissionIds() != null && !req.getPermissionIds().isEmpty()) {
                Long roleId = nameToId.get(req.getName());
                for (Long pid : req.getPermissionIds()) {
                    RolePermission link = new RolePermission();
                    link.setRole(Role.builder().id(roleId).build());
                    link.setPermission(Permission.builder().id(pid).build());
                    rp.add(link);
                }
            }
        }
        if (!rp.isEmpty()) rolePermissionService.saveAll(rp);

        return CreateRoleResponse.builder()
                .message("Roles created successfully")
                .created(names)
                .build();
    }

    @Transactional(readOnly = true)
    public GetRolesResponse getRoles(String search, int page, int size) {
        if (page < 0 || size <= 0) throw new IllegalArgumentException("Invalid pagination or search parameters");
        Pageable pageable = PageRequest.of(page, size);
        Page<Role> rolePage = roleRepository.findByNameContainingIgnoreCase(search == null ? "" : search, pageable);
        var roles = rolePage.getContent().stream()
                .map(r -> RoleResponse.builder().id(r.getId()).name(r.getName()).build())
                .toList();
        return GetRolesResponse.builder()
                .roles(roles)
                .page(page)
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
    public String assignPermissionsToRoles(List<AssignPermissionsToRolesRequest> items) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Invalid or empty input");

        Set<Long> roleIds = items.stream().map(AssignPermissionsToRolesRequest::getRoleId).collect(Collectors.toSet());
        Set<Long> permIds = items.stream().flatMap(i -> i.getPermissionIds().stream()).collect(Collectors.toSet());

        List<Long> validRoleIds = getExistingIds(new ArrayList<>(roleIds));
        List<Long> validPermIds = permissionService.getExistingPermissionIds(new ArrayList<>(permIds));

        int inserted = rolePermissionService.assignPermissionsToRoles(validRoleIds, validPermIds);
        return (inserted == 0) ? "No new permissions were assigned (all already exist)" : "Permissions assigned successfully";
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
}
