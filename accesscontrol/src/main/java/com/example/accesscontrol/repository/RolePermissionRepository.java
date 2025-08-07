package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.RolePermission;
import com.example.accesscontrol.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Set;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {
    void deleteAllByRoleIdInAndPermissionIdIn(Set<Long> roleIds, Set<Long> permissionIds);
}
