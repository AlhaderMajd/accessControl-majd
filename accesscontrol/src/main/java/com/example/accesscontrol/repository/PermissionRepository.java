package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.Permission;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    @Query("SELECT p FROM Permission p WHERE LOWER(p.name) IN :namesLower")
    List<Permission> findByNameInIgnoreCase(@Param("namesLower") Collection<String> namesLower);

    Page<Permission> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("""
       SELECT p
       FROM Role r
       JOIN r.permissions p
       WHERE r.id = :roleId
       ORDER BY p.name ASC, p.id ASC
       """)
    List<Permission> findByRoleId(@Param("roleId") Long roleId);
}
