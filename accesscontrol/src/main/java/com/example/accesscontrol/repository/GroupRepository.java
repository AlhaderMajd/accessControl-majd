package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.Group;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT g FROM Group g WHERE LOWER(g.name) IN :namesLower")
    List<Group> findByNameInIgnoreCase(@Param("namesLower") Collection<String> namesLower);

    Page<Group> findByNameContainingIgnoreCase(String search, Pageable pageable);

    @EntityGraph(attributePaths = {"users", "roles"})
    Optional<Group> findWithUsersAndRolesById(Long id);
}
