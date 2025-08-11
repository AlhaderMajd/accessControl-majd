package com.example.accesscontrol.service;

import com.example.accesscontrol.entity.Permission;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.RolePermission;
import com.example.accesscontrol.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;

    @Transactional
    public void saveAll(List<RolePermission> rolePermissions) {
        if (rolePermissions == null || rolePermissions.isEmpty()) return;
        rolePermissionRepository.saveAll(rolePermissions);
    }

    @Transactional
    public int assignPermissionsToRoles(List<Long> roleIds, List<Long> permissionIds) {
        if (roleIds == null || roleIds.isEmpty() || permissionIds == null || permissionIds.isEmpty()) return 0;

        var existing = rolePermissionRepository.findByRole_IdInAndPermission_IdIn(roleIds, permissionIds);
        Set<String> existingKeys = existing.stream()
                .map(rp -> rp.getRole().getId() + "_" + rp.getPermission().getId())
                .collect(Collectors.toSet());

        List<RolePermission> toInsert = new ArrayList<>();
        for (Long rId : roleIds) {
            for (Long pId : permissionIds) {
                String key = rId + "_" + pId;
                if (!existingKeys.contains(key)) {
                    RolePermission rp = new RolePermission();
                    rp.setRole(Role.builder().id(rId).build());
                    rp.setPermission(Permission.builder().id(pId).build());
                    toInsert.add(rp);
                }
            }
        }
        if (!toInsert.isEmpty()) rolePermissionRepository.saveAll(toInsert);
        return toInsert.size();
    }

    @Transactional
    public int deassignPermissionsFromRoles(List<Long> roleIds, List<Long> permissionIds) {
        if (roleIds == null || roleIds.isEmpty() || permissionIds == null || permissionIds.isEmpty()) return 0;
        return rolePermissionRepository.deleteByRole_IdInAndPermission_IdIn(roleIds, permissionIds);
    }

    @Transactional
    public void deleteByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return;
        rolePermissionRepository.deleteByRole_IdIn(roleIds);
    }

    @Transactional
    public void deleteByPermissionIds(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) return;
        rolePermissionRepository.deleteByPermission_IdIn(permissionIds);
    }
}
