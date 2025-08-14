package com.example.accesscontrol.service;

import com.example.accesscontrol.entity.Permission;
import com.example.accesscontrol.entity.Role;
import com.example.accesscontrol.entity.RolePermission;
import com.example.accesscontrol.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
    public int assignRolePermissionPairs(Map<Long, Set<Long>> wanted) {
        if (wanted == null || wanted.isEmpty()) return 0;

        var roleIds = new java.util.ArrayList<>(wanted.keySet());
        var allPermissionIds = wanted.values().stream().flatMap(Set::stream).distinct().toList();

        var existingBefore = rolePermissionRepository
                .findByRole_IdInAndPermission_IdIn(roleIds, allPermissionIds);
        var existingKeys = existingBefore.stream()
                .map(rp -> rp.getRole().getId() + "_" + rp.getPermission().getId())
                .collect(java.util.stream.Collectors.toSet());

        java.util.List<RolePermission> toInsert = new java.util.ArrayList<>();
        for (var e : wanted.entrySet()) {
            Long roleId = e.getKey();
            for (Long permId : e.getValue()) {
                String key = roleId + "_" + permId;
                if (!existingKeys.contains(key)) {
                    RolePermission rp = new RolePermission();
                    rp.setRole(Role.builder().id(roleId).build());
                    rp.setPermission(Permission.builder().id(permId).build());
                    toInsert.add(rp);
                }
            }
        }
        if (toInsert.isEmpty()) return 0;

        try {
            rolePermissionRepository.saveAll(toInsert);
            return toInsert.size();
        } catch (DataIntegrityViolationException ex) {
            var after = rolePermissionRepository
                    .findByRole_IdInAndPermission_IdIn(roleIds, allPermissionIds);
            return Math.max(0, after.size() - existingBefore.size());
        }
    }

    @Transactional
    public int deleteRolePermissionPairs(Map<Long, Set<Long>> wanted) {
        if (wanted == null || wanted.isEmpty()) return 0;

        var roleIds = new java.util.ArrayList<>(wanted.keySet());
        var permIds = wanted.values().stream().flatMap(Set::stream).distinct().toList();

        var existing = rolePermissionRepository.findByRole_IdInAndPermission_IdIn(roleIds, permIds);

        Set<String> wantedKeys = new java.util.HashSet<>();
        for (var e : wanted.entrySet()) {
            Long roleId = e.getKey();
            for (Long pid : e.getValue()) {
                wantedKeys.add(roleId + "_" + pid);
            }
        }

        var toDelete = existing.stream()
                .filter(rp -> wantedKeys.contains(rp.getRole().getId() + "_" + rp.getPermission().getId()))
                .toList();

        if (toDelete.isEmpty()) return 0;

        rolePermissionRepository.deleteAllInBatch(toDelete);
        return toDelete.size();
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
