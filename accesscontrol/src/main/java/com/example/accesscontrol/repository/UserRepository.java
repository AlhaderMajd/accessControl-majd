package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findAllByEmailIn(List<String> emails);
    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"roles"})
    @Query("""
        select u
        from User u
        where (:q = '' or lower(u.email) like lower(concat('%', :q, '%')))
        """)
    Page<User> searchUsersWithRoles(@Param("q") String q, Pageable pageable);

    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findWithRolesByEmail(String email);

    @EntityGraph(attributePaths = {"roles", "groups"})
    Optional<User> findWithRolesAndGroupsById(Long id);
}
