package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.Permission;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    List<Permission> findByNameInIgnoreCase(Collection<String> names);

    Page<Permission> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // N+1‑safe: permissions for a role via join path
    @Query("""
               SELECT p
               FROM RolePermission rp
               JOIN rp.permission p
               WHERE rp.role.id = :roleId
            """)
    List<Permission> findByRoleId(@Param("roleId") Long roleId);

    // Locking helpers
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select p from Permission p where p.id = :id")
    Optional<Permission> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select p from Permission p where p.id = :id")
    Optional<Permission> findAndBumpVersion(@Param("id") Long id);
}
