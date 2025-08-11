package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.User;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Basic lookups
    Optional<User> findByEmail(String email);

    List<User> findAllByEmailIn(List<String> emails);

    // N+1 safe aggregate loads (on demand)
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role", "userGroups", "userGroups.group"})
    Optional<User> findWithRolesAndGroupsById(Long id);

    // Use EntityGraph for pageable listing to avoid per-row role N+1 (still lazy for groups)
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    // Locking helpers for write flows
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")) // 5s wait
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select u from User u where u.id = :id")
    Optional<User> findAndBumpVersion(@Param("id") Long id);

    // Bulk helper to prefetch roles for many users (reduces N+1 when you already have user ids)
    @Query("""
                select ur.user.id, r.name
                from UserRole ur
                join ur.role r
                where ur.user.id in :userIds
            """)
    List<Object[]> findRoleNamesByUserIds(@Param("userIds") List<Long> userIds);
}
