package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    // Exists (used in initializer)
    boolean existsByRole_IdAndPermission_Id(Long roleId, Long permissionId);

    // Used by RolePermissionService.assignPermissionsToRoles
    List<RolePermission> findByRole_IdInAndPermission_IdIn(List<Long> roleIds, List<Long> permissionIds);

    // Used by RolePermissionService.deassignPermissionsFromRoles
    int deleteByRole_IdInAndPermission_IdIn(List<Long> roleIds, List<Long> permissionIds);

    // Used by RolePermissionService.deleteByRoleIds / deleteByPermissionIds
    void deleteByRole_IdIn(List<Long> roleIds);

    void deleteByPermission_IdIn(List<Long> permissionIds);
}
