package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.Group;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByNameInIgnoreCase(Collection<String> names);

    Page<Group> findByNameContainingIgnoreCase(String search, Pageable pageable);

    // N+1‑safe: group with roles & users when needed
    @EntityGraph(attributePaths = {
            "groupRoles", "groupRoles.role",
            "userGroups", "userGroups.user"
    })
    Optional<Group> findWithUsersAndRolesById(Long id);

    // Locking helpers
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select g from Group g where g.id = :id")
    Optional<Group> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select g from Group g where g.id = :id")
    Optional<Group> findAndBumpVersion(@Param("id") Long id);
}
