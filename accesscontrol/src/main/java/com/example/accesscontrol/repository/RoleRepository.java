package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.Role;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    @Query("SELECT r.name FROM Role r WHERE r.name IN :names")
    List<String> findExistingNames(@Param("names") List<String> names);

    Page<Role> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // N+1‑safe: role with permissions
    @EntityGraph(attributePaths = {"rolePermissions", "rolePermissions.permission"})
    Optional<Role> findWithPermissionsById(Long id);

    // Locking helpers
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select r from Role r where r.id = :id")
    Optional<Role> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select r from Role r where r.id = :id")
    Optional<Role> findAndBumpVersion(@Param("id") Long id);
}
