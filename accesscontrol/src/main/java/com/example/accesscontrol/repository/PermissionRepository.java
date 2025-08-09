package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<Permission> findByNameInIgnoreCase(Collection<String> names);
    Page<Permission> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("""
           SELECT p
             FROM Permission p
             JOIN RolePermission rp ON rp.id.permissionId = p.id
            WHERE rp.id.roleId = :roleId
           """)
    List<Permission> findByRoleId(@Param("roleId") Long roleId);
}
