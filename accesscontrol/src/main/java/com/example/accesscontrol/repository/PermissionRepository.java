package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    @Query("SELECT p FROM Permission p JOIN RolePermission rp ON p.id = rp.permissionId WHERE rp.roleId = :roleId")
    List<Permission> findByRoleId(@Param("roleId") Long roleId);
}
