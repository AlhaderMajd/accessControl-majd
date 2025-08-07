package com.example.accesscontrol.service;

import com.example.accesscontrol.entity.RolePermission;
import com.example.accesscontrol.entity.RolePermissionId;
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

    public void saveAll(List<RolePermission> rolePermissions) {
        rolePermissionRepository.saveAll(rolePermissions);
    }

    @Transactional
    public int assignPermissionsToRoles(List<Long> roleIds, List<Long> permissionIds) {
        Set<RolePermissionId> idsToInsert = new HashSet<>();
        for (Long roleId : roleIds) {
            for (Long permissionId : permissionIds) {
                idsToInsert.add(new RolePermissionId(roleId, permissionId));
            }
        }
        List<RolePermissionId> existing = rolePermissionRepository.findAllById(idsToInsert)
                .stream()
                .map(rp -> new RolePermissionId(rp.getRoleId(), rp.getPermissionId()))
                .toList();
        idsToInsert.removeAll(existing);
        if (idsToInsert.isEmpty()) {
            return 0;
        }
        List<RolePermission> toSave = idsToInsert.stream()
                .map(id -> new RolePermission(id.getRoleId(), id.getPermissionId()))
                .toList();
        rolePermissionRepository.saveAll(toSave);
        return toSave.size();
    }



    @Transactional
    public int deassignPermissionsFromRoles(List<Long> roleIds, List<Long> permissionIds) {
        Set<RolePermissionId> idsToDelete = new HashSet<>();
        for (Long roleId : roleIds) {
            for (Long permissionId : permissionIds) {
                idsToDelete.add(new RolePermissionId(roleId, permissionId));
            }
        }
        rolePermissionRepository.deleteAllByRoleIdInAndPermissionIdIn(
                idsToDelete.stream().map(RolePermissionId::getRoleId).collect(Collectors.toSet()),
                idsToDelete.stream().map(RolePermissionId::getPermissionId).collect(Collectors.toSet())
        );
        return idsToDelete.size();

    }

    @Transactional
    public void deleteByRoleIds(List<Long> roleIds) {
        rolePermissionRepository.deleteAllByRoleIdIn(roleIds);
    }

}
