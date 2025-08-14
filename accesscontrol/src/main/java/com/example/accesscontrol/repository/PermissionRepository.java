package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.Permission;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findByNameInIgnoreCase(Collection<String> names);
    Page<Permission> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("""
       SELECT p
       FROM RolePermission rp
       JOIN rp.permission p
       WHERE rp.role.id = :roleId
       ORDER BY p.name ASC, p.id ASC
       """)
    List<Permission> findByRoleId(@Param("roleId") Long roleId);
}
