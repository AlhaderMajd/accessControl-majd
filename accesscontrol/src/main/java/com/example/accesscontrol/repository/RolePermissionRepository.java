package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.RolePermission;
import com.example.accesscontrol.entity.RolePermission.Id;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Set;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Id> {

    List<RolePermission> findByIdRoleIdInAndIdPermissionIdIn(List<Long> roleIds, List<Long> permissionIds);

    void deleteAllByIdRoleIdInAndIdPermissionIdIn(Set<Long> roleIds, Set<Long> permissionIds);

    void deleteAllByIdRoleIdIn(List<Long> roleIds);

    void deleteAllByIdPermissionIdIn(List<Long> permissionIds);
}
