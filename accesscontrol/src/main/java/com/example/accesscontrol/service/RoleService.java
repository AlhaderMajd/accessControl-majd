package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.CreateRoleRequest;
import com.example.accesscontrol.dto.CreateRoleResponse;
import com.example.accesscontrol.dto.GetRolesResponse;
import com.example.accesscontrol.dto.RoleResponse;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.RolePermission;
import com.example.accesscontrol.exception.DuplicateResourceException;
import com.example.accesscontrol.exception.ResourceNotFoundException;
import com.example.accesscontrol.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionService rolePermissionService;


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

        List<Role> roles = new ArrayList<>();
        for (CreateRoleRequest req : requests) {
            roles.add(Role.builder().name(req.getName()).build());
        }

        roleRepository.saveAll(roles);

        Map<String, Long> nameToId = roles.stream()
                .collect(Collectors.toMap(Role::getName, Role::getId));

        List<RolePermission> rolePermissions = new ArrayList<>();
        for (CreateRoleRequest req : requests) {
            if (req.getPermissionIds() != null && !req.getPermissionIds().isEmpty()) {
                Long roleId = nameToId.get(req.getName());
                for (Long permId : req.getPermissionIds()) {
                    rolePermissions.add(new RolePermission(roleId, permId));
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
                .map(role -> {
                    RoleResponse dto = new RoleResponse();
                    dto.setId(role.getId());
                    dto.setName(role.getName());
                    return dto;
                })
                .toList();

        return GetRolesResponse.builder()
                .roles(roles)
                .page(page)
                .total(rolePage.getTotalElements())
                .build();
    }

}
