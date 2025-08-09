package com.example.accesscontrol.service;

import com.example.accesscontrol.entity.RolePermission;
import com.example.accesscontrol.repository.PermissionRepository;
import com.example.accesscontrol.repository.RolePermissionRepository;
import com.example.accesscontrol.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public void saveAll(List<RolePermission> rolePermissions) {
        rolePermissionRepository.saveAll(rolePermissions);
    }

    @Transactional
    public int assignPermissionsToRoles(List<Long> roleIds, List<Long> permissionIds) {
        if (roleIds == null || roleIds.isEmpty() || permissionIds == null || permissionIds.isEmpty()) return 0;

        List<Long> validRoleIds = roleRepository.findAllById(roleIds).stream().map(r -> r.getId()).toList();
        List<Long> validPermIds = permissionRepository.findAllById(permissionIds).stream().map(p -> p.getId()).toList();
        if (validRoleIds.isEmpty() || validPermIds.isEmpty()) return 0;

        Set<RolePermission.Id> candidates = new HashSet<>();
        for (Long r : validRoleIds) for (Long p : validPermIds) candidates.add(new RolePermission.Id(r, p));

        Set<RolePermission.Id> existing = rolePermissionRepository
                .findByIdRoleIdInAndIdPermissionIdIn(validRoleIds, validPermIds)
                .stream().map(RolePermission::getId).collect(java.util.stream.Collectors.toSet());

        candidates.removeAll(existing);
        if (candidates.isEmpty()) return 0;

        var toInsert = candidates.stream().map(id -> RolePermission.builder().id(id).build()).toList();
        rolePermissionRepository.saveAll(toInsert);
        return toInsert.size();
    }

    @Transactional
    public int deassignPermissionsFromRoles(List<Long> roleIds, List<Long> permissionIds) {
        if (roleIds == null || roleIds.isEmpty() || permissionIds == null || permissionIds.isEmpty()) return 0;
        rolePermissionRepository.deleteAllByIdRoleIdInAndIdPermissionIdIn(new HashSet<>(roleIds), new HashSet<>(permissionIds));
        return roleIds.size() * permissionIds.size();
    }

    @Transactional
    public void deleteByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return;
        rolePermissionRepository.deleteAllByIdRoleIdIn(roleIds);
    }

    @Transactional
    public void deleteByPermissionIds(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) return;
        rolePermissionRepository.deleteAllByIdPermissionIdIn(permissionIds);
    }
}
